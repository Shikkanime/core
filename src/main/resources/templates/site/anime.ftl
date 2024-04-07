<#import "_navigation.ftl" as navigation />
<#import "components/episode.ftl" as episodeComponent />
<#import "components/langType.ftl" as langTypeComponent />

<#assign canonicalUrl = "<link rel=\"canonical\" href=\"https://www.shikkanime.fr/animes/${anime.slug}\" />">

<@navigation.display header="${canonicalUrl}">
    <div class="container">
        <div class="row g-3 mt-3">
            <div class="col-md-4 col-12 mt-0 text-center">
                <img src="https://api.shikkanime.fr/v1/attachments?uuid=${anime.uuid}&type=image"
                     alt="${su.sanitizeXSS(anime.shortName)} anime image" class="img-fluid w-50 rounded-4 border-light"
                     width="480"
                     height="720">
            </div>

            <div class="col-md-8 col-12 text-start mt-md-0 mt-5 d-flex flex-column justify-content-center">
                <h1 class="h6 fw-bold mb-0">${anime.shortName?upper_case}</h1>

                <div class="mt-1">
                    <#list anime.langTypes as langType>
                        <p class="text-muted mb-0">
                            <@langTypeComponent.display langType=langType />
                        </p>
                    </#list>
                </div>

                <#if (anime.simulcasts?size > 0)>
                    <div class="mt-3 d-inline">
                        <#list anime.simulcasts as simulcast>
                            <a href="/catalog/${simulcast.slug}" class="btn btn-outline-light">${simulcast.label}</a>
                        </#list>
                    </div>
                </#if>

                <span class="mt-3">${anime.description}</span>
            </div>
        </div>
    </div>

    <div class="row g-3 mt-4 justify-content-center">
        <#list episodes as episode>
            <@episodeComponent.display episode=episode cover=false desktopColSize="col-md-2" mobileColSize="col-6" />
        </#list>
    </div>
</@navigation.display>