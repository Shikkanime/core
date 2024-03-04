package fr.shikkanime.platforms.configuration

import io.ktor.http.*

open class ReleaseDayPlatformSimulcast(
    @Transient
    open var releaseDay: Int,
    @Transient
    open var image: String,
    @Transient
    open var releaseTime: String,
) : PlatformSimulcast() {
    override fun of(parameters: Parameters) {
        super.of(parameters)
        parameters["releaseDay"]?.let { releaseDay = it.toInt() }
        parameters["image"]?.let { image = it }
        parameters["releaseTime"]?.let { releaseTime = it }
    }

    override fun toConfigurationFields() = super.toConfigurationFields().apply {
        add(
            ConfigurationField(
                label = "Release day",
                caption = "1 = Monday, 2 = Tuesday, 3 = Wednesday, 4 = Thursday, 5 = Friday, 6 = Saturday, 7 = Sunday",
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
        add(
            ConfigurationField(
                label = "Release time",
                caption = "Format: HH:mm:ss (In UTC)",
                name = "releaseTime",
                type = "time",
                value = releaseTime
            ),
        )
    }
}