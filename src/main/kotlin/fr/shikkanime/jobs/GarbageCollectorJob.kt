package fr.shikkanime.jobs

class GarbageCollectorJob : AbstractJob {
    override fun run() {
        System.gc()
    }
}