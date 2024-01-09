package fr.shikkanime.socialnetworks

import com.google.inject.Inject
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.services.ConfigService

abstract class AbstractSocialNetwork {
    @Inject
    protected lateinit var configService: ConfigService

    abstract fun login()
    abstract fun logout()

    abstract fun sendMessage(message: String)
    abstract fun sendEpisodeRelease(episodeDto: EpisodeDto)
}