package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.ConfigDto
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class ConfigServiceTest {
    @Inject
    private lateinit var configService: ConfigService

    @Inject
    private lateinit var traceActionService: TraceActionService

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)
    }

    @AfterEach
    fun tearDown() {
        configService.deleteAll()
        traceActionService.deleteAll()
        MapCache.invalidate(Config::class.java)
    }

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