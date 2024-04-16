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
                    Are you sure you want to delete this episode?
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    <a href="/admin/episodes/${episode.uuid}/delete" class="btn btn-danger">Confirm</a>
                </div>
            </div>
        </div>
    </div>

    <form action="/admin/episodes/${episode.uuid}" method="POST">
        <div class="d-flex mb-3">
            <a href="/admin/episodes" class="btn btn-secondary ms-0 me-auto">Back</a>
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
                                   value="${episode.uuid.toString()}" aria-label="UUID"
                                   aria-describedby="basic-addon" disabled>
                            <span class="input-group-text" id="basic-addon"
                                  onclick="copyToClipboard('${episode.uuid.toString()}')" style="cursor: pointer"><i
                                        class="bi bi-clipboard"></i></span>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <label for="anime" class="form-label">Anime</label>
                        <input type="text" class="form-control" id="anime" name="anime" value="${su.sanitizeXSS(episode.anime.name)}">
                    </div>
                    <div class="col-md-6">
                        <label for="episodeType" class="form-label">Episode type</label>

                        <select class="form-select" id="episodeType" name="episodeType">
                            <#list episodeTypes as episodeType>
                                <option value="${episodeType}"<#if episodeType == episode.episodeType> selected</#if>>${episodeType}</option>
                            </#list>
                        </select>
                    </div>
                    <div class="col-md-6">
                        <label for="langType" class="form-label">Lang type</label>

                        <select class="form-select" id="langType" name="langType">
                            <#list langTypes as langType>
                                <option value="${langType}"<#if langType == episode.langType> selected</#if>>${langType}</option>
                            </#list>
                        </select>
                    </div>
                    <div class="col-md-6">
                        <label for="audioLocale" class="form-label">Audio locale</label>
                        <input type="text" class="form-control" id="audioLocale" name="audioLocale" value="${episode.audioLocale!""}">
                    </div>
                    <div class="col-md-6">
                        <label for="hash" class="form-label">Hash</label>
                        <input type="text" class="form-control" id="hash" name="hash" value="${episode.hash}">
                    </div>
                    <div class="col-md-6">
                        <label for="releaseDateTime" class="form-label">Release date time</label>
                        <input type="datetime-local" class="form-control" id="releaseDateTime" name="releaseDateTime"
                               value="${episode.releaseDateTime?keep_before_last(":")}">
                    </div>
                    <div class="col-md-6">
                        <label for="lastUpdateDateTime" class="form-label">Last update date time</label>
                        <input type="datetime-local" class="form-control disabled" id="lastUpdateDateTime"
                               name="lastUpdateDateTime"
                               value="<#if episode.lastUpdateDateTime??>${episode.lastUpdateDateTime?keep_before_last(":")}</#if>"
                               disabled>
                    </div>
                    <div class="col-md-6">
                        <label for="season" class="form-label">Season</label>
                        <input type="number" class="form-control" id="season" name="season" value="${episode.season?c}">
                    </div>
                    <div class="col-md-6">
                        <label for="number" class="form-label">Number</label>
                        <input type="number" class="form-control" id="number" name="number" value="${episode.number?c}">
                    </div>
                    <div class="col-md-6">
                        <label for="title" class="form-label">Title</label>
                        <input type="text" class="form-control" id="title" name="title" value="${episode.title!""}">
                    </div>
                    <div class="col-md-6">
                        <label for="description" class="form-label">Description</label>
                        <textarea class="form-control" id="description" name="description"
                                  rows="6">${episode.description!""}</textarea>
                    </div>
                    <div class="col-md-6">
                        <label for="url" class="form-label">URL</label>
                        <input type="text" class="form-control" id="url" name="url" value="${episode.url}">
                    </div>
                    <div class="col-md-6">
                        <label for="image" class="form-label">Image</label>
                        <input type="text" class="form-control" id="image" name="image" value="${episode.image}">
                    </div>
                    <div class="col-md-6">
                        <label for="duration" class="form-label">Duration</label>
                        <input type="number" class="form-control" id="duration" name="duration"
                               value="${episode.duration?c}">
                    </div>
                    <div class="col-md-6">
                        <p>Image</p>
                        <img src="/api/v1/episodes/${episode.uuid}/media-image" class="img-fluid w-50"
                             alt="Responsive image">
                    </div>
                </div>
            </div>
        </div>
    </form>
</@navigation.display>