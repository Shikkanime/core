<#macro main header="">
    <!DOCTYPE html>
    <html lang="fr">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">

        <#if title??>
            <title>${title}</title>
        <#else>
            <title>Shikkanime</title>
        </#if>

        <#if description??>
            <meta name="description" content="${description}">
        </#if>

        <#-- Favicons -->
        <link rel="icon" type="image/png" sizes="64x64" href="/assets/img/favicons/favicon-64x64.png">
        <link rel="icon" type="image/png" sizes="32x32" href="/assets/img/favicons/favicon-32x32.png">
        <link rel="icon" type="image/png" sizes="16x16" href="/assets/img/favicons/favicon-16x16.png">

        <link rel="stylesheet" href="/assets/css/bootstrap.min.css" crossorigin="anonymous">
        <link rel="stylesheet" href="/assets/css/main.css" crossorigin="anonymous">
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.2/font/bootstrap-icons.min.css"
              crossorigin="anonymous">

        <#if header??>
            ${header}
        </#if>

        <style>
            body {
                background-color: #000;
            }
        </style>
    </head>
    <body>
    <main>
        <#nested 0>
    </main>

    <script src="/assets/js/bootstrap.bundle.min.js" crossorigin="anonymous"></script>
    </body>
    </html>
</#macro>