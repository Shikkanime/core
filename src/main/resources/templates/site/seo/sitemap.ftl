<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
    <#list urls as url>
        <url>
            <loc>${url.absoluteURL}</loc>
            <lastmod>${url.lastModification}</lastmod>
        </url>
    </#list>
</urlset>