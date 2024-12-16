package fr.shikkanime.socialnetworks

import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.socialnetworks.listeners.SlashCommandInteractionListener
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.StringUtils
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.utils.FileUpload
import java.io.File
import java.time.ZonedDateTime
import java.util.logging.Level

class DiscordSocialNetwork : AbstractSocialNetwork() {
    data class Channel(
        val id: Long,
        var releaseType: String,
        val animes: MutableList<String>
    )

    private val logger = LoggerFactory.getLogger(DiscordSocialNetwork::class.java)
    private var isInitialized = false
    private var jda: JDA? = null

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
        this.jda!!.addEventListener(SlashCommandInteractionListener())
    }

    private fun getFile() = File(Constant.configFolder, "discord_channels.json")

    private fun getChannels(file: File): MutableList<Channel> {
        return (if (file.exists()) {
            ObjectParser.fromJson(file.readText(), Array<Channel>::class.java)
        } else {
            arrayOf()
        }).toMutableList()
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

    override fun sendEpisodeRelease(episodeDto: EpisodeVariantDto, mediaImage: ByteArray?) {
        login()
        if (!isInitialized) return
        if (episodeDto.mapping.image.isBlank()) return

        val embedMessage = EmbedBuilder()
        embedMessage.setTitle(episodeDto.mapping.anime.shortName, getShikkanimeUrl(episodeDto))
        embedMessage.setThumbnail(episodeDto.mapping.anime.image)
        embedMessage.setDescription(
            "**${episodeDto.mapping.title ?: "Untitled"}**\n${
                StringUtils.toEpisodeVariantString(
                    episodeDto
                )
            }"
        )
        embedMessage.setImage("attachment://media-image.jpg")
        embedMessage.setFooter(Constant.NAME, "${Constant.baseUrl}/assets/img/favicons/favicon-64x64.png")
        embedMessage.setTimestamp(ZonedDateTime.parse(episodeDto.releaseDateTime).toInstant())
        val embed = embedMessage.build()
        val channels = getChannels(getFile())
        val fileUpload = FileUpload.fromData(mediaImage ?: byteArrayOf(), "media-image.jpg")

        channels.forEach { channel ->
            if (channel.releaseType == "ALL" || channel.animes.contains(episodeDto.mapping.anime.shortName)) {
                jda?.getTextChannelById(channel.id)
                    ?.sendFiles(fileUpload)
                    ?.setEmbeds(embed)
                    ?.queue()
            }
        }
    }
}