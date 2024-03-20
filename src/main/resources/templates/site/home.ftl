<#import "_navigation.ftl" as navigation />
<#import "components/episode.ftl" as episodeComponent />
<#import "components/anime.ftl" as animeComponent />

<@navigation.display>
    <h1 class="h3 my-3">Nouveaux épisodes</h1>

    <#if episodes?? && episodes?size != 0>
        <div class="row g-3">
            <#list episodes as episode>
            <#-- If episode is the first element -->
                <#assign col="col-md-2">
                <#assign firstRow=false>

                <#if episode?index == 0>
                    <#assign col="col-md-7">
                    <#assign firstRow=true>
                <#elseif episode?index == 1>
                    <#assign col="col-md-5">
                    <#assign firstRow=true>
                </#if>

                <@episodeComponent.display episode=episode desktopColSize=col mobileColSize="col-12" cover=firstRow />
            </#list>
        </div>
    <#else>
        <div class="d-flex justify-content-center align-items-center my-5">
            <p class="text-muted p-5">Aucun épisode disponible pour le moment</p>
        </div>
    </#if>

    <div class="d-flex align-content-center my-3">
        <h1 class="h3 ms-0 me-auto">Simulcast en cours</h1>

        <#if currentSimulcast??>
            <a href="/catalog/${currentSimulcast.slug}" class="btn btn-outline-light ms-auto me-0 rounded-pill px-3">
                PLUS
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                     class="bi bi-chevron-right" viewBox="0 0 16 16">
                    <path fill-rule="evenodd"
                          d="M4.646 1.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1 0 .708l-6 6a.5.5 0 0 1-.708-.708L10.293 8 4.646 2.354a.5.5 0 0 1 0-.708"/>
                </svg>
            </a>
        </#if>
    </div>

    <#if animes?? && animes?size != 0>
        <div class="row">
            <#list animes as anime>
                <@animeComponent.display anime=anime />
            </#list>
        </div>
    <#else>
        <div class="d-flex justify-content-center align-items-center my-5">
            <p class="text-muted p-5">Aucun animé disponible pour le moment</p>
        </div>
    </#if>
</@navigation.display>