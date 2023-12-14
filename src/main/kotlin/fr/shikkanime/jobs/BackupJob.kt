package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.dtos.UnsecuredMemberDto
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.services.MemberService
import fr.shikkanime.services.SimulcastService
import fr.shikkanime.utils.FileManager
import fr.shikkanime.utils.ObjectParser
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class BackupJob : AbstractJob() {
    private val utcZone = ZoneId.of("UTC")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")

    @Inject
    private lateinit var simulcastService: SimulcastService

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var episodeService: EpisodeService

    @Inject
    private lateinit var memberService: MemberService

    override fun run() {
        val simulcasts = AbstractConverter.convert(simulcastService.findAll(), SimulcastDto::class.java)
        val animes = AbstractConverter.convert(animeService.findAll(), AnimeDto::class.java)
        val episodes = AbstractConverter.convert(episodeService.findAll(), EpisodeDto::class.java)
        val members = AbstractConverter.convert(memberService.findAll(), UnsecuredMemberDto::class.java)

        val folder = File("backups")
        if (!folder.exists()) folder.mkdirs()

        FileManager.zipFiles(
            File(folder, "backup-${ZonedDateTime.now().withZoneSameInstant(utcZone).format(dateFormatter)}.zip"),
            listOf(
                "simulcasts.shikk" to FileManager.toGzip(ObjectParser.toJson(simulcasts).toByteArray()),
                "animes.shikk" to FileManager.toGzip(ObjectParser.toJson(animes).toByteArray()),
                "episodes.shikk" to FileManager.toGzip(ObjectParser.toJson(episodes).toByteArray()),
                "members.shikk" to FileManager.toGzip(ObjectParser.toJson(members).toByteArray())
            )
        )
    }
}