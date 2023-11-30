package fr.shikkanime.utils

import com.google.inject.Guice
import fr.shikkanime.modules.DefaultModule
import org.reflections.Reflections

object Constant {
    val reflections = Reflections("fr.shikkanime")
    val guice = Guice.createInjector(DefaultModule())
}