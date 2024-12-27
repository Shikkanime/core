<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div x-data="{
        loading: false,
        data: [],
        selected: {
            platform: {}
        },
        async init() {
            await this.fetchRules();
        },
        async fetchRules() {
            if (this.loading) {
                return;
            }

            this.loading = true;
            this.data = await getRules();
            this.loading = false;
        }
    }" x-init="init">
        <div class="modal fade" id="createModal" tabindex="-1" aria-labelledby="createModalLabel"
             aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h1 class="modal-title fs-5" id="createModalLabel">Create</h1>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row g-3">
                            <div class="col-md-6">
                                <label for="platform" class="form-label">Platform</label>
                                <input type="text" class="form-control" id="platform" name="platform"
                                       x-model="selected.platform.name">
                            </div>
                            <div class="col-md-6">
                                <label for="seriesId" class="form-label">Series ID</label>
                                <input type="text" class="form-control" id="seriesId" name="seriesId"
                                       x-model="selected.seriesId">
                            </div>
                            <div class="col-md-6">
                                <label for="seasonId" class="form-label">Season ID</label>
                                <input type="text" class="form-control" id="seasonId" name="seasonId"
                                       x-model="selected.seasonId">
                            </div>
                            <div class="col-md-6">
                                <label for="action" class="form-label">Action</label>
                                <select class="form-select" id="action" name="action" x-model="selected.action">
                                    <option value="REPLACE_ANIME_NAME">Replace anime name</option>
                                    <option value="REPLACE_SEASON_NUMBER">Replace season number</option>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label for="actionValue" class="form-label">Action value</label>
                                <input type="text" class="form-control" id="actionValue" name="actionValue"
                                       x-model="selected.actionValue">
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>

                        <button class="btn btn-success"
                                @click="loading = true; await createRule(selected); window.location.reload(true)">
                            Confirm
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <div class="modal fade" id="deleteModal" tabindex="-1" aria-labelledby="deleteModalLabel"
             aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h1 class="modal-title fs-5" id="deleteModalLabel">Delete (<strong x-text="selected.uuid"></strong>)</h1>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        Are you sure you want to delete this rule?
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>

                        <button class="btn btn-danger"
                                @click="loading = true; await deleteRule(selected); window.location.reload(true)">
                            Confirm
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <div class="d-flex mb-3">
            <div class="spinner-border ms-0 me-auto" role="status" x-show="loading">
                <span class="visually-hidden">Loading...</span>
            </div>

            <button type="submit" class="btn btn-success ms-auto me-0" :disabled="loading" data-bs-toggle="modal"
                    data-bs-target="#createModal" @click="selected = {platform: {}}">
                <i class="bi bi-plus-circle me-2"></i>
                Create
            </button>
        </div>

        <table class="table table-striped table-bordered">
            <thead>
            <tr>
                <th scope="col">Platform</th>
                <th scope="col">Series ID</th>
                <th scope="col">Season ID</th>
                <th scope="col">Action name</th>
                <th scope="col">Action value</th>
                <th scope="col">Last usage</th>
                <th scope="col">Actions</th>
            </tr>
            </thead>
            <tbody class="table-group-divider">
            <template x-for="rule in data">
                <tr>
                    <td x-text="rule.platform.name"></td>
                    <td x-text="rule.seriesId"></td>
                    <td x-text="rule.seasonId"></td>
                    <td x-text="rule.action"></td>
                    <td x-text="rule.actionValue"></td>
                    <td x-text="rule.lastUsageDateTime ? new Date(rule.lastUsageDateTime).toLocaleString() : 'Never'"></td>
                    <td>
                        <button type="button" class="btn btn-danger" data-bs-toggle="modal"
                                data-bs-target="#deleteModal" @click="selected = rule">
                            <i class="bi bi-trash me-2"></i>
                            Delete
                        </button>
                    </td>
                </tr>
            </template>
            </tbody>
        </table>
    </div>

    <script>
        async function getRules() {
            const response = await axios.get(`/api/rules`);
            return response.data;
        }

        async function createRule(dto) {
            await axios.post('/api/rules', dto);
        }

        async function deleteRule(dto) {
            await axios.delete('/api/rules/' + dto.uuid);
        }
    </script>
</@navigation.display>