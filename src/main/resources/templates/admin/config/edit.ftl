<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <form action="/admin/config/${config.uuid}" method="POST">
        <div class="d-flex mb-3">
            <a href="/admin/config" class="btn btn-secondary ms-0 me-auto">Back</a>
            <button type="submit" class="btn btn-success ms-2 me-0">Update</button>
        </div>

        <div class="card">
            <div class="card-body">
                <div class="row g-3">
                    <div class="col-md-6">
                        <label for="uuid" class="form-label">UUID</label>

                        <div class="input-group">
                            <input type="text" class="form-control disabled" id="uuid" name="uuid"
                                   value="${config.uuid.toString()}" aria-label="UUID"
                                   aria-describedby="basic-addon" disabled>

                            <span class="input-group-text" id="basic-addon"
                                  onclick="copyToClipboard('${config.uuid.toString()}')" style="cursor: pointer"><i
                                        class="bi bi-clipboard"></i></span>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <label for="key" class="form-label">Key</label>
                        <input type="text" class="form-control" id="key" name="key" value="${config.propertyKey}"
                               disabled>
                    </div>
                    <div class="col-md-6">
                        <label for="value" class="form-label">Value</label>
                        <input type="text" class="form-control" id="value" name="value" value="${config.propertyValue}">
                    </div>
                </div>
            </div>
        </div>
    </form>
</@navigation.display>