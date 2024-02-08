<#import "_navigation.ftl" as navigation />
<#import "components/episode.ftl" as episodeComponent />

<@navigation.display>
    <div class="container">
        <div class="row g-3 mt-3">
            <div class="col-md-4 col-12 mt-0 text-center">
                <img loading="lazy" src="https://api.shikkanime.fr/v1/attachments?uuid=${anime.uuid}&type=image"
                     alt="${anime.shortName?replace("\"", "'")} anime image" class="w-50">
            </div>

            <div class="col-md-8 col-12 mt-0 text-start mt-md-0 mt-5 d-flex flex-column justify-content-center">
                <h6 class="h6">${anime.shortName?upper_case}</h6>
                <p class="text-muted">${anime.name}</p>
                <span class="mt-2">${anime.description}</span>
            </div>
        </div>
    </div>

    <div class="row g-3 mt-5 justify-content-center">
        <#list episodes as episode>
            <@episodeComponent.display episode=episode />
        </#list>
    </div>

    <script src="/assets/js/hover_cards.js"></script>
    <script src="/assets/js/intersection_observer.js"></script>
</@navigation.display>