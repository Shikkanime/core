package fr.shikkanime.platforms.configuration

class PrimeVideoConfiguration : PlatformConfiguration<PrimeVideoConfiguration.PrimeVideoSimulcast>() {
    data class PrimeVideoSimulcast(
        override var releaseDay: Int = 1,
        override var image: String = "",
    ) : ReleaseDayPlatformSimulcast(releaseDay, image) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PrimeVideoSimulcast) return false
            if (!super.equals(other)) return false

            if (releaseDay != other.releaseDay) return false
            if (image != other.image) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + releaseDay
            result = 31 * result + image.hashCode()
            return result
        }
    }

    override fun newPlatformSimulcast() = PrimeVideoSimulcast()
}