<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <div class="toast-container position-fixed top-0 end-0 p-3">
        <div id="successToast" class="toast align-items-center text-bg-success border-0" role="alert"
             aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    Episode updated successfully!
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"
                        aria-label="Close"></button>
            </div>
        </div>

        <div id="errorToast" class="toast align-items-center text-bg-danger border-0" role="alert" aria-live="assertive"
             aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    Error updating episode!
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"
                        aria-label="Close"></button>
            </div>
        </div>
    </div>

    <div x-data="{
    episode: {},
    loading: true,
    selectedVariant: {}
    }">
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

                        <button class="btn btn-danger"
                                @click="loading = true; await deleteEpisode(); window.location.href = '/admin/episodes'">
                            Confirm
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <div class="modal fade" id="separateVariantModal" tabindex="-1" aria-labelledby="separateVariantModalLabel"
             aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h1 class="modal-title fs-5" id="separateVariantModalLabel">Separate variant</h1>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row g-3">
                            <div class="col-md-6">
                                <label for="uuid" class="form-label">UUID</label>

                                <div class="input-group">
                                    <input type="text" class="form-control disabled" id="uuid" name="uuid"
                                           x-model="selectedVariant.uuid" aria-label="UUID"
                                           aria-describedby="basic-addon" disabled>
                                    <span class="input-group-text" id="basic-addon"
                                          @click="copyToClipboard(selectedVariant.uuid)" style="cursor: pointer"><i
                                                class="bi bi-clipboard"></i></span>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <label for="episodeType" class="form-label">Episode type</label>
                                <input type="text" class="form-control" id="episodeType" name="episodeType"
                                       x-model="selectedVariant.episodeType">
                            </div>
                            <div class="col-md-6">
                                <label for="season" class="form-label">Season</label>
                                <input type="number" class="form-control" id="season" name="season"
                                       x-model="selectedVariant.season">
                            </div>
                            <div class="col-md-6">
                                <label for="number" class="form-label">Number</label>
                                <input type="number" class="form-control" id="number" name="number"
                                       x-model="selectedVariant.number">
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>

                        <button class="btn btn-success"
                                @click="loading = true; await separateVariant(selectedVariant); location.reload();">
                            Confirm
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <div class="d-flex mb-3">
            <a class="btn btn-secondary ms-0" @click="window.history.back()">
                Back
            </a>

            <div class="spinner-border ms-2 me-auto" role="status" x-show="loading">
                <span class="visually-hidden">Loading...</span>
            </div>

            <button type="button" class="btn btn-danger ms-auto me-0" data-bs-toggle="modal"
                    data-bs-target="#deleteModal" :disabled="loading">
                Delete
            </button>

            <button type="submit" class="btn btn-success ms-2 me-0" :disabled="loading"
                    @click="loading = true; await updateEpisode(episode); loading = false;">
                Update
            </button>
        </div>

        <template x-init="loading = true;
        episode = await loadEpisode();
        loading = false;" x-if="episode.uuid">
            <div class="card">
                <div class="card-body">
                    <div class="row g-3">
                        <div class="col-md-6">
                            <label for="uuid" class="form-label">UUID</label>

                            <div class="input-group">
                                <input type="text" class="form-control disabled" id="uuid" name="uuid"
                                       x-model="episode.uuid" aria-label="UUID"
                                       aria-describedby="basic-addon" disabled>
                                <span class="input-group-text" id="basic-addon"
                                      @click="copyToClipboard(episode.uuid)" style="cursor: pointer"><i
                                            class="bi bi-clipboard"></i></span>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <label for="anime" class="form-label">Anime</label>
                            <input type="text" class="form-control" id="anime" name="anime"
                                   x-model="episode.anime.name">
                        </div>
                        <div class="col-md-6">
                            <label for="releaseDateTime" class="form-label">Release date time</label>
                            <input type="datetime-local" class="form-control" id="releaseDateTime"
                                   name="releaseDateTime"
                                   :value="episode.releaseDateTime.substring(0, 16)"
                                   @input="episode.releaseDateTime = $event.target.value + ':00Z'">
                        </div>
                        <div class="col-md-6">
                            <label for="lastReleaseDateTime" class="form-label">Last release date time</label>
                            <input type="datetime-local" class="form-control" id="lastReleaseDateTime"
                                   name="lastReleaseDateTime"
                                   :value="episode.lastReleaseDateTime.substring(0, 16)"
                                   @input="episode.lastReleaseDateTime = $event.target.value + ':00Z'">
                        </div>
                        <div class="col-md-6">
                            <label for="lastUpdateDateTime" class="form-label">Last update date time</label>
                            <input type="datetime-local" class="form-control" id="lastUpdateDateTime"
                                   name="lastUpdateDateTime"
                                   :value="episode.lastUpdateDateTime.substring(0, 16)"
                                   @input="episode.lastUpdateDateTime = $event.target.value + ':00Z'">
                        </div>
                        <div class="col-md-6">
                            <label for="episodeType" class="form-label">Episode type</label>
                            <input type="text" class="form-control" id="episodeType" name="episodeType"
                                   x-model="episode.episodeType">
                        </div>
                        <div class="col-md-6">
                            <label for="season" class="form-label">Season</label>
                            <input type="number" class="form-control" id="season" name="season"
                                   x-model="episode.season">
                        </div>
                        <div class="col-md-6">
                            <label for="number" class="form-label">Number</label>
                            <input type="number" class="form-control" id="number" name="number"
                                   x-model="episode.number">
                        </div>
                        <div class="col-md-6">
                            <label for="title" class="form-label">Title</label>
                            <input type="text" class="form-control" id="title" name="title" x-model="episode.title">
                        </div>
                        <div class="col-md-6">
                            <label for="description" class="form-label">Description</label>
                            <textarea class="form-control" id="description" name="description"
                                      rows="6" x-model="episode.description"></textarea>
                        </div>
                        <div class="col-md-6">
                            <label for="image" class="form-label">
                                Image

                                <a style="cursor: pointer" @click="goToMediaImage(episode.variants.map(variant => variant.uuid))">
                                    <i class="ms-2 bi bi-image"></i>
                                </a>
                            </label>

                            <div class="input-group mb-2">
                                <input type="text" class="form-control" id="image" name="image"
                                       x-model="episode.image" aria-label="Image"
                                       aria-describedby="basic-addon">

                                <a class="input-group-text" id="basic-addon"
                                   style="cursor: pointer" target="_blank" :href="episode.image">
                                    <i class="bi bi-box-arrow-up-right"></i>
                                </a>
                            </div>

                            <img :src="'${apiUrl}/v1/attachments?uuid=' + episode.uuid + '&type=BANNER'" class="w-50" alt="Episode preview">
                        </div>
                        <div class="col-md-6">
                            <label for="duration" class="form-label">Duration</label>
                            <input type="number" class="form-control" id="duration" name="duration"
                                   x-model="episode.duration">
                        </div>
                        <div class="col-md-12 mt-3">
                            <hr>
                            <table class="table table-striped table-bordered">
                                <thead>
                                <tr>
                                    <th scope="col">Identifier</th>
                                    <th scope="col">Release date time</th>
                                    <th scope="col">Platform</th>
                                    <th scope="col">Audio locale</th>
                                    <th scope="col">URL</th>
                                    <th scope="col">Uncensored</th>
                                    <th scope="col">Actions</th>
                                </tr>
                                </thead>
                                <tbody class="table-group-divider">
                                <template x-for="variant in episode.variants">
                                    <tr>
                                        <th scope="row">
                                            <span x-text="variant.identifier"></span>
                                        </th>
                                        <td>
                                            <input type="datetime-local" class="form-control"
                                                   id="variantReleaseDateTime"
                                                   name="variantReleaseDateTime"
                                                   :value="variant.releaseDateTime.substring(0, 16)"
                                                   @input="variant.releaseDateTime = $event.target.value + ':00Z'">
                                        </td>
                                        <td x-text="variant.platform.name"></td>
                                        <td x-text="variant.audioLocale"></td>
                                        <td>
                                            <div class="input-group">
                                                <input type="text" class="form-control" id="variantUrl"
                                                       name="variantUrl"
                                                       x-model="variant.url" aria-label="Image"
                                                       aria-describedby="basic-addon">

                                                <a class="input-group-text" id="basic-addon" style="cursor: pointer"
                                                   target="_blank" :href="variant.url">
                                                    <i class="bi bi-box-arrow-up-right"></i>
                                                </a>
                                            </div>
                                        </td>
                                        <td>
                                            <input type="checkbox" class="form-check-input" id="variantUncensored"
                                                   name="variantUncensored"
                                                   x-model="variant.uncensored">
                                        </td>
                                        <td>
                                            <button type="button" class="btn btn-warning" data-bs-toggle="modal"
                                                    data-bs-target="#separateVariantModal"
                                                    @click="selectedVariant = {uuid: variant.uuid}">
                                                Separate
                                            </button>

                                            <button type="button" class="btn btn-danger"
                                                    @click="episode.variants.splice(episode.variants.indexOf(variant), 1)">
                                                Delete
                                            </button>
                                        </td>
                                    </tr>
                                </template>
                                </tbody>
                            </table>
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

        async function loadEpisode() {
            const uuid = getUuid();

            return fetch('/admin/api/episode-mappings/' + uuid)
                .then(response => response.json())
                .catch(() => {
                    const toastEl = document.getElementById('errorToast');
                    const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastEl)
                    toastBootstrap.show();
                    // Wait 1s
                    return new Promise(resolve => setTimeout(resolve, 1000)).then(() => window.location.href = '/admin/episodes');
                });
        }

        async function updateEpisode(episode) {
            const uuid = getUuid();

            await fetch('/admin/api/episode-mappings/' + uuid, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(episode)
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

        async function deleteEpisode() {
            const uuid = getUuid();

            await fetch('/admin/api/episode-mappings/' + uuid, {
                method: 'DELETE'
            })
                .catch(() => {
                    const toastEl = document.getElementById('errorToast');
                    const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastEl)
                    toastBootstrap.show();
                });
        }

        async function separateVariant(dto) {
            await fetch('/admin/api/episode-variants/' + dto.uuid + '/separate', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(dto)
            });
        }

        const compress = string => {
            const blobToBase64 = blob => new Promise((resolve, _) => {
                const reader = new FileReader();
                reader.onloadend = () => resolve(reader.result.split(',')[1]);
                reader.readAsDataURL(blob);
            });
            const byteArray = new TextEncoder().encode(string);
            const cs = new CompressionStream('gzip');
            const writer = cs.writable.getWriter();
            writer.write(byteArray);
            writer.close();
            return new Response(cs.readable).blob().then(blobToBase64);
        };

        function goToMediaImage(uuids) {
            compress(uuids.join(',')).then(base64 => {
                const url = '/api/v1/episode-mappings/media-image?uuids=' + encodeURIComponent(base64);
                window.open(url, '_blank');
            });
        }
    </script>
</@navigation.display>