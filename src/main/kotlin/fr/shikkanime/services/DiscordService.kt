package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.StringUtils
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import java.net.URI
import java.time.ZonedDateTime
import java.util.logging.Level
import javax.imageio.ImageIO

class DiscordService {
    @Inject
    private lateinit var configService: ConfigService

    private val logger = LoggerFactory.getLogger(DiscordService::class.java)
    private var isInitialized = false
    private var jda: JDA? = null

    fun init() {
        if (isInitialized) return

        try {
            val builder = JDABuilder.createDefault(configService.getValueAsString("discord_token"))
            builder.setActivity(Activity.playing("https://www.shikkanime.fr/"))
            jda = builder.build()
            jda?.awaitReady()
            isInitialized = true
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while initializing DiscordService", e)
        }
    }

    private fun getTextChannels(): MutableList<TextChannel>? =
        jda?.getTextChannelsByName("bot\uD83E\uDD16", true)

    fun sendEpisodeRelease(episodeDto: EpisodeDto) {
        init()
        if (!isInitialized) return
        if (episodeDto.image.isBlank()) return

        try {
            val embedMessage = EmbedBuilder()
            val image = ImageIO.read(URI(episodeDto.image).toURL())
            embedMessage.setColor(ImageService.getDominantColor(image))
            embedMessage.setAuthor(
                episodeDto.platform.platformName,
                episodeDto.platform.url,
                "https://www.shikkanime.fr/admin/assets/img/platforms/${episodeDto.platform.image}"
            )
            embedMessage.setTitle(episodeDto.anime.shortName, episodeDto.url)
            embedMessage.setDescription("**${episodeDto.title ?: "Untitled"}**\n${StringUtils.toEpisodeString(episodeDto)}")
            embedMessage.setImage(episodeDto.image)
            embedMessage.setFooter("Shikkanime", "https://www.shikkanime.fr/admin/assets/favicons/favicon-64x64.png")
            embedMessage.setTimestamp(ZonedDateTime.parse(episodeDto.releaseDateTime).toInstant())
            val embed = embedMessage.build()

            getTextChannels()?.forEach { it.sendMessageEmbeds(embed).queue() }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while sending message to Discord", e)
        }
    }
}