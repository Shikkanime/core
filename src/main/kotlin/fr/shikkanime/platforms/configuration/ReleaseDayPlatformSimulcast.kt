package fr.shikkanime.platforms.configuration

import io.ktor.http.*

open class ReleaseDayPlatformSimulcast(
    open var releaseDay: Int,
    open var image: String,
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
}