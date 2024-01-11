<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <div class="accordion" id="accordionPlatforms">
        <#list platforms as abstractPlatform>
            <#assign platformName = abstractPlatform.platform.name()>
            <#assign collapseId = "collapse${platformName}">

            <div class="accordion-item">
                <h2 class="accordion-header">
                    <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse"
                            data-bs-target="#${collapseId}" aria-expanded="false"
                            aria-controls="${collapseId}">
                        <img src="/assets/img/platforms/${abstractPlatform.platform.image}"
                             alt="${abstractPlatform.platform.platformName}"
                             class="me-2 rounded-circle" height="24">
                        ${abstractPlatform.platform.platformName}
                    </button>
                </h2>
                <div id="${collapseId}" class="accordion-collapse collapse" data-bs-parent="#accordionPlatforms">
                    <form action="/admin/platforms" method="post">
                        <input class="d-none" type="text" name="platform" value="${abstractPlatform.platform.name()}">

                        <div class="accordion-body">
                            <div class="row g-3">
                                <#list abstractPlatform.configuration.toConfigurationFields() as configurationField>
                                    <div class="col-md-6">
                                        <#assign fieldNameForm = "${platformName}${configurationField.name}">

                                        <label class="form-label"
                                               for="${fieldNameForm}">${configurationField.label}</label>

                                        <input id="${fieldNameForm}" name="${configurationField.name}"
                                               type="${configurationField.type}"
                                               class="form-control"
                                               value="${configurationField.value}">

                                        <#if configurationField.caption??>
                                            <div class="form-text">${configurationField.caption}</div>
                                        </#if>
                                    </div>
                                </#list>
                            </div>

                            <div class="mt-3">
                                <h3 class="mb-3">Simulcasts</h3>

                                <#list abstractPlatform.configuration.simulcasts as simulcast>
                                    <a href="/admin/platforms/${abstractPlatform.platform.name()}/simulcasts/${simulcast.uuid}"
                                       class="card text-decoration-none mb-3">
                                        <div class="card-body">
                                            ${simulcast.name}
                                        </div>
                                    </a>
                                </#list>

                                <a href="/admin/platforms/${abstractPlatform.platform.name()}/simulcasts"
                                   class="card text-decoration-none">
                                    <div class="card-body">
                                        <i class="bi bi-plus-circle me-2"></i>
                                        Add simulcast
                                    </div>
                                </a>
                            </div>

                            <hr class="my-3">

                            <div class="d-flex">
                                <button type="submit" class="btn btn-primary ms-auto mr-0">Update</button>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
        </#list>
    </div>
</@navigation.display>