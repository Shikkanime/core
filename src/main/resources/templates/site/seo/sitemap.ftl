<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
    <url>
        <loc>https://www.shikkanime.fr/</loc>
        <lastmod>${episode.releaseDateTime?replace("Z", "+00:00")}</lastmod>
    </url>
    <#list simulcastsUpdated as simulcast>
        <url>
            <loc>https://www.shikkanime.fr/catalog/${simulcast.slug}</loc>
            <lastmod>${simulcast.lastReleaseDateTime?replace("Z", "+00:00")}</lastmod>
        </url>
    </#list>
    <#list animesUpdated as anime>
        <url>
            <loc>https://www.shikkanime.fr/animes/${anime.slug}</loc>
            <lastmod>${anime.lastReleaseDateTime?replace("Z", "+00:00")}</lastmod>
        </url>
    </#list>
</urlset>