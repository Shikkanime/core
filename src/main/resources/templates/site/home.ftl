<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div class="mt-3">
        <h3>Nouveaux épisodes</h3>

        <div class="row g-3 mt-3">
            <#list episodes as episode>
                <div class="col-md-2 col-6 mt-0">
                    <article>
                        <a href="${episode.url}" target="_blank" class="text-decoration-none text-white">
                            <div class="hover-card position-relative">
                                <div class="position-relative">
                                    <img src="https://api.shikkanime.fr/v1/attachments?uuid=${episode.uuid}&type=image"
                                         alt="${episode.anime.shortName?replace("\"", "'")} episode preview image"
                                         class="w-100<#if episode.uncensored> blur</#if>">
                                    <img src="https://www.shikkanime.fr/assets/img/platforms/${episode.platform.image}"
                                         alt="${episode.platform.name()} platform image"
                                         class="position-absolute top-0 end-0 rounded-circle me-1 mt-1" width="24"
                                         height="24">
                                </div>

                                <span class="h6 mt-2 text-truncate-2">${episode.anime.shortName}</span>
                                <p class="text-muted mb-0">Saison ${episode.season?c} |
                                    Épisode ${episode.number?c}<#if episode.uncensored> non censuré</#if></p>
                                <p class="text-muted mt-0"><#if episode.langType == 'SUBTITLES'>Sous-titrage<#else>Doublage</#if></p>

                                <#if episode.title?? || episode.description??>
                                    <div class="hover-card-description d-none bg-black bg-opacity-75 position-absolute top-0 start-0 w-100 h-100 mh-100 p-3">
                                        <div class="h6 text-truncate-2">
                                            ${episode.title}
                                        </div>

                                        <span class="text-muted">
                                            <i class="bi bi-calendar4 me-1"></i>
                                            ${episode.releaseDateTime?datetime("yyyy-MM-dd'T'HH:mm:ss")?string("dd/MM/yyyy")}
                                        </span>

                                        <#if episode.description??>
                                            <div class="text-truncate-6 mt-3">
                                                ${episode.description}
                                            </div>
                                        </#if>
                                    </div>
                                </#if>
                            </div>
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
                            <div class="hover-card position-relative">
                                <img src="https://api.shikkanime.fr/v1/attachments?uuid=${anime.uuid}&type=image"
                                     alt="${anime.shortName?replace("\"", "'")} anime image" class="w-100">
                                <span class="h6 mt-2 text-truncate-2">${anime.shortName}</span>

                                <div class="hover-card-description d-none bg-black bg-opacity-75 position-absolute top-0 start-0 w-100 h-100 mh-100 p-3">
                                    <div class="h6 text-truncate-2">
                                        ${anime.name?upper_case}
                                    </div>

                                    <hr>

                                    <#if anime.description??>
                                        <div class="text-truncate-6">
                                            ${anime.description}
                                        </div>
                                    </#if>
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
        const hoverCards = document.querySelectorAll('.hover-card');

        hoverCards.forEach((animeCard) => {
            const element = animeCard.querySelector('.hover-card-description');

            if (element === null) {
                return;
            }

            animeCard.addEventListener('mouseover', () => {
                element.classList.remove('d-none');
            });

            animeCard.addEventListener('mouseout', () => {
                element.classList.add('d-none');
            });
        });
    </script>
</@navigation.display>