<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
    <#assign baseUrl = "https://www.shikkanime.fr">
    <#if episodeMapping??>
        <url>
            <loc>${baseUrl}/</loc>
            <lastmod>${episodeMapping.lastReleaseDateTime?replace("Z", "+00:00")}</lastmod>
        </url>
        <url>
            <loc>${baseUrl}/calendar</loc>
            <lastmod>${episodeMapping.lastReleaseDateTime?replace("Z", "+00:00")}</lastmod>
        </url>
    </#if>
    <url>
        <loc>${baseUrl}/search</loc>
        <lastmod>2024-03-20T17:00:00+00:00</lastmod>
    </url>
    <#list simulcasts as simulcast>
        <url>
            <loc>${baseUrl}/catalog/${simulcast.slug}</loc>
            <lastmod>${simulcast.lastReleaseDateTime?replace("Z", "+00:00")}</lastmod>
        </url>
    </#list>
    <#list animes as anime>
        <url>
            <loc>${baseUrl}/animes/${anime.slug}</loc>
            <lastmod>${anime.seasons?first.lastReleaseDateTime?replace("Z", "+00:00")}</lastmod>
        </url>
        <#list anime.seasons as season>
            <url>
                <loc>${baseUrl}/animes/${anime.slug}/season-${season.number}</loc>
                <lastmod>${season.lastReleaseDateTime?replace("Z", "+00:00")}</lastmod>
            </url>
        </#list>
        <#list anime.episodes as episode>
            <url>
                <loc>${baseUrl}/animes/${anime.slug}/season-${episode.season?c}/${episode.episodeType.slug}
                    -${episode.number?c}</loc>
                <lastmod>${episode.lastReleaseDateTime?replace("Z", "+00:00")}</lastmod>
            </url>
        </#list>
    </#list>
    <#list seoLinks as link>
        <url>
            <loc>${baseUrl}${link.href}</loc>
            <lastmod>2024-02-27T17:00:00+00:00</lastmod>
        </url>
    </#list>
</urlset>