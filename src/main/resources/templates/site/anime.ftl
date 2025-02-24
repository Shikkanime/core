<#import "_navigation.ftl" as navigation />
<#import "components/episode-mapping.ftl" as episodeMappingComponent />
<#import "components/langType.ftl" as langTypeComponent />

<#assign canonicalUrl = baseUrl + "/animes/" + anime.slug>

<#if season??>
    <#assign canonicalUrl = canonicalUrl + "/season-" + season.number>
</#if>

<#assign animeSanitized = anime.shortName?html />

<@navigation.display canonicalUrl=canonicalUrl openGraphImage="${apiUrl}/v1/attachments?uuid=${anime.uuid}&type=banner">
    <div class="position-relative">
        <#-- Bottom to top background gradient -->
        <div class="position-absolute w-100"
             style="background: linear-gradient(0deg, rgba(0, 0, 0, 1), rgba(0, 0, 0, 0.75), rgba(0, 0, 0, 0)); height: 400px;"></div>
        <div style="background-image: url('${apiUrl}/v1/attachments?uuid=${anime.uuid}&type=banner'); background-size: cover; background-position: center; height: 400px;"></div>
    </div>

    <div class="container mb-3 anime-infos">
        <div class="row g-3 mt-3">
            <div class="col-md-4 col-12 mt-0 text-center">
                <img src="${apiUrl}/v1/attachments?uuid=${anime.uuid}&type=image"
                     alt="${animeSanitized} anime" class="img-fluid w-50 rounded-4"
                     width="480"
                     height="720">
            </div>

            <div class="col-md-8 col-12 text-start mt-md-0 mt-3 d-flex flex-column justify-content-center">
                <h1 class="h6 fw-bold mb-0 text-uppercase">${animeSanitized}<#if season??> - ${su.toSeasonString(anime.countryCode, season.number)}</#if></h1>

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

                <span class="mt-3">${(anime.description!"Aucune description pour le moment...")?html}</span>

                <button class="btn btn-dark dropdown-toggle mt-3" data-bs-toggle="dropdown" aria-expanded="false">
                    <#assign selectedSeason = anime.seasons?first.number>

                    <#if season??>
                        <#assign selectedSeason = season.number>
                    </#if>

                    ${su.toSeasonString(anime.countryCode, selectedSeason)}
                </button>

                <ul class="dropdown-menu dropdown-menu-dark" style="max-height: 300px; overflow-y: auto;">
                    <#list anime.seasons as season>
                        <li><a class="dropdown-item"
                               href="/animes/${anime.slug}/season-${season.number}">${su.toSeasonString(anime.countryCode, season.number)}</a>
                        </li>
                    </#list>
                </ul>
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

        <#if season??>
            <#assign currentSeason = season.number>
        </#if>

        <#if page??>
            <#assign currentPage = page>
        </#if>

        <div class="d-flex justify-content-center mt-3">
            <#if showLess?? && showLess>
                <a href="/animes/${anime.slug}/season-${currentSeason}?page=${currentPage - 1}"
                   class="btn btn-dark ms-0 me-auto">Page précédente</a>
            </#if>

            <#if showMore?? && showMore>
                <a href="/animes/${anime.slug}/season-${currentSeason}?page=${currentPage + 1}"
                   class="btn btn-dark ms-auto me-0">Page suivante</a>
            </#if>
        </div>
    </#if>
</@navigation.display>