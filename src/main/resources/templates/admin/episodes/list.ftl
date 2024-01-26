<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <div class="row g-3 align-items-center mb-3">
        <div class="col-auto">
            <label class="form-label" for="animeInput">Anime UUID</label>
            <input type="text" class="form-control" id="animeInput">
        </div>
    </div>

    <table class="table table-striped table-bordered">
        <thead>
        <tr>
            <th scope="col">Anime</th>
            <th scope="col">Platform</th>
            <th scope="col">Episode type</th>
            <th scope="col">Lang type</th>
            <th scope="col">Season</th>
            <th scope="col">Number</th>
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
        function buildTableElement(uuid, anime, platform, episodeType, langType, season, number) {
            return `<tr>
                <th scope="row">` + anime + `</th>
                <td>` + platform + `</td>
                <td>` + episodeType + `</td>
                <td>` + langType + `</td>
                <td>` + season + `</td>
                <td>` + number + `</td>
                <td>
                    <a href="/admin/episodes/` + uuid + `" class="btn btn-warning">
                        <i class="bi bi-pencil-square"></i>
                        Edit
                    </a>
                </td>
            </tr>`
        }

        async function getEpisodes(anime, page) {
            let params = '?sort=releaseDateTime&desc=releaseDateTime';

            if (anime) {
                params = '?anime=' + anime;
            }

            return await callApi('/api/v1/episodes' + params + '&page=' + (page || 1) + '&limit=12');
        }

        function buildTable(episodes) {
            const table = document.getElementById('episodes-table');

            table.innerHTML = '';

            episodes.forEach(episode => {
                table.innerHTML += buildTableElement(episode.uuid, episode.anime.name, episode.platform, episode.episodeType, episode.langType, episode.season, episode.number);
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
    </script>
</@navigation.display>