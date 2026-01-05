<#import "_navigation.ftl" as navigation />
<#import "components/episode-mapping.ftl" as episodeMappingComponent />
<#import "components/langType.ftl" as langTypeComponent />

<#assign canonicalUrl = baseUrl + "/animes/" + anime.slug>

<#if season??>
    <#assign canonicalUrl = canonicalUrl + "/season-" + season.number>
</#if>

<#assign animeSanitized = anime.shortName?html />

<@navigation.display canonicalUrl=canonicalUrl openGraphImage="${apiUrl}/v1/attachments?uuid=${anime.uuid}&type=BANNER">
    <div class="position-relative">
        <#-- Bottom to top background gradient -->
        <div class="position-absolute w-100"
             style="background: linear-gradient(0deg, rgba(0, 0, 0, 1), rgba(0, 0, 0, 0.75), rgba(0, 0, 0, 0)); height: 400px;"></div>
        <div style="background-image: url('${apiUrl}/v1/attachments?uuid=${anime.uuid}&type=BANNER'); background-size: cover; background-position: center; height: 400px;"></div>
    </div>

    <div class="container mb-3 anime-infos">
        <div class="row g-3 mt-3">
            <div class="col-md-4 col-12 mt-0 text-center">
                <img src="${apiUrl}/v1/attachments?uuid=${anime.uuid}&type=THUMBNAIL"
                     alt="${animeSanitized} anime" class="img-fluid w-50 rounded-4"
                     width="480"
                     height="720">
            </div>

            <div class="col-md-8 col-12 text-start mt-md-0 mt-3 d-flex flex-column justify-content-center">
                <h1 class="h6 fw-bold mb-0 text-uppercase">${animeSanitized}<#if season??> - ${su.toSeasonString(anime.countryCode, season.number?string)}</#if></h1>

                <div class="mt-1">
                    <#list anime.langTypes as langType>
                        <p class="text-muted mb-0">
                            <@langTypeComponent.display langType=langType />
                        </p>
                    </#list>
                </div>

                <#if (anime.simulcasts?size > 0)>
                    <div class="mt-1 row g-3" x-data="{ showMore: false }">
                        <#list anime.simulcasts as simulcast>
                            <div class="col-4 col-md-2" <#if (simulcast?index > 4)> x-show="showMore" style="display: none" <#else> x-show="true"</#if>>
                                <a href="/catalog/${simulcast.slug}"
                                   class="btn btn-dark w-100 h-100 text-center align-content-around">${simulcast.label}</a>
                            </div>
                        </#list>

                        <#if (anime.simulcasts?size > 5)>
                            <div class="col-4 col-md-2">
                                <button class="btn btn-outline-light w-100 h-100 text-center align-content-around"
                                        @click="showMore = !showMore">
                                    <span x-show="!showMore">Voir plus</span>
                                    <span x-show="showMore">Voir moins</span>
                                </button>
                            </div>
                        </#if>
                    </div>
                </#if>

                <span class="my-3">${(anime.description!"Aucune description pour le moment...")?html}</span>

                <div class="row g-3">
                    <div class="col-12 col-md-9">
                        <button class="btn btn-dark dropdown-toggle w-100" data-bs-toggle="dropdown" aria-expanded="false">
                            <#assign selectedSeason = anime.seasons?first.number>

                            <#if season??>
                                <#assign selectedSeason = season.number>
                            </#if>

                            ${su.toSeasonString(anime.countryCode, selectedSeason?string)}
                        </button>

                        <ul class="dropdown-menu dropdown-menu-dark" style="max-height: 300px; overflow-y: auto;">
                            <#list anime.seasons as season>
                                <li><a class="dropdown-item"
                                       href="/animes/${anime.slug}/season-${season.number}">${su.toSeasonString(anime.countryCode, season.number?string)}</a>
                                </li>
                            </#list>
                        </ul>
                    </div>

                    <div class="col-12 col-md-3">
                        <#assign selectedSort = 'oldest'>

                        <#if sort??>
                            <#assign selectedSort = sort>
                        </#if>

                        <button class="btn btn-dark dropdown-toggle w-100" data-bs-toggle="dropdown" aria-expanded="false">
                            <#if selectedSort == 'newest'>
                                Les plus récents
                            <#else>
                                Les plus anciens
                            </#if>
                        </button>

                        <ul class="dropdown-menu dropdown-menu-dark">
                            <#assign currentSeason = anime.seasons?first.number>
                            <#assign selectedSort = 'oldest'>

                            <#if season??>
                                <#assign currentSeason = season.number>
                            </#if>

                            <li><a class="dropdown-item" href="/animes/${anime.slug}/season-${currentSeason}?page=1&sort=oldest">Les plus anciens</a></li>
                            <li><a class="dropdown-item" href="/animes/${anime.slug}/season-${currentSeason}?page=1&sort=newest">Les plus récents</a></li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="row g-3 justify-content-center">
        <#list episodeMappings as episodeMapping>
            <@episodeMappingComponent.display episodeMapping=episodeMapping cover=false desktopColSize="col-md-2" mobileColSize="col-6" showAnime=false showSeason=false />
        </#list>
    </div>

    <#if (showMore?? && showMore) || (showLess?? && showLess)>
        <#assign currentSeason = anime.seasons?first.number>
        <#assign currentPage = 1>
        <#assign selectedSort = 'oldest'>

        <#if season??>
            <#assign currentSeason = season.number>
        </#if>

        <#if page??>
            <#assign currentPage = page>
        </#if>

        <#if sort??>
            <#assign selectedSort = sort>
        </#if>

        <div class="d-flex justify-content-center mt-3">
            <#if showLess?? && showLess>
                <a href="/animes/${anime.slug}/season-${currentSeason}?page=${currentPage - 1}&sort=${selectedSort}"
                   class="btn btn-dark ms-0 me-auto d-flex align-items-center">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                         class="bi bi-chevron-left me-1" viewBox="0 0 16 16">
                        <path fill-rule="evenodd"
                              d="M11.354 1.646a.5.5 0 0 1 0 .708L5.707 8l5.647 5.646a.5.5 0 0 1-.708.708l-6-6a.5.5 0 0 1 0-.708l6-6a.5.5 0 0 1 .708 0"/>
                    </svg>
                    Page précédente
                </a>
            </#if>

            <#if showMore?? && showMore>
                <a href="/animes/${anime.slug}/season-${currentSeason}?page=${currentPage + 1}&sort=${selectedSort}"
                   class="btn btn-dark ms-auto me-0 d-flex align-items-center">
                    Page suivante
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                         class="bi bi-chevron-right ms-1" viewBox="0 0 16 16">
                        <path fill-rule="evenodd"
                              d="M4.646 1.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1 0 .708l-6 6a.5.5 0 0 1-.708-.708L10.293 8 4.646 2.354a.5.5 0 0 1 0-.708"/>
                    </svg>
                </a>
            </#if>
        </div>
    </#if>

    <#if anime.jsonLd??>
        <script type="application/ld+json">${anime.jsonLd}</script>
    </#if>
</@navigation.display>