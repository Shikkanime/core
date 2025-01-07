<#import "langType.ftl" as langTypeComponent />

<#macro display anime>
    <#assign animeSanitized = anime.shortName?html />

    <div class="col-md-2 col-6 mt-0 mb-4">
        <article x-data="{ hover: false }" class="shikk-element">
            <a href="/animes/${anime.slug}" @mouseenter="hover = true" @mouseleave="hover = false">
                <div class="shikk-element-content">
                    <img loading="lazy" src="${apiUrl}/v1/attachments?uuid=${anime.uuid}&type=image" alt="${animeSanitized} anime" width="480" height="720" class="shikk-element-content-img">

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