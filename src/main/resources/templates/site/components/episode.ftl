<#function getPrefixEpisode(episodeType)>
    <#switch episodeType>
        <#case "EPISODE">
            <#return "Épisode">
        <#case "FILM">
            <#return "Film">
        <#case "SPECIAL">
            <#return "Spécial">
    </#switch>
</#function>

<#macro display episode>
    <div class="col-md-2 col-6 mt-0">
        <article x-data="{ hover: false }">
            <a href="${episode.url}" target="_blank" class="text-decoration-none text-white" @mouseenter="hover = true"
               @mouseleave="hover = false">
                <div class="position-relative">
                    <div class="position-relative">
                        <img loading="lazy" data-src="https://api.shikkanime.fr/v1/attachments?uuid=${episode.uuid}&type=image"
                             alt="${episode.anime.shortName?replace("\"", "'")} episode preview image"
                             class="w-100<#if episode.uncensored> blur</#if>">
                        <img src="https://www.shikkanime.fr/assets/img/platforms/${episode.platform.image}"
                             alt="${episode.platform.name()} platform image"
                             class="position-absolute top-0 end-0 rounded-circle me-1 mt-1" width="24"
                             height="24">
                    </div>

                    <span class="h6 mt-2 text-truncate-2">${episode.anime.shortName}</span>

                    <p class="text-muted mb-0">Saison ${episode.season?c} |
                        ${getPrefixEpisode(episode.episodeType)} ${episode.number?c}<#if episode.uncensored> non censuré</#if>
                    </p>

                    <p class="text-muted mt-0"><#if episode.langType == 'SUBTITLES'>Sous-titrage<#else>Doublage</#if></p>

                    <#if episode.title?? || episode.description??>
                        <div class="bg-black bg-opacity-75 position-absolute top-0 start-0 w-100 h-100 mh-100 p-3"
                             x-show="hover">
                            <#if episode.title??>
                                <div class="h6 text-truncate-2">
                                    ${episode.title}
                                </div>
                            </#if>

                            <span class="text-muted">
                                <i class="bi bi-calendar4 me-1"></i>
                                ${episode.releaseDateTime?datetime("yyyy-MM-dd'T'HH:mm:ss")?string("dd/MM/yyyy")}
                            </span>

                            <#if episode.description??>
                                <div class="text-truncate-6 mt-3">
                                    ${episode.description}
                                </div>
                            </#if>
                        </div>
                    </#if>
                </div>
            </a>
        </article>
    </div>
</#macro>