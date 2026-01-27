<#import "../_navigation.ftl" as navigation />
<#import "../macros/pagination.ftl" as ui>

<@navigation.display>
    <style>
        .search-container {
            position: relative;
        }

        .search-input {
            padding-left: 40px;
        }

        .search-icon {
            position: absolute;
            top: 50%;
            left: 15px;
            transform: translateY(-50%);
            color: #888;
        }
    </style>

    <div x-data="{
        loading: false,
        pageable: {},
        page: 1,
        maxPage: 1,
        limit: 10,
        filter: {
            anime: '',
            season: ''
        },
        searchAnimeName: '',
        animeSuggestions: [],
        showAnimeSuggestions: false,
        updateAll: {
            uuids: []
        },
        aggregation: {
            loading: false,
            results: []
        },
        getUniqueSources() {
            const sources = [];
            const seen = new Set();
            this.aggregation.results.forEach(result => {
                result.aggregations.forEach(agg => {
                    agg.sources.forEach(source => {
                        const key = source.platform + (source.url || '');
                        if (!seen.has(key)) {
                            seen.add(key);
                            sources.push(source);
                        }
                    });
                });
            });
            return sources;
        },
        async init() {
            const urlParams = new URLSearchParams(window.location.search);
            const animeUuidFromUrl = urlParams.get('anime');
            const seasonFromUrl = urlParams.get('season');
            const pageFromUrl = urlParams.get('page');
            const limitFromUrl = urlParams.get('limit');

            if (animeUuidFromUrl) {
                this.filter.anime = animeUuidFromUrl;
            }

            if (seasonFromUrl) {
                this.filter.season = seasonFromUrl;
            }

            if (pageFromUrl) {
                this.page = parseInt(pageFromUrl);
            }

            if (limitFromUrl) {
                this.limit = parseInt(limitFromUrl);
            }

            await this.fetchEpisodes();
            this.applyFilterParameters();
        },
        async fetchEpisodes() {
            if (this.loading) {
                return;
            }

            this.loading = true;
            this.pageable = await getEpisodes(this.filter, this.page, this.limit);
            this.loading = false;
            this.maxPage = Math.ceil(this.pageable.total / this.limit);
        },
        applyFilterParameters() {
            if (this.filter.anime === '' && this.filter.season === '' && this.page === 1 && this.limit === 10) {
                window.history.pushState({}, '', '/admin/episodes');
            } else {
                const params = new URLSearchParams();

                if (this.filter.anime) {
                    params.append('anime', this.filter.anime);
                }

                if (this.filter.season) {
                    params.append('season', this.filter.season);
                }

                if (this.page !== 1) {
                    params.append('page', this.page);
                }

                if (this.limit !== 10) {
                    params.append('limit', this.limit);
                }

                window.history.pushState({}, '', '/admin/episodes?' + params.toString());
            }
        },
        async setPage(newPage) {
            this.page = newPage;
            this.applyFilterParameters();
            await this.fetchEpisodes();
        },
        async applyFilters() {
            this.page = 1;
            this.applyFilterParameters();
            await this.fetchEpisodes();
        },
        async searchAnimes() {
            if (!this.searchAnimeName.trim()) {
                this.filter.anime = '';
                this.animeSuggestions = [];
                this.showAnimeSuggestions = false;
                await this.applyFilters();
                return;
            }

            try {
                const suggestions = await getAnimeSuggestions(this.searchAnimeName, 5);
                this.animeSuggestions = suggestions.data;
                this.showAnimeSuggestions = true;
            } catch (error) {
                console.error('Error fetching anime suggestions:', error);
                this.animeSuggestions = [];
                this.showAnimeSuggestions = false;
            }
        },
        async selectAnime(anime) {
            this.filter.anime = anime.uuid;
            this.searchAnimeName = anime.shortName;
            this.animeSuggestions = [];
            this.showAnimeSuggestions = false;
            await this.applyFilters();
        },
        isAllElementsChecked() {
            return (this.pageable?.data || []).every(episode => this.updateAll.uuids.includes(episode.uuid));
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
                            <div class="col-md-12">
                                <label for="animeName" class="form-label">Anime name (for migration)</label>
                                <input type="text" class="form-control" id="animeName" name="animeName"
                                       x-model="updateAll.animeName"
                                       placeholder="Leave empty to keep current anime">
                            </div>
                            <div class="col-md-6">
                                <label for="season" class="form-label">Season</label>
                                <input type="number" class="form-control" id="season" name="season"
                                       x-model="updateAll.season">
                            </div>
                            <div class="col-md-6">
                                <label for="episodeType" class="form-label">Episode type</label>
                                <input type="text" class="form-control" id="episodeType" name="episodeType"
                                       x-model="updateAll.episodeType">
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

        <div class="modal fade" id="aggregationModal" tabindex="-1" aria-labelledby="aggregationModalLabel"
             aria-hidden="true" x-data="{ aggregationSaving: false }">
            <div class="modal-dialog modal-xl">
                <div class="modal-content">
                    <div class="modal-header">
                        <h1 class="modal-title fs-5" id="aggregationModalLabel">Episode Aggregation</h1>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div x-show="aggregation.loading" class="text-center py-5">
                            <div class="spinner-border text-primary" role="status">
                                <span class="visually-hidden">Loading...</span>
                            </div>
                            <p class="mt-2">Aggregating data from multiple sources...</p>
                        </div>

                        <div x-show="!aggregation.loading">
                            <div class="mb-3">
                                <template x-for="source in getUniqueSources()">
                                    <span>
                                        <a :href="source.url" target="_blank" x-show="source.url"
                                           class="badge bg-info text-dark me-2 text-decoration-none"
                                           x-text="source.platform"></a>
                                        <span class="badge bg-info text-dark me-2"
                                              x-show="!source.url" x-text="source.platform"></span>
                                    </span>
                                </template>
                            </div>

                            <table class="table table-sm align-middle">
                                <thead>
                                <tr>
                                    <th>Episode</th>
                                    <th>Current Date</th>
                                    <th>New Date</th>
                                </tr>
                                </thead>
                                <tbody>
                                <template x-for="result in aggregation.results" :key="result.episodeUuid">
                                    <tr x-data="{ episodeReleaseDate: result.releaseDate }">
                                        <td>
                                            <strong x-text="result.animeName"></strong><br>
                                            <small x-text="result.episodeName"></small>
                                        </td>
                                        <td>
                                            <input type="date" class="form-control form-control-sm disabled"
                                                   :value="episodeReleaseDate" readonly disabled>
                                        </td>
                                        <td>
                                            <div class="input-group input-group-sm">
                                                <input type="date" class="form-control form-control-sm"
                                                       x-model="result.releaseDate">

                                                <button class="btn btn-outline-secondary dropdown-toggle" type="button"
                                                        data-bs-toggle="dropdown" aria-expanded="false"
                                                        x-show="result.aggregations.length > 0">
                                                </button>
                                                <ul class="dropdown-menu dropdown-menu-end">
                                                    <template x-for="agg in result.aggregations">
                                                        <template x-for="airing in agg.airings">
                                                            <li>
                                                                <a class="dropdown-item position-relative" href="#" @click.prevent="result.releaseDate = airing.date">
                                                                    <input type="date" class="form-control form-control-sm"
                                                                           :value="airing.date" readonly disabled>

                                                                    <span class="badge bg-secondary position-absolute top-0 end-0 translate-middle-x"
                                                                          x-text="airing.occurrenceCount"></span>
                                                                </a>
                                                            </li>
                                                        </template>
                                                    </template>
                                                </ul>
                                            </div>
                                        </td>
                                    </tr>
                                </template>
                                </tbody>
                            </table>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                        <button class="btn btn-success" x-bind:disabled="aggregationSaving || aggregation.loading"
                                @click="aggregationSaving = true; await updateAggregated(aggregation.results); location.reload();">
                            <span x-show="aggregationSaving" class="spinner-border spinner-border-sm me-1"
                                  role="status"></span>
                            Confirm & Update
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <div class="row g-3 align-items-center mb-3">
            <div class="col-auto">
                <div class="search-container position-relative">
                    <input type="text" class="form-control search-input"
                           x-model="searchAnimeName"
                           @input.debounce.300ms="searchAnimes()"
                           @focus="showAnimeSuggestions = true"
                           @keydown.escape="showAnimeSuggestions = false"
                           @blur="setTimeout(() => showAnimeSuggestions = false, 200)"
                           placeholder="Anime Name">
                    <i class="bi bi-search search-icon"></i>
                    <div x-show="showAnimeSuggestions && animeSuggestions.length > 0"
                         class="dropdown-menu show position-absolute w-100 mt-1" style="z-index: 1000;"
                         x-transition>
                        <template x-for="animeItem in animeSuggestions" :key="animeItem.uuid">
                            <a href="#" class="dropdown-item"
                               @click.prevent="selectAnime(animeItem)"
                               x-text="animeItem.shortName"
                               style="display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                            </a>
                        </template>
                    </div>
                </div>
            </div>
            <div class="col-auto">
                <div class="search-container">
                    <input type="number" class="form-control search-input" x-model="filter.season" @input="applyFilters" placeholder="Season" style="width: 150px;">
                    <i class="bi bi-search search-icon"></i>
                </div>
            </div>
            <@ui.pageSizeSelector />
            <div class="col-auto" x-show="updateAll.uuids.length > 0">
                <div class="position-relative">
                    <div class="input-group z-0">
                        <button class="btn btn-warning" type="button" data-bs-toggle="modal"
                                data-bs-target="#updateAllModal">
                            <i class="bi bi-pencil-square"></i>
                        </button>

                        <button class="btn btn-primary" type="button" data-bs-toggle="modal"
                                data-bs-target="#aggregationModal"
                                @click="aggregation.loading = true; aggregation.results = await aggregateSelected(updateAll.uuids); aggregation.loading = false;">
                            <i class="bi bi-recycle"></i>
                        </button>

                        <button class="btn btn-outline-warning" @click="updateAll.uuids = [];">
                            <i class="bi bi-x"></i>
                        </button>
                    </div>

                    <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger z-1"
                          x-text="updateAll.uuids.length"></span>
                </div>
            </div>
            <@ui.alpinePagination />
        </div>

        <table class="table table-striped table-bordered">
            <thead>
            <tr>
                <th scope="col">
                    <input type="checkbox" class="form-check-input" @change="pageable.data.forEach(episode => {
                        if (updateAll.uuids.includes(episode.uuid)) {
                            updateAll.uuids = updateAll.uuids.filter(uuid => uuid !== episode.uuid);
                        } else {
                            updateAll.uuids.push(episode.uuid);
                        }
                    })" :value="isAllElementsChecked">
                </th>
                <th scope="col">Anime</th>
                <th scope="col">Details</th>
                <th scope="col">Description</th>
                <th scope="col"></th>
            </tr>
            <tr x-show="isAllElementsChecked">
                <th colspan="5" class="text-center bg-light">
                    The <span x-text="Math.min(limit, pageable.total)"></span> episodes on this page are selected
                </th>
            </tr>
            </thead>
            <tbody class="table-group-divider">
            <template x-for="episode in pageable.data">
                <tr>
                    <td style="width: 25px;">
                        <input type="checkbox" :value="updateAll.uuids.includes(episode.uuid)" class="form-check-input" @change="if (updateAll.uuids.includes(episode.uuid)) { updateAll.uuids = updateAll.uuids.filter(uuid => uuid !== episode.uuid); } else { updateAll.uuids.push(episode.uuid); }">
                    </td>
                    <th scope="row" x-text="episode.anime.shortName"></th>
                    <td>
                        S<span x-text="episode.season"></span>
                        <span x-text="getEpisodeTypeLabel(episode.episodeType)"></span>
                        <span x-text="episode.number"></span>
                    </td>
                    <td  x-text="episode.description ? episode.description.substring(0, 125) + (episode.description.length > 125 ? '...' : '') : ''"></td>
                    <td style="width: 50px;">
                        <a :href="'/admin/episodes/' + episode.uuid" class="btn btn-warning">
                            <i class="bi bi-pencil-square"></i>
                        </a>
                    </td>
                </tr>
            </template>
            </tbody>
        </table>
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

        async function getEpisodes(filter, page, limit) {
            const params = new URLSearchParams({
                sort: 'lastReleaseDateTime,animeName,season,episodeType,number',
                desc: 'lastReleaseDateTime,animeName,season,episodeType,number',
                page: page || 1,
                limit: limit || 10
            });

            if (filter.anime) {
                params.append('anime', filter.anime);
            }

            if (filter.season) {
                params.append('season', filter.season);
            }

            const response = await fetch(`/api/v1/episode-mappings?` + params.toString());
            return response.json();
        }

        async function getAnimeSuggestions(name, limit) {
            if (!name.trim()) return { data: [] };
            try {
                const response = await fetch('/api/v1/animes?name=' + encodeURIComponent(name) + '&limit=' + limit);
                return response.json();
            } catch (error) {
                console.error('Error fetching anime suggestions:', error);
                return { data: [] };
            }
        }

        async function updateAllSelected(updateAll) {
            if (updateAll.season === '' || updateAll.season === 0) {
                updateAll.season = null;
            }

            try {
                await fetch('/admin/api/episode-mappings/update-all', {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(updateAll)
                });
            } catch (e) {
                console.error(e);
            }
        }

        async function aggregateSelected(uuids) {
            try {
                const response = await fetch('/admin/api/episode-mappings/aggregate', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({uuids: uuids})
                });

                return await response.json();
            } catch (e) {
                console.error(e);
                return [];
            }
        }

        async function updateAggregated(results) {
            try {
                await fetch('/admin/api/episode-mappings/update-aggregated', {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(results)
                });
            } catch (e) {
                console.error(e);
            }
        }
    </script>
</@navigation.display>