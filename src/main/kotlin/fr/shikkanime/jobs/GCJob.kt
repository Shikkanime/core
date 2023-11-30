package fr.shikkanime.jobs

class GCJob : AbstractJob() {

    override fun run() {
        System.gc()
    }
}