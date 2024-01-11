package fr.shikkanime.modules

import com.google.inject.AbstractModule
import fr.shikkanime.jobs.AbstractJob
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.repositories.AbstractRepository
import fr.shikkanime.services.AbstractService
import fr.shikkanime.socialnetworks.AbstractSocialNetwork
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.Database
import fr.shikkanime.utils.routes.Controller

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

        Constant.reflections.getSubTypesOf(AbstractJob::class.java).forEach {
            bind(it).asEagerSingleton()
        }

        Constant.reflections.getTypesAnnotatedWith(Controller::class.java).forEach {
            bind(it).asEagerSingleton()
        }

        Constant.reflections.getSubTypesOf(AbstractSocialNetwork::class.java).forEach {
            bind(it).asEagerSingleton()
        }
    }
}