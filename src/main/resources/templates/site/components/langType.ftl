<#macro display langType>
    <#if langType == 'SUBTITLES'>
        <img src="${baseUrl}/assets/img/icons/subtitles.svg" alt="Subtitles" class="me-1">
        Sous-titrage
    <#else>
        <img src="${baseUrl}/assets/img/icons/voice.svg" alt="Voice" class="me-1">
        Doublage
    </#if>
</#macro>