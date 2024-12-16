<?xml version="1.0" encoding="utf-8" ?>
<rss version="2.0">
    <channel>
        <title>Shikkanime - Flux</title>
        <description>Shikkanime RSS Feed</description>
        <#if episodeMappings?size != 0>
        <lastBuildDate>${episodeMappings[0].lastReleaseDateTime?replace("Z", "+0000")}</lastBuildDate>
        </#if>
        <link>${baseUrl}</link>
        <#list episodeMappings as episodeMapping>
        <item>
            <title>${episodeMapping.anime.shortName} - ${su.toEpisodeMappingString(episodeMapping, true, false)}<#if episodeMapping.title??> - ${su.sanitizeXSS(episodeMapping.title)}</#if></title>
            <description>${episodeMapping.description!""}</description>
            <pubDate>${episodeMapping.releaseDateTime?replace("Z", "+0000")}</pubDate>
            <lastBuildDate>${episodeMapping.lastReleaseDateTime?replace("Z", "+0000")}</lastBuildDate>
            <link>${baseUrl}/animes/${episodeMapping.anime.slug}/season-${episodeMapping.season?c}/${episodeMapping.episodeType.slug}-${episodeMapping.number?c}</link>
            <image>${apiUrl}/v1/attachments?uuid=${episodeMapping.uuid}&amp;type=image</image>
        </item>
        </#list>
    </channel>
</rss>