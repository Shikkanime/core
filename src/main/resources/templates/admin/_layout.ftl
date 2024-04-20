<#macro main>
    <!DOCTYPE html>
    <html lang="fr">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>${title}</title>

        <#-- Favicons -->
        <link rel="icon" type="image/png" sizes="64x64" href="/assets/img/favicons/favicon-64x64.png">
        <link rel="icon" type="image/png" sizes="32x32" href="/assets/img/favicons/favicon-32x32.png">
        <link rel="icon" type="image/png" sizes="16x16" href="/assets/img/favicons/favicon-16x16.png">

        <link rel="stylesheet" href="/assets/css/bootstrap.min.css" crossorigin="anonymous">
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.2/font/bootstrap-icons.min.css"
              crossorigin="anonymous">

        <style>
            body {
                min-height: 100vh;
            }

            main {
                display: flex;
                flex-wrap: nowrap;
                min-height: 100vh;
                overflow-x: auto;
            }
        </style>

        <script defer src="/assets/js/alpinejs.min.js" crossorigin="anonymous"></script>
    </head>
    <body>
    <main>
        <#nested 0>
    </main>

    <script src="/assets/js/bootstrap.bundle.min.js" crossorigin="anonymous"></script>
    <script src="/assets/js/axios.min.js" crossorigin="anonymous"></script>
    <script src="/assets/js/main.js" crossorigin="anonymous"></script>
    </body>
    </html>
</#macro>