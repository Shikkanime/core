<#macro main canonicalUrl="" openGraphImage="">
    <!DOCTYPE html>
    <html lang="fr">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">

        <#if title??>
            <title>${title}</title>
            <meta property="og:title" content="${title}">
            <meta property="twitter:title" content="${title}">
        <#else>
            <title>Shikkanime</title>
            <meta property="og:title" content="Shikkanime">
            <meta property="twitter:title" content="Shikkanime">
        </#if>

        <#if description?? && description?length != 0>
            <meta name="description" content="${description}">
            <meta property="og:description" content="${description}">
            <meta property="twitter:description" content="${description}">
        <#elseif seoDescription?? && seoDescription?length != 0>
            <meta name="description" content="${seoDescription}">
            <meta property="og:description" content="${seoDescription}">
            <meta property="twitter:description" content="${seoDescription}">
        </#if>

        <meta property="og:type" content="website">
        <meta property="og:locale" content="fr_FR">
        <meta property="og:site_name" content="Shikkanime">
        <meta property="twitter:card" content="summary_large_image">
        <meta property="twitter:site" content="@Shikkanime">

        <#if openGraphImage?? && openGraphImage?length != 0>
            <meta property="og:image" content="${openGraphImage}">
            <meta property="twitter:image" content="${openGraphImage}">
        </#if>

        <#if canonicalUrl?? && canonicalUrl?length != 0>
            <link rel="canonical" href="${canonicalUrl}">
            <meta property="og:url" content="${canonicalUrl}">
            <meta property="twitter:url" content="${canonicalUrl}">
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
        <script defer src="/assets/js/alpinejs.min.js" crossorigin="anonymous"></script>

        <#if (analyticsDomain?? && analyticsDomain?length != 0) && (analyticsApi?? && analyticsApi?length != 0) && (analyticsScript?? && analyticsScript?length != 0)>
            <script data-domain="${analyticsDomain}" data-api="${analyticsApi}">
                ${su.unSanitizeXSS(analyticsScript)}
            </script>
        </#if>
    </head>
    <body>
    <#nested 0>

    <script src="/assets/js/bootstrap.bundle.min.js" crossorigin="anonymous"></script>
    <script src="/assets/js/main.js" crossorigin="anonymous"></script>
    </body>
    </html>
</#macro>