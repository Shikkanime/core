package fr.shikkanime.modules

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer

class CustomLuceneAnalysisDefinitionProvider : LuceneAnalysisConfigurer {
    override fun configure(p0: LuceneAnalysisConfigurationContext?) {
        p0!!.analyzer("shikkanime_analyzer").custom()
            .tokenizer("standard")
            .tokenFilter("lowercase")
            .tokenFilter("asciifolding")
            .tokenFilter("ngram")
            .param("minGramSize", "2")
            .param("maxGramSize", "4")
    }
}