<#macro display anime>
    <div class="col-md-2 col-6 mt-0">
        <article>
            <a href="/animes/${anime.slug}" class="text-decoration-none text-white">
                <div class="hover-card position-relative">
                    <img loading="lazy" src="https://api.shikkanime.fr/v1/attachments?uuid=${anime.uuid}&type=image"
                         alt="${anime.shortName?replace("\"", "'")} anime image" class="w-100">
                    <span class="h6 mt-2 text-truncate-2">${anime.shortName}</span>

                    <div class="hover-card-description d-none bg-black bg-opacity-75 position-absolute top-0 start-0 w-100 h-100 mh-100 p-3">
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