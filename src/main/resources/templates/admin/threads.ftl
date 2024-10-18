<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div class="card p-3">
        <div class="d-flex align-items-center">
            <a href="${askCodeUrl}" class="btn btn-primary">Ask Code</a>

            <#if success?? && success == 1>
                <div class="alert alert-success ms-3 mb-0" role="alert">
                    Code confirmed and token generated successfully.
                </div>
            </#if>
        </div>
    </div>

    <div class="card mt-3 p-3">
        <form action="${baseUrl}/admin/threads-publish" method="get">
            <textarea id="message" name="message" class="form-control" rows="3" placeholder="Message"></textarea>
            <input type="text" id="image_url" name="image_url" class="form-control mt-3" placeholder="Image URL">
            <button type="submit" class="btn btn-primary mt-3">Publish</button>
        </form>
    </div>
</@navigation.display>