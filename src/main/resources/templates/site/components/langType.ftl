<#macro display langType>
    <span class="d-flex align-items-center">
        <#if langType == 'SUBTITLES'>
            <img src="${baseUrl}/assets/img/icons/subtitles.svg" alt="Subtitles" class="me-2">
            Sous-titrage
        <#else>
            <img src="${baseUrl}/assets/img/icons/voice.svg" alt="Voice" class="me-2">
            Doublage
        </#if>
    </span>
</#macro>