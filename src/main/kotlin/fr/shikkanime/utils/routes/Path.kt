package fr.shikkanime.utils.routes

import fr.shikkanime.utils.StringUtils

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Path(val value: String = StringUtils.EMPTY_STRING)
