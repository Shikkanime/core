<#import "_navigation.ftl" as navigation />

<#assign canonicalUrl = "<link rel=\"canonical\" href=\"https://www.shikkanime.fr/calendar\"/>" />

<@navigation.display header="${canonicalUrl}">
    <div class="table-responsive">
        <table class="table table-dark table-borderless my-5">
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
                    <td style="background-color: #060606">
                        <#list dailyAnimes.releases as release>
                            <article x-data="{ hover: false }">
                                <a href="/animes/${release.anime.slug}" class="text-decoration-none text-white"
                                   @mouseenter="hover = true"
                                   @mouseleave="hover = false">
                                    <div class="position-relative">
                                        <div class="position-relative">
                                            <img src="https://api.shikkanime.fr/v1/attachments?uuid=${release.anime.uuid}&type=banner"
                                                 alt="${su.sanitizeXSS(release.anime.shortName)} anime banner"
                                                 class="img-fluid" width="640"
                                                 height="360">

                                            <div class="position-absolute top-0 end-0 p-1">
                                                <div class="d-flex">
                                                    <#list release.platforms as platform>
                                                        <img src="https://www.shikkanime.fr/assets/img/platforms/${platform.image}"
                                                             alt="${platform.name} platform image"
                                                             class="rounded-circle me-1" width="20"
                                                             height="20">
                                                    </#list>
                                                </div>
                                            </div>
                                        </div>

                                        <div class="mt-1 mb-2">
                                            <span class="h6 text-truncate-2 mb-0">${release.anime.shortName}</span>
                                            <span class="text-muted mt-0"
                                                  data-release-date-time="${release.releaseDateTime}"></span>
                                        </div>

                                        <div class="bg-black bg-opacity-75 position-absolute top-0 start-0 w-100 h-100 mh-100 p-3"
                                             x-show="hover">
                                            <#if release.anime.description??>
                                                <div class="text-truncate-6">
                                                    ${release.anime.description}
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
        <#-- Get release date and time and convert it to the user's timezone -->
        const releaseDateTimeElements = document.querySelectorAll('[data-release-date-time]');

        releaseDateTimeElements.forEach(element => {
            const releaseDateTime = new Date(element.getAttribute('data-release-date-time'));
            element.innerHTML = releaseDateTime.toLocaleTimeString().split(':').slice(0, 2).join(':');
        });
    </script>
</@navigation.display>