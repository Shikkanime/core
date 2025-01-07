<#import "_navigation.ftl" as navigation />
<#import "components/langType.ftl" as langTypeComponent />
<#import "components/simple-episode-mapping.ftl" as simpleEpisodeMapping />

<#assign canonicalUrl = baseUrl + "/animes/" + episodeMapping.anime.slug + "/season-" + episodeMapping.season?c + "/" + episodeMapping.episodeType.slug + "-" + episodeMapping.number?c />
<#assign animeSanitized = episodeMapping.anime.shortName?html />

<@navigation.display canonicalUrl=canonicalUrl openGraphImage="${apiUrl}/v1/attachments?uuid=${episodeMapping.uuid}&type=image">
    <div class="position-relative">
        <#-- Bottom to top background gradient -->
        <div class="position-absolute w-100"
             style="background: linear-gradient(0deg, rgba(0, 0, 0, 1), rgba(0, 0, 0, 0.75), rgba(0, 0, 0, 0)); height: 400px;"></div>
        <div style="background-image: url('${apiUrl}/v1/attachments?uuid=${episodeMapping.uuid}&type=image'); background-size: cover; background-position: center; height: 400px;"></div>
    </div>

    <div class="container mb-3 anime-infos">
        <div class="row g-3 mt-3">
            <div class="col-md-4 col-12 mt-0 text-center">
                <a class="text-decoration-none"
                   href="/animes/${episodeMapping.anime.slug}/season-${episodeMapping.season?c}">
                    <img src="${apiUrl}/v1/attachments?uuid=${episodeMapping.anime.uuid}&type=image"
                         alt="${animeSanitized} anime" class="img-fluid w-50 rounded-4"
                         width="480"
                         height="720">
                </a>
            </div>

            <div class="col-md-8 col-12 text-start mt-md-0 mt-3 d-flex flex-column justify-content-center">
                <h1 class="h6 fw-bold mb-0 text-uppercase">
                    <a href="/animes/${episodeMapping.anime.slug}/season-${episodeMapping.season?c}"
                       class="text-white text-decoration-none">${animeSanitized}&NonBreakingSpace;-&NonBreakingSpace;${su.toSeasonString(episodeMapping.anime.countryCode, episodeMapping.season)}</a>
                    &NonBreakingSpace;${su.toEpisodeMappingString(episodeMapping, false, false)}
                </h1>

                <div class="mt-1">
                    <#list episodeMapping.langTypes as langType>
                        <p class="text-muted mb-0">
                            <@langTypeComponent.display langType=langType />
                        </p>
                    </#list>
                </div>

                <h2 class="mt-3 h6 fw-bold mb-0">${(episodeMapping.title!"＞︿＜")?html}</h2>
                <span class="my-2">${(episodeMapping.description!"Aucune description pour le moment...")?html}</span>

                <#if previousEpisode?? || nextEpisode??>
                    <div class="row g-3">
                        <div class="col-12 col-md-6 px-0 px-md-1">
                            <#if previousEpisode??>
                                <@simpleEpisodeMapping.display episodeMapping=previousEpisode label="Épisode précédent" />
                            </#if>
                        </div>

                        <div class="col-12 col-md-6 px-0 px-md-1">
                            <#if nextEpisode??>
                                <@simpleEpisodeMapping.display episodeMapping=nextEpisode label="Épisode suivant" />
                            </#if>
                        </div>
                    </div>
                </#if>
            </div>
        </div>
    </div>

    <div class="row g-3 justify-content-center">
        <#list episodeMapping.platforms as platform>
            <div class="col-12 col-md-${12 / episodeMapping.platforms?size}">
                <article class="shikk-element d-flex justify-content-center align-items-center" style="height: 100px;">
                    <#assign episodePlatformUrl = episodeMapping.variants?filter(v -> v.platform.id == platform.id)?first.url>

                    <a href="${episodePlatformUrl}" target="_blank" rel="noopener noreferrer"
                       class="text-center text-decoration-none text-white fw-bold">
                        Redirection vers
                        <br>
                        <img loading="lazy" src="${baseUrl}/assets/img/platforms/${platform.image}"
                             alt="${platform.name}"
                             class="rounded-circle mx-1" width="20"
                             height="20"> ${platform.name}
                    </a>
                </article>
            </div>
        </#list>
    </div>
</@navigation.display>