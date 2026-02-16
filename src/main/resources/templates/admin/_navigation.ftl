<#import "_layout.ftl" as layout />

<#macro display>
    <@layout.main>
        <div class="d-flex flex-column flex-shrink-0 bg-black">
            <a href="/admin/dashboard"
               class="d-block p-3 link-body-emphasis text-decoration-none">
                <img src="/assets/img/light_icon.webp" width="32" height="32" crossorigin="anonymous"
                     class="img-fluid" alt="Shikkanime logo">
            </a>
            <ul class="nav nav-pills nav-flush flex-column mb-auto text-center">
                <#list links as link>
                    <li class="nav-item">
                        <a href="${link.href}" class="nav-link border-bottom rounded-0 ${link.active?then('bg-light text-dark', 'link-light')}"
                           aria-current="${link.active?then('page', '')}" title="${link.name?html}">
                            <i class="${link.icon}"></i>
                        </a>
                    </li>
                </#list>
            </ul>

            <ul class="nav nav-pills nav-flush flex-column mb-0 mt-auto text-center">
                <li class="nav-item">
                    <a href="/admin/logout" class="nav-link border-bottom rounded-0 link-light" title="Log out">
                        <i class="bi bi-outlet"></i>
                    </a>
                </li>
            </ul>
        </div>

        <div class="p-4 container-fluid" style="max-height: 100vh; overflow-y: auto;">
            <#nested 1>
        </div>
    </@layout.main>
</#macro>