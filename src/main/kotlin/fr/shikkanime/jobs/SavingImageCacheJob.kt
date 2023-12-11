package fr.shikkanime.jobs

import fr.shikkanime.services.ImageService

class SavingImageCacheJob : AbstractJob() {

    override fun run() {
        ImageService.saveCache()
    }
}