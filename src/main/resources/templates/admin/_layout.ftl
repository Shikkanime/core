<#macro main>
    <!DOCTYPE html>
    <html lang="fr">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>${title}</title>

        <#-- Favicons -->
        <link rel="icon" type="image/png" sizes="32x32" href="/assets/favicons/favicon-32x32.png">
        <link rel="icon" type="image/png" sizes="16x16" href="/assets/favicons/favicon-16x16.png">

        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css"
              crossorigin="anonymous">
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.2/font/bootstrap-icons.min.css"
              crossorigin="anonymous">

        <style>
            body {
                min-height: 100vh;
            }

            main {
                display: flex;
                flex-wrap: nowrap;
                height: 100vh;
                max-height: 100vh;
                overflow-x: auto;
                overflow-y: hidden;
            }
        </style>
    </head>
    <body>
    <main>
        <div class="d-flex flex-column flex-shrink-0 p-3 bg-light shadow">
            <a href="/admin/dashboard"
               class="d-flex align-items-center mb-3 mb-md-0 me-md-auto link-dark text-decoration-none">
                <img src="/assets/icon.jpg" class="me-2 rounded-circle" width="32" height="32" alt="Icon">
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
        </div>

        <div class="p-4 container-fluid">
            <#nested>
        </div>
    </main>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"
            crossorigin="anonymous"></script>
    </body>
    </html>
</#macro>