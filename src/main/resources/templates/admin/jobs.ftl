<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <form action="/admin/jobs" method="post">
        <div class="d-flex mb-3">
            <button type="submit" class="btn btn-success ms-auto me-0">
                Launch
            </button>
        </div>

        <div class="card">
            <div class="card-body">
                <div class="row">
                    <div class="col-md-2">
                        <label for="jobName" class="form-label">Job name</label>
                    </div>

                    <div class="col-md-10">
                        <select name="jobName" id="jobName" class="form-select" required>
                            <option value="" selected disabled>Select a job</option>
                            <#list jobs as job>
                                <option value="${job}">${job}</option>
                            </#list>
                        </select>
                    </div>
                </div>
            </div>
        </div>
    </form>
</@navigation.display>