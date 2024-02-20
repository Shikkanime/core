<#macro display anime>
    <div class="col-md-2 col-6 mt-0">
        <article x-data="{ hover: false }">
            <a href="/animes/${anime.slug}" class="text-decoration-none text-white" @mouseenter="hover = true"
               @mouseleave="hover = false">
                <div class="position-relative">
                    <img src="https://api.shikkanime.fr/v1/attachments?uuid=${anime.uuid}&type=image"
                         alt="${anime.shortName?replace("\"", "'")} anime image" class="img-fluid" width="480" height="720">

                    <span class="h6 mt-2 text-truncate-2">${anime.shortName}</span>

                    <div class="bg-black bg-opacity-75 position-absolute top-0 start-0 w-100 h-100 mh-100 p-3"
                         x-show="hover">
                        <div class="h6 text-truncate-2">
                            ${anime.name?upper_case}
                        </div>

                        <hr>

                        <#if anime.description??>
                            <div class="text-truncate-6">
                                ${anime.description}
                            </div>
                        </#if>
                    </div>
                </div>
            </a>
        </article>
    </div>
</#macro>