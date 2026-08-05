package fr.shikkanime.jobs

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Expression(val value: String)
