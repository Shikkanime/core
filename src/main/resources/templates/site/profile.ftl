<#import "_navigation.ftl" as navigation />
<#import "components/anime.ftl" as animeComponent />
<#import "components/episode-mapping.ftl" as episodeMappingComponent />

<@navigation.display canonicalUrl="${baseUrl}/@${member.username}">
    <div class="row mt-3">
        <div class="col-md-3">
            <div class="d-flex align-items-center">
                <img loading="lazy" src="${apiUrl}/v1/attachments?uuid=${member.uuid}&type=image"
                     alt="${member.username?html} profile"
                     class="img-fluid rounded-4"
                     width="128"
                     height="128">

                <div class="ms-3">
                    <h1 class="h1 fw-bold">${member.username}</h1>
                    <span class="text-muted mb-0">Dernière connexion : ...</span>
                    <br>
                    <span class="text-muted">Compte créer le : ${member.creationDateTime?datetime.iso?string["dd/MM/yyyy"]}</span>
                </div>
            </div>

            <div class="shikk-element mt-3">
                <div class="p-3 d-flex flex-column align-items-center justify-content-center" style="height: 100%;">
                    ${member.followedAnimesDto.total} animés ajoutés
                </div>
            </div>

            <div class="shikk-element mt-3">
                <div class="p-3 d-flex flex-column align-items-center justify-content-center" style="height: 100%;">
                    ${member.followedEpisodesDto.total} épisodes vus
                </div>
            </div>

            <div class="shikk-element mt-3">
                <div class="p-3 d-flex flex-column align-items-center justify-content-center" style="height: 100%;">
                    <#function printDuration duration>
                        <#local days = duration / 86400>
                        <#local hours = duration / 3600 % 24>
                        <#local minutes = duration % 60>
                        <#return "${days?int?c}j ${hours?int?c}h ${minutes?int?c}m">
                    </#function>

                    <p>${printDuration(member.totalDuration)} de visionnage</p>

                    <div class="progress w-100" style="border-radius: 0">
                        <div class="progress-bar" style="width: ${((member.totalDuration / (member.totalDuration + member.totalUnseenDuration)) * 100)?c}%"></div>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-md-9">
            <div class="row mt-3 mt-md-0 g-3">
                <#list member.followedAnimesDto.data as anime>
                    <@animeComponent.display anime=anime />
                </#list>
            </div>

            <div class="row g-3">
                <#list member.followedEpisodesDto.data as episodeMapping>
                    <@episodeMappingComponent.display episodeMapping=episodeMapping cover=false desktopColSize="col-md-2" mobileColSize="col-6" showAnime=false showSeason=false />
                </#list>
            </div>
        </div>
    </div>
</@navigation.display>