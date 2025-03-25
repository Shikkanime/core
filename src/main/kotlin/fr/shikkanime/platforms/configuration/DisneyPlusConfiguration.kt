package fr.shikkanime.platforms.configuration

class DisneyPlusConfiguration : PlatformConfiguration<ReleaseDayPlatformSimulcast>() {
    override fun newPlatformSimulcast() = ReleaseDayPlatformSimulcast()
}