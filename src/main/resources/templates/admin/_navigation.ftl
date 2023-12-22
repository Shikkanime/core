<#import "_layout.ftl" as layout />

<#macro display>
    <@layout.main>
        <div class="d-flex flex-column flex-shrink-0 p-3 bg-light shadow">
            <a href="/admin/dashboard"
               class="d-flex align-items-center mb-3 mb-md-0 me-md-auto link-dark text-decoration-none">
                <img src="/admin/assets/img/icon.jpg" class="me-2 rounded-circle" width="32" height="32" alt="Icon" crossorigin="anonymous">
                <span class="fs-4">Shikkanime</span>
            </a>
            <hr>
            <ul class="nav nav-pills flex-column mb-auto">
                <#list links as link>
                    <li class="nav-item">
                        <a href="${link.href}" class="nav-link ${link.active?then('active', 'link-dark')}"
                           aria-current="${link.active?then('page', '')}">
                            <i class="${link.icon} me-2"></i>
                            ${link.name}
                        </a>
                    </li>
                </#list>
            </ul>

            <ul class="nav nav-pills flex-column mb-0 mt-auto">
                <li class="nav-item">
                    <a href="/admin/logout" class="nav-link link-dark">
                        <i class="bi bi-box-arrow-right me-2"></i>
                        Log out
                    </a>
                </li>
            </ul>
        </div>

        <div class="p-4 container-fluid">
            <#nested 1>
        </div>
    </@layout.main>
</#macro>