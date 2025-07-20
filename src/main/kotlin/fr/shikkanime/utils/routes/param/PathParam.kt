package fr.shikkanime.utils.routes.param

import fr.shikkanime.utils.StringUtils

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class PathParam(val name: String = StringUtils.EMPTY_STRING)
