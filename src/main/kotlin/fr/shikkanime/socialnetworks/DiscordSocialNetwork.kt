package fr.shikkanime.socialnetworks

import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.miscellaneous.GroupedEpisode
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
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.utils.FileUpload
import java.io.File
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

    override val priority: Int
        get() = 1

    override fun login() {
        if (isInitialized) return

        if (!configCacheService.getValueAsBoolean(ConfigPropertyKey.DISCORD_ENABLED)) {
            logger.info("Discord is disabled in configuration")
            return
        }

        try {
            val token = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.DISCORD_TOKEN))
            require(token.isNotBlank()) { "Discord token is empty" }
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
                .setContexts(InteractionContextType.GUILD),
            Commands.slash("remove-release-channel", "Remove a channel from release notifications")
                .addOption(OptionType.CHANNEL, "channel", "The channel to remove from notifications", true)
                .addOption(OptionType.STRING, "anime", "The anime to remove from notifications, by default: all", false)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .setContexts(InteractionContextType.GUILD),
            Commands.slash("view-release-channel", "View the notification type of a channel and its anime list")
                .addOption(OptionType.CHANNEL, "channel", "The channel to view notifications for", true)
                .setContexts(InteractionContextType.GUILD)
        )

        commands.submit()
        this.jda!!.addEventListener(SlashCommandInteractionListener(this))
    }

    fun getFile() = File(Constant.configFolder, "discord_channels.json")

    fun getChannels(file: File) = if (file.exists()) {
        ObjectParser.fromJson(file.readText(), Array<Channel>::class.java)
    } else {
        arrayOf()
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

    override suspend fun sendEpisodeRelease(groupedEpisodes: List<GroupedEpisode>, mediaImage: ByteArray?) {
        login()
        if (!isInitialized) return

        val embedMessage = EmbedBuilder()

        if (groupedEpisodes.size == 1) {
            val groupedEpisode = groupedEpisodes.first()
            embedMessage.setTitle(StringUtils.getShortName(groupedEpisode.anime.name!!), getInternalUrl(groupedEpisode))
        } else {
            embedMessage.setTitle("Nouveaux épisodes", Constant.baseUrl)
        }

        embedMessage.setImage("attachment://media-image.jpg")
        embedMessage.setFooter(Constant.NAME, "${Constant.baseUrl}/assets/img/favicons/favicon-64x64.png")
        embedMessage.setTimestamp(groupedEpisodes.minOf { it.releaseDateTime })
        val embed = embedMessage.build()
        val channels = getChannels(getFile())
        val fileUpload = FileUpload.fromData(mediaImage ?: byteArrayOf(), "media-image.jpg")

        channels.forEach { channel ->
            val anyAnimeMatch = groupedEpisodes.any { StringUtils.getShortName(it.anime.name!!) in channel.animes }

            if (channel.releaseType == "ALL" || anyAnimeMatch) {
                jda?.getTextChannelById(channel.id)
                    ?.sendFiles(fileUpload)
                    ?.setEmbeds(embed)
                    ?.queue()
            }
        }
    }
}
