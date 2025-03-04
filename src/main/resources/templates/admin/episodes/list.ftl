<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <div x-data="{
        loading: false,
        pageable: {},
        page: 1,
        maxPage: 1,
        filter: {
            anime: '',
            invalid: false,
            season: ''
        },
        pages: [],
        updateAll: {
            uuids: []
        },
        async init() {
            await this.fetchEpisodes();
            this.pages = this.generatePageNumbers(this.page, this.maxPage);
        },
        async fetchEpisodes() {
            if (this.loading) {
                return;
            }

            this.loading = true;
            this.pageable = await getEpisodes(this.filter, this.page);
            this.loading = false;
            this.maxPage = Math.ceil(this.pageable.total / this.pageable.limit);
        },
        applyFilterParameters() {
            if (this.filter.anime === '' && this.filter.season === '' && !this.filter.invalid && this.page === 1) {
                window.history.pushState({}, '', '/admin/episodes');
            } else {
                const params = new URLSearchParams();

                if (this.filter.anime) {
                    params.append('anime', this.filter.anime);
                }

                if (this.filter.season) {
                    params.append('season', this.filter.season);
                }

                if (this.filter.invalid) {
                    params.append('invalid', 'true');
                }

                if (this.page !== 1) {
                    params.append('page', this.page);
                }

                window.history.pushState({}, '', '/admin/episodes?' + params.toString());
            }
        },
        async setPage(newPage) {
            this.page = newPage;
            this.applyFilterParameters();
            await this.fetchEpisodes();
            this.pages = this.generatePageNumbers(this.page, this.maxPage);
        },
        async applyFilters() {
            this.page = 1;
            this.applyFilterParameters();
            await this.fetchEpisodes();
            this.pages = this.generatePageNumbers(this.page, this.maxPage);
        },
        generatePageNumbers(currentPage, maxPage) {
            const delta = 3;
            const range = [];
            for (let i = Math.max(2, currentPage - delta); i <= Math.min(maxPage - 1, currentPage + delta); i++) {
                range.push(i);
            }

            if (currentPage - delta > 2) {
                range.unshift('...');
            }

            if (currentPage + delta < maxPage - 1) {
                range.push('..');
            }

            range.unshift(1);

            if (maxPage !== 1) {
                range.push(maxPage);
            }

            return range;
        }
    }" x-init="init">
        <div class="modal fade" id="updateAllModal" tabindex="-1" aria-labelledby="updateAllModalLabel"
             aria-hidden="true" x-data="{ updateAllLoading: false }">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h1 class="modal-title fs-5" id="updateAllModalLabel">Update selected</h1>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row g-3">
                            <div class="col-md-6">
                                <label for="episodeType" class="form-label">Episode type</label>
                                <input type="text" class="form-control" id="episodeType" name="episodeType"
                                       x-model="updateAll.episodeType">
                            </div>
                            <div class="col-md-6">
                                <label for="season" class="form-label">Season</label>
                                <input type="number" class="form-control" id="season" name="season"
                                       x-model="updateAll.season">
                            </div>
                            <div class="col-md-6">
                                <label for="startDate" class="form-label">Start date</label>
                                <input type="date" class="form-control" id="startDate" name="startDate"
                                       x-model="updateAll.startDate">
                            </div>
                            <div class="col-md-6">
                                <div>
                                    <input class="form-check-input" type="checkbox" id="incrementDate" x-model="updateAll.incrementDate">
                                    <label class="form-check-label" for="incrementDate">Increment date</label>
                                </div>
                                <div>
                                    <input class="form-check-input" type="checkbox" id="bindVoiceVariants" x-model="updateAll.bindVoiceVariants">
                                    <label class="form-check-label" for="bindVoiceVariants">Bind voice variants</label>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div>
                                    <input class="form-check-input" type="checkbox" id="forceUpdate" x-model="updateAll.forceUpdate">
                                    <label class="form-check-label" for="forceUpdate">Force update</label>
                                </div>
                                <div>
                                    <input class="form-check-input" type="checkbox" id="bindNumber" x-model="updateAll.bindNumber">
                                    <label class="form-check-label" for="bindNumber">Bind number</label>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>

                        <button class="btn btn-success" x-bind:disabled="updateAllLoading"
                                @click="updateAllLoading = true; await updateAllSelected(updateAll); location.reload();">
                            Confirm
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <div class="row g-3 align-items-center mb-3">
            <div class="col-auto">
                <label class="form-label" for="animeInput">Anime UUID</label>
                <input type="text" class="form-control" id="animeInput"
                       x-model="filter.anime" @input="applyFilters">
            </div>
            <div class="col-auto">
                <label class="form-label" for="seasonInput">Season</label>
                <input type="number" class="form-control" id="seasonInput"
                       x-model="filter.season" @input="applyFilters">
            </div>
            <div class="col-auto">
                <input class="form-check-input" type="checkbox" id="invalidInput"
                       x-model="filter.invalid" @change="applyFilters">
                <label class="form-check-label" for="invalidInput">Only invalid</label>
            </div>
            <div class="col-auto ms-auto">
                <button class="btn btn-primary" type="button" @click="updateAll.uuids.push(...pageable.data.map(episode => episode.uuid));">
                    <i class="bi bi-check-all me-2"></i>
                    Check all
                </button>

                <button class="btn btn-primary" type="button" data-bs-toggle="modal" data-bs-target="#updateAllModal" x-show="updateAll.uuids.length > 0">
                    <i class="bi bi-pencil-square me-2"></i>
                    Update selected
                </button>

                <div class="mt-1" x-show="updateAll.uuids.length > 0">
                    <strong x-text="updateAll.uuids.length"></strong> rows selected.

                    <button class="ms-3 btn btn-danger" type="button" @click="updateAll.uuids = []">
                        <i class="bi bi-x-circle me-2"></i>
                        Clear selection
                    </button>
                </div>
            </div>
        </div>

        <table class="table table-striped table-bordered">
            <thead>
            <tr>
                <th scope="col">Anime</th>
                <th scope="col">Details</th>
                <th scope="col">Description</th>
                <th scope="col">Actions</th>
            </tr>
            </thead>
            <tbody class="table-group-divider">
            <template x-for="episode in pageable.data">
                <tr @dblclick="if (updateAll.uuids.includes(episode.uuid)) { updateAll.uuids = updateAll.uuids.filter(uuid => uuid !== episode.uuid); } else { updateAll.uuids.push(episode.uuid); }">
                    <th scope="row">
                        <i class="bi bi-check2 me-2" x-show="updateAll.uuids.includes(episode.uuid)"></i>

                        <span class="me-1 badge"
                              :class="episode.status === 'INVALID' ? 'bg-danger' : 'bg-success'"
                              x-text="episode.status === 'INVALID' ? 'Invalid' : 'Valid'"></span>
                        <span x-text="episode.anime.shortName"></span>
                    </th>
                    <td>
                        S<span x-text="episode.season"></span>
                        <span x-text="getEpisodeTypeLabel(episode.episodeType)"></span>
                        <span x-text="episode.number"></span>
                    </td>
                    <td x-text="episode.description"></td>
                    <td>
                        <a :href="'/admin/episodes/' + episode.uuid" class="btn btn-warning">
                            <i class="bi bi-pencil-square"></i>
                            Edit
                        </a>
                    </td>
                </tr>
            </template>
            </tbody>
        </table>

        <div class="mt-3">
            <nav aria-label="Page navigation">
                <ul class="pagination justify-content-center">
                    <li class="page-item" :class="{ disabled: page === 1 }">
                        <a class="page-link" @click="setPage(1)">&laquo;</a>
                    </li>
                    <template x-for="i in pages">
                        <li class="page-item" :class="{ active: page === i }">
                            <a class="page-link" @click="setPage(i)" x-text="i"></a>
                        </li>
                    </template>
                    <li class="page-item" :class="{ disabled: page === maxPage }">
                        <a class="page-link" @click="setPage(maxPage)">&raquo;</a>
                    </li>
                </ul>
            </nav>
        </div>
    </div>

    <script>
        function getEpisodeTypeLabel(episodeType) {
            const labels = {
                'EPISODE': 'EP',
                'FILM': 'MOV',
                'SPECIAL': 'SP',
                'SUMMARY': 'SUM',
                'SPIN_OFF': 'SO',
            };
            return labels[episodeType] || episodeType;
        }

        async function getEpisodes(filter, page) {
            const params = new URLSearchParams({
                sort: 'lastReleaseDateTime,animeName,season,episodeType,number',
                desc: 'lastReleaseDateTime,animeName,season,episodeType,number',
                page: page || 1,
                limit: 9
            });

            if (filter.anime) {
                params.append('anime', filter.anime);
            }

            if (filter.invalid) {
                params.append('status', 'INVALID');
            }

            if (filter.season) {
                params.append('season', filter.season);
            }

            const response = await axios.get(`/api/v1/episode-mappings?` + params.toString());
            return response.data;
        }

        async function updateAllSelected(updateAll) {
            if (updateAll.season === '' || updateAll.season === 0) {
                updateAll.season = null;
            }

            try {
                await axios.put('/admin/api/episode-mappings/update-all', updateAll);
            } catch (e) {
                console.error(e);
            }
        }
    </script>
</@navigation.display>