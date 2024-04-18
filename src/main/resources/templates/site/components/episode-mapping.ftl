<#import "langType.ftl" as langTypeComponent />

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

<#macro display episodeMapping desktopColSize mobileColSize cover>
    <div class="${desktopColSize} ${mobileColSize}" x-data="{ hover: false }" @mouseenter="hover = true"
         @mouseleave="hover = false">
        <article class="rounded-4 card">
            <a href="${episodeMapping.variants?first.url}" target="_blank" class="text-decoration-none text-white">
                <div class="position-relative">
                    <div class="position-relative">
                        <img src="${apiUrl}/v1/attachments?uuid=${episodeMapping.uuid}&type=image"
                             alt="${su.sanitizeXSS(episodeMapping.anime.shortName)} episode preview image"
                             class="<#if cover>w-100 object-fit-cover<#else>img-fluid</#if> rounded-top-4"
                             width="640" height="360">

                        <div class="position-absolute top-0 end-0 mt-1 d-flex">
                            <#list episodeMapping.platforms as platform>
                                <img src="${baseUrl}/assets/img/platforms/${platform.image}"
                                     alt="${platform.name} platform image"
                                     class="rounded-circle me-1" width="20"
                                     height="20">
                            </#list>
                        </div>
                    </div>

                    <div class="mx-2 mb-1">
                        <h6 class="h6 mt-2 mb-1 text-truncate-2 fw-bold">${episodeMapping.anime.shortName}</h6>

                        <p class="text-muted mb-0">Saison ${episodeMapping.season?c}
                            | ${getPrefixEpisode(episodeMapping.episodeType)} ${episodeMapping.number?c}</p>

                        <#list episodeMapping.langTypes as langTypes>
                            <p class="text-muted mt-0 mb-0"><@langTypeComponent.display langType=langTypes /></p>
                        </#list>
                    </div>

                    <div class="bg-black bg-opacity-75 bg-blur position-absolute top-0 start-0 w-100 h-100 mh-100 p-3 rounded-4"
                         style="display: none;" x-show="hover">
                        <#if episodeMapping.title??>
                            <div class="h6 text-truncate-2 fw-bold">
                                ${episodeMapping.title}
                            </div>
                        </#if>

                        <span class="text-muted">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                                 class="bi bi-calendar4 me-1" viewBox="0 0 16 16">
                                <path d="M3.5 0a.5.5 0 0 1 .5.5V1h8V.5a.5.5 0 0 1 1 0V1h1a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V3a2 2 0 0 1 2-2h1V.5a.5.5 0 0 1 .5-.5M2 2a1 1 0 0 0-1 1v1h14V3a1 1 0 0 0-1-1zm13 3H1v9a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1z"/>
                            </svg>

                            ${episodeMapping.releaseDateTime?datetime("yyyy-MM-dd'T'HH:mm:ss")?string("dd/MM/yyyy")}
                        </span>

                        <#if episodeMapping.description??>
                            <div class="text-truncate-4 mt-3">
                                ${episodeMapping.description}
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