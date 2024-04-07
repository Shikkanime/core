<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <div class="d-flex align-content-center align-items-center mb-3">
        <div class="ms-0 me-auto row g-3 align-items-center">
            <div class="col-auto">
                <label class="form-label" for="animeInput">Anime UUID</label>
                <input type="text" class="form-control" id="animeInput">
            </div>
            <div class="col-auto">
                <div class="form-check">
                    <input class="form-check-input" type="checkbox" value="" id="invalidCheckbox">
                    <label class="form-check-label" for="invalidCheckbox">
                        Invalid
                    </label>
                </div>
            </div>
        </div>

        <div class="ms-auto me-0">
            <a href="/admin/episodes/update-all" class="btn btn-danger">
                <i class="bi bi-arrow-clockwise"></i>
                Update all
            </a>
        </div>
    </div>

    <table class="table table-striped table-bordered">
        <thead>
        <tr>
            <th scope="col">Anime</th>
            <th scope="col">Platform</th>
            <th scope="col">Details</th>
            <th scope="col">Description</th>
            <th scope="col">Actions</th>
        </tr>
        </thead>
        <tbody class="table-group-divider" id="episodes-table">

        </tbody>
    </table>

    <div class="mt-3">
        <nav aria-label="Page navigation" id="pagination">
            <ul class="pagination justify-content-center">

            </ul>
        </nav>
    </div>

    <script>
        function buildTableElement(uuid, anime, platform, episodeType, langType, season, number, description, status) {
            let episodeTypePrefix = '';

            switch (episodeType) {
                case 'EPISODE':
                    episodeTypePrefix = 'EP';
                    break;
                case 'SPECIAL':
                    episodeTypePrefix = 'SP';
                    break;
                case 'FILM':
                    episodeTypePrefix = 'MOV';
                    break;
            }

            let langTypePrefix = '';

            switch (langType) {
                case 'SUBTITLES':
                    langTypePrefix = 'SUB';
                    break;
                case 'VOICE':
                    langTypePrefix = 'DUB';
                    break;
            }

            const details = 'S' + season + ' ' + episodeTypePrefix + number + ' ' + langTypePrefix;
            const isInvalid = status === 'INVALID';

            return `<tr>
                <th scope="row"><span class="me-2 badge bg-` + (isInvalid ? 'danger' : 'success') + `">` + (isInvalid ? 'Invalid' : 'Valid') + `</span>` + anime + `</th>
                <td>` + platform.name + `</td>
                <td>` + details + `</td>
                <td>` + description + `</td>
                <td>
                    <a href="/admin/episodes/` + uuid + `" class="btn btn-warning">
                        <i class="bi bi-pencil-square"></i>
                        Edit
                    </a>
                </td>
            </tr>`
        }

        async function getEpisodes(anime, page) {
            let params = '?sort=lastUpdateDateTime&desc=lastUpdateDateTime';

            if (anime) {
                params = '?anime=' + anime;
            }

            let isInvalid = document.getElementById('invalidCheckbox').checked;

            if (isInvalid) {
                params += '&status=INVALID';
            }

            return await callApi('/api/v1/episodes' + params + '&page=' + (page || 1) + '&limit=12');
        }

        function buildTable(episodes) {
            const table = document.getElementById('episodes-table');
            table.innerHTML = '';

            episodes.forEach(episode => {
                table.innerHTML += buildTableElement(
                    episode.uuid,
                    episode.anime.shortName,
                    episode.platform,
                    episode.episodeType,
                    episode.langType,
                    episode.season,
                    episode.number,
                    episode.description || '',
                    episode.status,
                );
            });
        }

        async function changePage(page) {
            const anime = document.getElementById('animeInput').value;
            const pageable = await getEpisodes(anime, page);
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

            pagination.innerHTML += createPageItem(!hasPrevious, null, previousPage, '&laquo;', 'Previous');

            const startPage = Math.max(1, pageable.page - 4);
            const endPage = Math.min(totalPages, pageable.page + 4);
            for (let i = startPage; i <= endPage; i++) {
                const isActive = pageable.page === i;
                pagination.innerHTML += createPageItem(false, isActive, i, i);
            }

            pagination.innerHTML += createPageItem(!hasNext, null, nextPage, '&raquo;', 'Next');
        }

        function createPageItem(isDisable, isActive, page, text, ariaLabel = '') {
            const activeClass = !isDisable && isActive ? 'active' : '';
            const disabledClass = isDisable ? 'disabled' : '';

            return `<li class="page-item ` + activeClass + `">
        <a class="page-link ` + disabledClass + `" aria-label="` + ariaLabel + `" onclick="changePage(` + page + `)" ` + disabledClass + `>
            <span aria-hidden="true">` + text + `</span>
        </a>
    </li>`;
        }

        document.addEventListener('DOMContentLoaded', async () => {
            const pageable = await getEpisodes();
            buildTable(pageable.data);
            buildPagination(pageable);
        });

        let timeout = null;

        document.getElementById('animeInput').addEventListener('input', async (event) => {
            // Avoids calling the API on every key press
            clearTimeout(timeout);

            timeout = setTimeout(async () => {
                const pageable = await getEpisodes(event.target.value);
                buildTable(pageable.data);
                buildPagination(pageable);
            }, 500);
        });

        document.getElementById('invalidCheckbox').addEventListener('change', async (event) => {
            const pageable = await getEpisodes(document.getElementById('animeInput').value);
            buildTable(pageable.data);
            buildPagination(pageable);
        });
    </script>
</@navigation.display>