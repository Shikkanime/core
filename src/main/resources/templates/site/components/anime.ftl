<#import "langType.ftl" as langTypeComponent />

<#macro display anime>
    <#assign animeSanitized = anime.shortName?html />

    <div class="col-md-2 col-6 mt-0 mb-4">
        <article x-data="{ hover: false }" class="shikk-element">
            <a href="/animes/${anime.slug}" @mouseenter="hover = true" @mouseleave="hover = false">
                <div class="shikk-element-content">
                    <div class="position-relative">
                        <img loading="lazy" src="${apiUrl}/v1/attachments?uuid=${anime.uuid}&type=image"
                             alt="${animeSanitized} anime" width="480" height="720" class="shikk-element-content-img">

                        <div class="platforms">
                            <#assign platforms = []>

                            <#list anime.platformIds as platform>
                                <#if platforms?filter(p -> p.id == platform.platform.id)?size != 0>
                                    <#continue>
                                <#else>
                                    <#assign platforms = platforms + [platform.platform]>
                                </#if>
                            </#list>

                            <#list platforms as platform>
                                <img loading="lazy" src="${baseUrl}/assets/img/platforms/${platform.image}"
                                     alt="${platform.name}"
                                     class="rounded-circle ms-1" width="20"
                                     height="20">
                            </#list>
                        </div>
                    </div>

                    <span class="h6 text-truncate-2 fw-bold mt-2 mb-0">${animeSanitized}</span>

                    <#list anime.langTypes as langType>
                        <p class="text-muted my-0"><@langTypeComponent.display langType=langType /></p>
                    </#list>
                </div>

                <div class="overlay" style="display: none;" x-show="hover">
                    <div class="h6 text-truncate-2 fw-bold">
                        ${animeSanitized?upper_case}
                    </div>

                    <hr>

                    <#if anime.description??>
                        <div class="text-truncate-6">
                            ${anime.description?html}
                        </div>
                    </#if>
                </div>
            </a>
        </article>
    </div>
</#macro>