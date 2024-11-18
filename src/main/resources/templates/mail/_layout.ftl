<#macro main title="" description="" showCode=true>
    <div style="font-family: Arial, sans-serif; margin: 0; padding: 0; box-sizing: border-box; width: 100%;">
        <table style="width: 100%; border-collapse: collapse; background-color: #f5f5f5;">
            <tr>
                <td style="vertical-align: top; text-align: center;">
                    <div style="display: inline-block; max-width: 800px; width: 100%; margin: 50px 10px; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); padding: 20px; box-sizing: border-box;">
                        <a href="${baseUrl}">
                            <img src="${baseUrl}/assets/img/dark_banner.png"
                                 alt="Illustration"
                                 style="width: 100%; max-width: 400px; height: auto; margin-bottom: 20px;">
                        </a>

                        <#if title?? && title?length != 0>
                            <div style="font-size: 24px; font-weight: bold; margin-bottom: 20px;">
                                ${title}
                            </div>
                        </#if>

                        <#if showCode && code?? && code?length != 0>
                            <div style="background-color: #f0f0f0; border-radius: 4px; padding: 20px 30px; margin-bottom: 20px; display: inline-table;">
                                <table style="border-collapse: collapse;">
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
                            <div style="font-size: 14px; color: #666666; margin-bottom: 30px;">
                                ${description}
                            </div>
                        </#if>

                        <#nested />

                        <p style="font-size: 16px; color: #333333; margin-bottom: 20px;">
                            Merci d'utiliser notre application ! Nous apprécions votre confiance et espérons que notre
                            service répondra à vos attentes.
                        </p>
                        <p style="font-size: 12px; color: #999999; margin: 0;">
                            Ceci est un email automatique, merci de ne pas répondre.
                        </p>
                    </div>
                </td>
            </tr>
        </table>
    </div>
</#macro>