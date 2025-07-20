package fr.shikkanime.platforms.configuration

class CrunchyrollConfiguration : PlatformConfiguration<PlatformSimulcast>() {
    override fun newPlatformSimulcast() = PlatformSimulcast()
}