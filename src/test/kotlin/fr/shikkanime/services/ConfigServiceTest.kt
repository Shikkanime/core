package fr.shikkanime.services

import fr.shikkanime.AbstractTest
import fr.shikkanime.dtos.ConfigDto
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.TraceAction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class ConfigServiceTest : AbstractTest() {
    @Test
    fun updateConfigSuccessfully() {
        val saved = configService.save(Config(null, "propertyName", "oldValue"))
        val configDto = ConfigDto(null, "propertyName", "newValue")
        val result = configService.update(saved.uuid!!, configDto)

        assertNotNull(result)
        assertEquals("newValue", result!!.propertyValue)
        assertTrue(
            traceActionService.findAll().any { it.action == TraceAction.Action.UPDATE && it.entityUuid == saved.uuid })
    }

    @Test
    fun updateConfigNoChange() {
        val saved = configService.save(Config(null, "propertyName", "sameValue"))
        val configDto = ConfigDto(null, "propertyName", "sameValue")
        val result = configService.update(saved.uuid!!, configDto)

        assertNotNull(result)
        assertEquals("sameValue", result!!.propertyValue)
        assertFalse(
            traceActionService.findAll().any { it.action == TraceAction.Action.UPDATE && it.entityUuid == saved.uuid })
    }

    @Test
    fun updateConfigNotFound() {
        val uuid = UUID.randomUUID()
        val configDto = ConfigDto(null, "propertyName", "newValue")
        val result = configService.update(uuid, configDto)

        assertNull(result)
    }
}