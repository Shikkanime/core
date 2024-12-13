<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div x-data="{
        loading: false,
        pageable: {},
        page: 1,
        maxPage: 1,
        pages: [],
        async init() {
            await this.fetchAnimeAlerts();
            this.pages = this.generatePageNumbers(this.page, this.maxPage);
        },
        async fetchAnimeAlerts() {
            if (this.loading) {
                return;
            }

            this.loading = true;
            this.pageable = await getAnimeAlerts(this.page);
            this.loading = false;
            this.maxPage = Math.ceil(this.pageable.total / this.pageable.limit);
        },
        async setPage(newPage) {
            this.page = newPage;
            await this.fetchAnimeAlerts();
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
        <template x-for="animeAlert in pageable.data">
            <div class="card p-3 mb-3">
                <div class="d-flex align-items-center my-3">
                    <img :src="'${apiUrl}/v1/attachments?uuid=' + animeAlert.anime.uuid + '&type=image'" class="img-fluid" style="width: 5rem; border-radius: 8px">

                    <div class="ms-3">
                        <a x-bind:href="'/admin/animes/' + animeAlert.anime.uuid"
                           class="text-decoration-none text-dark fw-bold"
                           x-text="animeAlert.anime.shortName"></a>

                        <ul>
                            <template x-for="error in animeAlert.errors">
                                <li>
                                    <span x-text="error.type"></span>
                                    (<span class="text-muted" x-text="error.reason"></span>)
                                </li>
                            </template>
                        </ul>
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
        async function getAnimeAlerts(page) {
            const params = new URLSearchParams({
                page: page || 1,
                limit: 4
            });

            const response = await axios.get(`/api/v1/animes/alerts?` + params.toString());
            return response.data;
        }
    </script>
</@navigation.display>