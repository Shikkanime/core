<#function prettyPrintDuration duration>
    <#assign str = "" />

    <#if (duration / 3600)?int gt 0>
        <#assign str = str + (duration / 3600)?int + ":" />
    </#if>

    <#if (duration / 3600)?int gt 0 && (duration / 60 % 60)?int lt 10>
        <#assign str = str + "0" + (duration / 60 % 60)?int />
    <#else>
        <#assign str = str + (duration / 60 % 60)?int />
    </#if>

    <#return str + ":" + (duration % 60)?string("00")>
</#function>

<#macro display duration>
    <div class="duration">
        ${prettyPrintDuration(duration)}
    </div>
</#macro>