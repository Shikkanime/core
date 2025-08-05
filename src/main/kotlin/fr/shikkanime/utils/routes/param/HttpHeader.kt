package fr.shikkanime.utils.routes.param

import fr.shikkanime.utils.StringUtils

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class HttpHeader(
    val name: String,
    val defaultValue: String = StringUtils.EMPTY_STRING,
)
