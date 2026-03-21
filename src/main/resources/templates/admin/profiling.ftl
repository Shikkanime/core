<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div class="row g-2">
        <div class="col-12">
            <div class="card p-3">
                <h5 class="card-title">JFR Reports</h5>
                <p class="text-muted">Deep analysis reports that can be opened in IntelliJ IDEA or Java Mission Control.</p>
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                        <tr>
                            <th>Filename</th>
                            <th>Size</th>
                            <th>Last Modified</th>
                            <th>Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        <#list dumpFiles as file>
                            <tr>
                                <td>${file.name}</td>
                                <td>${(file.length() / 1024 / 1024)?string("0.##")} MB</td>
                                <td>${file.lastModified()?number_to_datetime}</td>
                                <td>
                                    <a href="/admin/profiling/download/${file.name}" class="btn btn-sm btn-primary">
                                        <i class="bi bi-download"></i> Download
                                    </a>
                                </td>
                            </tr>
                        <#else>
                            <tr>
                                <td colspan="4" class="text-center">No dump reports found yet.</td>
                            </tr>
                        </#list>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</@navigation.display>