<#import "episode-duration.ftl" as durationComponent />
<#import "langType.ftl" as langTypeComponent />

<#macro display episodeMapping desktopColSize mobileColSize cover showAnime=true showSeason=true>
    <#assign animeSanitized = episodeMapping.anime.shortName?html />

    <div class="${desktopColSize} ${mobileColSize}" x-data="{ hover: false }" @mouseenter="hover = true"
         @mouseleave="hover = false">
        <article class="shikk-element">
            <a href="/animes/${episodeMapping.anime.slug}/season-${episodeMapping.season?c}/${episodeMapping.episodeType.slug}-${episodeMapping.number?c}">
                <div class="shikk-element-content">
                    <div class="position-relative">
                        <img loading="lazy" src="${apiUrl}/v1/attachments?uuid=${episodeMapping.uuid}&type=BANNER"
                             alt="${animeSanitized} episode preview"
                             class="shikk-element-content-img <#if cover>responsive</#if>"
                             width="640" height="360">

                        <div class="platforms">
                            <#assign uniqueSources = [] />

                            <#list episodeMapping.sources as source>
                                <#if uniqueSources?filter(s -> s.platform.id == source.platform.id)?size == 0>
                                    <#assign uniqueSources = uniqueSources + [source]>
                                </#if>
                            </#list>

                            <#list uniqueSources as sources>
                                <img loading="lazy" src="${baseUrl}/assets/img/platforms/${sources.platform.image}"
                                     alt="${sources.platform.name}"
                                     class="rounded-circle ms-1" width="20"
                                     height="20">
                            </#list>
                        </div>

                        <@durationComponent.display duration=episodeMapping.duration />
                    </div>

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

                    <#assign uniqueSources = [] />

                    <#list episodeMapping.sources as source>
                        <#if uniqueSources?filter(s -> s.langType == source.langType)?size == 0>
                            <#assign uniqueSources = uniqueSources + [source]>
                        </#if>
                    </#list>

                    <#list uniqueSources as source>
                        <p class="text-muted my-0"><@langTypeComponent.display langType=source.langType /></p>
                    </#list>
                </div>

                <div class="overlay" style="display: none;" x-show="hover">
                    <div class="h6 text-truncate-2 fw-bold mb-0">
                        ${(episodeMapping.title!"＞︿＜")?html}
                    </div>

                    <span class="text-muted mt-0 d-flex align-items-center">
                        <img src="${baseUrl}/assets/img/icons/calendar.svg" alt="Calendar" class="me-2">
                        ${episodeMapping.releaseDateTime?datetime("yyyy-MM-dd'T'HH:mm:ss")?string("dd/MM/yyyy")}
                    </span>

                    <#if episodeMapping.description??>
                        <div class="text-truncate-4 my-2" style="font-size: 0.9rem;">
                            ${episodeMapping.description?html}
                        </div>
                    </#if>

                    <div class="text-warning fw-bold d-flex align-items-center">
                        <img src="${baseUrl}/assets/img/icons/redirect.svg" alt="Redirect" class="me-2">
                        Regarder maintenant
                    </div>
                </div>
            </a>
        </article>
    </div>
</#macro>