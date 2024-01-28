<#import "_navigation.ftl" as navigation />
<#import "components/episode.ftl" as episodeComponent />
<#import "components/anime.ftl" as animeComponent />

<@navigation.display>
    <div class="container">
        <div class="row g-3 mt-3">
            <div class="col-md-4 col-12 mt-0 text-center">
                <img src="https://api.shikkanime.fr/v1/attachments?uuid=${recommendedAnime.uuid}&type=image"
                     alt="${su.sanitizeXSS(recommendedAnime.shortName)} anime image" class="img-fluid w-75" width="480"
                     height="720">
            </div>

            <div class="col-md-8 col-12 text-start mt-md-0 mt-5 d-flex flex-column justify-content-center">
                <h6 class="h6 mb-0">${recommendedAnime.shortName?upper_case}</h6>

                <#if (recommendedAnime.simulcasts?size > 1)>
                    <div class="mt-3 d-inline">
                        <#list recommendedAnime.simulcasts as simulcast>
                            <a href="/catalog/${simulcast.slug}" class="text-white">${simulcast.label}</a>

                            <#if (simulcast?has_next)>
                                <span class="mx-1 text-muted">-</span>
                            </#if>
                        </#list>
                    </div>
                </#if>

                <span class="my-4">
                    ${recommendedAnime.description}
                    <#if (recommendedAnime.genres?size > 1)>
                        <div class="d-inline">
                        <#list recommendedAnime.genres as genre>
                            <span class="badge bg-secondary">${genre?replace("_", " ")?capitalize}</span>
                        </#list>
                        </div>
                    </#if>
                </span>

                <#if (recommendedAnime.recommendations?size > 0)>
                    <h6 class="h6 mb-3">RECOMMANDATIONS</h6>

                    <div class="row">
                        <#list recommendedAnime.recommendations as recommendation>
                            <@animeComponent.display anime=recommendation />
                        </#list>
                    </div>
                </#if>
            </div>
        </div>
    </div>

    <div class="row g-3 mt-5 justify-content-center">
        <#list episodes as episode>
            <@episodeComponent.display episode=episode cover=false desktopColSize="col-md-2" mobileColSize="col-6" />
        </#list>
    </div>
</@navigation.display>