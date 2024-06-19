<#import "_navigation.ftl" as navigation />

<@navigation.display canonicalUrl="${baseUrl}/search">
    <div x-data="{
        animes: [],
        value: <#if query?? && query?has_content>'${query}'<#else>''</#if>
    }"<#if query?? && query?has_content> x-init="animes = (await search(value)).data"</#if>>
        <div class="container my-3">
            <input type="text" id="search" class="form-control-lg w-100 bg-dark text-white"
                   placeholder="Rechercher" x-model="value" @input="animes = (await search(value)).data" autofocus>
        </div>

        <div class="row g-3 mt-3 justify-content-center" style="min-height: 60vh;">
            <template x-for="anime in animes">
                <div class="col-md-2 col-6 mt-0 mb-4">
                    <article x-data="{hover:false}" class="shikk-element">
                        <a x-bind:href="'/animes/' + anime.slug"
                           class="text-decoration-none text-white"
                           @mouseenter="hover = true" @mouseleave="hover = false">
                            <div class="position-relative">
                                <img x-bind:src="'${apiUrl}/v1/attachments?uuid=' + anime.uuid + '&type=image'"
                                     x-bind:alt="anime.shortName + ' anime'"
                                     loading="lazy"
                                     class="img-fluid rounded-top-4"
                                     width="480"
                                     height="720">

                                <div class="mt-2 mx-2 mb-1">
                                    <span class="h6 text-truncate-2 fw-bold mb-0" x-text="anime.shortName"></span>

                                    <template x-for="langType in anime.langTypes">
                                        <p class="text-muted mt-0 mb-0">
                                            <template x-if="langType === 'SUBTITLES'">
                                                <span>
                                                    <img src="${baseUrl}/assets/img/icons/subtitles.svg" alt="Subtitles"
                                                         class="me-2">
                                                    Sous-titrage
                                                </span>
                                            </template>
                                            <template x-if="langType === 'VOICE'">
                                                <span>
                                                    <img src="${baseUrl}/assets/img/icons/voice.svg" alt="Voice"
                                                         class="me-2">
                                                    Doublage
                                                </span>
                                            </template>
                                        </p>
                                    </template>
                                </div>

                                <div class="bg-black bg-opacity-75 bg-blur position-absolute top-0 start-0 w-100 h-100 mh-100 p-3 rounded-top-4"
                                     style="display: none;" x-show="hover">
                                    <div class="h6 text-truncate-2 fw-bold"
                                         x-text="anime.shortName.toUpperCase()"></div>
                                    <hr>
                                    <div class="text-truncate-6" x-text="anime.description"></div>
                                </div>
                            </div>
                        </a>
                    </article>
                </div>
            </template>
        </div>
    </div>

    <script src="${baseUrl}/assets/js/axios.min.js"></script>
    <script>
        let abortController = null;

        async function search(query) {
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

            window.history.pushState({}, '', '/search?q=' + encodeURIComponent(trimmedQuery));

            return axios.get('/api/v1/animes?name=' + trimmedQuery + '&limit=12', {signal: abortController.signal})
                .then(response => response.data)
                .catch(error => {
                    if (error.name === 'AbortError') {
                        return [];
                    }

                    throw error;
                });
        }
    </script>
</@navigation.display>