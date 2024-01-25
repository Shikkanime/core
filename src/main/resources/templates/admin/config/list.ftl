<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <div class="row g-3 align-items-center mb-3">
        <div class="col-auto">
            <label class="form-label" for="nameInput">Name</label>
            <input type="text" class="form-control" id="nameInput">
        </div>
    </div>

    <div class="table-responsive">
        <table class="table table-striped table-bordered">
            <thead>
            <tr>
                <th scope="col">Key</th>
                <th scope="col">Value</th>
                <th scope="col">Actions</th>
            </tr>
            </thead>
            <tbody class="table-group-divider" id="config-table">

            </tbody>
        </table>
    </div>

    <script>
        function buildTableElement(uuid, key, value) {
            return `<tr>
                <th scope="row">` + key + `</th>
                <td>` + value + `</td>
                <td>
                    <a href="/admin/config/` + uuid + `" class="btn btn-warning">
                        <i class="bi bi-pencil-square"></i>
                        Edit
                    </a>
                </td>
            </tr>`
        }

        async function getConfigs(name) {
            let params = '';

            if (name) {
                params = '?name=' + name;
            }

            return await callApi('/api/config' + params);
        }

        function buildTable(configs) {
            const table = document.getElementById('config-table');

            table.innerHTML = '';

            configs.forEach(config => {
                table.innerHTML += buildTableElement(config.uuid, config.propertyKey, config.propertyValue);
            });
        }

        document.addEventListener('DOMContentLoaded', async () => {
            const configs = await getConfigs();
            buildTable(configs);
        });

        let timeout = null;

        document.getElementById('nameInput').addEventListener('input', async (event) => {
            // Avoids calling the API on every key press
            clearTimeout(timeout);

            timeout = setTimeout(async () => {
                const configs = await getConfigs(event.target.value);
                buildTable(configs);
            }, 500);
        });
    </script>
</@navigation.display>