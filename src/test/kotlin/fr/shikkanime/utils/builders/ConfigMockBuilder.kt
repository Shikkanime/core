package fr.shikkanime.utils.builders

import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import io.mockk.every
import io.mockk.mockk

class ConfigMockBuilder : IMockBuilder<Config> {
    private var propertyKey: ConfigPropertyKey? = null
    private var propertyValue: String? = null

    override fun build(): Config {
        val entity = mockk<Config>(relaxed = true)

        every { entity.propertyKey } returns propertyKey?.key
        every { entity.propertyValue } returns propertyValue

        return entity
    }

    fun propertyKey(propertyKey: ConfigPropertyKey) = apply { this.propertyKey = propertyKey }
    fun propertyValue(propertyValue: String) = apply { this.propertyValue = propertyValue }
}