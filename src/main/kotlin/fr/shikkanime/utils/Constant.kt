package fr.shikkanime.utils

import com.google.inject.Guice
import com.google.inject.Injector
import fr.shikkanime.modules.DefaultModule
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.socialnetworks.AbstractSocialNetwork
import org.reflections.Reflections
import java.io.File

object Constant {
    val reflections = Reflections("fr.shikkanime")
    val injector: Injector = Guice.createInjector(DefaultModule())
    val abstractPlatforms = reflections.getSubTypesOf(AbstractPlatform::class.java).map { injector.getInstance(it) }
    val seasons = listOf("WINTER", "SPRING", "SUMMER", "AUTUMN")
    val dataFolder: File
        get() {
            val dataFolder = File("data")

            if (!dataFolder.exists()) {
                dataFolder.mkdirs()
            }

            return dataFolder
        }
    var isDev = System.getenv("ENV") == "dev"
    val abstractSocialNetworks =
        reflections.getSubTypesOf(AbstractSocialNetwork::class.java).map { injector.getInstance(it) }

    init {
        abstractPlatforms.forEach {
            it.loadConfiguration()
        }

        abstractSocialNetworks.forEach {
            it.login()
        }
    }
}