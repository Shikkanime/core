package fr.shikkanime.platforms.configuration

class AnimationDigitalNetworkConfiguration : PlatformConfiguration<PlatformSimulcast>() {
    override fun newPlatformSimulcast() = PlatformSimulcast()
}