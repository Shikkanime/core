<#import "_navigation.ftl" as navigation />
<#import "components/langType.ftl" as langTypeComponent />

<@navigation.display canonicalUrl="${baseUrl}/calendar">
    <div class="table-responsive">
        <table class="table table-dark table-borderless my-3">
            <thead>
            <tr>
                <#list weeklyAnimes as dailyAnimes>
                    <th scope="col" class="text-center" style="width: 14.28%">
                        ${dailyAnimes.dayOfWeek}
                    </th>
                </#list>
            </tr>
            </thead>
            <tbody>
            <tr>
                <#list weeklyAnimes as dailyAnimes>
                    <td class="bg-black border-start border-end border-dark">
                        <#list dailyAnimes.releases as release>
                            <article x-data="{ hover: false }" class="shikk-element mb-3">
                                <a href="/animes/${release.anime.slug}" class="text-decoration-none text-white"
                                   @mouseenter="hover = true"
                                   @mouseleave="hover = false">
                                    <div class="position-relative">
                                        <div class="position-relative">
                                            <img loading="lazy"
                                                 src="${apiUrl}/v1/attachments?uuid=${release.anime.uuid}&type=banner"
                                                 alt="${su.sanitizeXSS(release.anime.shortName)} anime"
                                                 class="img-fluid rounded-top-4" width="640"
                                                 height="360">

                                            <div class="position-absolute top-0 end-0 p-1">
                                                <div class="d-flex">
                                                    <#list release.platforms as platform>
                                                        <img src="${baseUrl}/assets/img/platforms/${platform.image}"
                                                             alt="${platform.name}"
                                                             class="rounded-circle me-1" width="20"
                                                             height="20">
                                                    </#list>
                                                </div>
                                            </div>
                                        </div>

                                        <div class="mx-2">
                                            <div class="d-flex align-items-center py-1">
                                                <span class="text-muted"
                                                      data-release-date-time="${release.releaseDateTime}"></span>
                                                <div class="vr mx-2"></div>
                                                <div class="d-block mt-2">
                                                    <span class="h6 text-truncate-2 mb-0 fw-bold">${release.anime.shortName}</span>
                                                    <p class="text-muted mt-0 mb-1"><@langTypeComponent.display langType=release.langType /></p>
                                                </div>
                                            </div>
                                        </div>

                                        <div class="bg-black bg-opacity-75 bg-blur position-absolute top-0 start-0 w-100 h-100 mh-100 p-3 rounded-4"
                                             style="display: none;" x-show="hover">
                                            <#if release.anime.description??>
                                                <div class="text-truncate-4">
                                                    ${release.anime.description}
                                                </div>
                                            </#if>

                                            <#if release.variant??>
                                                <div class="mt-3 text-warning fw-bold">
                                                    <i class="me-2">
                                                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                                                             fill="currentColor"
                                                             class="bi bi-box-arrow-up-right" viewBox="0 0 16 16">
                                                            <path fill-rule="evenodd"
                                                                  d="M8.636 3.5a.5.5 0 0 0-.5-.5H1.5A1.5 1.5 0 0 0 0 4.5v10A1.5 1.5 0 0 0 1.5 16h10a1.5 1.5 0 0 0 1.5-1.5V7.864a.5.5 0 0 0-1 0V14.5a.5.5 0 0 1-.5.5h-10a.5.5 0 0 1-.5-.5v-10a.5.5 0 0 1 .5-.5h6.636a.5.5 0 0 0 .5-.5"/>
                                                            <path fill-rule="evenodd"
                                                                  d="M16 .5a.5.5 0 0 0-.5-.5h-5a.5.5 0 0 0 0 1h3.793L6.146 9.146a.5.5 0 1 0 .708.708L15 1.707V5.5a.5.5 0 0 0 1 0z"/>
                                                        </svg>
                                                    </i>

                                                    Regarder maintenant
                                                </div>
                                            </#if>
                                        </div>
                                    </div>
                                </a>
                            </article>
                        </#list>
                    </td>
                </#list>
            </tr>
            </tbody>
        </table>
    </div>

    <script>
        document.querySelectorAll('[data-release-date-time]').forEach(element => {
            const releaseDateTime = new Date(element.getAttribute('data-release-date-time'));
            element.textContent = releaseDateTime.toLocaleTimeString().split(':').slice(0, 2).join(':');
        });
    </script>
</@navigation.display>