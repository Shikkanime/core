<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div x-data="{
        loading: false,
        filter: {},
        pageable: {},
        page: 1,
        maxPage: 1,
        pages: [],
        async init() {
            await this.fetchTraceActions();
            this.pages = this.generatePageNumbers(this.page, this.maxPage);
        },
        async fetchTraceActions() {
            if (this.loading) {
                return;
            }

            this.loading = true;
            this.pageable = await getTraceActions(this.filter, this.page);
            this.loading = false;
            this.maxPage = Math.ceil(this.pageable.total / this.pageable.limit);
        },
        async setPage(newPage) {
            this.page = newPage;
            await this.fetchTraceActions();
            this.pages = this.generatePageNumbers(this.page, this.maxPage);
        },
        async applyFilters() {
            this.page = 1;
            await this.fetchTraceActions();
            this.pages = this.generatePageNumbers(this.page, this.maxPage);
        },
        generatePageNumbers(currentPage, maxPage) {
            if (currentPage === 0 || maxPage === 0) {
                return [];
            }

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
        <div class="row g-3 align-items-center mb-3">
            <div class="col-auto">
                <label class="form-label" for="entityTypeInput">Entity type</label>
                <select class="form-select" id="entityTypeInput" x-model="filter.entityType" @change="applyFilters">
                    <option value="" selected>All</option>
                    <option value="Anime">Anime</option>
                    <option value="EpisodeMapping">Episode mapping</option>
                    <option value="EpisodeVariant">Episode variant</option>
                    <option value="Simulcast">Simulcast</option>
                    <option value="Config">Config</option>
                    <option value="Member">Member</option>
                    <option value="MemberFollowAnime">Member follow anime</option>
                    <option value="MemberFollowEpisode">Member follow episode</option>
                </select>
            </div>
            <div class="col-auto">
                <label class="form-label" for="actionInput">Action</label>
                <select class="form-select" id="actionInput" x-model="filter.action" @change="applyFilters">
                    <option value="" selected>All</option>
                    <option value="CREATE">Create</option>
                    <option value="UPDATE">Update</option>
                    <option value="DELETE">Delete</option>
                    <option value="LOGIN">Login</option>
                </select>
            </div>
        </div>

        <template x-for="traceAction in pageable.data">
            <div class="card px-3 mb-3">
                <div class="d-flex align-items-center my-3">
                    <i x-show="traceAction.action === 'CREATE'" class="bi bi-database-add text-success"></i>
                    <i x-show="traceAction.action === 'UPDATE'" class="bi bi-database-up text-primary"></i>
                    <i x-show="traceAction.action === 'DELETE'" class="bi bi-database-x text-danger"></i>
                    <i x-show="traceAction.action === 'LOGIN'" class="bi bi-person-check text-warning"></i>

                    <div class="ms-3">
                        <p class="mb-0">
                            <a x-show="traceAction.action !== 'DELETE'" x-bind:href="traceAction.entityType === 'Anime' ? '/admin/animes/' + traceAction.entityUuid :
                                    traceAction.entityType === 'EpisodeMapping' ? '/admin/episodes/' + traceAction.entityUuid :
                                    traceAction.entityType === 'Member' ? '/admin/members/' + traceAction.entityUuid :
                                    `#`"
                               class="text-muted text-decoration-none"
                               x-text="traceAction.entityType"
                               x-bind:title="traceAction.entityUuid"></a>
                            <span x-show="traceAction.action === 'DELETE'" class="text-muted" x-text="traceAction.entityType" x-bind:title="traceAction.entityUuid"></span>
                            has been
                            <span x-show="traceAction.action === 'CREATE'" class="text-success">created</span>
                            <span x-show="traceAction.action === 'UPDATE'" class="text-primary">updated</span>
                            <span x-show="traceAction.action === 'DELETE'" class="text-danger">deleted</span>
                            <span x-show="traceAction.action === 'LOGIN'" class="text-warning">logged in</span>
                        </p>
                        <span class="text-muted" x-text="new Date(traceAction.actionDateTime).toLocaleString()"></span>
                    </div>
                </div>
            </div>
        </template>

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
                    <li class="page-item" :class="{ disabled: page === maxPage || maxPage === 0 }">
                        <a class="page-link" @click="setPage(maxPage)">&raquo;</a>
                    </li>
                </ul>
            </nav>
        </div>
    </div>

    <script>
        async function getTraceActions(filter, page) {
            const params = new URLSearchParams({
                page: page || 1,
                limit: 8
            });

            if (filter && filter.entityType) {
                params.append('entityType', filter.entityType);
            }

            if (filter && filter.action) {
                params.append('action', filter.action);
            }

            const response = await fetch(`/admin/api/trace-actions?` + params.toString());
            return response.json();
        }
    </script>
</@navigation.display>