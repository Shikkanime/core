package fr.shikkanime.platforms.configuration

class PrimeVideoConfiguration : PlatformConfiguration<PrimeVideoConfiguration.PrimeVideoSimulcast>() {
    data class PrimeVideoSimulcast(
        override var releaseDay: Int = 1,
        override var image: String = "",
        override var releaseTime: String = "",
    ) : ReleaseDayPlatformSimulcast(releaseDay, image, releaseTime)

    override fun newPlatformSimulcast() = PrimeVideoSimulcast()
}