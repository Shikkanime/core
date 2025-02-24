<#import "_layout.ftl" as layout />

<#macro display canonicalUrl="" openGraphImage="">
    <@layout.main canonicalUrl="${canonicalUrl}" openGraphImage="${openGraphImage}">
        <#assign margin = "ms-md-2">

        <header class="position-fixed" style="z-index: 1000; width: 100%;">
            <nav class="navbar navbar-expand-lg bg-black bg-opacity-75" data-bs-theme="dark"
                 style="backdrop-filter: blur(0.5rem);">
                <div class="container-fluid px-md-5 d-md-flex">
                    <a class="navbar-brand" href="/">
                        <img src="/assets/img/light_banner.webp" alt="Shikkanime" width="181" height="24"
                             class="d-inline-block align-text-top">
                    </a>
                    <button class="navbar-toggler" type="button" data-bs-toggle="collapse"
                            data-bs-target="#navbarNavDropdown" aria-controls="navbarNavDropdown"
                            aria-expanded="false" aria-label="Toggle navigation">
                        <span class="navbar-toggler-icon"></span>
                    </button>
                    <div class="collapse navbar-collapse" id="navbarNavDropdown">
                        <ul class="navbar-nav ms-md-auto me-md-0">
                            <#list links as link>
                                <li class="nav-item ${margin}">
                                    <a href="${link.href}"
                                       class="nav-link${link.active?then(' active', '')}"${link.active?then(' aria-current="page"', '')}>
                                        ${link.name}
                                    </a>
                                </li>
                            </#list>
                        </ul>
                    </div>
                </div>
            </nav>
        </header>

        <main style="padding-top: 50px;">
            <div class="callout">
                <div class="me-2">
                    <p class="mb-1 mb-md-0"><b>NOUVEAU</b> : découvrez l'application <b>Shikkanime</b> !</p>
                    <span class="text-muted">Votre gestionnaire de watchlist d'animés, pour ne rien manquer !</span>
                </div>
                <div class="ms-auto me-0 d-md-flex align-items-center justify-content-center">
                    <a href="https://play.google.com/store/apps/details?id=fr.shikkanime.application"
                       class="text-decoration-none" target="_blank">
                        <img src="/assets/img/google_play.png" width="160" alt="Google Play"
                             class="me-md-2 mb-2 mb-md-0">
                    </a>
                    <a href="https://apps.apple.com/fr/app/shikkanime/id6708237178"
                       class="text-decoration-none" target="_blank">
                        <img src="/assets/img/app_store.svg" width="160" alt="App Store">
                    </a>
                </div>
            </div>


            <div class="text-white container-fluid px-md-5">
                <#nested 1>
            </div>
        </main>

        <footer class="container-fluid px-md-5">
            <div class="d-flex flex-wrap justify-content-between align-items-center py-3 my-4 border-top">
                <div class="col-md-4 d-flex align-items-center">
                    <div>
                        <a href="/" class="mb-3 me-2 mb-md-0 text-muted text-decoration-none lh-1">
                            <img loading="lazy" src="/assets/img/light_icon.webp" alt="Shikkanime" width="24"
                                 height="24">
                        </a>

                        <#if seoDescription?? && seoDescription?length != 0>
                            <p class="my-3 text-white">
                                ${seoDescription}
                            </p>
                        </#if>

                        <small class="d-block text-muted">&copy; 2023 - ${.now?string("yyyy")} Shikkanime</small>
                    </div>
                </div>

                <div class="col-md-4">
                    <ul class="mt-3 mt-md-0 nav d-flex justify-content-end list-unstyled">
                        <li class="me-3">
                            <a class="text-muted" href="https://github.com/Shikkanime" target="_blank"
                               aria-label="GitHub">
                                <img loading="lazy" src="${baseUrl}/assets/img/icons/github.svg" alt="GitHub">
                            </a>
                        </li>
                        <li class="me-3">
                            <a class="text-muted" href="https://x.com/Shikkanime" target="_blank"
                               aria-label="X (Twitter)">
                                <img loading="lazy" src="${baseUrl}/assets/img/icons/x.svg" alt="X (Twitter)">
                            </a>
                        </li>
                        <li class="me-3">
                            <a class="text-muted" href="https://www.threads.net/@shikkanime" target="_blank"
                               aria-label="Threads">
                                <img loading="lazy" src="${baseUrl}/assets/img/icons/threads.svg" alt="Threads">
                            </a>
                        </li>
                        <li class="me-3">
                            <a class="text-muted" href="https://bsky.app/profile/shikkanime.fr" target="_blank"
                               aria-label="Bluesky">
                                <img loading="lazy" src="${baseUrl}/assets/img/icons/bluesky.svg" alt="Bluesky">
                            </a>
                        </li>
                        <li class="me-3">
                            <a class="text-muted" href="https://discord.gg/4TmPZpqD67" target="_blank"
                               aria-label="Discord">
                                <img loading="lazy" src="${baseUrl}/assets/img/icons/discord.svg" alt="Discord">
                            </a>
                        </li>
                        <li class="me-3">
                            <a class="text-muted" href="mailto:contact@shikkanime.fr" aria-label="Email">
                                <img loading="lazy" src="${baseUrl}/assets/img/icons/email.svg" alt="Email">
                            </a>
                        </li>
                        <li>
                            <a class="text-muted" href="${baseUrl}/feed/episodes" aria-label="Flux RSS">
                                <img loading="lazy" src="${baseUrl}/assets/img/icons/rss.svg" alt="Flux RSS">
                            </a>
                        </li>
                    </ul>

                    <ul class="mt-3 list-unstyled justify-content-end text-md-end">
                        <#if randomAnimeSlug?? && randomAnimeSlug?length != 0>
                            <li>
                                <a href="/animes/${randomAnimeSlug}" class="text-muted text-decoration-none">
                                    Animé aléatoire
                                </a>
                            </li>
                        </#if>

                        <#list footerLinks as link>
                            <li>
                                <a href="${link.href}" class="text-muted text-decoration-none">
                                    ${link.name}
                                </a>
                            </li>
                        </#list>
                    </ul>
                </div>
            </div>
        </footer>

        <script src="/assets/js/bootstrap.bundle.min.js" crossorigin="anonymous"></script>
    </@layout.main>
</#macro>