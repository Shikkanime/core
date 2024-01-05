<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div class="row g-3 align-items-center mb-3">
        <div class="col-auto">
            <label class="form-label" for="nameInput">Name</label>
            <input type="text" class="form-control" id="nameInput" placeholder="One Piece">
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

    <script>
        function buildTableElement(uuid, name, image, description, status) {
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

        async function getAnimes(name) {
            let params = '';

            if (name) {
                params += '?name=' + name;
            }

            return await fetch('/api/v1/animes' + params)
                .then(response => response.json())
                .catch(error => console.error(error));
        }

        function buildTable(animes) {
            const table = document.getElementById('animes-table');

            table.innerHTML = '';

            animes.forEach(anime => {
                table.innerHTML += buildTableElement(anime.uuid, anime.name, anime.image, anime.description, anime.status);
            });
        }

        document.addEventListener('DOMContentLoaded', async () => {
            const animes = await getAnimes();
            buildTable(animes);
        });

        let timeout = null;

        document.getElementById('nameInput').addEventListener('input', async (event) => {
            // Avoids calling the API on every key press
            clearTimeout(timeout);

            timeout = setTimeout(async () => {
                const animes = await getAnimes(event.target.value);
                buildTable(animes);
            }, 500);
        });
    </script>
</@navigation.display>