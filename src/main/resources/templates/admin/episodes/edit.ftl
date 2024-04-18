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
    loading: true
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

        <div class="d-flex mb-3">
            <a href="/admin/episodes" class="btn btn-secondary ms-0">Back</a>

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
                            <label for="image" class="form-label">Image</label>

                            <div class="input-group">
                                <input type="text" class="form-control" id="image" name="image"
                                       x-model="episode.image" aria-label="Image"
                                       aria-describedby="basic-addon">

                                <a class="input-group-text" id="basic-addon"
                                   @click="" style="cursor: pointer" target="_blank" x-bind:href="episode.image">
                                    <i class="bi bi-box-arrow-up-right"></i>
                                </a>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <label for="duration" class="form-label">Duration</label>
                            <input type="number" class="form-control" id="duration" name="duration"
                                   x-model="episode.duration">
                        </div>
                        <div class="col-md-12 mt-3">
                            <hr>
                            <template x-for="variant in episode.variants" :id="variant.uuid">
                                <div class="row g-3 mt-3">
                                    <div class="col-md-auto">
                                        <label for="variantReleaseDateTime" class="form-label">Release date time</label>
                                        <input type="datetime-local" class="form-control" id="variantReleaseDateTime" name="variantReleaseDateTime"
                                               x-model="variant.releaseDateTime.substring(0, 16)" disabled>
                                    </div>
                                    <div class="col-md-auto">
                                        <label for="variantPlatform" class="form-label">Platform</label>
                                        <input type="text" class="form-control" id="variantPlatform" name="variantPlatform"
                                               x-model="variant.platform.name" disabled>
                                    </div>
                                    <div class="col-md-auto">
                                        <label for="variantAudioLocale" class="form-label">Audio locale</label>
                                        <input type="text" class="form-control" id="variantAudioLocale" name="variantAudioLocale"
                                               x-model="variant.audioLocale" disabled>
                                    </div>
                                    <div class="col-md-auto">
                                        <label for="variantIdentifier" class="form-label">Identifier</label>
                                        <input type="text" class="form-control" id="variantIdentifier" name="variantIdentifier"
                                               x-model="variant.identifier" disabled>
                                    </div>
                                    <div class="col-md-auto">
                                        <label for="variantUrl" class="form-label">URL</label>
                                        <input type="text" class="form-control" id="variantUrl" name="variantUrl"
                                               x-model="variant.url" disabled>
                                    </div>
                                    <div class="col-md-auto">
                                        <input type="checkbox" class="form-check-input" id="variantUncensored" name="variantUncensored"
                                               x-model="variant.uncensored" disabled>
                                        <label for="variantUncensored" class="form-label">Uncensored</label>
                                    </div>
                                    <div class="col-md-auto">
                                        <img x-bind:src="'/api/v1/episode-mappings/' + variant.uuid + '/media-image'"
                                             class="rounded-4 w-25" alt="Variant image">
                                    </div>
                                </div>
                            </template>
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
            return await callApi('/api/v1/episode-mappings/' + uuid);
        }

        async function updateEpisode(episode) {
            const uuid = getUuid();

            await callApi('/api/v1/episode-mappings/' + uuid, {
                method: 'PUT',
                body: episode
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

            await callApi('/api/v1/episode-mappings/' + uuid, {
                method: 'DELETE'
            }).catch(() => {
                const toastEl = document.getElementById('errorToast');
                const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastEl)
                toastBootstrap.show();
            });
        }
    </script>
</@navigation.display>