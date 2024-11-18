<#import "_layout.ftl" as layout />

<@layout.main
title="Votre code à usage unique"
description="Ce code est valable pendant 15 minutes. Veuillez le saisir dans l'application pour associer votre email à votre compte.">
    <#if webToken?? && webToken?length != 0>
        <div style="margin-bottom: 30px;">
            Ou alors, vous pouvez cliquer sur le lien suivant pour associer votre email à votre compte :
            <a href="https://www.shikkanime.fr/v/${webToken}" style="color: #007bff; text-decoration: none;">Associer mon email</a>
        </div>
    </#if>
</@layout.main>