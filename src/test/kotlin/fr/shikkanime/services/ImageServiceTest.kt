package fr.shikkanime.services

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class ImageServiceTest {
    @BeforeEach
    fun setUp() {
        ImageService.cache.clear()
        ImageService.change.set(true)
        ImageService.saveCache()
        ImageService.cache.clear()
        ImageService.change.set(true)
    }

    @Test
    fun loadCache() {
        ImageService.loadCache()
        assertEquals(0, ImageService.cache.size)
    }

    @Test
    fun saveCache() {
        ImageService.saveCache()
        assertEquals(false, ImageService.change.get())
    }

    @Test
    fun toEpisodeImage() {
        val dto = EpisodeDto(
            uuid = UUID.fromString("0335449b-87af-489e-9513-57cb2c854738"),
            platform = AbstractConverter.convert(Platform.CRUN, PlatformDto::class.java),
            anime = AnimeDto(
                uuid = UUID.fromString("ebf540e4-42d2-4c35-92fc-6069b08d6db3"),
                countryCode = CountryCode.FR,
                name = "Frieren",
                shortName = "Frieren",
                releaseDateTime = "2024-01-05T16:00:00Z",
                image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/f446d7a2a155c6120742978fb528fb82.jpe",
                banner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/bcc213e8825420a85790049366d409fd.jpe",
                description = "L’elfe Frieren a vaincu le roi des démons aux côtés du groupe mené par le jeune héros Himmel. Après dix années d’efforts, ils ont ramené la paix dans le royaume. Parce qu’elle est une elfe, Frieren peut vivre plus de mille ans. Elle part seule en voyage, promettant à ses amis qu'elle reviendra les voir. Cinquante ans plus tard, Frieren est de retour, elle n'a pas changé, mais ces retrouvailles sont aussi les derniers instants passés avec Himmel, devenu un vieillard qui s’éteint paisiblement sous ses yeux. Attristée de n’avoir pas passé plus de temps à connaître les gens qu’elle aime, elle décide de reprendre son voyage et de partir à la rencontre de nouvelles personnes…",
                simulcasts = listOf(
                    SimulcastDto(
                        uuid = UUID.fromString("d1993c0c-4f65-474b-9c04-c577435aa7d5"),
                        season = "WINTER",
                        year = 2024,
                        slug = "winter-2024",
                        label = "Winter 2024",
                    )
                ),
                status = Status.VALID,
                slug = "frieren",
                lastReleaseDateTime = "2024-03-01T20:30:00Z"
            ),
            episodeType = EpisodeType.EPISODE,
            langType = LangType.VOICE,
            hash = "FR-CRUN-922724-VOICE",
            releaseDateTime = "2024-03-01T20:30:00Z",
            season = 1,
            number = 22,
            title = "D'alliés à ennemis",
            url = "https://www.crunchyroll.com/media-922724",
            image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/9c5901edffefae3af8732ebad4a5a51b.jpe",
            duration = 1470,
            description = "Frieren et Fern retournent en ville après la première épreuve. Elles retrouvent Stark, affalé sur son lit alors que la nuit tombe, ce qui ne manque pas de mettre Fern en colère. Commence alors la quête d'un moyen de lui redonner le sourire...",
            uncensored = false,
            lastUpdateDateTime = "2024-03-01T20:30:00Z",
            status = Status.VALID,
        )

        val image = ImageService.toEpisodeImage(dto)
        assertNotNull(image)
    }

    @Test
    fun `add bypass images`() {
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        ImageService.add(
            uuid1,
            ImageService.Type.IMAGE,
            "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/efd4a6f29ce3b03a6f9d14d818b804bf.jpe",
            1920,
            1080,
            true
        )
        ImageService.add(
            uuid2,
            ImageService.Type.IMAGE,
            "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/1e9c79ce0d5cebd2670bddd2a27cd9dd.jpe",
            1920,
            1080,
            true
        )
        Thread.sleep(5000)
        ImageService.add(
            uuid1,
            ImageService.Type.IMAGE,
            "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/efd4a6f29ce3b03a6f9d14d818b804bf.jpe",
            1920,
            1080,
            true
        )
        ImageService.add(
            uuid2,
            ImageService.Type.IMAGE,
            "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/1e9c79ce0d5cebd2670bddd2a27cd9dd.jpe",
            1920,
            1080,
            true
        )
        Thread.sleep(5000)
        ImageService.cache.clear()
        ImageService.change.set(true)
        ImageService.saveCache()
    }

    @Test
    fun `invalid images`() {
        Thread.sleep(5000)
        ImageService.cache.clear()
        ImageService.change.set(true)
        ImageService.saveCache()

        Thread.sleep(5000)
        ImageService.invalidate()

        Thread.sleep(5000)
        ImageService.cache.clear()
        ImageService.change.set(true)
        ImageService.saveCache()
    }
}