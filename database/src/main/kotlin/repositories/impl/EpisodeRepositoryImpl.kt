package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.repositories.EpisodeRepository
import org.koin.core.annotation.Single

@Suppress("unused")
@Single(binds = [EpisodeRepository::class])
class EpisodeRepositoryImpl : EpisodeRepository()
