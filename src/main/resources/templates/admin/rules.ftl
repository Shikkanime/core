<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div x-data="{
        loading: false,
        data: [],
        platforms: [],
        sortCol: 'platform_name',
        sortDesc: false,
        selected: {
            platform: {}
        },
        async init() {
            await Promise.all([this.fetchRules(), this.fetchPlatforms()]);
        },
        async fetchRules() {
            if (this.loading) {
                return;
            }

            this.loading = true;
            this.data = await getRules();
            this.loading = false;
        },
        async fetchPlatforms() {
            const response = await fetch('/api/v1/platforms');
            this.platforms = await response.json();
        },
        sortBy(col) {
            if (this.sortCol === col) {
                this.sortDesc = !this.sortDesc;
            } else {
                this.sortCol = col;
                this.sortDesc = false;
            }
        },
        get sortedData() {
            return [...this.data].sort((a, b) => {
                let valA;
                let valB;

                switch(this.sortCol) {
                    case 'platform_name':
                        valA = a.platform?.name || '';
                        valB = b.platform?.name || '';
                        break;
                    case 'seriesId':
                        valA = a.seriesId || '';
                        valB = b.seriesId || '';
                        break;
                    case 'seasonId':
                        valA = a.seasonId || '';
                        valB = b.seasonId || '';
                        break;
                    case 'action_name':
                        valA = a.action || '';
                        valB = b.action || '';
                        break;
                    case 'action_value':
                        valA = a.actionValue || '';
                        valB = b.actionValue || '';
                        break;
                    case 'lastUsage':
                        valA = a.lastUsageDateTime || '';
                        valB = b.lastUsageDateTime || '';
                        break;
                    default:
                        valA = '';
                        valB = '';
                }

                if (valA < valB) return this.sortDesc ? 1 : -1;
                if (valA > valB) return this.sortDesc ? -1 : 1;
                return 0;
            });
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
                                <select class="form-select" id="platform" name="platform" x-model="selected.platform.name">
                                    <template x-for="p in platforms" :key="p.id">
                                        <option :value="p.name" x-text="p.name"></option>
                                    </template>
                                </select>
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
                                    <option value="REPLACE_EPISODE_TYPE">Replace episode type</option>
                                    <option value="ADD_TO_NUMBER">Add to number</option>
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
                <th scope="col" style="cursor: pointer;" @click="sortBy('platform_name')">
                    Platform
                    <i class="bi" :class="sortCol === 'platform_name' ? (sortDesc ? 'bi-caret-down-fill' : 'bi-caret-up-fill') : 'bi-caret-down'"></i>
                </th>
                <th scope="col" style="cursor: pointer;" @click="sortBy('seriesId')">
                    Series ID
                    <i class="bi" :class="sortCol === 'seriesId' ? (sortDesc ? 'bi-caret-down-fill' : 'bi-caret-up-fill') : 'bi-caret-down'"></i>
                </th>
                <th scope="col" style="cursor: pointer;" @click="sortBy('seasonId')">
                    Season ID
                    <i class="bi" :class="sortCol === 'seasonId' ? (sortDesc ? 'bi-caret-down-fill' : 'bi-caret-up-fill') : 'bi-caret-down'"></i>
                </th>
                <th scope="col" style="cursor: pointer;" @click="sortBy('action_name')">
                    Action name
                    <i class="bi" :class="sortCol === 'action_name' ? (sortDesc ? 'bi-caret-down-fill' : 'bi-caret-up-fill') : 'bi-caret-down'"></i>
                </th>
                <th scope="col" style="cursor: pointer;" @click="sortBy('action_value')">
                    Action value
                    <i class="bi" :class="sortCol === 'action_value' ? (sortDesc ? 'bi-caret-down-fill' : 'bi-caret-up-fill') : 'bi-caret-down'"></i>
                </th>
                <th scope="col" style="cursor: pointer;" @click="sortBy('lastUsage')">
                    Last usage
                    <i class="bi" :class="sortCol === 'lastUsage' ? (sortDesc ? 'bi-caret-down-fill' : 'bi-caret-up-fill') : 'bi-caret-down'"></i>
                </th>
                <th scope="col">Actions</th>
            </tr>
            </thead>
            <tbody class="table-group-divider">
            <template x-for="rule in sortedData">
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
            const response = await fetch(`/admin/api/rules`);
            return response.json();
        }

        async function createRule(dto) {
            await fetch('/admin/api/rules', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(dto)
            });
        }

        async function deleteRule(dto) {
            await fetch('/admin/api/rules/' + dto.uuid, {
                method: 'DELETE'
            });
        }
    </script>
</@navigation.display>