<#import "_layout.ftl" as layout />
<@layout.main>
    <div class="accordion" id="accordionPlatforms">
        <#list platforms as abstractPlatform>
            <#assign platformName = abstractPlatform.platform.name()>
            <#assign collapseId = "collapse${platformName}">

            <div class="accordion-item">
                <h2 class="accordion-header">
                    <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse"
                            data-bs-target="#${collapseId}" aria-expanded="false"
                            aria-controls="${collapseId}">
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

                                        <#if configurationField.type == "list">
                                            <div class="form">
                                                <div class="list-values">
                                                    <#list configurationField.value as value>
                                                        <div class="input-group mb-3">
                                                            <input id="${fieldNameForm}"
                                                                   name="${configurationField.name}" type="text"
                                                                   class="form-control"
                                                                   value="${value}">
                                                            <button class="btn btn-outline-danger" type="button"
                                                                    onclick="deleteElement(this)">
                                                                <i class="bi bi-trash"></i>
                                                            </button>
                                                        </div>
                                                    </#list>
                                                </div>

                                                <div class="input-group mb-3">
                                                    <input type="text" class="form-control" placeholder="Add new"
                                                           value="" data-id="${fieldNameForm}"
                                                           data-name="${configurationField.name}">
                                                    <button class="btn btn-outline-success" type="button"
                                                            onclick="addElement(this)">
                                                        <i class="bi bi-plus"></i>
                                                    </button>
                                                </div>
                                            </div>

                                        <#else>
                                            <input id="${fieldNameForm}" name="${configurationField.name}"
                                                   type="${configurationField.type}"
                                                   class="form-control"
                                                   value="${configurationField.value}">
                                        </#if>
                                    </div>
                                </#list>
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

    <script>
        function deleteElement(deleteButton) {
            const closestInputGroup = deleteButton.closest('.input-group');
            const closestInput = closestInputGroup.querySelector('input');

            if (confirm('Are you sure you want to delete ' + closestInput.value + '?')) {
                closestInputGroup.remove();
            }
        }

        function addElement(addButton) {
            const closestInputGroup = addButton.closest('.input-group');
            const closestInput = closestInputGroup.querySelector('input');
            const list = closestInputGroup.closest('form').querySelector('.list-values');

            const newInputGroup = document.createElement('div');
            newInputGroup.classList.add('input-group', 'mb-3');
            newInputGroup.innerHTML = `
                <input id="` + closestInput.dataset['id'] + `" name="` + closestInput.dataset['name'] + `" type="text" class="form-control" value="` + closestInput.value + `">
                <button class="btn btn-outline-danger" type="button" onclick="deleteElement(this)">
                    <i class="bi bi-trash"></i>
                </button>
            `;

            list.appendChild(newInputGroup);
            closestInput.value = '';
            closestInput.focus();
        }
    </script>
</@layout.main>