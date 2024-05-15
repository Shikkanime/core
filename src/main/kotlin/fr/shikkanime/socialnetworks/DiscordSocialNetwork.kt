package fr.shikkanime.socialnetworks

import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.ImageService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.StringUtils
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import java.io.File
import java.net.URI
import java.time.ZonedDateTime
import java.util.logging.Level
import javax.imageio.ImageIO

class DiscordSocialNetwork : AbstractSocialNetwork() {
    data class Channel(
        val id: Long,
        var releaseType: String,
        val animes: MutableList<String>
    )

    private val logger = LoggerFactory.getLogger(DiscordSocialNetwork::class.java)
    private var isInitialized = false
    private var jda: JDA? = null

    private fun getTextChannels(): MutableList<TextChannel>? =
        jda?.getTextChannelsByName("bot\uD83E\uDD16", true)

    override fun utmSource() = "discord"

    override fun login() {
        if (isInitialized) return

        try {
            val token = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.DISCORD_TOKEN))
            if (token.isBlank()) throw Exception("Token is empty")
            val builder = JDABuilder.createDefault(token)
            builder.setActivity(Activity.playing(Constant.baseUrl))
            this.jda = builder.build().awaitReady()
            addCommands()
            isInitialized = true
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while initializing DiscordSocialNetwork", e)
        }
    }

    private fun addCommands() {
        val commands = this.jda!!.updateCommands()

        commands.addCommands(
            Commands.slash("add-release-channel", "Add a channel to receive release notifications")
                .addOption(OptionType.CHANNEL, "channel", "The channel to receive notifications", true)
                .addOption(OptionType.STRING, "anime", "The anime to receive notifications for, by default: all", false)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .setGuildOnly(true)
        )

        commands.submit()

        this.jda!!.addEventListener(object : ListenerAdapter() {
            override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
                if (!event.name.equals("add-release-channel", true)) return

                val channel = event.getOption("channel")?.asChannel ?: run {
                    event.reply("Channel is required").setEphemeral(true).queue()
                    return
                }

                // If we can send messages to the channel
                if (!channel.type.isMessage) {
                    event.reply("I can't send messages to this channel").setEphemeral(true).queue()
                    return
                }

                val animeName = event.getOption("anime")?.asString
                val releaseType = if (animeName.isNullOrBlank()) "ALL" else "CUSTOM"

                // Save the channel to the database
                event.reply("Channel added to receive release notifications").queue()

                val file = getFile()
                val channels = getChannels(file)

                // If channel id already exists, update the release type and animes
                val existingChannel = channels.firstOrNull { it.id == channel.idLong }

                if (existingChannel != null) {
                    val oldReleaseType = existingChannel.releaseType
                    existingChannel.releaseType = releaseType

                    if (oldReleaseType != releaseType && oldReleaseType == "CUSTOM") {
                        existingChannel.animes.clear()
                    }

                    if (releaseType == "CUSTOM" && !animeName.isNullOrBlank() && !existingChannel.animes.contains(
                            animeName
                        )
                    ) {
                        existingChannel.animes.add(animeName)
                    }
                } else {
                    channels.add(
                        Channel(
                            channel.idLong,
                            releaseType,
                            animeName?.let { mutableListOf(it) } ?: mutableListOf()))
                }

                file.writeText(ObjectParser.toJson(channels))
            }
        })
    }

    private fun getFile() = File(Constant.configFolder, "discord_channels.json")

    private fun getChannels(file: File): MutableList<Channel> {
        return if (file.exists()) {
            ObjectParser.fromJson(file.readText(), Array<Channel>::class.java).toMutableList()
        } else {
            mutableListOf()
        }
    }

    override fun logout() {
        if (!isInitialized) return

        try {
            jda?.shutdown()
            isInitialized = false
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while shutting down DiscordSocialNetwork", e)
        }
    }

    override fun sendEpisodeRelease(episodeDto: EpisodeVariantDto, mediaImage: ByteArray) {
        login()
        if (!isInitialized) return
        if (episodeDto.mapping.image.isBlank()) return

        val embedMessage = EmbedBuilder()
        val image = ImageIO.read(URI(episodeDto.mapping.image).toURL())
        embedMessage.setColor(ImageService.getDominantColor(image))
        embedMessage.setAuthor(
            episodeDto.platform.name,
            episodeDto.platform.url,
            "${Constant.baseUrl}/assets/img/platforms/${episodeDto.platform.image}"
        )
        embedMessage.setTitle(episodeDto.mapping.anime.shortName, getShikkanimeUrl(episodeDto))
        embedMessage.setThumbnail(episodeDto.mapping.anime.image)
        embedMessage.setDescription(
            "**${episodeDto.mapping.title ?: "Untitled"}**\n${
                StringUtils.toEpisodeString(
                    episodeDto
                )
            }"
        )
        embedMessage.setImage(episodeDto.mapping.image)
        embedMessage.setFooter(Constant.NAME, "${Constant.baseUrl}/assets/img/favicons/favicon-64x64.png")
        embedMessage.setTimestamp(ZonedDateTime.parse(episodeDto.releaseDateTime).toInstant())
        val embed = embedMessage.build()
        val channels = getChannels(getFile())

        channels.forEach { channel ->
            if (channel.releaseType == "ALL" || channel.animes.contains(episodeDto.mapping.anime.shortName)) {
                jda?.getTextChannelById(channel.id)?.sendMessageEmbeds(embed)?.queue()
            }
        }
    }
}