<#-- @ftlvariable name="files" type="java.util.List<java.io.File>" -->
<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Download Generated Files</h3>
                </div>
                <div class="card-body">
                    <#if files?has_content>
                        <ul class="list-group">
                            <#list files as file>
                                <li class="list-group-item d-flex justify-content-between align-items-center">
                                    ${file.name}
                                    <a href="/admin/episode-manager/download?file=${file.name}" class="btn btn-primary btn-sm">Download</a>
                                </li>
                            </#list>
                        </ul>
                    <#else>
                        <p>No files found.</p>
                    </#if>
                </div>
            </div>
        </div>
    </div>

    <div class="row mt-4">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Import File</h3>
                </div>
                <div class="card-body">
                    <form action="/admin/episode-manager/import" method="post" enctype="multipart/form-data">
                        <div class="mb-3">
                            <label for="importFile" class="form-label">Select XLSX file to import:</label>
                            <input class="form-control" type="file" id="importFile" name="importFile" accept=".xlsx" required>
                        </div>
                        <button type="submit" class="btn btn-primary">Import</button>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <div class="row mt-4">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Update from AniDB</h3>
                </div>
                <div class="card-body">
                    <form action="/admin/episode-manager/anidb-update" method="post" enctype="multipart/form-data">
                        <div class="mb-3">
                            <label for="animeName" class="form-label">Anime Name:</label>
                            <input type="text" class="form-control" id="animeName" name="animeName" required>
                        </div>
                        <div class="mb-3">
                            <label for="anidbFile" class="form-label">Select XLSX file to update:</label>
                            <input class="form-control" type="file" id="anidbFile" name="anidbFile" accept=".xlsx" required>
                        </div>
                        <button type="submit" class="btn btn-primary">Update and Download</button>
                    </form>
                </div>
            </div>
        </div>
    </div>
</@navigation.display>
