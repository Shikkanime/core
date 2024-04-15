<#import "langType.ftl" as langTypeComponent />

<#macro display anime>
    <div class="col-md-2 col-6 mt-0 mb-4">
        <article x-data="{ hover: false }" class="rounded-4 card">
            <a href="/animes/${anime.slug}" class="text-decoration-none text-white" @mouseenter="hover = true"
               @mouseleave="hover = false">
                <div class="position-relative">
                    <img src="${apiUrl}/v1/attachments?uuid=${anime.uuid}&type=image"
                         alt="${su.sanitizeXSS(anime.shortName)} anime image" class="img-fluid rounded-top-4"
                         width="480"
                         height="720">

                    <div class="mt-2 mx-2 mb-1">
                        <span class="h6 text-truncate-2 fw-bold mb-0">${anime.shortName}</span>

                        <#list anime.langTypes as langType>
                            <p class="text-muted mt-0 mb-0"><@langTypeComponent.display langType=langType /></p>
                        </#list>
                    </div>

                    <div class="bg-black bg-opacity-75 bg-blur position-absolute top-0 start-0 w-100 h-100 mh-100 p-3 rounded-top-4"
                         x-show="hover">
                        <div class="h6 text-truncate-2 fw-bold">
                            ${anime.shortName?upper_case}
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
</#macro>