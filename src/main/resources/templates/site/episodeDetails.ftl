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


<#assign canonicalUrl = baseUrl + "/animes/" + episodeMapping.anime.slug + "/season-" + episodeMapping.season?c + "/" + episodeMapping.episodeType.slug + "-" + episodeMapping.number?c />

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
                         alt="${su.sanitizeXSS(episodeMapping.anime.shortName)} anime" class="img-fluid w-50 rounded-4"
                         width="480"
                         height="720">
                </a>
            </div>

            <div class="col-md-8 col-12 text-start mt-md-0 mt-3 d-flex flex-column justify-content-center">
                <h1 class="h6 fw-bold mb-0 text-uppercase">${episodeMapping.anime.shortName} - Saison ${episodeMapping.season?c} ${getPrefixEpisode(episodeMapping.episodeType)} ${episodeMapping.number?c}</h1>

                <div class="mt-1">
                    <#list episodeMapping.langTypes as langType>
                        <p class="text-muted mb-0">
                            <@langTypeComponent.display langType=langType />
                        </p>
                    </#list>
                </div>

                <h2 class="mt-3 h6 fw-bold mb-0">${episodeMapping.title}</h2>
                <span class="mt-2">${episodeMapping.description}</span>

                <div class="d-flex justify-content-center mt-3">
                    <#if previousEpisode??>
                        <a href="/animes/${previousEpisode.anime.slug}/season-${previousEpisode.season}/${previousEpisode.episodeType.slug}-${previousEpisode.number?c}"
                           class="btn btn-light ms-0 me-auto d-flex align-items-center">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                                 class="bi bi-chevron-left me-1" viewBox="0 0 16 16">
                                <path fill-rule="evenodd"
                                      d="M11.354 1.646a.5.5 0 0 1 0 .708L5.707 8l5.647 5.646a.5.5 0 0 1-.708.708l-6-6a.5.5 0 0 1 0-.708l6-6a.5.5 0 0 1 .708 0"/>
                            </svg>
                            Épisode précédent
                        </a>
                    </#if>

                    <#if nextEpisode??>
                        <a href="/animes/${nextEpisode.anime.slug}/season-${nextEpisode.season}/${nextEpisode.episodeType.slug}-${nextEpisode.number?c}"
                           class="btn btn-light ms-auto me-0 d-flex align-items-center">
                            Épisode suivant
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                                 class="bi bi-chevron-right ms-1" viewBox="0 0 16 16">
                                <path fill-rule="evenodd"
                                      d="M4.646 1.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1 0 .708l-6 6a.5.5 0 0 1-.708-.708L10.293 8 4.646 2.354a.5.5 0 0 1 0-.708"/>
                            </svg>
                        </a>
                    </#if>
                </div>
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