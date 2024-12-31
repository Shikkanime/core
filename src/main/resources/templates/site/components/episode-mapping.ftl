<#import "langType.ftl" as langTypeComponent />

<#macro display episodeMapping desktopColSize mobileColSize cover showAnime=true showSeason=true>
    <#assign animeSanitized = episodeMapping.anime.shortName?html />

    <div class="${desktopColSize} ${mobileColSize}" x-data="{ hover: false }" @mouseenter="hover = true"
         @mouseleave="hover = false">
        <article class="shikk-element">
            <a href="/animes/${episodeMapping.anime.slug}/season-${episodeMapping.season?c}/${episodeMapping.episodeType.slug}-${episodeMapping.number?c}"
               class="text-decoration-none text-white">
                <div class="position-relative">
                    <div class="position-relative">
                        <img loading="lazy" src="${apiUrl}/v1/attachments?uuid=${episodeMapping.uuid}&type=image"
                             alt="${animeSanitized} episode preview"
                             class="<#if cover>w-100 object-fit-cover<#else>img-fluid</#if>"
                             width="640" height="360">

                        <div class="position-absolute top-0 end-0 mt-1 d-flex">
                            <#list episodeMapping.platforms as platform>
                                <img loading="lazy" src="${baseUrl}/assets/img/platforms/${platform.image}"
                                     alt="${platform.name}"
                                     class="rounded-circle me-1" width="20"
                                     height="20">
                            </#list>
                        </div>
                    </div>

                    <div class="mx-2 mb-1">
                        <#if showAnime>
                            <div class="h6 mt-2 mb-1 text-truncate-2 fw-bold">${animeSanitized}</div>

                            <p class="text-muted mb-0">
                                ${su.toEpisodeMappingString(episodeMapping, showSeason, true)}
                            </p>
                        <#else>
                            <div class="h6 mt-2 mb-0 text-truncate-2 fw-bold">
                                ${su.toEpisodeMappingString(episodeMapping, showSeason, true)}
                            </div>
                        </#if>

                        <#list episodeMapping.langTypes as langTypes>
                            <p class="text-muted mt-0 mb-0"><@langTypeComponent.display langType=langTypes /></p>
                        </#list>
                    </div>

                    <div class="overlay" style="display: none;" x-show="hover">
                        <div class="h6 text-truncate-2 fw-bold mb-0">
                            ${(episodeMapping.title!"＞︿＜")?html}
                        </div>

                        <span class="text-muted mt-0">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                                 class="bi bi-calendar4 me-2" viewBox="0 0 16 16">
                                <path d="M3.5 0a.5.5 0 0 1 .5.5V1h8V.5a.5.5 0 0 1 1 0V1h1a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V3a2 2 0 0 1 2-2h1V.5a.5.5 0 0 1 .5-.5M2 2a1 1 0 0 0-1 1v1h14V3a1 1 0 0 0-1-1zm13 3H1v9a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1z"/>
                            </svg>

                            ${episodeMapping.releaseDateTime?datetime("yyyy-MM-dd'T'HH:mm:ss")?string("dd/MM/yyyy")}
                        </span>

                        <#if episodeMapping.description??>
                            <div class="text-truncate-4 my-2 m-0" style="font-size: 0.9rem;">
                                ${episodeMapping.description?html}
                            </div>
                        </#if>

                        <div class="text-warning fw-bold">
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