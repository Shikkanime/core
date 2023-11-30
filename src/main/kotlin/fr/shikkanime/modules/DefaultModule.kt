package fr.shikkanime.modules

import com.google.inject.AbstractModule
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.repositories.AbstractRepository
import fr.shikkanime.services.AbstractService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.Database

class DefaultModule : AbstractModule() {
    override fun configure() {
        bind(Database::class.java).asEagerSingleton()

        Constant.reflections.getSubTypesOf(AbstractRepository::class.java).forEach {
            bind(it).asEagerSingleton()
        }

        Constant.reflections.getSubTypesOf(AbstractService::class.java).forEach {
            bind(it).asEagerSingleton()
        }

        Constant.reflections.getSubTypesOf(AbstractPlatform::class.java).forEach {
            bind(it).asEagerSingleton()
        }
    }
}