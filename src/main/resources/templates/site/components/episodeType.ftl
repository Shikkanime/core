<#function getPrefixEpisode(episodeType)>
    <#switch episodeType>
        <#case "EPISODE">
            <#return "Épisode">
        <#case "FILM">
            <#return "Film">
        <#case "SPECIAL">
            <#return "Spécial">
        <#case "SUMMARY">
            <#return "Épisode récapitulatif">
        <#case "SPIN_OFF">
            <#return "Spin-off">
    </#switch>
</#function>

<#macro display episodeType>${getPrefixEpisode(episodeType)}</#macro>