<#import "episode-duration.ftl" as durationComponent />
<#import "langType.ftl" as langTypeComponent />

<#macro display episodeMapping>
    <#assign animeSanitized = episodeMapping.anime.shortName?html />

    <article class="shikk-element">
        <a href="/animes/${episodeMapping.anime.slug}/season-${episodeMapping.season?c}/${episodeMapping.episodeType.slug}-${episodeMapping.number?c}">
            <div class="shikk-element-content">
                <p class="mb-1 text-uppercase fw-bold d-flex align-items-center">
                    <#nested 0>
                </p>

                <div class="row">
                    <div class="col-6">
                        <div class="position-relative">
                            <img loading="lazy" src="${apiUrl}/v1/attachments?uuid=${episodeMapping.uuid}&type=BANNER"
                                 alt="${animeSanitized} episode preview"
                                 class="shikk-element-content-img"
                                 width="640" height="360">

                            <div class="platforms">
                                <#assign uniqueSources = [] />

                                <#list episodeMapping.sources as source>
                                    <#if uniqueSources?filter(s -> s.platform.id == source.platform.id)?size == 0>
                                        <#assign uniqueSources = uniqueSources + [source]>
                                    </#if>
                                </#list>

                                <#list uniqueSources as source>
                                    <img loading="lazy" src="${baseUrl}/assets/img/platforms/${source.platform.image}"
                                         alt="${source.platform.name}"
                                         class="rounded-circle ms-1" width="20"
                                         height="20">
                                </#list>
                            </div>

                            <@durationComponent.display duration=episodeMapping.duration />
                        </div>
                    </div>

                    <div class="col-6 d-flex flex-column justify-content-center">
                        <div class="h6 mt-2 mb-0 text-truncate-2 fw-bold">
                            ${su.toEpisodeMappingString(episodeMapping, false, false)}&NonBreakingSpace;-&NonBreakingSpace;${(episodeMapping.title!"＞︿＜")?html}
                        </div>

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
                </div>
            </div>
        </a>
    </article>
</#macro>