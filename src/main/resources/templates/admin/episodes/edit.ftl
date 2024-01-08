<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <form action="/admin/episodes/${episode.uuid}" method="POST">
        <div class="d-flex mb-3">
            <a href="/admin/episodes" class="btn btn-secondary ms-0 me-auto">Back</a>
            <a href="/admin/episodes/${episode.uuid}/delete" class="btn btn-danger ms-auto me-0">Delete</a>
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
                        <input type="text" class="form-control" id="anime" name="anime" value="${episode.anime.name}"
                               disabled>
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
                        <label for="hash" class="form-label">Hash</label>
                        <input type="text" class="form-control" id="hash" name="hash" value="${episode.hash}">
                    </div>
                    <div class="col-md-6">
                        <label for="releaseDateTime" class="form-label">Release date time</label>
                        <input type="datetime-local" class="form-control" id="releaseDateTime" name="releaseDateTime"
                               value="${episode.releaseDateTime?keep_before_last(":")}">
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
                        <input type="text" class="form-control" id="title" name="title" value="${episode.title}">
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
                </div>
            </div>
        </div>
    </form>

    <script>
        function copyToClipboard(content) {
            const textarea = document.createElement("textarea");
            textarea.style.height = 0;
            document.body.appendChild(textarea);
            textarea.value = content;
            textarea.select();
            document.execCommand("copy");
            document.body.removeChild(textarea);
        }
    </script>
</@navigation.display>