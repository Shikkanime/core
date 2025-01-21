package fr.shikkanime.services.caches

import fr.shikkanime.utils.MapCache
import org.apache.tika.language.detect.LanguageDetector

class LanguageCacheService : AbstractCacheService {
    private val languageDetector: LanguageDetector = LanguageDetector.getDefaultLanguageDetector().loadModels()

    fun detectLanguage(text: String?) = MapCache.getOrCompute(
        "LanguageCacheService.detectLanguage",
        key = text ?: "",
    ) { languageDetector.detect(it).language.lowercase() }
}