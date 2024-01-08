<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <form action="/admin/animes/${anime.uuid}" method="POST">
        <div class="d-flex mb-3">
            <a href="/admin/animes" class="btn btn-secondary ms-0 me-auto">Back</a>
            <a href="/admin/animes/${anime.uuid}/delete" class="btn btn-danger ms-auto me-0">Delete</a>
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
                        <input type="text" class="form-control" id="name" name="name" value="${anime.name}">
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