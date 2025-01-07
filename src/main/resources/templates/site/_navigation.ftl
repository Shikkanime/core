<#import "_layout.ftl" as layout />

<#macro display canonicalUrl="" openGraphImage="">
    <@layout.main canonicalUrl="${canonicalUrl}" openGraphImage="${openGraphImage}">
        <#assign margin = "ms-md-2">

        <header class="position-fixed" style="z-index: 1000; width: 100%;">
            <nav class="navbar navbar-expand-lg bg-black bg-opacity-75" data-bs-theme="dark" style="backdrop-filter: blur(0.5rem);">
                <div class="container-fluid px-md-5 d-md-flex">
                    <a class="navbar-brand" href="/">
                        <img src="/assets/img/light_banner.webp" alt="Shikkanime" width="181" height="24" class="d-inline-block align-text-top">
                    </a>
                    <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNavDropdown" aria-controls="navbarNavDropdown"
                            aria-expanded="false" aria-label="Toggle navigation">
                        <span class="navbar-toggler-icon"></span>
                    </button>
                    <div class="collapse navbar-collapse" id="navbarNavDropdown">
                        <ul class="navbar-nav ms-md-auto me-md-0">
                            <#list links as link>
                                <li class="nav-item ${margin}">
                                    <a href="${link.href}" class="nav-link${link.active?then(' active', '')}"${link.active?then(' aria-current="page"', '')}>
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

        <div class="container-fluid px-md-5">
            <footer class="d-flex flex-wrap justify-content-between align-items-center py-3 my-4 border-top">
                <div class="col-md-4 d-flex align-items-center">
                    <div>
                        <a href="/" class="mb-3 me-2 mb-md-0 text-muted text-decoration-none lh-1">
                            <img src="/assets/img/light_icon.webp" alt="Shikkanime" width="24" height="24">
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
                            <a class="text-muted" href="https://github.com/Shikkanime" target="_blank" aria-label="GitHub">
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-github" viewBox="0 0 16 16">
                                    <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8"/>
                                </svg>
                            </a>
                        </li>
                        <li class="me-3">
                            <a class="text-muted" href="https://x.com/Shikkanime" target="_blank" aria-label="X (Twitter)">
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-twitter-x" viewBox="0 0 16 16">
                                    <path d="M12.6.75h2.454l-5.36 6.142L16 15.25h-4.937l-3.867-5.07-4.425 5.07H.316l5.733-6.57L0 .75h5.063l3.495 4.633L12.601.75Zm-.86 13.028h1.36L4.323 2.145H2.865z"/>
                                </svg>
                            </a>
                        </li>
                        <li class="me-3">
                            <a class="text-muted" href="https://www.threads.net/@shikkanime" target="_blank" aria-label="Threads">
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-threads" viewBox="0 0 16 16">
                                    <path d="M6.321 6.016c-.27-.18-1.166-.802-1.166-.802.756-1.081 1.753-1.502 3.132-1.502.975 0 1.803.327 2.394.948s.928 1.509 1.005 2.644q.492.207.905.484c1.109.745 1.719 1.86 1.719 3.137 0 2.716-2.226 5.075-6.256 5.075C4.594 16 1 13.987 1 7.994 1 2.034 4.482 0 8.044 0 9.69 0 13.55.243 15 5.036l-1.36.353C12.516 1.974 10.163 1.43 8.006 1.43c-3.565 0-5.582 2.171-5.582 6.79 0 4.143 2.254 6.343 5.63 6.343 2.777 0 4.847-1.443 4.847-3.556 0-1.438-1.208-2.127-1.27-2.127-.236 1.234-.868 3.31-3.644 3.31-1.618 0-3.013-1.118-3.013-2.582 0-2.09 1.984-2.847 3.55-2.847.586 0 1.294.04 1.663.114 0-.637-.54-1.728-1.9-1.728-1.25 0-1.566.405-1.967.868ZM8.716 8.19c-2.04 0-2.304.87-2.304 1.416 0 .878 1.043 1.168 1.6 1.168 1.02 0 2.067-.282 2.232-2.423a6.2 6.2 0 0 0-1.528-.161"/>
                                </svg>
                            </a>
                        </li>
                        <li class="me-3">
                            <a class="text-muted" href="https://bsky.app/profile/shikkanime.fr" target="_blank"
                               aria-label="Bluesky">
                                <svg width="16" height="16" viewBox="0 0 360 320" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
                                    <path d="M180 141.964C163.699 110.262 119.308 51.1817 78.0347 22.044C38.4971 -5.86834 23.414 -1.03207 13.526 3.43594C2.08093 8.60755 0 26.1785 0 36.5164C0 46.8542 5.66748 121.272 9.36416 133.694C21.5786 174.738 65.0603 188.607 105.104 184.156C107.151 183.852 109.227 183.572 111.329 183.312C109.267 183.642 107.19 183.924 105.104 184.156C46.4204 192.847 -5.69621 214.233 62.6582 290.33C137.848 368.18 165.705 273.637 180 225.702C194.295 273.637 210.76 364.771 295.995 290.33C360 225.702 313.58 192.85 254.896 184.158C252.81 183.926 250.733 183.645 248.671 183.315C250.773 183.574 252.849 183.855 254.896 184.158C294.94 188.61 338.421 174.74 350.636 133.697C354.333 121.275 360 46.8568 360 36.519C360 26.1811 357.919 8.61012 346.474 3.43851C336.586 -1.02949 321.503 -5.86576 281.965 22.0466C240.692 51.1843 196.301 110.262 180 141.964Z"/>
                                </svg>
                            </a>
                        </li>
                        <li class="me-3">
                            <a class="text-muted" href="https://discord.gg/4TmPZpqD67" target="_blank" aria-label="Discord">
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-discord" viewBox="0 0 16 16">
                                    <path d="M13.545 2.907a13.2 13.2 0 0 0-3.257-1.011.05.05 0 0 0-.052.025c-.141.25-.297.577-.406.833a12.2 12.2 0 0 0-3.658 0 8 8 0 0 0-.412-.833.05.05 0 0 0-.052-.025c-1.125.194-2.22.534-3.257 1.011a.04.04 0 0 0-.021.018C.356 6.024-.213 9.047.066 12.032q.003.022.021.037a13.3 13.3 0 0 0 3.995 2.02.05.05 0 0 0 .056-.019q.463-.63.818-1.329a.05.05 0 0 0-.01-.059l-.018-.011a9 9 0 0 1-1.248-.595.05.05 0 0 1-.02-.066l.015-.019q.127-.095.248-.195a.05.05 0 0 1 .051-.007c2.619 1.196 5.454 1.196 8.041 0a.05.05 0 0 1 .053.007q.121.1.248.195a.05.05 0 0 1-.004.085 8 8 0 0 1-1.249.594.05.05 0 0 0-.03.03.05.05 0 0 0 .003.041c.24.465.515.909.817 1.329a.05.05 0 0 0 .056.019 13.2 13.2 0 0 0 4.001-2.02.05.05 0 0 0 .021-.037c.334-3.451-.559-6.449-2.366-9.106a.03.03 0 0 0-.02-.019m-8.198 7.307c-.789 0-1.438-.724-1.438-1.612s.637-1.613 1.438-1.613c.807 0 1.45.73 1.438 1.613 0 .888-.637 1.612-1.438 1.612m5.316 0c-.788 0-1.438-.724-1.438-1.612s.637-1.613 1.438-1.613c.807 0 1.451.73 1.438 1.613 0 .888-.631 1.612-1.438 1.612"/>
                                </svg>
                            </a>
                        </li>
                        <li class="me-3">
                            <a class="text-muted" href="mailto:contact@shikkanime.fr" aria-label="Email">
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-envelope" viewBox="0 0 16 16">
                                    <path d="M0 4a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2zm2-1a1 1 0 0 0-1 1v.217l7 4.2 7-4.2V4a1 1 0 0 0-1-1zm13 2.383-4.708 2.825L15 11.105zm-.034 6.876-5.64-3.471L8 9.583l-1.326-.795-5.64 3.47A1 1 0 0 0 2 13h12a1 1 0 0 0 .966-.741M1 11.105l4.708-2.897L1 5.383z"/>
                                </svg>
                            </a>
                        </li>
                        <li>
                            <a class="text-muted" href="${baseUrl}/feed/episodes" aria-label="Flux RSS">
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-rss-fill" viewBox="0 0 16 16">
                                    <path d="M2 0a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V2a2 2 0 0 0-2-2zm1.5 2.5c5.523 0 10 4.477 10 10a1 1 0 1 1-2 0 8 8 0 0 0-8-8 1 1 0 0 1 0-2m0 4a6 6 0 0 1 6 6 1 1 0 1 1-2 0 4 4 0 0 0-4-4 1 1 0 0 1 0-2m.5 7a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3"/>
                                </svg>
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
            </footer>
        </div>

        <script src="/assets/js/bootstrap.bundle.min.js" crossorigin="anonymous"></script>
    </@layout.main>
</#macro>