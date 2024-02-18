<#import "_navigation.ftl" as navigation />
<#import "components/episode.ftl" as episodeComponent />
<#import "components/anime.ftl" as animeComponent />

<@navigation.display>
    <h1 class="h3 my-3">Nouveaux épisodes</h1>

    <div class="row">
        <#list episodes as episode>
            <@episodeComponent.display episode=episode />
        </#list>
    </div>

    <div class="d-flex align-content-center my-3">
        <h1 class="h3 ms-0 me-auto">Simulcast en cours</h1>
        <a href="/catalog" class="btn btn-outline-light ms-auto me-0 rounded-pill px-3">
            PLUS
            <i class="bi bi-chevron-right"></i>
        </a>
    </div>

    <div class="row">
        <#list animes as anime>
            <@animeComponent.display anime=anime />
        </#list>
    </div>

    <script src="/assets/js/intersection_observer.js"></script>
</@navigation.display>