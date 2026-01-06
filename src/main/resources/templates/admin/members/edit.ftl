<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <div x-data="{
        member: {},
        animes: {},
        episodes: {},
        titleSize: 2
    }" x-init="member = await loadMember(); animes = await getAnimes(); episodes = await getEpisodes()" class="d-flex flex-column dashboard-wrapper pb-3">

        <!-- Header: Profile Info -->
        <div class="card border-0 shadow-sm mb-3 flex-shrink-0">
            <div class="card-body py-3 px-4">
                <div class="d-flex align-items-md-center gap-4 flex-column flex-md-row">
                    <!-- Actions -->
                    <div class="flex-shrink-0">
                        <a class="btn btn-secondary ms-0" @click="window.history.back()">Back</a>
                    </div>

                    <!-- Avatar -->
                    <div class="flex-shrink-0">
                        <template x-if="member.hasProfilePicture">
                            <img x-bind:src="'/api/v1/attachments?uuid=' + member.uuid + '&type=MEMBER_PROFILE&v=' + new Date(member.attachmentLastUpdateDateTime).getTime()"
                                 class="rounded-circle shadow-sm" width="64" height="64" style="object-fit: cover;" alt="Profile">
                        </template>
                        <template x-if="!member.hasProfilePicture">
                            <div class="rounded-circle bg-light d-flex align-items-center justify-content-center shadow-sm" style="width: 64px; height: 64px;">
                                <span class="text-secondary fs-4">👤</span>
                            </div>
                        </template>
                    </div>

                    <!-- Info -->
                    <div class="flex-grow-1 overflow-hidden">
                        <div class="d-flex align-items-baseline gap-3 mb-1">
                            <h4 class="mb-0 text-truncate" x-text="member.email || 'N/A'"></h4>
                        </div>
                        <div class="d-flex flex-wrap gap-3 text-muted small">
                            <span title="Member Since"><i class="bi bi-calendar3 me-1"></i> <span
                                        x-text="new Date(member.creationDateTime).toLocaleDateString()"></span></span>
                            <span title="Last Login"><i class="bi bi-clock-history me-1"></i> <span
                                        x-text="member.lastLoginDateTime ? new Date(member.lastLoginDateTime).toLocaleDateString() : '-'"></span></span>
                            <span title="Device"><i class="bi bi-phone me-1"></i> <span x-text="member.additionalData?.device || '-'"></span> (<span
                                        x-text="member.additionalData?.appVersion || '-'"></span>)</span>
                            <span title="Locale"><i class="bi bi-translate me-1"></i> <span x-text="member.additionalData?.locale || '-'"></span></span>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Dashboard Grid -->
        <div class="row g-3 dashboard-row">

            <!-- Left: Charts -->
            <div class="col-12 col-xl-4 d-flex flex-column dashboard-col">
                <div class="card border-0 shadow-sm dashboard-card-full overflow-hidden">
                    <div class="card-header bg-white py-2">
                        <h6 class="card-title mb-0 fw-bold">Activity Overview</h6>
                    </div>
                    <div class="card-body overflow-auto custom-scrollbar p-3">
                        <div class="mb-4">
                            <small class="text-uppercase text-muted fw-bold d-block mb-2">Logins</small>
                            <div style="height: 200px">
                                <canvas id="loginActivitiesChart"></canvas>
                            </div>
                        </div>
                        <div class="mb-4">
                            <small class="text-uppercase text-muted fw-bold d-block mb-2">Anime Interaction</small>
                            <div style="height: 200px">
                                <canvas id="followAnimeActivitiesChart"></canvas>
                            </div>
                        </div>
                        <div>
                            <small class="text-uppercase text-muted fw-bold d-block mb-2">Episode Interaction</small>
                            <div style="height: 200px">
                                <canvas id="followEpisodeActivitiesChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Right: Lists -->
            <div class="col-12 col-xl-8 d-flex flex-column gap-3">

                <!-- Followed Animes -->
                <div class="card border-0 shadow-sm flex-fill overflow-hidden" style="height: 30vh">
                    <div class="card-header bg-white py-2 d-flex justify-content-between align-items-center">
                        <h6 class="card-title mb-0 fw-bold">Followed Animes</h6>
                        <span class="badge bg-light text-dark border" x-text="animes.total"></span>
                    </div>
                    <div class="card-body overflow-auto custom-scrollbar p-3">
                        <div class="row row-cols-2 row-cols-md-4 row-cols-lg-5 row-cols-xl-6 g-3">
                            <template x-for="anime in animes.data">
                                <div class="col">
                                    <div class="card h-100 border-0" :title="anime.shortName">
                                        <div class="position-relative">
                                            <img x-bind:src="'/api/v1/attachments?uuid=' + anime.uuid + '&type=THUMBNAIL&v=' + new Date(anime.lastUpdateDateTime).getTime()"
                                                 class="rounded w-100 shadow-sm" style="aspect-ratio: 2/3; object-fit: cover;">
                                        </div>
                                        <div class="mt-1">
                                            <p class="small fw-bold text-truncate mb-0" x-text="anime.shortName"></p>
                                        </div>
                                    </div>
                                </div>
                            </template>
                            <template x-if="!animes.data || animes.total === 0">
                                <div class="col-12 text-center py-4 text-muted small">
                                    No anime followed.
                                </div>
                            </template>
                        </div>
                    </div>
                </div>

                <!-- Watched Episodes -->
                <div class="card border-0 shadow-sm flex-fill overflow-hidden" style="height: 40vh">
                    <div class="card-header bg-white py-2 d-flex justify-content-between align-items-center">
                        <h6 class="card-title mb-0 fw-bold">Recently Watched</h6>
                        <span class="badge bg-light text-dark border" x-text="episodes.total"></span>
                    </div>
                    <div class="card-body overflow-auto custom-scrollbar p-3">
                        <div class="row row-cols-1 row-cols-sm-2 row-cols-md-3 row-cols-xl-4 g-3">
                            <template x-for="episode in episodes.data">
                                <div class="col">
                                    <div class="card h-100 border-0">
                                        <div class="position-relative">
                                            <img x-bind:src="'/api/v1/attachments?uuid=' + episode.uuid + '&type=BANNER&v=' + new Date(episode.lastUpdateDateTime).getTime()"
                                                 class="rounded w-100 shadow-sm" style="aspect-ratio: 16/9; object-fit: cover;">
                                            <div class="position-absolute bottom-0 start-0 w-100 p-1 px-2 text-white bg-gradient-dark"
                                                 style="background: linear-gradient(to top, rgba(0,0,0,0.7), transparent);">
                                                <p class="small fw-bold text-truncate mb-0" x-text="episode.anime.shortName"></p>
                                            </div>
                                        </div>
                                        <div class="d-flex justify-content-between align-items-center mt-1 px-1">
                                            <span class="badge bg-secondary bg-opacity-10 text-secondary border border-secondary border-opacity-10 rounded-pill px-2">S<span
                                                        x-text="episode.season"></span> E<span x-text="episode.number"></span></span>
                                            <small class="text-muted extra-small" x-text="new Date(episode.lastUpdateDateTime).toLocaleDateString()"></small>
                                        </div>
                                    </div>
                                </div>
                            </template>
                            <template x-if="!episodes.data || episodes.total === 0">
                                <div class="col-12 text-center py-4 text-muted small">
                                    No episodes watched.
                                </div>
                            </template>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <style>
            .custom-scrollbar::-webkit-scrollbar {
                width: 6px;
                height: 6px;
            }

            .custom-scrollbar::-webkit-scrollbar-track {
                background: transparent;
            }

            .custom-scrollbar::-webkit-scrollbar-thumb {
                background-color: rgba(0, 0, 0, 0.1);
                border-radius: 3px;
            }

            .custom-scrollbar::-webkit-scrollbar-thumb:hover {
                background-color: rgba(0, 0, 0, 0.2);
            }

            .extra-small {
                font-size: 0.75rem;
            }
        </style>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/chart.js" crossorigin="anonymous"></script>

    <script>
        function getUuid() {
            const url = new URL(window.location.href);
            return url.pathname.split('/').pop();
        }

        async function loadMember() {
            const uuid = getUuid();

            return await fetch('/admin/api/members/' + uuid)
                .then(response => response.json());
        }

        async function getMemberLoginActivities() {
            return await fetch('/admin/api/members/' + getUuid() + '/login-activities')
                .then(response => response.json())
                .catch(() => []);
        }

        async function getMemberFollowAnimeActivities() {
            return await fetch('/admin/api/members/' + getUuid() + '/follow-anime-activities')
                .then(response => response.json())
                .catch(() => []);
        }

        async function getMemberFollowEpisodeActivities() {
            return await fetch('/admin/api/members/' + getUuid() + '/follow-episode-activities')
                .then(response => response.json())
                .catch(() => []);
        }

        async function getAnimes() {
            return await fetch('/admin/api/members/' + getUuid() + '/animes?limit=12')
                .then(response => response.json())
                .catch(() => []);
        }

        async function getEpisodes() {
            return await fetch('/admin/api/members/' + getUuid() + '/episode-mappings?limit=8')
                .then(response => response.json())
                .catch(() => []);
        }

        function setChartData()
        {
            const commonOptions = {
                maintainAspectRatio: false,
                responsive: true,
                scales: {
                    x: {
                        grid: {display: false}
                    },
                    y: {
                        beginAtZero: true,
                        grid: {color: '#f3f4f6'},
                        border: {display: false},
                        ticks: {display: true}
                    }
                },
                plugins: {
                    legend: {display: false}
                },
                elements: {
                    point: {radius: 0, hitRadius: 10},
                    line: {tension: 0.3, borderWidth: 2}
                },
                animation: {duration: 0}
            };
            const loginActivitiesChart = new Chart(document.getElementById('loginActivitiesChart').getContext('2d'), {
                type: 'bar',
                data: {
                    labels: [],
                    datasets: [
                        {
                            label: 'Login activities',
                            data: [],
                            backgroundColor: '#3b82f6', // Blue 500
                            borderRadius: 4,
                            barPercentage: 0.6
                        }
                    ]
                },
                options: commonOptions
            });
            const followAnimeActivitiesChart = new Chart(document.getElementById('followAnimeActivitiesChart').getContext('2d'), {
                type: 'line',
                data: {
                    labels: [],
                    datasets: [
                        {
                            label: 'Follow anime activities',
                            data: [],
                            fill: true,
                            backgroundColor: 'rgba(139, 92, 246, 0.1)', // Violet 500 alpha 0.1
                            borderColor: '#8b5cf6' // Violet 500
                        }
                    ]
                },
                options: commonOptions
            });
            const followEpisodeActivitiesChart = new Chart(document.getElementById('followEpisodeActivitiesChart').getContext('2d'), {
                type: 'line',
                data: {
                    labels: [],
                    datasets: [
                        {
                            label: 'Follow episode activities',
                            data: [],
                            fill: true,
                            backgroundColor: 'rgba(16, 185, 129, 0.1)', // Emerald 500 alpha 0.1
                            borderColor: '#10b981' // Emerald 500
                        }
                    ]
                },
                options: commonOptions
            });

            getMemberLoginActivities().then(data =>
            {
                loginActivitiesChart.data.labels = data.map(a => a.key);
                loginActivitiesChart.data.datasets[0].data = data.map(a => a.count);
                loginActivitiesChart.update();
            });

            getMemberFollowAnimeActivities().then(data =>
            {
                followAnimeActivitiesChart.data.labels = data.map(a => a.key);
                followAnimeActivitiesChart.data.datasets[0].data = data.map(a => a.count);
                followAnimeActivitiesChart.update();
            });

            getMemberFollowEpisodeActivities().then(data =>
            {
                followEpisodeActivitiesChart.data.labels = data.map(a => a.key);
                followEpisodeActivitiesChart.data.datasets[0].data = data.map(a => a.count);
                followEpisodeActivitiesChart.update();
            });
        }

        document.addEventListener('DOMContentLoaded', setChartData);
    </script>
</@navigation.display>