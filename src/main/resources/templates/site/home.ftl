<#import "_navigation.ftl" as navigation />
<#import "components/episode-mapping.ftl" as episodeMappingComponent />
<#import "components/grouped-episode.ftl" as groupedEpisodeComponent />
<#import "components/anime.ftl" as animeComponent />

<@navigation.display canonicalUrl="${baseUrl}">
    <div x-data="{
        allSearchTypes: ['SUBTITLES', 'VOICE'],
        searchTypes: <#if searchTypes?? && searchTypes?has_content>[<#list searchTypes?split(',') as searchType>'${searchType}'<#if searchType_has_next>,</#if></#list>]<#else>['SUBTITLES', 'VOICE']</#if>
    }" x-init="
    $watch('searchTypes', (value) => {
        if (value.length > 0 && value.length < allSearchTypes.length) {
            window.location.href = '${baseUrl}/?searchTypes=' + value.join(',');
        } else {
            window.location.href = '${baseUrl}/';
        }
    });
    ">
        <div class="d-flex align-items-center my-3">
            <h1 class="h3 mb-0">Nouveaux épisodes</h1>

            <div class="d-none d-md-flex ms-3 me-auto">
                <div class="form-check me-3">
                    <input class="form-check-input" type="checkbox" value="SUBTITLES" id="subtitlesInputMd"
                           x-model="searchTypes">
                    <label class="form-check-label" for="subtitlesInputMd">
                        Sous-titrage
                    </label>
                </div>

                <div class="form-check">
                    <input class="form-check-input" type="checkbox" value="VOICE" id="voiceInputMd"
                           x-model="searchTypes">
                    <label class="form-check-label" for="voiceInputMd">
                        Doublage
                    </label>
                </div>
            </div>
        </div>

        <div class="d-flex d-md-none mb-3">
            <div class="form-check me-3">
                <input class="form-check-input" type="checkbox" value="SUBTITLES" id="subtitlesInputSm"
                       x-model="searchTypes">
                <label class="form-check-label" for="subtitlesInputSm">
                    Sous-titrage
                </label>
            </div>

            <div class="form-check">
                <input class="form-check-input" type="checkbox" value="VOICE" id="voiceInputSm" x-model="searchTypes">
                <label class="form-check-label" for="voiceInputSm">
                    Doublage
                </label>
            </div>
        </div>
    </div>

    <#if groupedEpisodes?? && groupedEpisodes?size != 0>
        <div class="row g-3">
            <#list groupedEpisodes as groupedEpisode>
            <#-- If episode is the first element -->
                <#assign col="col-md-2">
                <#assign firstRow=false>

                <#if groupedEpisode?index == 0>
                    <#assign col="col-md-7">
                    <#assign firstRow=true>
                <#elseif groupedEpisode?index == 1>
                    <#assign col="col-md-5">
                    <#assign firstRow=true>
                </#if>

                <@groupedEpisodeComponent.display groupedEpisode=groupedEpisode desktopColSize=col mobileColSize="col-6" cover=firstRow />
            </#list>
        </div>
    <#else>
        <div class="d-flex justify-content-center align-items-center my-5">
            <p class="text-muted p-5">Aucun épisode disponible pour le moment</p>
        </div>
    </#if>

    <div class="d-flex align-content-center my-4">
        <h1 class="h3 ms-0 me-auto">Simulcast en cours</h1>

        <#if currentSimulcast??>
            <a href="/catalog/${currentSimulcast.slug}"
               class="btn btn-dark ms-auto me-0 px-4 d-flex align-items-center">
                PLUS
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                     class="bi bi-chevron-right ms-1" viewBox="0 0 16 16">
                    <path fill-rule="evenodd"
                          d="M4.646 1.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1 0 .708l-6 6a.5.5 0 0 1-.708-.708L10.293 8 4.646 2.354a.5.5 0 0 1 0-.708"/>
                </svg>
            </a>
        </#if>
    </div>

    <#if animes?? && animes?size != 0>
        <div class="row g-3">
            <#list animes as anime>
                <@animeComponent.display anime=anime />
            </#list>
        </div>
    <#else>
        <div class="d-flex justify-content-center align-items-center my-5">
            <p class="text-muted p-5">Aucun animé disponible pour le moment</p>
        </div>
    </#if>
</@navigation.display>