<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <div class="toast-container position-fixed top-0 end-0 p-3">
        <div id="successToast" class="toast align-items-center text-bg-success border-0" role="alert"
             aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    Anime updated successfully!
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"
                        aria-label="Close"></button>
            </div>
        </div>

        <div id="errorToast" class="toast align-items-center text-bg-danger border-0" role="alert" aria-live="assertive"
             aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    Error updating anime!
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"
                        aria-label="Close"></button>
            </div>
        </div>
    </div>

    <div x-data="{
    anime: {},
    loading: true,
    tab: 0,
    titleSize: 2
    }">
        <div class="modal fade" id="deleteModal" tabindex="-1" aria-labelledby="deleteModalLabel" aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h1 class="modal-title fs-5" id="deleteModalLabel">Delete confirmation</h1>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        Are you sure you want to delete <strong x-text="anime.shortName"></strong>?
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>

                        <button class="btn btn-danger"
                                @click="loading = true; await deleteAnime(); window.location.href = '/admin/animes'">
                            Confirm
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <div class="d-flex">
            <a class="btn btn-secondary ms-0"  @click="window.history.back()">Back</a>

            <div class="spinner-border ms-2 me-auto" role="status" x-show="loading">
                <span class="visually-hidden">Loading...</span>
            </div>

            <button type="button" class="btn btn-danger ms-auto" data-bs-toggle="modal"
                    data-bs-target="#deleteModal" :disabled="loading">
                Delete
            </button>

            <button type="button" class="btn btn-info ms-2" :disabled="loading"
                    @click="loading = true; forceUpdateAnime().finally(() => loading = false)">
                Force Update
            </button>

            <button type="submit" class="btn btn-success ms-2 me-0" :disabled="loading"
                    @click="loading = true; updateAnime(anime).finally(() => loading = false)">
                Update
            </button>
        </div>

        <ul class="nav nav-underline nav-fill mt-3">
            <li class="nav-item">
                <a class="nav-link" :class="tab === 0 ? 'active' : ''" @click="tab = 0">General</a>
            </li>
            <li class="nav-item">
                <a class="nav-link" :class="tab === 1 ? 'active' : ''" @click="tab = 1">Images</a>
            </li>
            <li class="nav-item">
                <a class="nav-link" :class="tab === 2 ? 'active' : ''" @click="tab = 2">More informations</a>
            </li>
        </ul>

        <template x-init="loading = true;
        anime = await loadAnime();
        loading = false;" x-if="anime.uuid">
            <div class="card">
                <div x-show="tab === 0" class="card-body">
                    <div class="row mb-2">
                        <div :class="'col-md-' + titleSize">
                            <label for="uuid" class="form-label">UUID</label>
                        </div>

                        <div :class="'col-md-' + (12 - titleSize)">
                            <div class="input-group">
                                <input type="text" class="form-control disabled" id="uuid" name="uuid"
                                       x-model="anime.uuid" aria-label="UUID"
                                       aria-describedby="basic-addon" disabled>

                                <span class="input-group-text" id="basic-addon"
                                      @click="copyToClipboard(anime.uuid)" style="cursor: pointer"><i
                                            class="bi bi-clipboard"></i></span>
                            </div>
                        </div>
                    </div>
                    <div class="row mb-2">
                        <div :class="'col-md-' + titleSize">
                            <label for="name" class="form-label">Name</label>
                        </div>
                        <div :class="'col-md-' + (12 - titleSize)">
                            <input type="text" class="form-control" id="name" name="name" x-model="anime.name">
                        </div>
                    </div>
                    <div class="row mb-2">
                        <div :class="'col-md-' + titleSize">
                            <label for="shortName" class="form-label">Short name</label>
                            <p class="text-muted">This is the name that will be displayed on the website and it's automatically generated from the name</p>
                        </div>
                        <div :class="'col-md-' + (12 - titleSize)">
                            <input type="text" class="form-control" id="shortName" name="shortName"
                                   x-model="anime.shortName" disabled>
                        </div>
                    </div>
                    <div class="row mb-2">
                        <div :class="'col-md-' + titleSize">
                            <label for="slug" class="form-label">Slug</label>
                            <p class="text-muted">This is the slug that will be used in the URL to access the anime page</p>
                        </div>
                        <div :class="'col-md-' + (12 - titleSize)">
                            <input type="text" class="form-control mb-2" id="slug" name="slug"
                                   x-model="anime.slug">
                            <span class="text-warning">If you put a slug that already exists, the two animes will be merged</span>
                        </div>
                    </div>
                    <div class="row mb-2">
                        <div :class="'col-md-' + titleSize">
                            <label for="releaseDateTime" class="form-label">Release date time</label>
                            <p class="text-muted">This is the date and time when the anime was released based on the first episode</p>
                        </div>
                        <div :class="'col-md-' + (12 - titleSize)">
                            <input type="datetime-local" class="form-control" id="releaseDateTime"
                                   name="releaseDateTime"
                                   :value="anime.releaseDateTime.substring(0, 16)" disabled>
                        </div>
                    </div>
                    <div class="row mb-2">
                        <div :class="'col-md-' + titleSize">
                            <label for="lastReleaseDateTime" class="form-label">Last release date time</label>
                            <p class="text-muted">This is the date and time when the anime was last released based on the last episode</p>
                        </div>
                        <div :class="'col-md-' + (12 - titleSize)">
                            <input type="datetime-local" class="form-control" id="lastReleaseDateTime"
                                   name="lastReleaseDateTime"
                                   :value="anime.lastReleaseDateTime.substring(0, 16)" disabled>
                        </div>
                    </div>
                    <div class="row mb-2">
                        <div :class="'col-md-' + titleSize">
                            <label for="lastUpdateDateTime" class="form-label">Last update date time</label>
                            <p class="text-muted">This is the date and time when the anime was last updated</p>
                        </div>
                        <div :class="'col-md-' + (12 - titleSize)">
                            <input type="datetime-local" class="form-control" id="lastUpdateDateTime"
                                   name="lastUpdateDateTime"
                                   :value="anime.lastUpdateDateTime.substring(0, 16)" disabled>
                        </div>
                    </div>
                    <div class="row">
                        <div :class="'col-md-' + titleSize">
                            <label for="description" class="form-label">Description</label>
                        </div>
                        <div :class="'col-md-' + (12 - titleSize)">
                        <textarea class="form-control" id="description" name="description"
                                  rows="6" x-model="anime.description"></textarea>
                        </div>
                    </div>
                </div>
                <div x-show="tab === 1" class="card-body">
                    <div class="row g-4">
                        <div class="col-md-6">
                            <label for="image" class="form-label fw-bold">Image (Thumbnail)</label>
                            <div class="input-group mb-3">
                                <input type="text" class="form-control" id="image" x-model="anime.thumbnail" placeholder="URL">
                                <a class="input-group-text" :href="anime.thumbnail" target="_blank" x-show="anime.thumbnail">
                                    <i class="bi bi-box-arrow-up-right"></i>
                                </a>
                            </div>
                            <div class="border rounded p-2 text-center bg-light d-flex align-items-center justify-content-center" style="height: 200px;">
                                <img :src="'${apiUrl}/v1/attachments?uuid=' + anime.uuid + '&type=THUMBNAIL'" class="img-fluid rounded shadow-sm" style="max-height: 100%;" alt="Thumbnail">
                            </div>
                        </div>

                        <div class="col-md-6">
                            <label for="banner" class="form-label fw-bold">Banner</label>
                            <div class="input-group mb-3">
                                <input type="text" class="form-control" id="banner" x-model="anime.banner" placeholder="URL">
                                <a class="input-group-text" :href="anime.banner" target="_blank" x-show="anime.banner">
                                    <i class="bi bi-box-arrow-up-right"></i>
                                </a>
                            </div>
                            <div class="border rounded p-2 text-center bg-light d-flex align-items-center justify-content-center" style="height: 200px;">
                                <img :src="'${apiUrl}/v1/attachments?uuid=' + anime.uuid + '&type=BANNER'" class="img-fluid rounded shadow-sm" style="max-height: 100%;" alt="Banner">
                            </div>
                        </div>

                        <div class="col-md-6">
                            <label for="carousel" class="form-label fw-bold">Carousel</label>
                            <div class="input-group mb-3">
                                <input type="text" class="form-control" id="carousel" x-model="anime.carousel" placeholder="URL">
                                <a class="input-group-text" :href="anime.carousel" target="_blank" x-show="anime.carousel">
                                    <i class="bi bi-box-arrow-up-right"></i>
                                </a>
                            </div>
                            <div class="border rounded p-2 text-center bg-light d-flex align-items-center justify-content-center" style="height: 200px;">
                                <img :src="'${apiUrl}/v1/attachments?uuid=' + anime.uuid + '&type=CAROUSEL'" class="img-fluid rounded shadow-sm" style="max-height: 100%;" alt="Carousel">
                            </div>
                        </div>

                        <div class="col-md-6">
                            <label for="title" class="form-label fw-bold">Title</label>
                            <div class="input-group mb-3">
                                <input type="text" class="form-control" id="title" x-model="anime.title" placeholder="URL">
                                <a class="input-group-text" :href="anime.title" target="_blank" x-show="anime.title">
                                    <i class="bi bi-box-arrow-up-right"></i>
                                </a>
                            </div>
                            <div class="border rounded p-2 text-center bg-black d-flex align-items-center justify-content-center" style="height: 200px;">
                                <img :src="'${apiUrl}/v1/attachments?uuid=' + anime.uuid + '&type=TITLE'" class="img-fluid" style="max-height: 100%;" alt="Title">
                            </div>
                        </div>
                    </div>
                </div>
                <div x-show="tab === 2" class="card-body">
                    <div class="row g-4">
                        <div class="col-md-6">
                            <label class="form-label d-block fw-bold mb-2">Genres</label>
                            <div class="d-flex flex-wrap gap-2">
                                <template x-for="genre in anime.genres">
                                    <span class="badge rounded-pill text-bg-info" x-text="genre.name"></span>
                                </template>
                                <template x-if="!anime.genres || anime.genres.length === 0">
                                    <span class="text-muted small">No genres</span>
                                </template>
                            </div>
                        </div>

                        <div class="col-md-6">
                            <label class="form-label d-block fw-bold mb-2">Tags</label>
                            <div class="d-flex flex-wrap gap-2">
                                <template x-for="animeTag in anime.tags">
                                    <div class="badge rounded-pill text-bg-secondary d-flex align-items-center gap-1">
                                        <span x-text="animeTag.tag.name"></span>
                                        <template x-if="animeTag.isAdult">
                                            <i class="bi bi-exclamation-triangle-fill text-danger" title="Adult"></i>
                                        </template>
                                        <template x-if="animeTag.isSpoiler">
                                            <i class="bi bi-eye-slash-fill text-warning" title="Spoiler"></i>
                                        </template>
                                    </div>
                                </template>
                                <template x-if="!anime.tags || anime.tags.length === 0">
                                    <span class="text-muted small">No tags</span>
                                </template>
                            </div>
                        </div>

                        <div class="col-md-6">
                            <label class="form-label d-block fw-bold mb-2">Simulcasts</label>
                            <div class="d-flex flex-wrap gap-2">
                                <template x-for="simulcast in anime.simulcasts">
                                    <span class="badge rounded-pill text-bg-primary" x-text="simulcast.label"></span>
                                </template>
                                <template x-if="!anime.simulcasts || anime.simulcasts.length === 0">
                                    <span class="text-muted small">No simulcasts</span>
                                </template>
                            </div>
                        </div>

                        <div class="col-12">
                            <label class="form-label fw-bold mb-2">Platform IDs</label>
                            <div class="table-responsive">
                                <table class="table table-hover table-sm align-middle border">
                                    <thead class="table-light">
                                    <tr>
                                        <th scope="col">Platform</th>
                                        <th scope="col">ID</th>
                                        <th scope="col">Last validation</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <template x-for="animePlatform in anime.platformIds">
                                        <tr>
                                            <td class="fw-semibold" x-text="animePlatform.platform.name"></td>
                                            <td>
                                                <a :href="animePlatform.url" class="text-decoration-none d-flex align-items-center gap-1" target="_blank">
                                                    <span x-text="animePlatform.platformId"></span>
                                                    <i class="bi bi-box-arrow-up-right small"></i>
                                                </a>
                                            </td>
                                            <td class="text-muted small" x-text="animePlatform.lastValidateDateTime ? new Date(animePlatform.lastValidateDateTime).toLocaleString() : 'N/A'"></td>
                                        </tr>
                                    </template>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </template>
    </div>

    <script>
        function getUuid() {
            const url = new URL(window.location.href);
            return url.pathname.split('/').pop();
        }

        async function loadAnime() {
            const uuid = getUuid();

            return await fetch('/admin/api/animes/' + uuid)
                .then(response => response.json())
                .catch(() => {
                    const toastEl = document.getElementById('errorToast');
                    const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastEl)
                    toastBootstrap.show();
                    // Wait 1s
                    return new Promise(resolve => setTimeout(resolve, 1000)).then(() => window.location.href = '/admin/animes');
                });
        }

        async function updateAnime(anime) {
            const uuid = getUuid();

            await fetch('/admin/api/animes/' + uuid, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(anime)
            })
                .then(() => {
                    const toastEl = document.getElementById('successToast');
                    const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastEl)
                    toastBootstrap.show();
                })
                .catch(() => {
                    const toastEl = document.getElementById('errorToast');
                    const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastEl)
                    toastBootstrap.show();
                });
        }

        async function forceUpdateAnime() {
            const uuid = getUuid();

            await fetch('/admin/api/animes/' + uuid + '/force-update')
                .then(() => {
                    const toastEl = document.getElementById('successToast');
                    toastEl.querySelector('.toast-body').textContent = 'Force update requested successfully!';
                    const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastEl)
                    toastBootstrap.show();
                    // Reload page to see updated status
                    setTimeout(() => window.location.reload(), 1000);
                })
                .catch(() => {
                    const toastEl = document.getElementById('errorToast');
                    toastEl.querySelector('.toast-body').textContent = 'Error during force update!';
                    const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastEl)
                    toastBootstrap.show();
                });
        }

        async function deleteAnime() {
            const uuid = getUuid();

            await fetch('/admin/api/animes/' + uuid, {
                method: 'DELETE'
            })
                .catch(() => {
                    const toastEl = document.getElementById('errorToast');
                    const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastEl)
                    toastBootstrap.show();
                });
        }
    </script>
</@navigation.display>