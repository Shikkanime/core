<#import "_layout.ftl" as layout />

<#macro display>
    <@layout.main>
        <div class="d-flex flex-column flex-shrink-0 p-3 bg-light shadow">
            <a href="/admin/dashboard"
               class="d-flex align-items-center mb-md-0 me-md-auto link-dark text-decoration-none">
                <img src="/assets/img/dark_banner.webp" width="200" height="44" crossorigin="anonymous"
                     class="img-fluid d-none d-md-inline-block" alt="Shikkanime dark banner">

                <img src="/assets/img/dark_icon.webp" width="44" height="44" crossorigin="anonymous"
                     class="img-fluid d-block d-md-none" alt="Shikkanime dark logo">
            </a>
            <hr>
            <ul class="nav nav-pills flex-column mb-auto">
                <#list links as link>
                    <li class="nav-item">
                        <a href="${link.href}" class="nav-link ${link.active?then('bg-dark text-white', 'link-dark')}"
                           aria-current="${link.active?then('page', '')}">
                            <i class="${link.icon}"></i>
                            <span class="d-none d-md-inline-block ms-2">${link.name}</span>
                        </a>
                    </li>
                </#list>
            </ul>

            <ul class="nav nav-pills flex-column mb-0 mt-auto">
                <li class="nav-item">
                    <a href="/admin/logout" class="nav-link link-dark">
                        <i class="bi bi-outlet"></i>
                        <span class="d-none d-md-inline-block ms-2">Log out</span>
                    </a>
                </li>
            </ul>
        </div>

        <div class="p-4 container-fluid" style="max-height: 100vh; overflow-y: auto;">
            <#nested 1>
        </div>
    </@layout.main>
</#macro>