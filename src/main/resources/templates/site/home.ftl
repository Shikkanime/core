<#import "_layout.ftl" as layout />

<@layout.main>
    <#assign margin = "mx-lg-2">

    <nav class="navbar navbar-expand-lg bg-dark" data-bs-theme="dark">
        <div class="container-fluid ${margin}">
            <a class="navbar-brand" href="/">
                <img src="/assets/img/light_banner.png" alt="Logo" height="24" class="d-inline-block align-text-top">
            </a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNavDropdown" aria-controls="navbarNavDropdown"
                    aria-expanded="false" aria-label="Toggle navigation">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navbarNavDropdown">
                <ul class="navbar-nav">
                    <#list links as link>
                        <li class="nav-item ${margin}">
                            <a href="${link.href}" class="nav-link ${link.active?then('active', '')}"
                               aria-current="${link.active?then('page', '')}">
                                <i class="${link.icon} me-2"></i>
                                ${link.name}
                            </a>
                        </li>
                    </#list>
                </ul>
            </div>
        </div>
    </nav>
</@layout.main>