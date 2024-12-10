<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div class="toast-container position-fixed top-0 end-0 p-3">
        <div id="successToast" class="toast align-items-center text-bg-success border-0" role="alert"
             aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    Config updated successfully!
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"
                        aria-label="Close"></button>
            </div>
        </div>

        <div id="errorToast" class="toast align-items-center text-bg-danger border-0" role="alert" aria-live="assertive"
             aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    Error updating config!
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"
                        aria-label="Close"></button>
            </div>
        </div>
    </div>

    <div x-data="{configs: []}" x-init="configs = await loadConfigs()">
        <div class="row g-3 align-items-center mb-3">
            <div class="col-auto">
                <label class="form-label" for="nameInput">Name</label>
                <input type="text" class="form-control" id="nameInput"
                       @input="configs = await loadConfigs($event.target.value)">
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
                <tbody class="table-group-divider">
                <template x-for="config in configs">
                    <tr>
                        <th scope="row" x-text="config.propertyKey"></th>
                        <td>
                            <textarea class="form-control"
                                      style="height: 100px"
                                      x-model="config.propertyValue"></textarea>
                        </td>
                        <td>
                            <a class="btn btn-success" @click="await updateConfig(config)">
                                <i class="bi bi-pencil-square me-2"></i>
                                Save
                            </a>
                        </td>
                    </tr>
                </template>
                </tbody>
            </table>
        </div>
    </div>

    <script>
        async function loadConfigs(name) {
            let params = '';

            if (name) {
                params = '?name=' + name;
            }

            return await axios.get('/api/configs' + params)
                .then(response => response.data)
                .catch(() => []);
        }

        async function updateConfig(config) {
            await axios.put('/api/configs/' + config.uuid, config)
                .then(() => {
                    const toastEl = document.getElementById('successToast');
                    const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastEl)
                    toastBootstrap.show();
                })
                .catch(() => {
                    const toastEl = document.getElementById('errorToast');
                    const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastEl)
                    toastBootstrap.show();
                });
        }
    </script>
</@navigation.display>