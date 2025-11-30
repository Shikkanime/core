<#import "_navigation.ftl" as navigation />
<#import "macros/pagination.ftl" as ui>

<@navigation.display>
    <div x-data="{
        loading: false,
        filter: {},
        pageable: {},
        page: 1,
        maxPage: 1,
        limit: 5,
        async init() {
            await this.fetchTraceActions();
        },
        async fetchTraceActions() {
            if (this.loading) {
                return;
            }

            this.loading = true;
            this.pageable = await getTraceActions(this.filter, this.page, this.limit);
            this.loading = false;
            this.maxPage = Math.ceil(this.pageable.total / this.pageable.limit);
        },
        async setPage(newPage) {
            this.page = newPage;
            await this.fetchTraceActions();
        },
        async applyFilters() {
            this.page = 1;
            await this.fetchTraceActions();
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
            <@ui.pageSizeSelector />
            <@ui.alpinePagination />
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
    </div>

    <script>
        async function getTraceActions(filter, page, limit) {
            const params = new URLSearchParams({
                page: page || 1,
                limit: limit
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