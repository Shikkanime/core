<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <div class="modal fade" id="deleteModal" tabindex="-1" aria-labelledby="deleteModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h1 class="modal-title fs-5" id="deleteModalLabel">Delete confirmation</h1>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    Are you sure you want to delete this anime?
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    <a href="/admin/animes/${anime.uuid}/delete" class="btn btn-danger">Confirm</a>
                </div>
            </div>
        </div>
    </div>

    <form action="/admin/animes/${anime.uuid}" method="POST">
        <div class="d-flex mb-3">
            <a href="/admin/animes" class="btn btn-secondary ms-0 me-auto">Back</a>
            <button type="button" class="btn btn-danger ms-auto me-0" data-bs-toggle="modal"
                    data-bs-target="#deleteModal">
                Delete
            </button>
            <button type="submit" class="btn btn-success ms-2 me-0">Update</button>
        </div>

        <div class="card">
            <div class="card-body">
                <div class="row g-3">
                    <div class="col-md-6">
                        <label for="uuid" class="form-label">UUID</label>

                        <div class="input-group">
                            <input type="text" class="form-control disabled" id="uuid" name="uuid"
                                   value="${anime.uuid.toString()}" aria-label="UUID"
                                   aria-describedby="basic-addon" disabled>
                            <span class="input-group-text" id="basic-addon"
                                  onclick="copyToClipboard('${anime.uuid.toString()}')" style="cursor: pointer"><i
                                        class="bi bi-clipboard"></i></span>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <label for="name" class="form-label">Name</label>
                        <input type="text" class="form-control" id="name" name="name" value="${su.sanitizeXSS(anime.name)}">
                    </div>
                    <div class="col-md-6">
                        <label for="shortName" class="form-label">Short name</label>
                        <input type="text" class="form-control" id="shortName" name="shortName"
                               value="${su.sanitizeXSS(anime.shortName)}" disabled>
                    </div>
                    <div class="col-md-6">
                        <label for="slug" class="form-label">Short name</label>
                        <input type="text" class="form-control" id="slug" name="slug"
                               value="${anime.slug}">
                    </div>
                    <div class="col-md-6">
                        <label for="releaseDateTime" class="form-label">Release date time</label>
                        <input type="datetime-local" class="form-control" id="releaseDateTime" name="releaseDateTime"
                               value="${anime.releaseDateTime?keep_before_last(":")}">
                    </div>
                    <div class="col-md-6">
                        <label for="image" class="form-label">Image</label>
                        <input type="text" class="form-control" id="image" name="image" value="${anime.image}">
                    </div>
                    <div class="col-md-6">
                        <label for="banner" class="form-label">Banner</label>
                        <input type="text" class="form-control" id="banner" name="banner" value="${anime.banner!""}">
                    </div>
                    <div class="col-md-6">
                        <label for="description" class="form-label">Description</label>
                        <textarea class="form-control" id="description" name="description"
                                  rows="6">${anime.description}</textarea>
                    </div>
                    <div class="col-md-6">
                        <label class="form-label">Simulcasts</label>

                        <ul>
                            <#list anime.simulcasts as simulcast>
                                <li>${simulcast.season} ${simulcast.year?c}</li>
                            </#list>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </form>
</@navigation.display>