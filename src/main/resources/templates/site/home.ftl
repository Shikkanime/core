<#import "_navigation.ftl" as navigation />
<#import "components/episode-mapping.ftl" as episodeMappingComponent />
<#import "components/grouped-episode.ftl" as groupedEpisodeComponent />
<#import "components/anime.ftl" as animeComponent />

<@navigation.display canonicalUrl="${baseUrl}">
    <div class="hero-section">
        <div class="row">
            <div class="col-md-6 col-12">
                <div class="hero-header">
                    <h1 class="fw-bold">Ne manquez plus aucun <span class="text-gradient">épisode</span></h1>
                    <span class="text-muted">Ne cherchez plus où regarder. Shikkanime indexe en temps réel les sorties de ADN, Crunchyroll, Disney+, Netflix et Prime Video pour vous offrir une timeline unique</span>
                </div>
                <div class="buttons-row">
                    <a href="#latest-releases" class="btn btn-outline-light">
                        Voir les dernières sorties
                    </a>
                </div>
            </div>
        </div>
    </div>

    <div class="hero-section">
        <div class="hero-header">
            <h1 class="h3">Pourquoi utiliser Shikkanime ?</h1>
            <span class="text-muted">Conçu par des fans, pour les fans. Une expérience fluide sans compromis</span>
        </div>

        <div class="row g-md-5 g-3">
            <div class="col-md-4 col-6">
                <div class="shikk-element p-md-5 p-3 d-flex flex-column gap-md-4 gap-2">
                    <span class="dynamic-icon">
                        <i>
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.268 21a2 2 0 0 0 3.464 0"/><path d="M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326"/></svg>
                        </i>
                    </span>
                    <h5 class="h5 mb-0 fw-bold">Alertes Instantanées</h5>
                    <span class="text-muted">Soyez notifié à la seconde près dès qu'un épisode est disponible. Fini le rafraîchissement compulsif des pages.</span>
                </div>
            </div>
            <div class="col-md-4 col-6">
                <div class="shikk-element p-md-5 p-3 d-flex flex-column gap-md-4 gap-2">
                    <span class="dynamic-icon">
                        <i>
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83z"/><path d="M2 12a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 12"/><path d="M2 17a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 17"/></svg>
                        </i>
                    </span>
                    <h5 class="h5 mb-0 fw-bold">Indexation Unifiée</h5>
                    <span class="text-muted">Retrouvez tout le catalogue légal français dans une seule interface. Filtrez par plateforme, genre ou saison.</span>
                </div>
            </div>
            <div class="col-md-4 col-6">
                <div class="shikk-element p-md-5 p-3 d-flex flex-column gap-md-4 gap-2">
                    <span class="dynamic-icon">
                        <i>
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 2v4"/><path d="M16 2v4"/><rect width="18" height="18" x="3" y="4" rx="2"/><path d="M3 10h18"/></svg>
                        </i>
                    </span>
                    <h5 class="h5 mb-0 fw-bold">Calendrier Intelligent</h5>
                    <span class="text-muted">Votre planning personnalisé. Visualisez en un coup d'œil les sorties de la semaine pour vos séries suivies.</span>
                </div>
            </div>
            <div class="col-md-4 col-6">
                <div class="shikk-element p-md-5 p-3 d-flex flex-column gap-md-4 gap-2">
                    <span class="dynamic-icon">
                        <i>
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"/></svg>
                        </i>
                    </span>
                    <h5 class="h5 mb-0 fw-bold">Ma Watchlist</h5>
                    <span class="text-muted">Marquez vos épisodes vus, suivez votre progression et reprenez exactement où vous vous êtes arrêté.</span>
                </div>
            </div>
            <div class="col-md-4 col-6">
                <div class="shikk-element p-md-5 p-3 d-flex flex-column gap-md-4 gap-2">
                    <span class="dynamic-icon">
                        <i>
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m18 16 4-4-4-4"/><path d="m6 8-4 4 4 4"/><path d="m14.5 4-5 16"/></svg>
                        </i>
                    </span>
                    <h5 class="h5 mb-0 fw-bold">100% Open Source</h5>
                    <span class="text-muted">Un projet transparent et communautaire. Le code est ouvert à tous pour garantir sécurité et pérennité.</span>
                </div>
            </div>
            <div class="col-md-4 col-6">
                <div class="shikk-element p-md-5 p-3 d-flex flex-column gap-md-4 gap-2">
                    <span class="dynamic-icon">
                        <i>
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z"/></svg>
                        </i>
                    </span>
                    <h5 class="h5 mb-0 fw-bold">Léger & Performant</h5>
                    <span class="text-muted">Pas de publicités intrusives, pas de trackers inutiles. Juste vos animes, chargés instantanément.</span>
                </div>
            </div>
        </div>
    </div>

    <div class="hero-section" id="latest-releases">
        <div class="hero-header">
            <h1 class="h3">Dernières sorties</h1>
            <span class="text-muted">Tout juste sortis du four</span>
        </div>

        <#if groupedEpisodes?? && groupedEpisodes?size != 0>
            <div class="row g-4">
                <#list groupedEpisodes as groupedEpisode>
                    <@groupedEpisodeComponent.display groupedEpisode=groupedEpisode desktopColSize="col-md-3" mobileColSize="col-6" />
                </#list>
            </div>
        <#else>
            <div class="d-flex justify-content-center align-items-center my-5">
                <p class="text-muted p-5">Aucun épisode disponible pour le moment</p>
            </div>
        </#if>
    </div>

    <div class="d-flex align-content-center my-4">
        <h1 class="h3 ms-0 me-auto">Simulcast en cours</h1>

        <#if currentSimulcast??>
            <a href="/catalog/${currentSimulcast.slug}"
               class="btn btn-dark ms-auto me-0 px-4 d-flex align-items-center">
                PLUS
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-chevron-right-icon lucide-chevron-right"><path d="m9 18 6-6-6-6"/></svg>
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