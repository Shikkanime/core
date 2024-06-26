<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <div x-data="{
        pageable: {},
        page: 1,
        maxPage: 1,
        search: '',
        invalid: false,
        pages: [],
        async init() {
            await this.fetchAnimes();
            this.pages = this.generatePageNumbers(this.page, this.maxPage);
        },
        async fetchAnimes() {
            this.pageable = await getAnimes(this.search, this.page, this.invalid);
            this.maxPage = Math.ceil(this.pageable.total / this.pageable.limit);
        },
        async setPage(newPage) {
            this.page = newPage;
            await this.fetchAnimes();
            this.pages = this.generatePageNumbers(this.page, this.maxPage);
        },
        async applyFilters() {
            this.page = 1;
            await this.fetchAnimes();
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
        <div class="row g-3 align-items-center mb-3">
            <div class="col-auto">
                <label class="form-label" for="nameInput">Name</label>
                <input type="text" class="form-control" id="nameInput"
                       x-model="search" @input="applyFilters">
            </div>
            <div class="col-auto">
                <input class="form-check-input" type="checkbox" id="invalidInput"
                       x-model="invalid" @change="applyFilters">
                <label class="form-check-label" for="invalidInput">Only invalid</label>
            </div>
        </div>

        <table class="table table-striped table-bordered">
            <thead>
            <tr>
                <th scope="col">Name</th>
                <th scope="col">Description</th>
                <th scope="col">Actions</th>
            </tr>
            </thead>
            <tbody class="table-group-divider">
            <template x-for="anime in pageable.data">
                <tr>
                    <th scope="row">
                        <span class="me-1 badge"
                              :class="anime.status === 'INVALID' ? 'bg-danger' : 'bg-success'"
                              x-text="anime.status === 'INVALID' ? 'Invalid' : 'Valid'"></span>
                        <span x-text="anime.shortName"></span>
                    </th>
                    <td x-text="anime.description"></td>
                    <td>
                        <a :href="'/admin/animes/' + anime.uuid" class="btn btn-warning">
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
        async function getAnimes(name, page, invalid) {
            let params = new URLSearchParams({
                sort: 'releaseDateTime',
                desc: 'releaseDateTime',
                page: page || 1,
                limit: 7
            });

            if (invalid) {
                params.append('status', 'INVALID');
            }

            if (name) {
                params = new URLSearchParams({
                    name: name,
                });
            }

            try {
                const response = await axios.get(`/api/v1/animes?` + params.toString());
                return response.data;
            } catch (error) {
                console.error('Error fetching animes:', error);
                return {data: [], total: 0, limit: 7};
            }
        }
    </script>
</@navigation.display>