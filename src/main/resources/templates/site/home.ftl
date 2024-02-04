<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div class="mt-3">
        <h3>Nouveaux épisodes</h3>

        <div class="row g-3 mt-3">
            <#list episodes as episode>
                <div class="col-md-2 col-6 mt-0">
                    <article>
                        <a href="${episode.url}" class="text-decoration-none text-white">
                            <img loading="lazy" src="https://api.shikkanime.fr/v1/attachments?uuid=${episode.uuid}&type=image"
                                 alt="${episode.anime.shortName?replace("\"", "'")} episode preview image"
                                 class="w-100<#if episode.uncensored> blur</#if>">
                            <span class="h6 mt-2 text-truncate-2">${episode.anime.shortName}</span>
                            <p class="text-muted mb-0">Saison ${episode.season?c} |
                                Épisode ${episode.number?c}<#if episode.uncensored> non censuré</#if></p>
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
                            <div class="anime-card position-relative">
                                <img loading="lazy" src="https://api.shikkanime.fr/v1/attachments?uuid=${anime.uuid}&type=image"
                                     alt="${anime.shortName?replace("\"", "'")} anime image" class="w-100">
                                <span class="h6 mt-2 text-truncate-2">${anime.shortName}</span>

                                <div class="d-none bg-black bg-opacity-75 anime-card-description position-absolute top-0 start-0 w-100 h-100 p-3">
                                    <div class="anime-card-description-truncate">
                                        ${anime.description}
                                    </div>
                                </div>
                            </div>
                        </a>
                    </article>
                </div>
            </#list>
        </div>
    </div>

    <script>
        // When hover on anime card, show description on the anime card
        const animeCards = document.querySelectorAll('.anime-card');

        animeCards.forEach((animeCard) => {
            animeCard.addEventListener('mouseover', () => {
                animeCard.querySelector('.anime-card-description').classList.remove('d-none');
            });

            animeCard.addEventListener('mouseout', () => {
                animeCard.querySelector('.anime-card-description').classList.add('d-none');
            });
        });
    </script>
</@navigation.display>