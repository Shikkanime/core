<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:news="http://www.google.com/schemas/sitemap-news/0.9">
    <#list episodeMappings as episodeMapping>
        <url>
            <loc>${baseUrl}/animes/${episodeMapping.anime.slug}/season-${episodeMapping.season?c}/${episodeMapping.episodeType.slug}-${episodeMapping.number?c}</loc>
            <news:news>
                <news:publication>
                    <news:name>${episodeMapping.anime.shortName?html} - ${su.toEpisodeMappingString(episodeMapping, true, false)}<#if episodeMapping.title??> - ${episodeMapping.title?html}</#if></news:name>
                    <news:language>fr</news:language>
                </news:publication>
                <news:publication_date>${episodeMapping.releaseDateTime?split("T")?first}</news:publication_date>
                <news:title>${episodeMapping.anime.shortName?html} - ${su.toEpisodeMappingString(episodeMapping, true, false)}</news:title>
            </news:news>
        </url>
    </#list>
</urlset>