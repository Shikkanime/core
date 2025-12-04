package fr.shikkanime.platforms.configuration

class NetflixConfiguration : PlatformConfiguration<ReleaseDayPlatformSimulcast>() {
    override fun newPlatformSimulcast() = ReleaseDayPlatformSimulcast()
}