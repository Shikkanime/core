<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div x-data="{
        pageable: {},
        page: 1,
        maxPage: 1,
        pages: [],
        async init() {
            await this.fetchTraceActions();
            this.pages = this.generatePageNumbers(this.page, this.maxPage);
        },
        async fetchTraceActions() {
            this.pageable = await getTraceActions(this.page);
            this.maxPage = Math.ceil(this.pageable.total / this.pageable.limit);
        },
        async setPage(newPage) {
            this.page = newPage;
            await this.fetchTraceActions();
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
        <template x-for="traceAction in pageable.data">
            <div class="card px-3 mb-3">
                <div class="d-flex align-items-center my-3">
                    <i x-show="traceAction.action === 'CREATE'" class="bi bi-database-add text-success"></i>
                    <i x-show="traceAction.action === 'UPDATE'" class="bi bi-database-up text-primary"></i>
                    <i x-show="traceAction.action === 'DELETE'" class="bi bi-database-x text-danger"></i>

                    <div class="ms-3">
                        <p class="mb-0">
                            <a x-show="traceAction.action === 'CREATE' || traceAction.action === 'UPDATE'" x-bind:href="traceAction.entityType === 'Anime' ? '/admin/animes/' + traceAction.entityUuid :
                                    traceAction.entityType === 'EpisodeMapping' ? '/admin/episodes/' + traceAction.entityUuid :
                                    `#`"
                               class="text-muted text-decoration-none"
                               x-text="traceAction.entityType"
                               x-bind:title="traceAction.entityUuid"></a>
                            <span x-show="traceAction.action === 'DELETE'" class="text-muted" x-text="traceAction.entityType" x-bind:title="traceAction.entityUuid"></span>
                            has been
                            <span x-show="traceAction.action === 'CREATE'" class="text-success">created</span>
                            <span x-show="traceAction.action === 'UPDATE'" class="text-primary">updated</span>
                            <span x-show="traceAction.action === 'DELETE'" class="text-danger">deleted</span>
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
                    <li class="page-item" :class="{ disabled: page === maxPage }">
                        <a class="page-link" @click="setPage(maxPage)">&raquo;</a>
                    </li>
                </ul>
            </nav>
        </div>
    </div>

    <script>
        async function getTraceActions(page) {
            const params = new URLSearchParams({
                page: page || 1,
                limit: 8
            });

            const response = await axios.get(`/api/trace-actions?` + params.toString());
            return response.data;
        }
    </script>
</@navigation.display>