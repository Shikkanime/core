<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div class="mt-3">
        <h3>Nouveaux épisodes</h3>

        <div class="row g-3 mt-3">
            <#list episodes as episode>
                <div class="col-md-2 col-6 mt-0">
                    <article>
                        <a href="${episode.url}" class="text-decoration-none text-white">
                            <img src="https://api.shikkanime.fr/v1/episodes/${episode.uuid}/image"
                                 alt="${episode.anime.shortName}" class="w-100<#if episode.uncensored> blur</#if>">
                            <span class="h6 mt-2 text-truncate-2">${episode.anime.shortName}</span>
                            <p class="text-muted mb-0">Saison ${episode.season} | Épisode ${episode.number}<#if episode.uncensored> non censuré</#if></p>
                            <p class="text-muted mt-0"><#if episode.langType == 'SUBTITLES'>Sous-titrage<#else>Doublage</#if></p>
                        </a>
                    </article>
                </div>
            </#list>
        </div>
    </div>

    <div class="mt-3">
        <div class="d-flex align-content-center">
            <h3 class="ms-0 me-auto">Simulcast en cours</h3>
            <a href="/catalog" class="btn btn-outline-light ms-auto me-0 rounded-pill px-3">
                PLUS
                <i class="bi bi-chevron-right"></i>
            </a>
        </div>

        <div class="row g-3 mt-3">
            <#list animes as anime>
                <div class="col-md-2 col-6 mt-0">
                    <article>
                        <a href="/animes/${anime.uuid}" class="text-decoration-none text-white">
                            <img src="https://api.shikkanime.fr/v1/animes/${anime.uuid}/image" alt="${anime.shortName} anime image"
                                 class="w-100">
                            <span class="h6 mt-2 text-truncate-2">${anime.shortName}</span>
                        </a>
                    </article>
                </div>
            </#list>
        </div>
    </div>
</@navigation.display>