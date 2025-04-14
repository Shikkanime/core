<#import "_layout.ftl" as layout />

<@layout.main
title="Votre code à usage unique"
description="Ce code est valable pendant <strong>15 minutes</strong>. Veuillez le saisir dans l'application pour associer votre email à votre compte.">
    <#if webToken?? && webToken?length != 0>
        <div style="margin-bottom: 30px; display: block; text-align: center;">
            <p style="margin-bottom: 15px;">Ou alors, vous pouvez cliquer sur le bouton suivant pour associer votre email à votre compte :</p>
            <a href="${baseUrl}/v/${webToken}"
               style="display: inline-block; background-color: #e6e6e6; color: #000; font-weight: bold; padding: 12px 25px; border-radius: 6px; text-decoration: none;">
                Associer mon email
            </a>
        </div>
    </#if>
</@layout.main>