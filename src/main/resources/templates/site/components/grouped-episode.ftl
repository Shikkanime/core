<#import "episode-duration.ftl" as durationComponent />
<#import "langType.ftl" as langTypeComponent />

<#macro display groupedEpisode desktopColSize mobileColSize cover>
    <#assign animeSanitized = groupedEpisode.anime.name?html />

    <div class="${desktopColSize} ${mobileColSize}" x-data="{ hover: false }" @mouseenter="hover = true"
         @mouseleave="hover = false">
        <article class="shikk-element">
            <a href="${groupedEpisode.internalUrl}">
                <div class="shikk-element-content">
                    <div class="position-relative">
                        <img loading="lazy" src="${apiUrl}/v1/attachments?uuid=${groupedEpisode.mappings?first}&type=image"
                             alt="${animeSanitized} episode preview"
                             class="shikk-element-content-img <#if cover>responsive</#if>"
                             width="640" height="360">

                        <div class="platforms">
                            <#list groupedEpisode.platforms as platform>
                                <img loading="lazy" src="${baseUrl}/assets/img/platforms/${platform.image}"
                                     alt="${platform.name}"
                                     class="rounded-circle ms-1" width="20"
                                     height="20">
                            </#list>
                        </div>

                        <#if groupedEpisode.duration??>
                            <@durationComponent.display duration=groupedEpisode.duration />
                        </#if>
                    </div>

                    <div class="h6 mt-2 mb-1 text-truncate-2 fw-bold">${animeSanitized}</div>

                    <p class="text-muted mb-0">
                        ${su.toEpisodeGroupedString(groupedEpisode, true, true)}
                    </p>

                    <#list groupedEpisode.langTypes as langType>
                        <p class="text-muted my-0"><@langTypeComponent.display langType=langType /></p>
                    </#list>
                </div>

                <div class="overlay" style="display: none;" x-show="hover">
                    <div class="h6 text-truncate-2 fw-bold mb-0">
                        ${(groupedEpisode.title!"＞︿＜")?html}
                    </div>

                    <span class="text-muted mt-0 d-flex align-items-center">
                        <img src="${baseUrl}/assets/img/icons/calendar.svg" alt="Calendar" class="me-2">
                        ${groupedEpisode.releaseDateTime?datetime("yyyy-MM-dd'T'HH:mm:ss")?string("dd/MM/yyyy")}
                    </span>

                    <#if groupedEpisode.description??>
                        <div class="text-truncate-4 my-2" style="font-size: 0.9rem;">
                            ${groupedEpisode.description?html}
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