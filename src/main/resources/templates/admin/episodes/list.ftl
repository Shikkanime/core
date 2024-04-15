<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <div x-data="{
        pageable: {},
        page: 1,
        maxPage: 0,
        anime: '',
        invalid: false,
        setPageable: function (page, pageable) {
            this.page = page;
            this.pageable = pageable;
            this.maxPage = Math.ceil(pageable.total / pageable.limit);
        }
    }" x-init="setPageable(1, await getEpisodes('', page, false));">
        <div class="row g-3 align-items-center mb-3">
            <div class="col-auto">
                <label class="form-label" for="animeInput">Anime UUID</label>
                <input type="text" class="form-control" id="animeInput"
                       x-model="anime" @input="setPageable(1, await getEpisodes(anime, 1, invalid))">
            </div>
            <div class="col-auto">
                <input class="form-check-input" type="checkbox" id="invalidInput"
                       @input="invalid = $event.target.checked; setPageable(1, await getEpisodes(anime, 1, invalid))">
                <label class="form-check-label" for="invalidInput">Only invalid</label>
            </div>
        </div>

        <table class="table table-striped table-bordered">
            <thead>
            <tr>
                <th scope="col">Anime</th>
                <th scope="col">Details</th>
                <th scope="col">Description</th>
                <th scope="col">Actions</th>
            </tr>
            </thead>
            <tbody class="table-group-divider">
            <template x-for="episode in pageable.data">
                <tr>
                    <th scope="row">
                        <span class="me-1 badge bg-danger" x-show="episode.status === 'INVALID'">Invalid</span>
                        <span class="me-1 badge bg-success" x-show="episode.status !== 'INVALID'">Valid</span>
                        <span x-text="episode.anime.shortName"></span>
                    </th>
                    <td>
                        S<span x-text="episode.season"></span>
                        <span x-show="episode.episodeType === 'EPISODE'">EP</span>
                        <span x-show="episode.episodeType === 'FILM'">MOV</span>
                        <span x-show="episode.episodeType === 'SPECIAL'">SP</span>
                        <span x-text="episode.number"></span>
                    </td>
                    <td x-text="episode.description"></td>
                    <td>
                        <a x-bind:href="'/admin/episodes/' + episode.uuid" class="btn btn-warning">
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
                           @click="setPageable(1, await getEpisodes(anime, page - 1, invalid))">
                            &laquo;
                        </a>
                    </li>
                    <template
                            x-for="i in Array.from({length: 7}, (_, index) => page + index - 3).filter(i => i >= 1 && i <= maxPage)">
                        <li class="page-item" x-bind:class="{active: page === i}">
                            <a class="page-link" @click="setPageable(i, await getEpisodes(anime, i, invalid))"
                               x-text="i"></a>
                        </li>
                    </template>
                    <li class="page-item">
                        <a class="page-link"
                           x-bind:class="{disabled: page === maxPage}"
                           @click="setPageable(maxPage, await getEpisodes(anime, maxPage, invalid))">
                            &raquo;
                        </a>
                    </li>
                </ul>
            </nav>
        </div>
    </div>

    <script>
        async function getEpisodes(anime, page, invalid) {
            let params = '?sort=lastReleaseDateTime,animeName,season,episodeType,number&desc=lastReleaseDateTime,animeName,season,episodeType,number' + (invalid ? '&status=INVALID' : '');

            if (anime) {
                params = '?anime=' + anime;
            }

            return await callApi('/api/v1/episode-mappings' + params + '&page=' + (page || 1) + '&limit=9');
        }
    </script>
</@navigation.display>