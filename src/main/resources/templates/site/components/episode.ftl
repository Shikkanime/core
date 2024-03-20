<#function getPrefixEpisode(episodeType)>
    <#switch episodeType>
        <#case "EPISODE">
            <#return "Épisode">
        <#case "FILM">
            <#return "Film">
        <#case "SPECIAL">
            <#return "Spécial">
    </#switch>
</#function>

<#macro display episode desktopColSize mobileColSize cover>
    <div class="${desktopColSize} ${mobileColSize}" x-data="{ hover: false }" @mouseenter="hover = true"
         @mouseleave="hover = false">
        <article>
            <a href="${episode.url}" target="_blank" class="text-decoration-none text-white">
                <div class="position-relative">
                    <div class="position-relative">
                        <img src="https://api.shikkanime.fr/v1/attachments?uuid=${episode.uuid}&type=image"
                             alt="${su.sanitizeXSS(episode.anime.shortName)} episode preview image"
                             class="<#if cover>w-100 object-fit-cover<#else>img-fluid</#if> <#if episode.uncensored>blur</#if>"
                             width="640" height="360">

                        <img src="https://www.shikkanime.fr/assets/img/platforms/${episode.platform.image}"
                             alt="${episode.platform.name} platform image"
                             class="position-absolute top-0 end-0 rounded-circle me-1 mt-1" width="24"
                             height="24">

                        <#if cover?? && cover>
                            <div class="position-absolute bottom-0 start-0 py-2 px-3 bg-black bg-opacity-50 m w-100">
                                <span class="h6 mt-2 text-truncate-2">${episode.anime.shortName}</span>

                                <p class="text-muted mb-0">Saison ${episode.season?c} |
                                    ${getPrefixEpisode(episode.episodeType)} ${episode.number?c}<#if episode.uncensored> non censuré</#if>
                                </p>

                                <p class="text-muted mt-0 mb-0 pb-0"><#if episode.langType == 'SUBTITLES'>Sous-titrage<#else>Doublage</#if></p>
                            </div>
                        </#if>
                    </div>

                    <#if cover?? && !cover>
                        <span class="h6 mt-2 text-truncate-2">${episode.anime.shortName}</span>

                        <p class="text-muted mb-0">Saison ${episode.season?c} |
                            ${getPrefixEpisode(episode.episodeType)} ${episode.number?c}<#if episode.uncensored> non censuré</#if>
                        </p>

                        <p class="text-muted mt-0 mb-0 pb-0"><#if episode.langType == 'SUBTITLES'>Sous-titrage<#else>Doublage</#if></p>
                    </#if>

                    <div class="bg-black bg-opacity-75 position-absolute top-0 start-0 w-100 h-100 mh-100 p-3"
                         style="display: none;" x-show="hover">
                        <#if episode.title??>
                            <div class="h6 text-truncate-2">
                                ${episode.title}
                            </div>
                        </#if>

                        <span class="text-muted">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                                 class="bi bi-calendar4 me-1" viewBox="0 0 16 16">
                                <path d="M3.5 0a.5.5 0 0 1 .5.5V1h8V.5a.5.5 0 0 1 1 0V1h1a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V3a2 2 0 0 1 2-2h1V.5a.5.5 0 0 1 .5-.5M2 2a1 1 0 0 0-1 1v1h14V3a1 1 0 0 0-1-1zm13 3H1v9a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1z"/>
                            </svg>

                            ${episode.releaseDateTime?datetime("yyyy-MM-dd'T'HH:mm:ss")?string("dd/MM/yyyy")}
                        </span>

                        <#if episode.description??>
                            <div class="text-truncate-4 mt-3">
                                ${episode.description}
                            </div>
                        </#if>

                        <div class="mt-3 text-warning fw-bold">
                            <i class="me-2">
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                                     class="bi bi-box-arrow-up-right" viewBox="0 0 16 16">
                                    <path fill-rule="evenodd"
                                          d="M8.636 3.5a.5.5 0 0 0-.5-.5H1.5A1.5 1.5 0 0 0 0 4.5v10A1.5 1.5 0 0 0 1.5 16h10a1.5 1.5 0 0 0 1.5-1.5V7.864a.5.5 0 0 0-1 0V14.5a.5.5 0 0 1-.5.5h-10a.5.5 0 0 1-.5-.5v-10a.5.5 0 0 1 .5-.5h6.636a.5.5 0 0 0 .5-.5"/>
                                    <path fill-rule="evenodd"
                                          d="M16 .5a.5.5 0 0 0-.5-.5h-5a.5.5 0 0 0 0 1h3.793L6.146 9.146a.5.5 0 1 0 .708.708L15 1.707V5.5a.5.5 0 0 0 1 0z"/>
                                </svg>
                            </i>

                            Regarder maintenant
                        </div>
                    </div>
                </div>
            </a>
        </article>
    </div>
</#macro>