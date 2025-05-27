package fr.shikkanime.platforms.configuration

import fr.shikkanime.utils.StringUtils
import io.ktor.http.*

open class ReleaseDayPlatformSimulcast(
    var releaseDay: Int = 0,
    var image: String = StringUtils.EMPTY_STRING
) : PlatformSimulcast() {
    override fun of(parameters: Parameters) {
        super.of(parameters)
        parameters["releaseDay"]?.let { releaseDay = it.toInt() }
        parameters["image"]?.let { image = it }
    }

    override fun toConfigurationFields() = super.toConfigurationFields().apply {
        add(
            ConfigurationField(
                label = "Release day",
                caption = "0 = All days, 1 = Monday, 2 = Tuesday, 3 = Wednesday, 4 = Thursday, 5 = Friday, 6 = Saturday, 7 = Sunday",
                name = "releaseDay",
                type = "number",
                value = releaseDay
            ),
        )
        add(
            ConfigurationField(
                label = "Image",
                name = "image",
                type = "text",
                value = image
            ),
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReleaseDayPlatformSimulcast) return false
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