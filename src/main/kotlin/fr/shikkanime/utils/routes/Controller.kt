package fr.shikkanime.utils.routes

import fr.shikkanime.utils.StringUtils

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Controller(val value: String = StringUtils.EMPTY_STRING)
