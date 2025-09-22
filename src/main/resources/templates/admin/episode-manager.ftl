<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div class="card mt-3 p-3 d-flex flex-column">
        <form class="ms-auto me-0 d-flex" action="/admin/episode-manager/import" method="post" enctype="multipart/form-data">
            <input class="form-control me-3" type="file" id="importFile" name="importFile" accept=".xlsx" required>
            <button type="submit" class="btn btn-primary">Import</button>
        </form>

        <#if files?has_content>
            <ul class="list-group mt-3">
                <#list files as file>
                    <li class="list-group-item d-flex justify-content-between align-items-center">
                        ${file.name}
                        <div>
                            <a href="/admin/episode-manager/download?file=${file.name}" class="btn btn-success btn-sm"><i class="bi bi-cloud-download-fill"></i></a>
                            <a href="/admin/episode-manager/delete?file=${file.name}" class="btn btn-danger btn-sm"><i class="bi bi-trash-fill"></i></a>
                        </div>
                    </li>
                </#list>
            </ul>
        </#if>
    </div>
</@navigation.display>