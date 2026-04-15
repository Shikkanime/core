package fr.shikkanime

import org.hibernate.SessionFactory
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import javax.cache.Caching

class CacheVerificationTest : AbstractTest() {

    @Test
    fun testEhcacheIsLoaded() {
        // 1. Vérifier que le cache de second niveau est activé dans Hibernate
        val sessionFactory = database.entityManager.entityManagerFactory.unwrap(SessionFactory::class.java)
        assertTrue(sessionFactory.getCache() != null, "Le cache Hibernate devrait être disponible")

        // 2. Vérifier que le CacheManager JCache (Ehcache) a bien chargé les régions de ehcache.xml
        val provider = Caching.getCachingProvider()
        val cacheManager = provider.cacheManager
        val cacheNames = cacheManager.cacheNames.toList()

        // "default-query-results-region" et "org.hibernate.cache.spi.UpdateTimestampsCache" sont définis dans ehcache.xml
        // Leur présence prouve que le fichier a été lu et appliqué.
        assertTrue(
            cacheNames.contains("default-query-results-region"),
            "Le fichier ehcache.xml devrait être chargé. Caches trouvés : $cacheNames"
        )

        println("Vérification réussie : ehcache.xml est bien chargé et utilisé par Hibernate.")
    }
}
