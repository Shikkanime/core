<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div class="my-5">
        <h3>Dernières sorties</h3>

        <div class="row g-3 mt-3">
            <#list animes as anime>
                <div class="col-md-2 col-6 mt-0">
                    <article>
                        <a href="/animes/${anime.uuid}" class="text-decoration-none text-white">
                            <img src="https://api.shikkanime.fr/v1/animes/${anime.uuid}/image" alt="${anime.shortName}" class="w-100">
                            <h6 class="mt-2 text-truncate-2">${anime.shortName}</h6>
                        </a>
                    </article>
                </div>
            </#list>
        </div>
    </div>

    <div class="my-5">
        <h3>Nouveaux épisodes</h3>

        <div class="row g-3 mt-3">
            <#list episodes as episode>
                <div class="col-md-2 col-6 mt-0">
                    <article>
                        <a href="${episode.url}" class="text-decoration-none text-white">
                            <img src="https://api.shikkanime.fr/v1/episodes/${episode.uuid}/image" alt="${episode.anime.shortName}" class="w-100">
                            <h6 class="mt-2 text-truncate-2">${episode.anime.shortName}</h6>
                            <p class="text-muted mb-0">Saison ${episode.season} | Épisode ${episode.number}</p>
                            <p class="text-muted mt-0"><#if episode.langType == 'SUBTITLES'>Sous-titrage<#else>Doublage</#if></p>
                        </a>
                    </article>
                </div>
            </#list>
        </div>
    </div>
</@navigation.display>