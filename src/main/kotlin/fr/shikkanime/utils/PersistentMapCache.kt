package fr.shikkanime.utils

import java.io.File
import java.io.Serializable

object PersistentMapCache {
    data class PersistentValue(
        val timestamp: Long,
        val value: Serializable,
    ) : Serializable

    private val persistentFile = File(Constant.dataFolder, "persistent-cache.shikk")
    val map = mutableMapOf<String, PersistentValue>()

    init {
        if (persistentFile.exists()) {
            map.putAll(FileManager.readFile(persistentFile))
        } else {
            persistentFile.createNewFile()
            save()
        }
    }

    fun contains(key: String) = map.containsKey(key)

    inline fun <reified T : Serializable> get(key: String): PersistentValue? {
        val value = map[key] ?: return null
        return if (value.value is T) value else null
    }

    fun <T : Serializable> put(key: String, value: T) {
        map[key] = PersistentValue(System.currentTimeMillis(), value)
        synchronized(PersistentMapCache) { save() }
    }

    @Synchronized
    fun save() = FileManager.writeFile(persistentFile, map)
}