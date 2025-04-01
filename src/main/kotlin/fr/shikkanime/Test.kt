package fr.shikkanime

import fr.shikkanime.jobs.UpdateAnimeJob
import fr.shikkanime.utils.Constant
import kotlin.system.exitProcess

fun main() {
    val updateAnimeJob = Constant.injector.getInstance(UpdateAnimeJob::class.java)
    updateAnimeJob.run()
    exitProcess(0)
}