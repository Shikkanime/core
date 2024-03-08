<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div class="container my-3">
        <input type="text" id="search" class="form-control-lg w-100 bg-dark text-white"
               placeholder="Rechercher" value="<#if query??>${query}</#if>">
    </div>

    <div class="row justify-content-center" id="result-list" style="min-height: 50vh;">

    </div>

    <script src="/assets/js/main.js"></script>
    <script>
        <#if query??>
        search('${query}');
        </#if>

        document.getElementById('search').addEventListener('input', function () {
            if (this.value.length > 0) {
                window.history.pushState({}, '', '/search?q=' + encodeURIComponent(this.value));
            } else {
                window.history.pushState({}, '', '/search');
            }

            search(this.value);
        });

        async function search(query) {
            const trimmedQuery = query.trim();

            if (trimmedQuery.length === 0) {
                document.getElementById('result-list').innerHTML = '';
                return;
            }

            const animes = await callApi('/api/v1/animes?name=' + trimmedQuery + '&limit=12');
            document.getElementById('result-list').innerHTML = animes.data.map(anime => template(anime)).join('');
        }

        function template(anime) {
            return `<div class="col-md-2 col-6 mt-0">
        <article x-data="{ hover: false }">
            <a href="/animes/` + anime.slug + `" class="text-decoration-none text-white" @mouseenter="hover = true"
               @mouseleave="hover = false">
                <div class="position-relative">
                    <img src="https://api.shikkanime.fr/v1/attachments?uuid=` + anime.uuid + `&type=image"
                         alt="` + anime.shortName.replace("\"", "'") + ` anime image" class="img-fluid" width="480"
                         height="720">

                    <span class="h6 mt-2 text-truncate-2">` + anime.shortName + `</span>

                    <div class="bg-black bg-opacity-75 position-absolute top-0 start-0 w-100 h-100 mh-100 p-3"
                         x-show="hover">
                        <div class="h6 text-truncate-2">
                            ` + anime.name.toUpperCase() + `
                        </div>

                        <hr>

                        ` + (anime.description != null ? `<div class="text-truncate-6">` + anime.description + `</div>` : ``) + `
                    </div>
                </div>
            </a>
        </article>
    </div>`;
        }
    </script>
</@navigation.display>