<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div class="d-flex mb-3">
        <a href="/admin/images/invalidate" class="btn btn-danger ms-auto me-0">Invalidate</a>
        <a href="/admin/images/save" class="btn btn-success ms-2 me-0">Save</a>
    </div>

    <div class="card">
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label" for="size">Size</label>
                    <input id="size" name="size" type="number" class="form-control disabled" value="${size?c}" disabled>
                </div>

                <div class="col-md-4">
                    <label class="form-label" for="originalSize">Original size</label>
                    <input id="originalSize" name="originalSize" type="text" class="form-control disabled" value="${originalSize}" disabled>
                </div>

                <div class="col-md-4">
                    <label class="form-label" for="compressedSize">Compressed size</label>
                    <input id="compressedSize" name="compressedSize" type="text" class="form-control disabled" value="${compressedSize}" disabled>
                </div>
            </div>
        </div>
    </div>
</@navigation.display>