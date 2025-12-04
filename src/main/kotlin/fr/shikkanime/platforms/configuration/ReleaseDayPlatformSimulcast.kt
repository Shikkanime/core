package fr.shikkanime.platforms.configuration

import io.ktor.http.*
import java.time.ZoneId
import java.time.ZonedDateTime

open class ReleaseDayPlatformSimulcast(
    var releaseDay: Int = 0
) : PlatformSimulcast() {
    override fun of(parameters: Parameters) {
        super.of(parameters)
        parameters["releaseDay"]?.let { releaseDay = it.toInt() }
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
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReleaseDayPlatformSimulcast) return false
        if (!super.equals(other)) return false

        if (releaseDay != other.releaseDay) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + releaseDay
        return result
    }

    open fun canBeFetch(zonedDateTime: ZonedDateTime): Boolean {
        if (releaseDay == 0) return true

        val dayInOriginalZone = zonedDateTime.dayOfWeek.value
        val dayInSystemZone = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).dayOfWeek.value

        return releaseDay == dayInOriginalZone || releaseDay == dayInSystemZone
    }
}