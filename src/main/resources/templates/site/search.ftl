<#import "_navigation.ftl" as navigation />

<@navigation.display canonicalUrl="${baseUrl}/search">
    <div x-data="{
        animes: [],
        hasMore: false,
        page: <#if page?? && page?has_content>${page}<#else>1</#if>,
        searchParameters: {
            searchTypes: <#if searchTypes?? && searchTypes?has_content>[<#list searchTypes?split(',') as searchType>'${searchType}'<#if searchType_has_next>,</#if></#list>]<#else>['SUBTITLES', 'VOICE']</#if>,
            query: <#if query?? && query?has_content>'${query}'<#else>''</#if>
        },
    }" x-init="
        const pageable = await search(searchParameters.query, searchParameters.searchTypes, page);
        animes = pageable.data;
        hasMore = (page * 12) < pageable.total;

        $watch('searchParameters', async (value) => {
            page = 1;
            const pageable = await search(value.query, value.searchTypes, page);
            animes = pageable.data;
            hasMore = (page * 12) < pageable.total;
        });

        loadMore = () => {
            page = page + 1;

            search(searchParameters.query, searchParameters.searchTypes, page)
                .then(pageable => {
                    animes = animes.concat(pageable.data);
                    hasMore = (page * 12) < pageable.total;
                });
        }
    ">
        <div class="container my-3">
            <input type="text" id="search" class="form-control-lg w-100 bg-dark text-white"
                   placeholder="Rechercher" x-model="searchParameters.query" autofocus>

            <div class="mt-3 p-2 shikk-element" x-data="{open: false}">
                <div class="d-flex align-items-center justify-content-center" @click="open = !open">
                    <h4 class="h4 mb-0 ms-0 me-auto">Recherche avancée</h4>

                    <div class="ms-auto me-0">
                        <svg x-show="!open" xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-chevron-down"
                             viewBox="0 0 16 16">
                            <path fill-rule="evenodd"
                                  d="M1.646 4.646a.5.5 0 0 1 .708 0L8 10.293l5.646-5.647a.5.5 0 0 1 .708.708l-6 6a.5.5 0 0 1-.708 0l-6-6a.5.5 0 0 1 0-.708"/>
                        </svg>
                        <svg x-show="open" xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-chevron-up"
                             viewBox="0 0 16 16">
                            <path fill-rule="evenodd" d="M7.646 4.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1-.708.708L8 5.707l-5.646 5.647a.5.5 0 0 1-.708-.708z"/>
                        </svg>
                    </div>
                </div>

                <div class="my-3" style="display: none;" x-show="open" x-transition>
                    <div class="d-flex flex-wrap justify-content-evenly justify-content-md-between gap-2">
                        <#list ['#', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'] as categoryLetter>
                            <button class="btn btn-outline-light btn-sm position-relative" style="min-width: 35px;"
                                    @click="searchParameters.query = '${categoryLetter}'"
                                    x-bind:class="{'active': searchParameters.query === '${categoryLetter}'}">
                                ${categoryLetter}
                            </button>
                        </#list>
                    </div>

                    <div class="mt-3 d-flex justify-content-evenly gap-2">
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" value="SUBTITLES" id="subtitlesInput"
                                   x-model="searchParameters.searchTypes">
                            <label class="form-check-label" for="subtitlesInput">
                                Sous-titrage
                            </label>
                        </div>

                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" value="VOICE" id="voiceInput"
                                   x-model="searchParameters.searchTypes">
                            <label class="form-check-label" for="voiceInput">
                                Doublage
                            </label>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="row g-3 mt-3 justify-content-center" style="min-height: 60vh;">
            <template x-for="anime in animes">
                <div class="col-md-2 col-6 mt-0 mb-4">
                    <article x-data="{hover:false}" class="shikk-element">
                        <a x-bind:href="'/animes/' + anime.slug" @mouseenter="hover = true" @mouseleave="hover = false">
                            <div class="shikk-element-content">
                                <img x-bind:src="'${apiUrl}/v1/attachments?uuid=' + anime.uuid + '&type=image'"
                                     x-bind:alt="anime.shortName + ' anime'"
                                     loading="lazy"
                                     class="shikk-element-content-img"
                                     width="480"
                                     height="720">

                                <span class="h6 text-truncate-2 fw-bold mt-2 mb-0" x-text="anime.shortName"></span>

                                <template x-for="langType in anime.langTypes">
                                    <p class="text-muted my-0">
                                        <template x-if="langType === 'SUBTITLES'">
                                                <span class="d-flex align-items-center">
                                                    <img src="${baseUrl}/assets/img/icons/subtitles.svg" alt="Subtitles"
                                                         class="me-2">
                                                    Sous-titrage
                                                </span>
                                        </template>
                                        <template x-if="langType === 'VOICE'">
                                                <span class="d-flex align-items-center">
                                                    <img src="${baseUrl}/assets/img/icons/voice.svg" alt="Voice"
                                                         class="me-2">
                                                    Doublage
                                                </span>
                                        </template>
                                    </p>
                                </template>
                            </div>

                            <div class="overlay" style="display: none;" x-show="hover">
                                <div class="h6 text-truncate-2 fw-bold"
                                     x-text="anime.shortName.toUpperCase()"></div>
                                <hr>
                                <div class="text-truncate-6" x-text="anime.description"></div>
                            </div>
                        </a>
                    </article>
                </div>
            </template>
        </div>

        <div class="text-center" x-show="hasMore">
            <button class="btn btn-light" @click="loadMore">
                Voir plus
            </button>
        </div>
    </div>

    <script>
        let abortController = null;

        async function search(query, searchTypes, page = 1) {
            if (abortController) {
                abortController.abort();
                abortController = null;
            }

            abortController = new AbortController();
            const trimmedQuery = query.trim();

            if (trimmedQuery.length === 0) {
                window.history.pushState({}, '', '/search');
                return [];
            }

            const encodedQuery = encodeURIComponent(trimmedQuery);
            const url = encodedQuery + '&searchTypes=' + searchTypes.join(',') + '&page=' + page;
            window.history.pushState({}, '', '/search?q=' + url);

            return fetch('/api/v1/animes?country=FR&name=' + url + '&limit=12', {signal: abortController.signal})
                .then(response => response.json())
                .catch(error => {
                    if (error.name === 'AbortError') {
                        return [];
                    }

                    throw error;
                });
        }
    </script>
</@navigation.display>