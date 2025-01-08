<#import "_navigation.ftl" as navigation />
<#import "components/episode-duration.ftl" as durationComponent />
<#import "components/langType.ftl" as langTypeComponent />

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
                    <td class="<#if !dailyAnimes?is_first>border-start</#if> <#if !dailyAnimes?is_last>border-end</#if> border-dark bg-black">
                        <#list dailyAnimes.releases as release>
                            <#assign animeSanitized = release.anime.shortName?html />

                            <#assign isReleased = (release.mappings?? && release.mappings?size > 0)>
                            <#assign isMultipleReleased = isReleased && (release.mappings?size > 1)>

                            <#assign imageUrl = "${apiUrl}/v1/attachments?uuid=${release.anime.uuid}&type=banner">

                            <#if isReleased>
                                <#assign imageUrl = "${apiUrl}/v1/attachments?uuid=${release.mappings?first.uuid}&type=image">
                            </#if>

                            <article x-data="{ hover: false }" class="shikk-element mb-3">
                                <a href="${release.slug}" @mouseenter="hover = true" @mouseleave="hover = false">
                                    <div class="shikk-element-content">
                                        <div class="position-relative">
                                            <img loading="lazy"
                                                 src="${imageUrl}"
                                                 alt="${animeSanitized} anime"
                                                 class="shikk-element-content-img" width="640"
                                                 height="360">

                                            <div class="platforms">
                                                <#list release.platforms as platform>
                                                    <img loading="lazy"
                                                         src="${baseUrl}/assets/img/platforms/${platform.image}"
                                                         alt="${platform.name}"
                                                         class="rounded-circle ms-1" width="20"
                                                         height="20">
                                                </#list>
                                            </div>

                                            <#if isReleased>
                                                <#assign totalDuration = 0>
                                                <#list release.mappings as mapping>
                                                    <#assign totalDuration = totalDuration + mapping.duration>
                                                </#list>
                                                <@durationComponent.display duration=totalDuration />
                                            </#if>
                                        </div>

                                        <div class="d-flex align-items-center mt-2">
                                            <span class="text-muted"
                                                  data-release-date-time="${release.releaseDateTime}"></span>
                                            <div class="vr mx-2"></div>
                                            <div class="d-block">
                                                <div class="h6 text-truncate-2 mb-0 fw-bold">${animeSanitized}</div>
                                                <#if release.minNumber?? || release.maxNumber?? || release.number??>
                                                    <p class="text-muted mt-1 mb-0">
                                                        ${su.getEpisodeTypeLabel(release.anime.countryCode, release.episodeType)} <#if isMultipleReleased>${release.minNumber?c} - ${release.maxNumber?c}<#else>${release.number?c}</#if>
                                                    </p>
                                                </#if>
                                                <#list release.langTypes as langType>
                                                    <p class="text-muted my-0"><@langTypeComponent.display langType=langType /></p>
                                                </#list>
                                            </div>
                                        </div>
                                    </div>

                                    <div class="overlay" style="display: none;" x-show="hover">
                                        <#if isReleased && !isMultipleReleased>
                                            <div class="h6 text-truncate-2 fw-bold mb-0">
                                                ${(release.mappings?first.title!"＞︿＜")?html}
                                            </div>
                                        </#if>

                                        <#if isReleased>
                                            <span class="text-muted mt-0 d-flex align-items-center">
                                                <img src="${baseUrl}/assets/img/icons/calendar.svg" alt="Calendar"
                                                     class="me-2">
                                                ${release.mappings?first.releaseDateTime?datetime("yyyy-MM-dd'T'HH:mm:ss")?string("dd/MM/yyyy")}
                                            </span>
                                        </#if>

                                        <#assign description = release.anime.description>

                                        <#if isReleased && !isMultipleReleased && release.mappings?first.description??>
                                            <#assign description = release.mappings?first.description>
                                        </#if>

                                        <#if description??>
                                            <div class="text-truncate-4<#if isReleased> my-2</#if>"
                                                 style="font-size: 0.9rem;">
                                                ${description?html}
                                            </div>
                                        </#if>

                                        <#if isReleased>
                                            <div class="mt-3 text-warning fw-bold d-flex align-items-center">
                                                <img src="${baseUrl}/assets/img/icons/redirect.svg" alt="Redirect"
                                                     class="me-2">
                                                Regarder maintenant
                                            </div>
                                        </#if>
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