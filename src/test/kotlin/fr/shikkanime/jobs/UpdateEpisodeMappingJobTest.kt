package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class UpdateEpisodeMappingJobTest : AbstractTest() {
    @Inject
    private lateinit var updateEpisodeMappingJob: UpdateEpisodeMappingJob

    @Test
    fun `run old Crunchyroll episodes`() {
        val zonedDateTime = ZonedDateTime.now().minusMonths(2)
        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = "Rent-a-Girlfriend",
                slug = "rent-a-girlfriend",
                image = "test.jpg",
                banner = "test.jpg",
            )
        )
        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = 1,
                episodeType = EpisodeType.EPISODE,
                number = 1,
                image = "test.jpg"
            )
        )
        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.CRUN,
                audioLocale = "fr-FR",
                identifier = "FR-CRUN-GZ7UV8KWZ-FR-FR",
                url = "https://www.crunchyroll.com/fr/watch/GZ7UV8KWZ/rent-a-girlfriend"
            )
        )
        updateEpisodeMappingJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(1, mappings.size)
        val variants = episodeVariantService.findAll()
        assertEquals(2, variants.size)

        assertEquals(
            "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/97ab10f90157c828a591cd4ec66e851c.jpg",
            mappings.first().image
        )
        assertEquals("Petite amie à louer", mappings.first().title)
        assertEquals(
            "Kazuya Kinoshita est un jeune étudiant qui vient de se faire plaquer par sa copine. Alors qu'il déprime complètement, il décide de télécharger une application permettant de louer une petite amie pour une journée.",
            mappings.first().description
        )
    }

    @Test
    fun `run old ADN episodes`() {
        val zonedDateTime = ZonedDateTime.now().minusMonths(2)
        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = "The Eminence in Shadow",
                slug = "the-eminence-in-shadow",
                image = "test.jpg",
                banner = "test.jpg"
            )
        )
        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = 1,
                episodeType = EpisodeType.EPISODE,
                number = 1,
                image = "test.jpg",
            )
        )
        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.ANIM,
                audioLocale = "ja-JP",
                identifier = "FR-ANIM-20568-JA-JP",
                url = "https://animationdigitalnetwork.fr/video/the-eminence-in-shadow/20568-episode-1-un-camarade-detestable"
            )
        )
        updateEpisodeMappingJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(1, mappings.size)
        val variants = episodeVariantService.findAll()
        assertEquals(2, variants.size)
    }

    @Test
    fun `run old ADN episodes Pon No Michi`() {
        val zonedDateTime = ZonedDateTime.now().minusMonths(2)
        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = "Pon no Michi",
                slug = "pon-no-michi",
                image = "https://image.animationdigitalnetwork.fr/license/ponnomichi/tv/web/affiche_350x500.jpg",
                banner = "https://image.animationdigitalnetwork.fr/license/ponnomichi/tv/web/license_640x360.jpg"
            )
        )
        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = 1,
                episodeType = EpisodeType.EPISODE,
                number = 5,
                image = "https://image.animationdigitalnetwork.fr/license/ponnomichi/tv/web/eps5_640x360.jpg",
                title = "Une arrivée inattendue",
                description = "Nashiko et ses amies reçoivent la visite de la personne contre qui elles jouaient en ligne : Haneru Emi. Cette dernière est déterminée à jouer à nouveau contre Riche pour la battre, mais avant ça, elle fait la connaissance des quatre filles. Et quoi de mieux pour cela qu’un barbecue sous le ciel bleu ?"
            )
        )
        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.ANIM,
                audioLocale = "ja-JP",
                identifier = "FR-ANIM-24026-JA-JP",
                url = "https://animationdigitalnetwork.fr/video/pon-no-michi/24026-episode-5-une-arrivee-inattendue"
            )
        )

        val now = ZonedDateTime.now()
        updateEpisodeMappingJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(1, mappings.size)
        assertTrue(mappings.first().lastUpdateDateTime.isAfter(now))
        val variants = episodeVariantService.findAll()
        assertEquals(1, variants.size)
    }
}