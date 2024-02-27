<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
    <#assign baseUrl = "https://www.shikkanime.fr">
    <#if episode??>
        <url>
            <loc>${baseUrl}/</loc>
            <lastmod>${episode.releaseDateTime?replace("Z", "+00:00")}</lastmod>
        </url>
    </#if>
    <#list simulcasts as simulcast>
        <url>
            <loc>${baseUrl}/catalog/${simulcast.slug}</loc>
            <lastmod>${simulcast.lastReleaseDateTime?replace("Z", "+00:00")}</lastmod>
        </url>
    </#list>
    <#list animes as anime>
        <url>
            <loc>${baseUrl}/animes/${anime.slug}</loc>
            <lastmod>${anime.lastReleaseDateTime?replace("Z", "+00:00")}</lastmod>
        </url>
    </#list>
    <#list seoLinks as link>
        <url>
            <loc>${baseUrl}${link.href}</loc>
            <lastmod>2024-02-27T17:00:00+00:00</lastmod>
        </url>
    </#list>
</urlset>