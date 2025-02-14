<?xml version="1.0" encoding="utf-8" ?>
<rss version="2.0">
    <channel>
        <title>Shikkanime - Flux</title>
        <description>Shikkanime RSS Feed</description>
        <#if groupedEpisodes?size != 0>
        <lastBuildDate>${groupedEpisodes[0].releaseDateTime?replace("Z", "+0000")}</lastBuildDate>
        </#if>
        <link>${baseUrl}</link>
        <#list groupedEpisodes as groupedEpisode>
        <item>
            <title>${groupedEpisode.anime.name?html} - ${su.toEpisodeGroupedString(groupedEpisode, true, false)}<#if groupedEpisode.title??> - ${groupedEpisode.title?html}</#if></title>
            <description>${(groupedEpisode.description!"")?html}</description>
            <pubDate>${groupedEpisode.releaseDateTime?replace("Z", "+0000")}</pubDate>
            <lastBuildDate>${groupedEpisode.releaseDateTime?replace("Z", "+0000")}</lastBuildDate>
            <link>${groupedEpisode.internalUrl}</link>
            <image>${apiUrl}/v1/attachments?uuid=${groupedEpisode.mappings?first}&amp;type=image</image>
        </item>
        </#list>
    </channel>
</rss>