package fr.shikkanime.jobs.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.jobs.platforms.models.PlatformAnime
import fr.shikkanime.jobs.platforms.models.PlatformEpisode
import fr.shikkanime.jobs.platforms.models.PlatformEpisodeLocalization
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.Locale
import io.mockk.every
import io.mockk.mockk
import java.time.ZonedDateTime

class PlatformEpisodeMockKBuilder(
    configuration: PlatformEpisodeMockKBuilder.() -> Unit = {}
) : TestBuilder<PlatformEpisode> {
    var anime: PlatformAnime? = null
    var id: String? = null
    var image: String? = null
    var releaseDateTime: ZonedDateTime? = null
    var season: Int? = null
    var number: Int? = null
    var episodeType: EpisodeType? = null
    var duration: Long? = null
    var url: String? = null
    var audioLocales: List<Locale>? = null
    var uncensored: Boolean? = null
    var localizations: List<PlatformEpisodeLocalization>? = null

    init {
        configuration(this)
    }

    override fun build(): PlatformEpisode {
        val mockK = mockk<PlatformEpisode>(relaxed = true)
        anime?.let { every { mockK.anime } returns it }
        id?.let { every { mockK.id } returns it }
        image?.let { every { mockK.image } returns it }
        releaseDateTime?.let { every { mockK.releaseDateTime } returns it }
        season?.let { every { mockK.season } returns it }
        number?.let { every { mockK.number } returns it }
        episodeType?.let { every { mockK.episodeType } returns it }
        duration?.let { every { mockK.duration } returns it }
        url?.let { every { mockK.url } returns it }
        audioLocales?.let { every { mockK.audioLocales } returns it }
        uncensored?.let { every { mockK.uncensored } returns it }
        localizations?.let { every { mockK.localizations } returns it }

        // The projection delegates read the anime member: a mock intercepts them too
        every { mockK.platform } answers { mockK.anime.platform }
        every { mockK.animeNormalizedTitle } answers { mockK.anime.normalizedTitle }

        every { mockK.toReleaseModels() } answers { callOriginal() }
        every { mockK.toModel() } answers { callOriginal() }

        return mockK
    }
}
