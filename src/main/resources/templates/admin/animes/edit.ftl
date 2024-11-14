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
    simulcasts: [],
    anime: {},
    loading: true,
    selectedSimulcast: '',
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
            <a href="/admin/animes" class="btn btn-secondary ms-0">Back</a>

            <div class="spinner-border ms-2 me-auto" role="status" x-show="loading">
                <span class="visually-hidden">Loading...</span>
            </div>

            <button type="button" class="btn btn-danger ms-auto me-0" data-bs-toggle="modal"
                    data-bs-target="#deleteModal" :disabled="loading">
                Delete
            </button>

            <button type="submit" class="btn btn-success ms-2 me-0" :disabled="loading"
                    @click="loading = true; await updateAnime(anime); loading = false;">
                Update
            </button>
        </div>

        <ul class="nav nav-underline nav-fill mb-3">
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
        simulcasts = await getSimulcasts();
        anime = await loadAnime();
        loading = false;" x-if="anime.uuid && simulcasts.length > 0">
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
                    <div class="row mb-2">
                        <div :class="'col-md-' + titleSize">
                            <label for="image" class="form-label">Image</label>
                        </div>
                        <div :class="'col-md-' + (12 - titleSize)">
                            <div class="input-group mb-2">
                                <input type="text" class="form-control" id="image" name="image"
                                       x-model="anime.image" aria-label="Image"
                                       aria-describedby="basic-addon">

                                <a class="input-group-text" id="basic-addon"
                                   @click="" style="cursor: pointer" target="_blank" x-bind:href="anime.image">
                                    <i class="bi bi-box-arrow-up-right"></i>
                                </a>
                            </div>

                            <img :src="'${apiUrl}/v1/attachments?uuid=' + anime.uuid + '&type=image'" class="w-25" alt="Anime image">
                        </div>
                    </div>
                    <div class="row">
                        <div :class="'col-md-' + titleSize">
                            <label for="banner" class="form-label">Banner</label>
                        </div>

                        <div :class="'col-md-' + (12 - titleSize)">
                            <div class="input-group mb-2">
                                <input type="text" class="form-control" id="banner" name="banner"
                                       x-model="anime.banner" aria-label="Banner"
                                       aria-describedby="basic-addon">

                                <a class="input-group-text" id="basic-addon"
                                   @click="" style="cursor: pointer" target="_blank" x-bind:href="anime.banner">
                                    <i class="bi bi-box-arrow-up-right"></i>
                                </a>
                            </div>

                            <img :src="'${apiUrl}/v1/attachments?uuid=' + anime.uuid + '&type=banner'" class="w-25" alt="Anime image">
                        </div>
                    </div>
                </div>
                <div x-show="tab === 2" class="card-body">
                    <div class="row mb-2">
                        <div :class="'col-md-' + titleSize">
                            <label class="form-label">Platform IDs</label>
                        </div>

                        <div :class="'col-md-' + (12 - titleSize)">
                            <ul class="list-group">
                            <template x-for="animePlatform in anime.platformIds">
                                <li class="list-group-item">
                                    <div>
                                        <span x-text="animePlatform.platform.name"></span>
                                        <strong x-text="animePlatform.platformId"></strong>
                                    </div>
                                </li>
                            </template>
                            </ul>
                        </div>
                    </div>
                    <div class="row">
                        <div :class="'col-md-' + titleSize">
                            <p class="form-label my-0">Simulcasts</p>
                        </div>

                        <div :class="'col-md-' + (12 - titleSize)">
                            <ul class="list-group">
                                <template x-for="simulcast in anime.simulcasts">
                                    <li class="list-group-item d-flex justify-content-between align-items-center">
                                        <span x-text="simulcast.season + ' ' + simulcast.year"></span>
                                        <button type="button" class="btn btn-outline-danger" @click="anime.simulcasts.splice(anime.simulcasts.indexOf(simulcast), 1)">
                                            <i class="bi bi-trash-fill"></i>
                                        </button>
                                    </li>
                                </template>
                            </ul>

                            <hr class="w-100">

                            <div class="d-flex align-items-center my-1">
                                <div class="ms-0 me-3 w-100">
                                    <select class="form-select" x-model="selectedSimulcast">
                                        <option value="" selected disabled>Select a simulcast</option>

                                        <template
                                                x-for="simulcast in simulcasts.filter(simulcast => !anime.simulcasts.find(animeSimulcast => animeSimulcast.uuid === simulcast.uuid))">
                                            <option :value="simulcast.uuid"
                                                    x-text="simulcast.season + ' ' + simulcast.year"></option>
                                        </template>
                                    </select>
                                </div>
                                <div class="ms-auto me-0">
                                    <button type="button" class="btn btn-outline-success" @click="addSimulcast(simulcasts, anime, selectedSimulcast); selectedSimulcast = ''">
                                        +
                                    </button>
                                </div>
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

        async function getSimulcasts() {
            return await axios.get('/api/v1/simulcasts')
                .then(response => response.data)
                .catch(() => []);
        }

        async function loadAnime() {
            const uuid = getUuid();

            return await axios.get('/api/v1/animes/' + uuid)
                .then(response => response.data)
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

            await axios.put('/api/v1/animes/' + uuid, anime)
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

        async function deleteAnime() {
            const uuid = getUuid();

            await axios.delete('/api/v1/animes/' + uuid)
                .catch(() => {
                    const toastEl = document.getElementById('errorToast');
                    const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastEl)
                    toastBootstrap.show();
                });
        }

        function addSimulcast(simulcasts, anime, uuid) {
            const simulcast = simulcasts.find(simulcast => simulcast.uuid === uuid);

            if (!simulcast) {
                return;
            }

            anime.simulcasts.push(simulcast);
        }
    </script>
</@navigation.display>