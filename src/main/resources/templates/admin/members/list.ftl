<#import "../_navigation.ftl" as navigation />
<#import "../macros/pagination.ftl" as ui>

<@navigation.display>
    <div x-data="{
        loading: false,
        pageable: {},
        page: 1,
        maxPage: 1,
        limit: 15,
        async init() {
            await this.fetchMembers();
        },
        async fetchMembers() {
            if (this.loading) {
                return;
            }

            this.loading = true;
            this.pageable = await getMembers(this.page, this.limit);
            this.loading = false;

            this.maxPage = Math.ceil(this.pageable.total / this.pageable.limit);
        },
        async setPage(newPage) {
            this.page = newPage;
            await this.fetchMembers();
        }
    }" x-init="init">
        <div class="row g-3 align-items-center mb-3">
            <@ui.pageSizeSelector />
            <@ui.alpinePagination />
        </div>
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
                        <template x-if="member.hasProfilePicture">
                            <img x-bind:src="'/api/v1/attachments?uuid=' + member.uuid + '&type=MEMBER_PROFILE&v=' + new Date(member.attachmentLastUpdateDateTime).getTime()" class="rounded-circle me-1" width="32" height="32" alt="Profile picture">
                        </template>

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
    </div>

    <script>
        async function getMembers(page, limit) {
            let params = new URLSearchParams({
                page: page || 1,
                limit: limit
            });

            try {
                const response = await fetch(`/admin/api/members?` + params.toString());
                return response.json();
            } catch (error) {
                console.error('Error fetching members:', error);
                return {data: [], total: 0, limit: 14};
            }
        }
    </script>
</@navigation.display>