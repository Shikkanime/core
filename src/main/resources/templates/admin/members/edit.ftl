<#import "../_navigation.ftl" as navigation />

<@navigation.display>
    <div x-data="{titleSize: 2}">
        <div class="d-flex mb-3">
            <a href="/admin/members" class="btn btn-secondary ms-0">Back</a>
        </div>

        <div class="card">
            <div class="card-body">
                <div class="row g-3">
                    <div class="col-md-4">
                        <div style="height: 50vh;">
                            <canvas id="loginActivitiesChart"></canvas>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div style="height: 50vh;">
                            <canvas id="followAnimeActivitiesChart"></canvas>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div style="height: 50vh;">
                            <canvas id="followEpisodeActivitiesChart"></canvas>
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

        async function getMemberLoginActivities() {
            return await axios.get('/api/v1/members/' + getUuid() + '/login-activities')
                .then(response => response.data)
                .catch(() => []);
        }

        async function getMemberFollowAnimeActivities() {
            return await axios.get('/api/v1/members/' + getUuid() + '/follow-anime-activities')
                .then(response => response.data)
                .catch(() => []);
        }

        async function getMemberFollowEpisodeActivities() {
            return await axios.get('/api/v1/members/' + getUuid() + '/follow-episode-activities')
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

            const loginActivities = await getMemberLoginActivities();

            Object.keys(loginActivities).forEach(key => {
                loginActivitiesChart.data.labels.push(key);
                loginActivitiesChart.data.datasets[0].data.push(loginActivities[key].length);
            });

            loginActivitiesChart.update();

            const followAnimeActivities = await getMemberFollowAnimeActivities();

            Object.keys(followAnimeActivities).forEach(key => {
                followAnimeActivitiesChart.data.labels.push(key);
                followAnimeActivitiesChart.data.datasets[0].data.push(followAnimeActivities[key].length);
            });

            followAnimeActivitiesChart.update();

            const followEpisodeActivities = await getMemberFollowEpisodeActivities();

            Object.keys(followEpisodeActivities).forEach(key => {
                followEpisodeActivitiesChart.data.labels.push(key);
                followEpisodeActivitiesChart.data.datasets[0].data.push(followEpisodeActivities[key].length);
            });

            followEpisodeActivitiesChart.update();
        }

        document.addEventListener('DOMContentLoaded', setChartData);
    </script>
</@navigation.display>