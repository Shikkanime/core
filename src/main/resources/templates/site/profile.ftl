<#import "_navigation.ftl" as navigation />

<@navigation.display canonicalUrl="${baseUrl}/@${member.username}">
    <div class="mt-3 container">
        <div class="row">
            <div class="col-md-4">
                <div class="d-flex justify-content-center align-items-center">
                    <img loading="lazy" src="${apiUrl}/v1/attachments?uuid=${member.uuid}&type=image"
                         alt="${su.sanitizeXSS(member.username)} profile"
                         class="img-fluid rounded-4"
                         width="128"
                         height="128">

                    <div class="ms-3">
                        <h1 class="h1 fw-bold">${member.username}</h1>
                        <p class="text-muted mb-0">Dernière connexion : ...</p>
                        <p class="text-muted">Compte créer le : ${member.creationDateTime?datetime.iso?string["dd/MM/yyyy"]}</p>
                    </div>
                </div>
            </div>
            <div class="col-md-8 shikk-element">
                <div class="d-flex flex-column align-items-center justify-content-center" style="height: 100%;">
                    <#function printDuration duration>
                        <#local days = duration / 86400>
                        <#local hours = duration / 3600 % 24>
                        <#local minutes = duration % 60>
                        <#return "${days?int?c}j ${hours?int?c}h ${minutes?int?c}m">
                    </#function>

                    <p>${printDuration(member.totalDuration)} de visionnage</p>

                    <div class="progress w-100">
                        <div class="progress-bar" style="width: ${((member.totalDuration / (member.totalDuration + member.totalUnseenDuration)) * 100)?c}%"></div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</@navigation.display>