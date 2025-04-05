<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <div x-data="{
        member: {},
        animes: {},
        episodes: {},
        titleSize: 2
    }" x-init="member = await loadMember(); animes = await getAnimes(); episodes = await getEpisodes()">
        <div class="d-flex mb-3">
            <a href="/admin/members" class="btn btn-secondary ms-0">Back</a>
        </div>

        <div class="card">
            <div class="card-body">
                <div class="d-flex mb-3">
                    <template x-if="member.hasProfilePicture">
                        <img x-bind:src="'/api/v1/attachments?uuid=' + member.uuid + '&type=MEMBER_PROFILE&v=' + new Date(member.lastUpdateDateTime).getTime()" class="rounded me-2" width="64" height="64" alt="Profile picture">
                    </template>
                    <div class="row row-cols-2">
                        <div class="col-auto">
                            <p class="my-0">Email: <span class="fw-bold" x-text="member.email || 'N/A'"></span></p>
                            <p class="text-muted my-0">Created at: <span x-text="new Date(member.creationDateTime).toLocaleString()"></span></p>
                            <p class="text-muted my-0">Last updated at: <span x-text="member.lastUpdateDateTime ? new Date(member.lastUpdateDateTime).toLocaleString() : 'N/A'"></span></p>
                        </div>
                        <div class="col-auto">
                            <p class="text-muted my-0">Last login at: <span x-text="member.lastLoginDateTime ? new Date(member.lastLoginDateTime).toLocaleString() : 'N/A'"></span></p>
                            <p class="text-muted my-0">App version: <span x-text="member.additionalData?.appVersion || 'N/A'"></span></p>
                            <p class="text-muted my-0">Device: <span x-text="member.additionalData?.device || 'N/A'"></span></p>
                            <p class="text-muted my-0">Locale: <span x-text="member.additionalData?.locale || 'N/A'"></span></p>
                        </div>
                    </div>
                </div>

                <div class="row g-3">
                    <div class="col-md-4">
                        <div style="height: 50vh;">
                            <canvas id="loginActivitiesChart"></canvas>
                        </div>
                    </div>
                    <div class="col-md-8">
                        <div class="row">
                            <div class="col-md-12">
                                <div style="height: 25vh;">
                                    <canvas id="followAnimeActivitiesChart"></canvas>
                                </div>
                            </div>
                            <div class="col-md-12">
                                <div style="height: 25vh;">
                                    <canvas id="followEpisodeActivitiesChart"></canvas>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="col-md-4">
                        <div class="row">
                            <template x-for="anime in animes.data">
                                <div class="col-6 col-md-3 mb-3">
                                    <div class="d-flex flex-column">
                                        <img x-bind:src="'/api/v1/attachments?uuid=' + anime.uuid + '&type=THUMBNAIL&v=' + new Date(anime.lastUpdateDateTime).getTime()" class="img-fluid rounded me-2">
                                        <p class="my-0 text-truncate"><span class="fw-bold" x-text="anime.shortName"></span></p>
                                    </div>
                                </div>
                            </template>
                        </div>
                    </div>

                    <div class="col-md-8">
                        <div class="row">
                            <template x-for="episode in episodes.data">
                                <div class="col-6 col-md-2 mb-3">
                                    <div class="d-flex flex-column">
                                        <img x-bind:src="'/api/v1/attachments?uuid=' + episode.uuid + '&type=BANNER&v=' + new Date(episode.lastUpdateDateTime).getTime()" class="img-fluid rounded me-2">
                                        <p class="my-0 text-truncate"><span class="fw-bold" x-text="episode.anime.shortName"></span></p>
                                        <p class="text-muted my-0 text-truncate">S<span x-text="episode.season"></span> - <span x-text="episode.episodeType.substring(0, 2)"></span> <span x-text="episode.number"></span></p>
                                    </div>
                                </div>
                            </template>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/chart.js" crossorigin="anonymous"></script>

    <script>
        function getUuid() {
            const url = new URL(window.location.href);
            return url.pathname.split('/').pop();
        }

        async function loadMember() {
            const uuid = getUuid();

            return await axios.get('/admin/api/members/' + uuid)
                .then(response => response.data);
        }

        async function getMemberLoginActivities() {
            return await axios.get('/admin/api/members/' + getUuid() + '/login-activities')
                .then(response => response.data)
                .catch(() => []);
        }

        async function getMemberFollowAnimeActivities() {
            return await axios.get('/admin/api/members/' + getUuid() + '/follow-anime-activities')
                .then(response => response.data)
                .catch(() => []);
        }

        async function getMemberFollowEpisodeActivities() {
            return await axios.get('/admin/api/members/' + getUuid() + '/follow-episode-activities')
                .then(response => response.data)
                .catch(() => []);
        }

        async function getAnimes() {
            return await axios.get('/admin/api/members/' + getUuid() + '/animes?limit=12')
                .then(response => response.data)
                .catch(() => []);
        }

        async function getEpisodes() {
            return await axios.get('/admin/api/members/' + getUuid() + '/episode-mappings?limit=24')
                .then(response => response.data)
                .catch(() => []);
        }

        async function setChartData()
        {
            const loginActivitiesChart = new Chart(document.getElementById('loginActivitiesChart').getContext('2d'), {
                type: 'bar',
                data: {
                    labels: [],
                    datasets: [
                        {
                            label: 'Login activities',
                            data: [],
                            backgroundColor: 'rgba(33,37,41, 1)',
                        }
                    ]
                },
                options: {
                    maintainAspectRatio: false,
                    scales: {
                        y: {
                            beginAtZero: true
                        }
                    },
                    elements: {
                        point: {
                            radius: 0
                        }
                    },
                    animation: {
                        duration: 0
                    },
                    plugins: {
                        legend: {
                            display: true
                        }
                    }
                }
            });
            const followAnimeActivitiesChart = new Chart(document.getElementById('followAnimeActivitiesChart').getContext('2d'), {
                type: 'line',
                data: {
                    labels: [],
                    datasets: [
                        {
                            label: 'Follow anime activities',
                            data: [],
                            fill: false,
                            borderColor: ['rgba(33,37,41, 1)'],
                            tension: 0.1
                        }
                    ]
                },
                options: {
                    maintainAspectRatio: false,
                    elements: {
                        point: {
                            radius: 0
                        }
                    },
                    animation: {
                        duration: 0
                    },
                    plugins: {
                        legend: {
                            display: true
                        }
                    }
                }
            });
            const followEpisodeActivitiesChart = new Chart(document.getElementById('followEpisodeActivitiesChart').getContext('2d'), {
                type: 'line',
                data: {
                    labels: [],
                    datasets: [
                        {
                            label: 'Follow episode activities',
                            data: [],
                            fill: false,
                            borderColor: ['rgba(33,37,41, 1)'],
                            tension: 0.1
                        }
                    ]
                },
                options: {
                    maintainAspectRatio: false,
                    elements: {
                        point: {
                            radius: 0
                        }
                    },
                    animation: {
                        duration: 0
                    },
                    plugins: {
                        legend: {
                            display: true
                        }
                    }
                }
            });

            const loginActivities = await getMemberLoginActivities();

            Object.keys(loginActivities).forEach(key => {
                loginActivitiesChart.data.labels.push(key);
                loginActivitiesChart.data.datasets[0].data.push(loginActivities[key].length);
            });

            loginActivitiesChart.update();

            const followAnimeActivities = await getMemberFollowAnimeActivities();
            const animeKeys = Object.keys(followAnimeActivities.activities);
            let followAnimesSum = followAnimeActivities.total - followAnimeActivities.activities[animeKeys[animeKeys.length - 1]].length;

            animeKeys.forEach(key => {
                const activity = followAnimeActivities.activities[key];
                followAnimesSum += activity.length;

                followAnimeActivitiesChart.data.labels.push(key);
                followAnimeActivitiesChart.data.datasets[0].data.push(followAnimesSum);
            });

            followAnimeActivitiesChart.update();

            const followEpisodeActivities = await getMemberFollowEpisodeActivities();
            const episodeKeys = Object.keys(followEpisodeActivities.activities);
            let followEpisodesSum = followEpisodeActivities.total - followEpisodeActivities.activities[episodeKeys[episodeKeys.length - 1]].length;

            episodeKeys.forEach(key => {
                const activity = followEpisodeActivities.activities[key];
                followEpisodesSum += activity.length;

                followEpisodeActivitiesChart.data.labels.push(key);
                followEpisodeActivitiesChart.data.datasets[0].data.push(followEpisodesSum);
            });

            followEpisodeActivitiesChart.update();
        }

        document.addEventListener('DOMContentLoaded', setChartData);
    </script>
</@navigation.display>