package fr.shikkanime.utils

import fr.shikkanime.entities.ShikkEntity
import jakarta.persistence.ElementCollection
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToMany
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.ExpiryPolicyBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.config.units.EntryUnit
import org.ehcache.config.units.MemoryUnit
import org.ehcache.jsr107.Eh107Configuration
import org.hibernate.annotations.Cache
import javax.cache.CacheManager
import javax.cache.Caching
import javax.cache.configuration.Configuration
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

object CacheHelper {
    private fun createCacheIfMissing(
        cacheManager: CacheManager,
        name: String,
        config: Configuration<Any, Any>
    ) {
        if (cacheManager.getCache<Any, Any>(name) == null) {
            cacheManager.createCache(name, config)
        }
    }

    private fun entityRegionName(entityClass: Class<*>) =
        entityClass.getAnnotation(Cache::class.java)?.region
            ?.takeIf { it.isNotBlank() }
            ?: entityClass.name

    private fun collectionRegionNames(entityClass: Class<*>) = entityClass.declaredFields
        .filter { field ->
            field.isAnnotationPresent(Cache::class.java) &&
                    (field.isAnnotationPresent(OneToMany::class.java) ||
                            field.isAnnotationPresent(ManyToMany::class.java) ||
                            field.isAnnotationPresent(ElementCollection::class.java))
        }
        .map { field ->
            field.getAnnotation(Cache::class.java)?.region
                ?.takeIf { it.isNotBlank() }
                ?: "${entityClass.name}.${field.name}"
        }

    fun configure() {
        val cachingProvider = Caching.getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider")
        val cacheManager = cachingProvider.cacheManager

        val entityConfig = Eh107Configuration.fromEhcacheCacheConfiguration(
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                Any::class.java,
                Any::class.java,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(1_000, EntryUnit.ENTRIES)
                    .offheap(4, MemoryUnit.MB)
            ).withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(1.hours.toJavaDuration()))
                .build()
        )

        val timestampsConfig = Eh107Configuration.fromEhcacheCacheConfiguration(
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                Any::class.java,
                Any::class.java,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(1_000, EntryUnit.ENTRIES)
                    .offheap(1, MemoryUnit.MB)
            ).build()
        )
        createCacheIfMissing(cacheManager, "default-query-results-region", entityConfig)
        createCacheIfMissing(cacheManager, "default-update-timestamps-region", timestampsConfig)

        Constant.reflections.getSubTypesOf(ShikkEntity::class.java).forEach { entityClass ->
            createCacheIfMissing(cacheManager, entityRegionName(entityClass), entityConfig)

            collectionRegionNames(entityClass).forEach { regionName ->
                createCacheIfMissing(cacheManager, regionName, entityConfig)
            }
        }
    }
}
