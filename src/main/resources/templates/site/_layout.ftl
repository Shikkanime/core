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

        <#if description?? && description?length != 0>
            <meta name="description" content="${description}">
        <#elseif seoDescription?? && seoDescription?length != 0>
            <meta name="description" content="${seoDescription}">
        </#if>

        <#if googleSiteVerification?? && googleSiteVerification?length != 0>
            <meta name="google-site-verification" content="${googleSiteVerification}"/>
        </#if>

        <#-- Favicons -->
        <link rel="icon" type="image/png" sizes="64x64" href="/assets/img/favicons/favicon-64x64.png">
        <link rel="icon" type="image/png" sizes="32x32" href="/assets/img/favicons/favicon-32x32.png">
        <link rel="icon" type="image/png" sizes="16x16" href="/assets/img/favicons/favicon-16x16.png">

        <link rel="stylesheet" href="/assets/css/purged/bootstrap.min.css" crossorigin="anonymous">
        <link rel="stylesheet" href="/assets/css/purged/main.min.css" crossorigin="anonymous">

        <#if header??>
            ${header}
        </#if>

        <script defer src="/assets/js/alpinejs.min.js" crossorigin="anonymous"></script>
    </head>
    <body>
    <#nested 0>

    <script src="/assets/js/bootstrap.bundle.min.js" crossorigin="anonymous"></script>
    </body>
    </html>
</#macro>