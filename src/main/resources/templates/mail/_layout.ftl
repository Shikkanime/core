<#macro main title="" description="" showCode=true>
    <div style="font-family: Arial, sans-serif; margin: 0; padding: 0; box-sizing: border-box; width: 100%; background-color: #f5f5f5;">
        <table role="presentation" style="width: 100%; border-collapse: collapse; background-color: #f5f5f5; margin: 0 auto;">
            <tr>
                <td style="vertical-align: top; text-align: center; padding: 20px 0;">
                    <div style="display: inline-block; max-width: 600px; width: 100%; margin: 0 auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); padding: 30px 20px; box-sizing: border-box;">
                        <a href="${baseUrl}" style="display: block; text-decoration: none;">
                            <img src="${baseUrl}/assets/img/dark_banner.webp"
                                 alt="Shikkanime"
                                 style="width: 100%; max-width: 350px; height: auto; margin-bottom: 25px;">
                        </a>

                        <#if title?? && title?length != 0>
                            <div style="font-size: 24px; font-weight: bold; margin-bottom: 25px;">
                                ${title}
                            </div>
                        </#if>

                        <#if showCode && code?? && code?length != 0>
                            <div style="background-color: #e6e6e6; border-radius: 8px; padding: 20px; margin-bottom: 20px; display: block;">
                                <table role="presentation" align="center" style="border-collapse: collapse;">
                                    <tr>
                                        <#list 1..(code?length) as i>
                                            <#assign c = code[i - 1]>
                                            <#if i == code?length>
                                                <td style="font-size: 48px; font-weight: bold; padding: 5px 10px;">${c}</td>
                                            <#else>
                                                <td style="font-size: 48px; font-weight: bold; border-right: 1px solid #cccccc; padding: 5px 10px;">${c}</td>
                                            </#if>
                                        </#list>
                                    </tr>
                                </table>
                            </div>
                        </#if>

                        <#if description?? && description?length != 0>
                            <div style="font-size: 14px; color: #666666; margin-bottom: 25px; line-height: 1.5;">
                                ${description}
                            </div>
                        </#if>

                        <#nested />

                        <p style="font-size: 16px; margin-bottom: 20px; line-height: 1.5;">
                            Merci d'utiliser notre application ! Nous apprécions votre confiance et espérons que notre
                            service répondra à vos attentes.
                        </p>

                        <p style="font-size: 12px; color: #999999; margin: 0; border-top: 1px solid #eeeeee; padding-top: 15px;">
                            Ceci est un email automatique, merci de ne pas répondre.
                        </p>
                    </div>
                </td>
            </tr>
        </table>
    </div>
</#macro>