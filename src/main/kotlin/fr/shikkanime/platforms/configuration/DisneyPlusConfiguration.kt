package fr.shikkanime.platforms.configuration

class DisneyPlusConfiguration : PlatformConfiguration<DisneyPlusConfiguration.DisneyPlusSimulcast>() {
    data class DisneyPlusSimulcast(
        override var releaseDay: Int = 1,
    ) : ReleaseDayPlatformSimulcast(releaseDay, "")

    override fun newPlatformSimulcast() = DisneyPlusSimulcast()
}