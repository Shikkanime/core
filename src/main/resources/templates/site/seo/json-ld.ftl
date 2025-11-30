<#macro anime anime>
    <#assign platforms = []>

    <#if anime.platformIds??>
        <#list anime.platformIds as platform>
            <#if platforms?filter(p -> p.id == platform.platform.id)?size == 0>
                <#assign platforms = platforms + [platform.platform]>
            </#if>
        </#list>
    </#if>

    <#assign totalEpisodes = 0>

    <#list anime.seasons as season>
        <#assign totalEpisodes = totalEpisodes + season.episodes>
    </#list>

    <script type="application/ld+json">
        {
            "@context": "https://schema.org",
            "@type": "TVSeries",
            "name": "${anime.name?json_string}",
            "alternateName": "${anime.shortName?json_string}",
            "url": "${baseUrl}/animes/${anime.slug}",
            "thumbnailUrl": "${apiUrl}/v1/attachments?uuid=${anime.uuid}&type=THUMBNAIL}",
            "image": "${apiUrl}/v1/attachments?uuid=${anime.uuid}&type=BANNER}"
            <#if anime.description?? && anime.description?has_content>
            ,"description": "${anime.description?json_string}"
            </#if>
            ,"startDate": "${anime.releaseDateTime}"
            ,"dateModified": "${anime.lastUpdateDateTime}"
            <#if anime.audioLocales?? && anime.audioLocales?has_content>
            ,"inLanguage": [
                <#list anime.audioLocales as locale>
                "${locale?json_string}"<#if locale_has_next>,</#if>
                </#list>
            ]
            </#if>
            <#if platforms?has_content>
            ,"provider": [
                <#list platforms as platform>
                {
                    "@type": "Organization",
                    "name": "${platform.name?json_string}",
                    "url": "${platform.url?json_string}",
                    "logo": "${baseUrl}/assets/img/platforms/${platform.image}"
                }<#if platform_has_next>,</#if>
                </#list>
            ]
            </#if>
            ,"numberOfEpisodes": ${totalEpisodes?c}
            <#if anime.seasons?? && anime.seasons?has_content>
            ,"numberOfSeasons": ${anime.seasons?size}
            ,"containsSeason": [
                <#list anime.seasons as season>
                {
                    "@type": "TVSeason",
                    "name": "Saison ${season.number?c}",
                    "seasonNumber": ${season.number?c},
                    "startDate": "${season.releaseDateTime}",
                    "dateModified": "${season.lastReleaseDateTime}",
                    "numberOfEpisodes": ${season.episodes?c}
                }<#if season_has_next>,</#if>
                </#list>
            ]
            </#if>
            <#if anime.simulcasts?? && anime.simulcasts?has_content>
            ,"keywords": "<#list anime.simulcasts as simulcast>${simulcast.label?json_string}<#if simulcast_has_next>, </#if></#list>"
            </#if>
        }
    </script>
</#macro>
