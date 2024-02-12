<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
    <url>
        <loc>https://www.shikkanime.fr/</loc>
        <lastmod>${episode.releaseDateTime}</lastmod>
        <priority>1</priority>
    </url>
    <#list simulcasts as simulcast>
        <url>
            <loc>https://www.shikkanime.fr/catalog/${simulcast.slug}</loc>
            <lastmod>${simulcast.lastReleaseDateTime}</lastmod>
            <priority>0.8</priority>
        </url>
    </#list>
    <#list animes as anime>
        <url>
            <loc>https://www.shikkanime.fr/animes/${anime.slug}</loc>
            <lastmod>${anime.lastReleaseDateTime}</lastmod>
            <priority>0.64</priority>
        </url>
    </#list>
</urlset>