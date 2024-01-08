<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <div class="row g-3 align-items-center mb-3">
        <div class="col-auto">
            <label class="form-label" for="nameInput">Name</label>
            <input type="text" class="form-control" id="nameInput">
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
        <tbody class="table-group-divider" id="animes-table">

        </tbody>
    </table>

    <div class="mt-3">
        <nav aria-label="Page navigation" id="pagination">
            <ul class="pagination justify-content-center">

            </ul>
        </nav>
    </div>

    <script>
        function buildTableElement(uuid, name, description, status) {
            const isInvalid = status === 'INVALID';

            return `<tr>
                <th scope="row"><span class="me-2 badge bg-` + (isInvalid ? 'danger' : 'success') + `">` + (isInvalid ? 'Invalid' : 'Valid') + `</span>` + name + `</th>
                <td>` + description + `</td>
                <td>
                    <a href="/admin/animes/` + uuid + `" class="btn btn-warning">
                        <i class="bi bi-pencil-square"></i>
                        Edit
                    </a>
                </td>
            </tr>`
        }

        async function getAnimes(name, page) {
            let params = '?sort=releaseDateTime&desc=releaseDateTime';

            if (name) {
                params = '?name=' + name;
            }

            return await fetch('/api/v1/animes' + params + '&page=' + (page || 1) + '&limit=6')
                .then(response => response.json())
                .catch(error => console.error(error));
        }

        function buildTable(animes) {
            const table = document.getElementById('animes-table');

            table.innerHTML = '';

            animes.forEach(anime => {
                table.innerHTML += buildTableElement(anime.uuid, anime.name, anime.description, anime.status);
            });
        }

        async function changePage(page) {
            const name = document.getElementById('nameInput').value;
            const pageable = await getAnimes(name, page);
            buildTable(pageable.data);
            buildPagination(pageable);
        }

        function buildPagination(pageable) {
            const totalPages = Math.ceil(pageable.total / pageable.limit);
            const pagination = document.getElementById('pagination').querySelector('ul');

            pagination.innerHTML = '';

            const hasPrevious = pageable.page > 1;
            const previousPage = hasPrevious ? pageable.page - 1 : 1;
            const hasNext = pageable.page < totalPages;
            const nextPage = hasNext ? pageable.page + 1 : totalPages;

            pagination.innerHTML += `<li class="page-item">
                <a class="page-link ` + (hasPrevious ? '' : 'disabled') + `" aria-label="Previous" onclick="changePage(` + previousPage + `)" ` + (hasPrevious ? '' : 'disabled') + `>
                    <span aria-hidden="true">&laquo;</span>
                </a>
            </li>`;

            for (let i = 1; i <= totalPages; i++) {
                const isActive = pageable.page === i;
                pagination.innerHTML += `<li class="page-item ` + (isActive ? 'active' : '') + `"><a class="page-link" onclick="changePage(` + i + `)">` + i + `</a></li>`;
            }

            pagination.innerHTML += `<li class="page-item">
                <a class="page-link ` + (hasNext ? '' : 'disabled') + `" aria-label="Next" onclick="changePage(` + nextPage + `)" ` + (hasNext ? '' : 'disabled') + `>
                    <span aria-hidden="true">&raquo;</span>
                </a>
            </li>`;
        }

        document.addEventListener('DOMContentLoaded', async () => {
            const pageable = await getAnimes();
            buildTable(pageable.data);
            buildPagination(pageable);
        });

        let timeout = null;

        document.getElementById('nameInput').addEventListener('input', async (event) => {
            // Avoids calling the API on every key press
            clearTimeout(timeout);

            timeout = setTimeout(async () => {
                const pageable = await getAnimes(event.target.value);
                buildTable(pageable.data);
                buildPagination(pageable);
            }, 500);
        });
    </script>
</@navigation.display>