package fr.shikkanime.services.caches

import fr.shikkanime.utils.MapCache
import org.apache.tika.language.detect.LanguageDetector

class LanguageCacheService : AbstractCacheService {
    private val languageDetector: LanguageDetector = LanguageDetector.getDefaultLanguageDetector().loadModels()

    private val detectCache = MapCache<String, String> {
        languageDetector.detect(it).language.lowercase()
    }

    fun detectLanguage(text: String?) = text?.let { detectCache[it] }
}