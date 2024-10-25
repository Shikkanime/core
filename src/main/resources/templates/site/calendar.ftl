<#import "_navigation.ftl" as navigation />
<#import "components/langType.ftl" as langTypeComponent />

<#function getPrefixEpisode(episodeType)>
    <#switch episodeType>
        <#case "EPISODE">
            <#return "Épisode">
        <#case "FILM">
            <#return "Film">
        <#case "SPECIAL">
            <#return "Spécial">
        <#case "SUMMARY">
            <#return "Épisode récapitulatif">
    </#switch>
</#function>

<@navigation.display canonicalUrl="${baseUrl}/calendar">
    <div class="d-flex">
        <a href="${baseUrl}/calendar?date=${previousWeek}"
           class="btn btn-light mt-3 ms-0 me-auto d-flex align-items-center">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                 class="bi bi-chevron-left me-1" viewBox="0 0 16 16">
                <path fill-rule="evenodd"
                      d="M11.354 1.646a.5.5 0 0 1 0 .708L5.707 8l5.647 5.646a.5.5 0 0 1-.708.708l-6-6a.5.5 0 0 1 0-.708l6-6a.5.5 0 0 1 .708 0"/>
            </svg>
            Semaine précédente
        </a>

        <#if nextWeek??>
            <a href="${baseUrl}/calendar?date=${nextWeek}"
               class="btn btn-light mt-3 ms-auto me-0 d-flex align-items-center">
                Semaine suivante
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                     class="bi bi-chevron-right ms-1" viewBox="0 0 16 16">
                    <path fill-rule="evenodd"
                          d="M4.646 1.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1 0 .708l-6 6a.5.5 0 0 1-.708-.708L10.293 8 4.646 2.354a.5.5 0 0 1 0-.708"/>
                </svg>
            </a>
        </#if>
    </div>

    <div class="table-responsive">
        <table class="table table-dark table-borderless my-3">
            <thead>
            <tr>
                <#list weeklyAnimes as dailyAnimes>
                    <th scope="col" style="width: 14.28%" class="text-center">
                        ${dailyAnimes.dayOfWeek}
                    </th>
                </#list>
            </tr>
            </thead>
            <tbody>
            <tr>
                <#list weeklyAnimes as dailyAnimes>
                    <td class="border-start border-end border-dark bg-black">
                        <#list dailyAnimes.releases as release>
                            <#assign isReleased = (release.mappings?? && release.mappings?size > 0)>
                            <#assign isMultipleReleased = isReleased && (release.mappings?size > 1)>

                            <#assign imageUrl = "${apiUrl}/v1/attachments?uuid=${release.anime.uuid}&type=banner">

                            <#if isReleased>
                                <#assign imageUrl = "${apiUrl}/v1/attachments?uuid=${release.mappings?first.uuid}&type=image">
                            </#if>

                            <article x-data="{ hover: false }" class="shikk-element mb-3 position-relative<#if isMultipleReleased> mt-2</#if>">
                                <#if isMultipleReleased>
                                    <div data-multiple-released>
                                        <div class="shikk-element-collection-2"></div>
                                        <div class="shikk-element-collection-1"></div>
                                    </div>
                                </#if>

                                <a href="${release.slug}" class="text-decoration-none text-white" @mouseenter="hover = true" @mouseleave="hover = false">
                                    <div class="position-relative">
                                        <div class="position-relative">
                                            <img loading="lazy"
                                                 src="${imageUrl}"
                                                 alt="${su.sanitizeXSS(release.anime.shortName)} anime"
                                                 class="img-fluid" width="640"
                                                 height="360">

                                            <div class="position-absolute top-0 end-0 p-1">
                                                <div class="d-flex">
                                                    <#list release.platforms as platform>
                                                        <img loading="lazy" src="${baseUrl}/assets/img/platforms/${platform.image}"
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
                                                    <span class="h6 text-truncate-2 mb-1 fw-bold">${su.sanitizeXSS(release.anime.shortName)}</span>
                                                    <#if release.minNumber?? || release.maxNumber?? || release.number??>
                                                        <p class="text-muted mb-0">
                                                            ${getPrefixEpisode(release.episodeType)} <#if isMultipleReleased>${release.minNumber?c} - ${release.maxNumber?c}<#else>${release.number?c}</#if>
                                                        </p>
                                                    </#if>
                                                    <p class="text-muted mt-0 mb-1"><@langTypeComponent.display langType=release.langType /></p>
                                                </div>
                                            </div>
                                        </div>

                                        <div class="overlay" style="display: none;" x-show="hover">
                                            <#if release.anime.description??>
                                                <div class="text-truncate-4">
                                                    ${release.anime.description}
                                                </div>
                                            </#if>

                                            <#if isReleased>
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

        document.querySelectorAll('.img-fluid').forEach(img => {
            const closest = img.closest('.shikk-element');
            if (!closest.querySelector('[data-multiple-released]')) return;

            img.crossOrigin = 'Anonymous';

            const processImage = () => {
                const rgb = getAverageRGB(img);
                closest.querySelectorAll('.shikk-element-collection-1, .shikk-element-collection-2')
                    .forEach(collection => {
                        collection.style.backgroundColor = 'rgb(' + rgb.r + ', ' + rgb.g + ', ' + rgb.b + ')';
                    });
            };

            img.complete ? processImage() : img.onload = processImage;
        });

        function getAverageRGB(imgEl) {
            const blockSize = 5;
            const defaultRGB = {r: 0, g: 0, b: 0};
            const canvas = document.createElement('canvas');
            const context = canvas.getContext('2d');
            if (!context) return defaultRGB;

            const width = canvas.width = imgEl.naturalWidth || imgEl.width;
            const height = canvas.height = imgEl.naturalHeight || imgEl.height;
            context.drawImage(imgEl, 0, 0);

            let data;
            try {
                data = context.getImageData(0, 0, width, height);
            } catch (e) {
                console.error(e);
                return defaultRGB;
            }

            const length = data.data.length;
            let rgb = {r: 0, g: 0, b: 0};
            let count = 0;

            for (let i = 0; i < length; i += blockSize * 4) {
                count++;
                rgb.r += data.data[i];
                rgb.g += data.data[i + 1];
                rgb.b += data.data[i + 2];
            }

            rgb.r = Math.floor(rgb.r / count);
            rgb.g = Math.floor(rgb.g / count);
            rgb.b = Math.floor(rgb.b / count);

            return rgb;
        }
    </script>
</@navigation.display>