<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <div x-data="{
        loading: false,
        pageable: {},
        page: 1,
        maxPage: 1,
        pages: [],
        async init() {
            await this.fetchMembers();
            this.pages = this.generatePageNumbers(this.page, this.maxPage);
        },
        async fetchMembers() {
            if (this.loading) {
                return;
            }

            this.loading = true;
            this.pageable = await getMembers(this.page);
            this.loading = false;

            this.maxPage = Math.ceil(this.pageable.total / this.pageable.limit);
        },
        async setPage(newPage) {
            this.page = newPage;
            await this.fetchMembers();
            this.pages = this.generatePageNumbers(this.page, this.maxPage);
        },
        generatePageNumbers(currentPage, maxPage) {
            if (currentPage === 0 || maxPage === 0) {
                return [];
            }

            const delta = 3;
            const range = [];
            for (let i = Math.max(2, currentPage - delta); i <= Math.min(maxPage - 1, currentPage + delta); i++) {
                range.push(i);
            }

            if (currentPage - delta > 2) {
                range.unshift('...');
            }

            if (currentPage + delta < maxPage - 1) {
                range.push('..');
            }

            range.unshift(1);

            if (maxPage !== 1) {
                range.push(maxPage);
            }

            return range;
        }
    }" x-init="init">
        <table class="table table-striped table-bordered">
            <thead>
            <tr>
                <th scope="col">Email</th>
                <th scope="col">Created at</th>
                <th scope="col">Last updated at</th>
                <th scope="col">Last login at</th>
                <th scope="col">Anime followed</th>
                <th scope="col">Episode followed</th>
                <th scope="col">Actions</th>
            </tr>
            </thead>
            <tbody class="table-group-divider">
            <template x-for="member in pageable.data">
                <tr>
                    <td>
                        <img x-show="member.hasProfilePicture" x-bind:src="'/api/v1/attachments?uuid=' + member.uuid + '&type=IMAGE'" class="rounded-circle me-1" width="32" height="32" alt="Profile picture">
                        <span class="me-1 badge" :class="member.isActive ? 'bg-success' : 'bg-danger'" x-text="member.isActive ? 'Active' : 'Inactive'"></span>
                        <span x-text="member.email || 'N/A'"></span>
                    </td>
                    <td x-text="new Date(member.creationDateTime).toLocaleString()"></td>
                    <td x-text="member.lastUpdateDateTime ? new Date(member.lastUpdateDateTime).toLocaleString() : 'N/A'"></td>
                    <td x-text="member.lastLoginDateTime ? new Date(member.lastLoginDateTime).toLocaleString() : 'N/A'"></td>
                    <td x-text="member.followedAnimesCount"></td>
                    <td x-text="member.followedEpisodesCount"></td>
                    <td>
                        <a :href="'/admin/members/' + member.uuid" class="btn btn-warning">
                            <i class="bi bi-pencil-square me-2"></i>
                            Edit
                        </a>
                    </td>
                </tr>
            </template>
            </tbody>
        </table>

        <div class="mt-3">
            <nav aria-label="Page navigation">
                <ul class="pagination justify-content-center">
                    <li class="page-item" :class="{ disabled: page === 1 }">
                        <a class="page-link" @click="setPage(1)">&laquo;</a>
                    </li>
                    <template x-for="i in pages">
                        <li class="page-item" :class="{ active: page === i }">
                            <a class="page-link" @click="setPage(i)" x-text="i"></a>
                        </li>
                    </template>
                    <li class="page-item" :class="{ disabled: page === maxPage || maxPage === 0 }">
                        <a class="page-link" @click="setPage(maxPage)">&raquo;</a>
                    </li>
                </ul>
            </nav>
        </div>
    </div>

    <script>
        async function getMembers(page) {
            let params = new URLSearchParams({
                page: page || 1,
                limit: 14
            });

            try {
                const response = await axios.get(`/api/v1/members?` + params.toString());
                return response.data;
            } catch (error) {
                console.error('Error fetching members:', error);
                return {data: [], total: 0, limit: 14};
            }
        }
    </script>
</@navigation.display>