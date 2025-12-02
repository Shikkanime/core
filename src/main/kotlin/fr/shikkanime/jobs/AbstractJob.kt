package fr.shikkanime.jobs

fun interface AbstractJob {
    suspend fun run()
}