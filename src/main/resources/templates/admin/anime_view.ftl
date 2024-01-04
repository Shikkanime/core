<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <form action="/admin/animes/${anime.uuid}" method="POST">
        <div class="d-flex mb-3">
            <a href="/admin/animes" class="btn btn-secondary ms-0 me-auto">Back</a>
            <button type="submit" class="btn btn-success ms-auto me-0">Save</button>
        </div>

        <div class="card">
            <div class="card-body">
                <div class="row g-3">
                    <div class="col-md-6">
                        <label for="uuid" class="form-label">UUID</label>
                        <input type="text" class="form-control disabled" id="uuid" name="uuid" value="${anime.uuid.toString()}">
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
                        <textarea class="form-control" id="description" name="description" rows="6">${anime.description}</textarea>
                    </div>
                </div>
            </div>
        </div>
    </form>
</@navigation.display>