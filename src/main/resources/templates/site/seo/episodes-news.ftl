<#import "../components/episodeType.ftl" as episodeTypeComponent />
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:news="http://www.google.com/schemas/sitemap-news/0.9">
    <#list episodeMappings as episodeMapping>
        <url>
            <loc>${baseUrl}/animes/${episodeMapping.anime.slug}/season-${episodeMapping.season?c}/${episodeMapping.episodeType.slug}-${episodeMapping.number?c}</loc>
            <news:news>
                <news:publication>
                    <news:name>${episodeMapping.anime.shortName} - Saison ${episodeMapping.season?c} <@episodeTypeComponent.display episodeType=episodeMapping.episodeType /> ${episodeMapping.number?c}<#if episodeMapping.title??> - ${su.sanitizeXSS(episodeMapping.title)}</#if></news:name>
                    <news:language>fr</news:language>
                </news:publication>
                <news:publication_date>${episodeMapping.releaseDateTime?replace("Z", "+0000")}</news:publication_date>
                <news:title>${episodeMapping.anime.shortName} - Saison ${episodeMapping.season?c} <@episodeTypeComponent.display episodeType=episodeMapping.episodeType /> ${episodeMapping.number?c}</news:title>
            </news:news>
        </url>
    </#list>
</urlset>