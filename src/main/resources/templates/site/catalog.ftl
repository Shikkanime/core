<#import "_navigation.ftl" as navigation />
<#import "components/episode.ftl" as episodeComponent />
<#import "components/anime.ftl" as animeComponent />

<#function getPrefixSimulcast(season)>
    <#switch season>
        <#case "WINTER">
            <#return "Hiver">
        <#case "SPRING">
            <#return "Printemps">
        <#case "SUMMER">
            <#return "Été">
        <#case "AUTUMN">
            <#return "Automne">
    </#switch>
</#function>

<@navigation.display>
    <div class="mt-3">
        <button class="btn btn-dark dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false">
            ${getPrefixSimulcast(currentSimulcast.season)} ${currentSimulcast.year?c}
        </button>

        <ul class="dropdown-menu dropdown-menu-dark">
            <#list simulcasts as simulcast>
                <li><a class="dropdown-item" href="/catalog/${simulcast.season?lower_case}-${simulcast.year?c}">${getPrefixSimulcast(simulcast.season)} ${simulcast.year?c}</a></li>
            </#list>
        </ul>

        <div class="row g-3 mt-3 justify-content-center">
            <#list animes as anime>
                <@animeComponent.display anime=anime />
            </#list>
        </div>
    </div>

    <script src="/assets/js/hover_cards.js"></script>
    <script src="/assets/js/intersection_observer.js"></script>
</@navigation.display>