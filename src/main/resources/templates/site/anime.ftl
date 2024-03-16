<#import "_navigation.ftl" as navigation />
<#import "components/episode.ftl" as episodeComponent />

<@navigation.display>
    <div class="container">
        <div class="row g-3 mt-3">
            <div class="col-md-4 col-12 mt-0 text-center">
                <img src="https://api.shikkanime.fr/v1/attachments?uuid=${anime.uuid}&type=image"
                     alt="${su.sanitizeXSS(anime.shortName)} anime image" class="img-fluid w-50" width="480"
                     height="720">
            </div>

            <div class="col-md-8 col-12 mt-0 text-start mt-md-0 mt-5 d-flex flex-column justify-content-center">
                <h6 class="h6">${anime.shortName?upper_case}</h6>
                <p class="text-muted">${anime.name}</p>

                <#if (anime.simulcasts?size > 1)>
                    <div class="my-3 d-inline">
                        <#list anime.simulcasts as simulcast>
                            <a href="/catalog/${simulcast.slug}" class="text-white">${simulcast.label}</a>

                            <#if (simulcast?has_next)>
                                <span class="mx-1 text-muted">-</span>
                            </#if>
                        </#list>
                    </div>
                </#if>

                <span class="mt-2">${anime.description}</span>
            </div>
        </div>
    </div>

    <div class="row mt-5 justify-content-center">
        <#list episodes as episode>
            <@episodeComponent.display episode=episode cover=false col="col-md-2" />
        </#list>
    </div>
</@navigation.display>