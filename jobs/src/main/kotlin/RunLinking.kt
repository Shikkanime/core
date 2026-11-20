package fr.shikkanime.jobs

import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.jobs.metadata.MetadataLinker
import kotlinx.coroutines.runBlocking

/**
 * One-shot entrypoint running the metadata linking pass outside the Quartz schedule.
 *
 * Temporary operator tool: run the packaged `jobs` binary's classpath from the repository
 * root so the relative `data/` folder resolves to the real database.
 */
fun main() {
    val koin = bootstrap()
    DatabaseManager.connect()
    val linker = koin.get<MetadataLinker>()
    runBlocking {
        val report = linker.linkAll()
        println(
            "animes=${report.animesProcessed} animeLinks=${report.animeLinksSaved} " +
                "episodeLinks=${report.episodeLinksSaved} unmatched=${report.unmatched.size}"
        )
        report.unmatched.forEach { (title, platforms) ->
            println("UNMATCHED '$title': $platforms")
        }
    }
}
