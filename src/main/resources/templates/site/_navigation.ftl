<#import "_layout.ftl" as layout />

<#macro display header="">
    <@layout.main header="${header}">
        <#assign margin = "mx-md-2">

        <header>
            <nav class="navbar navbar-expand-lg bg-dark" data-bs-theme="dark">
                <div class="container-fluid mx-md-5">
                    <a class="navbar-brand" href="/">
                        <img src="/assets/img/light_banner.webp" alt="Logo" height="24"
                             class="d-inline-block align-text-top">
                    </a>
                    <button class="navbar-toggler" type="button" data-bs-toggle="collapse"
                            data-bs-target="#navbarNavDropdown" aria-controls="navbarNavDropdown"
                            aria-expanded="false" aria-label="Toggle navigation">
                        <span class="navbar-toggler-icon"></span>
                    </button>
                    <div class="collapse navbar-collapse" id="navbarNavDropdown">
                        <ul class="navbar-nav">
                            <#list links as link>
                                <li class="nav-item ${margin}">
                                    <a href="${link.href}" class="nav-link ${link.active?then('active', '')}"
                                            ${link.active?then('aria-current="page"', '')}>
                                        <i class="${link.icon} me-2"></i>
                                        ${link.name}
                                    </a>
                                </li>
                            </#list>
                        </ul>
                    </div>
                </div>
            </nav>
        </header>

        <main>
            <div class="text-white container-fluid px-md-5">
                <#nested 1>
            </div>
        </main>

        <div class="container">
            <footer class="d-flex flex-wrap justify-content-between align-items-center py-3 my-4 border-top">
                <div class="col-md-4 d-flex align-items-center">
                    <div>
                        <a href="/" class="mb-3 me-2 mb-md-0 text-muted text-decoration-none lh-1">
                            <img src="/assets/img/favicons/favicon-64x64.png" alt="Logo" height="24">
                        </a>

                        <p class="my-3 text-white">
                            ${description}
                        </p>

                        <small class="d-block text-muted">© 2023 - ${.now?string("yyyy")} Shikkanime</small>
                    </div>
                </div>

                <ul class="nav col-md-4 justify-content-end list-unstyled d-flex">
                    <li class="ms-3">
                        <a class="text-muted" href="https://github.com/Shikkanime" target="_blank">
                            <i class="bi bi-github"></i>
                        </a>
                    </li>
                    <li class="ms-3">
                        <a class="text-muted" href="https://twitter.com/Shikkanime" target="_blank">
                            <i class="bi bi-twitter-x"></i>
                        </a>
                    </li>
                    <li class="ms-3">
                        <a class="text-muted" href="https://www.instagram.com/shikkanime/" target="_blank">
                            <i class="bi bi-instagram"></i>
                        </a>
                    </li>
                    <li class="ms-3">
                        <a class="text-muted" href="https://www.threads.net/@shikkanime" target="_blank">
                            <i class="bi bi-threads"></i>
                        </a>
                    </li>
                    <li class="ms-3">
                        <a class="text-muted" href="mailto:contact@shikkanime.fr">
                            <i class="bi bi-envelope"></i>
                        </a>
                    </li>
                </ul>
            </footer>
        </div>
    </@layout.main>
</#macro>