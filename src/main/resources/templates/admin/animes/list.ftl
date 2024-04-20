<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <div x-data="{
        pageable: {},
        page: 1,
        maxPage: 0,
        search: '',
        invalid: false,
        setPageable: function (page, pageable) {
            this.page = page;
            this.pageable = pageable;
            this.maxPage = Math.ceil(pageable.total / pageable.limit);
        }
    }" x-init="setPageable(1, await getAnimes('', page, false));">
        <div class="row g-3 align-items-center mb-3">
            <div class="col-auto">
                <label class="form-label" for="nameInput">Name</label>
                <input type="text" class="form-control" id="nameInput"
                       x-model="search" @input="setPageable(1, await getAnimes(search, 1, invalid))">
            </div>
            <div class="col-auto">
                <input class="form-check-input" type="checkbox" id="invalidInput"
                       @input="invalid = $event.target.checked; setPageable(1, await getAnimes(search, 1, invalid))">
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
                        <span class="me-1 badge bg-danger" x-show="anime.status === 'INVALID'">Invalid</span>
                        <span class="me-1 badge bg-success" x-show="anime.status !== 'INVALID'">Valid</span>
                        <span x-text="anime.shortName"></span>
                    </th>
                    <td x-text="anime.description"></td>
                    <td>
                        <a x-bind:href="'/admin/animes/' + anime.uuid" class="btn btn-warning">
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
                    <li class="page-item">
                        <a class="page-link"
                           x-bind:class="{disabled: page === 1}"
                           @click="setPageable(1, await getAnimes(search, page - 1, invalid))">
                            &laquo;
                        </a>
                    </li>
                    <template
                            x-for="i in Array.from({length: 7}, (_, index) => page + index - 3).filter(i => i >= 1 && i <= maxPage)">
                        <li class="page-item" x-bind:class="{active: page === i}">
                            <a class="page-link" @click="setPageable(i, await getAnimes(search, i, invalid))"
                               x-text="i"></a>
                        </li>
                    </template>
                    <li class="page-item">
                        <a class="page-link"
                           x-bind:class="{disabled: page === maxPage}"
                           @click="setPageable(maxPage, await getAnimes(search, page + 1, invalid))">
                            &raquo;
                        </a>
                    </li>
                </ul>
            </nav>
        </div>
    </div>

    <script>
        async function getAnimes(name, page, invalid) {
            let params = '?sort=releaseDateTime&desc=releaseDateTime' + (invalid ? '&status=INVALID' : '');

            if (name) {
                params = '?name=' + name;
            }

            return await axios.get('/api/v1/animes' + params + '&page=' + (page || 1) + '&limit=7')
                .then(response => response.data)
                .catch(() => []);
        }
    </script>
</@navigation.display>