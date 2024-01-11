<#macro main>
    <!DOCTYPE html>
    <html lang="fr">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>${title}</title>

        <link rel="stylesheet" href="/assets/css/bootstrap.min.css" crossorigin="anonymous">
        <link rel="stylesheet" href="/assets/css/main.css" crossorigin="anonymous">
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.2/font/bootstrap-icons.min.css"
              crossorigin="anonymous">

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