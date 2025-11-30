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
            name: ''
        },
        async init() {
            const urlParams = new URLSearchParams(window.location.search);
            const nameFromUrl = urlParams.get('name');
            const pageFromUrl = urlParams.get('page');
            const limitFromUrl = urlParams.get('limit');

            if (nameFromUrl) {
                this.filter.name = nameFromUrl;
            }

            if (pageFromUrl) {
                this.page = parseInt(pageFromUrl);
            }

            if (limitFromUrl) {
                this.limit = parseInt(limitFromUrl);
            }

            await this.fetchAnimes();
            this.applyFilterParameters(); // Ensure URL is updated after initial load
        },
        async fetchAnimes() {
            if (this.loading) {
                return;
            }

            this.loading = true;
            this.pageable = await getAnimes(this.filter, this.page, this.limit);
            this.loading = false;
            this.maxPage = Math.ceil(this.pageable.total / this.limit);
        },
        applyFilterParameters() {
            // Check against default limit as well
            if (this.filter.name === '' && this.page === 1 && this.limit === 10) {
                window.history.pushState({}, '', '/admin/animes');
            } else {
                const params = new URLSearchParams();

                if (this.filter.name) {
                    params.append('name', this.filter.name);
                }

                if (this.page !== 1) {
                    params.append('page', this.page);
                }

                // Add limit to URL params if not default
                if (this.limit !== 10) {
                    params.append('limit', this.limit);
                }

                window.history.pushState({}, '', '/admin/animes?' + params.toString());
            }
        },
        async setPage(newPage) {
            this.page = newPage;
            this.applyFilterParameters();
            await this.fetchAnimes();
        },
        async applyFilters() {
            this.page = 1;
            this.applyFilterParameters();
            await this.fetchAnimes();
        }
    }" x-init="init">
        <div class="modal fade" id="updateAllModal" tabindex="-1" aria-labelledby="updateAllModalLabel" aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h1 class="modal-title fs-5" id="updateAllModalLabel">Update confirmation</h1>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        Are you sure you want to force update all animes? This operation may take some time.
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>

                        <a class="btn btn-success" href="/admin/api/animes/force-update-all">
                            Confirm
                        </a>
                    </div>
                </div>
            </div>
        </div>
        
        <div class="row g-3 align-items-center mb-3">
            <div class="col-auto">
                <div class="search-container">
                    <input type="text" class="form-control search-input" id="nameInput" placeholder="Anime Name"
                           x-model="filter.name" @input="applyFilters">
                    <i class="bi bi-search search-icon"></i>
                </div>
            </div>
            <@ui.pageSizeSelector />
            <div class="col-auto">
                <div class="dropdown">
                    <button class="btn btn-light" type="button" id="actionsMenuButton" data-bs-toggle="dropdown" aria-expanded="false">
                        <i class="bi bi-three-dots-vertical"></i>
                    </button>
                    <ul class="dropdown-menu" aria-labelledby="actionsMenuButton">
                        <li>
                            <a class="dropdown-item" href="#" data-bs-toggle="modal" data-bs-target="#updateAllModal">
                                <i class="bi bi-check-all me-2"></i>Force update all
                            </a>
                        </li>
                    </ul>
                </div>
            </div>
            <@ui.alpinePagination />
        </div>

        <table class="table table-striped table-bordered">
            <thead>
            <tr>
                <th scope="col">Name</th>
                <th scope="col">Description</th>
                <th scope="col" style="width: 50px;"></th>
            </tr>
            </thead>
            <tbody class="table-group-divider">
            <template x-for="anime in pageable.data">
                <tr>
                    <th scope="row" x-text="anime.shortName">
                    </th>
                    <td x-text="anime.description ? anime.description.substring(0, 125) + (anime.description.length > 125 ? '...' : '') : ''"></td>
                    <td>
                        <a :href="'/admin/animes/' + anime.uuid" class="btn btn-warning">
                            <i class="bi bi-pencil-square"></i>
                        </a>
                    </td>
                </tr>
            </template>
            </tbody>
        </table>
    </div>

    <script>
        async function getAnimes(filter, page, limit) {
            let params = new URLSearchParams({
                sort: 'releaseDateTime',
                desc: 'releaseDateTime',
                page: page || 1,
                limit: limit || 10
            });

            if (filter.name) {
                params = new URLSearchParams({
                    name: filter.name,
                    page: page || 1,
                    limit: limit || 10
                });
            }

            try {
                const response = await fetch(`/api/v1/animes?` + params.toString());
                return response.json();
            } catch (error) {
                console.error('Error fetching animes:', error);
                return {data: [], total: 0, limit: limit || 10};
            }
        }
    </script>
</@navigation.display>