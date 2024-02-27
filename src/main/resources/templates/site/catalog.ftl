<#import "_navigation.ftl" as navigation />
<#import "components/episode.ftl" as episodeComponent />
<#import "components/anime.ftl" as animeComponent />

<@navigation.display>
    <div class="mt-3">
        <#if selectedSimulcast??>
            <button class="btn btn-dark dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false">
                ${selectedSimulcast.label}
            </button>

            <ul class="dropdown-menu dropdown-menu-dark" style="max-height: 300px; overflow-y: auto;">
                <#list simulcasts as simulcast>
                    <li><a class="dropdown-item" href="/catalog/${simulcast.slug}">${simulcast.label}</a></li>
                </#list>
            </ul>

            <div class="row mt-3 justify-content-center">
                <#list animes as anime>
                    <@animeComponent.display anime=anime />
                </#list>
            </div>
        </#if>
    </div>
</@navigation.display>