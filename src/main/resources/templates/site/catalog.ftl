<#import "_navigation.ftl" as navigation />
<#import "components/anime.ftl" as animeComponent />

<@navigation.display canonicalUrl="${baseUrl}/catalog/${selectedSimulcast.slug}">
    <div class="mt-3" x-data="{
        allSearchTypes: ['SUBTITLES', 'VOICE'],
        searchTypes: <#if searchTypes?? && searchTypes?has_content>[<#list searchTypes?split(',') as searchType>'${searchType}'<#if searchType_has_next>,</#if></#list>]<#else>['SUBTITLES', 'VOICE']</#if>
    }" x-init="
    $watch('searchTypes', (value) => {
        if (value.length > 0 && value.length < allSearchTypes.length) {
            window.location.href = '${baseUrl}/catalog/${selectedSimulcast.slug}?searchTypes=' + value.join(',');
        } else {
            window.location.href = '${baseUrl}/catalog/${selectedSimulcast.slug}';
        }
    });
    ">
        <#if selectedSimulcast??>
            <div class="d-flex align-items-center">
                <h1 class="h3 me-3 mb-0">Simulcast par saison</h1>

                <div>
                    <button class="btn btn-dark dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false">
                        ${selectedSimulcast.label}
                    </button>

                    <ul class="dropdown-menu dropdown-menu-dark" style="max-height: 300px; overflow-y: auto;">
                        <#list simulcasts as simulcast>
                            <li><a class="dropdown-item" href="/catalog/${simulcast.slug}">${simulcast.label}</a></li>
                        </#list>
                    </ul>
                </div>

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

            <div class="d-flex d-md-none">
                <div class="form-check me-3">
                    <input class="form-check-input" type="checkbox" value="SUBTITLES" id="subtitlesInputSm"
                           x-model="searchTypes">
                    <label class="form-check-label" for="subtitlesInputSm">
                        Sous-titrage
                    </label>
                </div>

                <div class="form-check">
                    <input class="form-check-input" type="checkbox" value="VOICE" id="voiceInputSm"
                           x-model="searchTypes">
                    <label class="form-check-label" for="voiceInputSm">
                        Doublage
                    </label>
                </div>
            </div>

            <div class="row g-3 mt-3 justify-content-center">
                <#list animes as anime>
                    <@animeComponent.display anime=anime />
                </#list>
            </div>
        </#if>
    </div>
</@navigation.display>