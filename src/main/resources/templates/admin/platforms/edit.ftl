<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <form action="/admin/platforms/${platform.name()}/simulcasts" method="post">
        <#if simulcast_config.uuid??>
            <input class="d-none" type="text" name="uuid" value="${simulcast_config.uuid}">
        </#if>

        <div class="d-flex mb-3">
            <a href="/admin/platforms" class="btn btn-secondary ms-0 me-auto">Back</a>

            <#if simulcast_config.uuid??>
                <a href="/admin/platforms/${platform.name()}/simulcasts/${simulcast_config.uuid}/delete"
                   class="btn btn-danger ms-auto me-0">Delete</a>
                <button type="submit" class="btn btn-success ms-2 me-0">Update</button>
            <#else>
                <button type="submit" class="btn btn-success ms-auto me-0">Save</button>
            </#if>
        </div>

        <div class="card">
            <div class="card-body">
                <div class="row g-3">
                    <#list simulcast_config.toConfigurationFields() as configurationField>
                        <div class="col-md-6">
                            <#assign fieldNameForm = "${configurationField.name}">

                            <label class="form-label"
                                   for="${fieldNameForm}">${configurationField.label}</label>

                            <input id="${fieldNameForm}" name="${configurationField.name}"
                                   type="${configurationField.type}"
                                   class="form-control"
                                   value="<#if configurationField.value??>${configurationField.value}</#if>">

                            <#if configurationField.caption??>
                                <div class="form-text">${configurationField.caption}</div>
                            </#if>
                        </div>
                    </#list>
                </div>
            </div>
        </div>
    </form>
</@navigation.display>