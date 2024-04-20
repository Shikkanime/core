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
    selectedSimulcast: ''
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

        <div class="d-flex mb-3">
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

        <template x-init="loading = true;
        simulcasts = await getSimulcasts();
        anime = await loadAnime();
        loading = false;" x-if="anime.uuid && simulcasts.length > 0">
            <div class="card">
                <div class="card-body">
                    <div class="row g-3">
                        <div class="col-md-6">
                            <label for="uuid" class="form-label">UUID</label>

                            <div class="input-group">
                                <input type="text" class="form-control disabled" id="uuid" name="uuid"
                                       x-model="anime.uuid" aria-label="UUID"
                                       aria-describedby="basic-addon" disabled>

                                <span class="input-group-text" id="basic-addon"
                                      @click="copyToClipboard(anime.uuid)" style="cursor: pointer"><i
                                            class="bi bi-clipboard"></i></span>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <label for="name" class="form-label">Name</label>
                            <input type="text" class="form-control" id="name" name="name" x-model="anime.name">
                        </div>
                        <div class="col-md-6">
                            <label for="shortName" class="form-label">Short name</label>
                            <input type="text" class="form-control" id="shortName" name="shortName"
                                   x-model="anime.shortName" disabled>
                        </div>
                        <div class="col-md-6">
                            <label for="slug" class="form-label">Slug</label>
                            <input type="text" class="form-control" id="slug" name="slug"
                                   x-model="anime.slug">
                        </div>
                        <div class="col-md-6">
                            <label for="releaseDateTime" class="form-label">Release date time</label>
                            <input type="datetime-local" class="form-control" id="releaseDateTime"
                                   name="releaseDateTime"
                                   :value="anime.releaseDateTime.substring(0, 16)"
                                   @input="anime.releaseDateTime = $event.target.value + ':00Z'">
                        </div>
                        <div class="col-md-6">
                            <label for="image" class="form-label">Image</label>

                            <div class="input-group">
                                <input type="text" class="form-control" id="image" name="image"
                                       x-model="anime.image" aria-label="Image"
                                       aria-describedby="basic-addon">

                                <a class="input-group-text" id="basic-addon"
                                   @click="" style="cursor: pointer" target="_blank" x-bind:href="anime.image">
                                    <i class="bi bi-box-arrow-up-right"></i>
                                </a>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <label for="banner" class="form-label">Banner</label>

                            <div class="input-group">
                                <input type="text" class="form-control" id="banner" name="banner"
                                       x-model="anime.banner" aria-label="Banner"
                                       aria-describedby="basic-addon">

                                <a class="input-group-text" id="basic-addon"
                                   @click="" style="cursor: pointer" target="_blank" x-bind:href="anime.banner">
                                    <i class="bi bi-box-arrow-up-right"></i>
                                </a>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <label for="description" class="form-label">Description</label>
                            <textarea class="form-control" id="description" name="description"
                                      rows="6" x-model="anime.description"></textarea>
                        </div>
                        <div class="col-md-6">
                            <p class="form-label my-0">Simulcasts</p>

                            <template x-for="simulcast in anime.simulcasts">
                                <div class="d-flex align-items-center my-1">
                                    <div class="ms-0 me-auto"
                                         x-text="'• ' + simulcast.season + ' ' + simulcast.year"></div>
                                    <div class="ms-auto me-0">
                                        <button type="button" class="btn btn-outline-danger"
                                                @click="anime.simulcasts.splice(anime.simulcasts.indexOf(simulcast), 1)">
                                            -
                                        </button>
                                    </div>
                                </div>
                            </template>

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
                                    <button type="button" class="btn btn-outline-success"
                                            @click="addSimulcast(simulcasts, anime, selectedSimulcast); selectedSimulcast = ''">
                                        +
                                    </button>
                                </div>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label">Genres</label>

                            <ul>
                                <template x-for="genre in anime.genres">
                                    <li x-text="genre"></li>
                                </template>
                            </ul>
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
                .catch(() => ({}));
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