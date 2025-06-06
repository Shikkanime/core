package fr.shikkanime.modules

import com.google.inject.AbstractModule
import fr.shikkanime.jobs.AbstractJob
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.repositories.AbstractRepository
import fr.shikkanime.services.AbstractService
import fr.shikkanime.services.admin.IAdminService
import fr.shikkanime.services.caches.ICacheService
import fr.shikkanime.socialnetworks.AbstractSocialNetwork
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.Database
import fr.shikkanime.utils.routes.Controller
import org.reflections.Reflections

class DefaultModule : AbstractModule() {
    override fun configure() {
        bind(Database::class.java).asEagerSingleton()

        setOf(
            Constant.reflections.getSubTypesOf(AbstractRepository::class.java),
            Constant.reflections.getSubTypesOf(AbstractService::class.java),
            Constant.reflections.getSubTypesOf(AbstractPlatform::class.java),
            Constant.reflections.getSubTypesOf(AbstractJob::class.java),
            Constant.reflections.getTypesAnnotatedWith(Controller::class.java),
            Constant.reflections.getSubTypesOf(AbstractSocialNetwork::class.java),
            Constant.reflections.getSubTypesOf(ICacheService::class.java),
            Constant.reflections.getSubTypesOf(IAdminService::class.java),
        ).flatten().forEach {
            bind(it).asEagerSingleton()
        }

        Reflections("fr.shikkanime.factories.impl").getSubTypesOf(Any::class.java).forEach {
            bind(it).asEagerSingleton()
        }
    }
}