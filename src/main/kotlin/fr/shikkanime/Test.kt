package fr.shikkanime

import fr.shikkanime.jobs.UpdateEpisodeMappingJob
import fr.shikkanime.utils.Constant
import kotlin.system.exitProcess

fun main() {
    val updateEpisodeMappingJob = Constant.injector.getInstance(UpdateEpisodeMappingJob::class.java)
    updateEpisodeMappingJob.run()
    exitProcess(0)
}