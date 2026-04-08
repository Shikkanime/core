<#import "_navigation.ftl" as navigation />

<@navigation.display canonicalUrl="${baseUrl}/analytics">
    <div x-data="{
        marketShare: [],
        subCoverage: [],
        genreCoverage: [],
        searchParameters: {
            startYear: <#if startYear?? && startYear?has_content>'${startYear?c?js_string}'<#else>''</#if>,
            endYear: <#if endYear?? && endYear?has_content>'${endYear?c?js_string}'<#else>''</#if>,
        }
    }" x-init="
        $watch('searchParameters', async (value) => {
            window.history.pushState({}, '', '/analytics?startYear=' + value.startYear + '&endYear=' + value.endYear);
            await updateCharts(value.startYear, value.endYear);
        });

        setTimeout(async () => {
            await updateCharts(searchParameters.startYear, searchParameters.endYear);
        }, 100);
    ">
        <div class="container my-3">
            <div class="shikk-element">
                <div class="shikk-element-content">
                    <h4>Statistiques</h4>

                    <div class="row">
                        <div class="col-md-6">
                            <label for="startYear">Année de début</label>
                            <select id="startYear" class="form-control bg-dark border-dark text-white"
                                    x-model="searchParameters.startYear">
                                <#list years as year>
                                    <option value="${year?c}">${year?c}</option>
                                </#list>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label for="endYear">Année de fin</label>
                            <select id="endYear" class="form-control bg-dark border-dark text-white"
                                    x-model="searchParameters.endYear">
                                <#list years as year>
                                    <option value="${year?c}">${year?c}</option>
                                </#list>
                            </select>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="row g-3">
            <div class="col-md-6">
                <div class="shikk-element">
                    <div class="shikk-element-content">
                        <h5>Parts de marché</h5>
                        <p class="text-muted small">Nombre total d'animés par plateforme pour chaque saison
                            sélectionnée.</p>
                        <div style="position: relative; height: 400px;">
                            <canvas id="marketShareChart"></canvas>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-md-6">
                <div class="shikk-element">
                    <div class="shikk-element-content">
                        <h5>Évolution du doublage</h5>
                        <p class="text-muted small">Pourcentage d'animés bénéficiant d'un doublage (VF) par plateforme,
                            permettant de suivre son évolution au fil des saisons.</p>
                        <div style="position: relative; height: 400px;">
                            <canvas id="subCoverageChart"></canvas>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-md-12">
                <div class="shikk-element">
                    <div class="shikk-element-content">
                        <h5>Répartition par genres</h5>
                        <p class="text-muted small">Représentation visuelle de la popularité des genres d'animés au fil
                            des saisons.</p>
                        <div style="position: relative; height: 600px;">
                            <canvas id="genreCoverageChart"></canvas>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="/assets/js/chart.min.js"></script>

    <script>
        const commonOptions = {
            maintainAspectRatio: false,
            responsive: true,
            scales: {
                x: {
                    stacked: true,
                    grid: {display: false}
                },
                y: {
                    stacked: true,
                    beginAtZero: true,
                    grid: {color: '#0c0b09'},
                    border: {display: false},
                    ticks: {display: true}
                }
            },
            plugins: {
                legend: {display: true}
            },
            elements: {
                point: {radius: 0, hitRadius: 10},
                line: {tension: 0.3, borderWidth: 2}
            },
            animation: {duration: 0}
        };
        const platformColors = {
            'ANIM': '#0098ff',
            'CRUN': '#ff640a',
            'NETF': '#e50914',
            'DISN': '#bcefff',
            'PRIM': '#00a8e1'
        };

        const colorPalette = [
            '#4e79a7', '#f28e2c', '#e15759', '#76b7b2', '#59a14f',
            '#edc949', '#af7aa1', '#ff9da7', '#9c755f', '#bab0ab'
        ];

        function hexToRgba(hex, alpha) {
            const r = parseInt(hex.slice(1, 3), 16);
            const g = parseInt(hex.slice(3, 5), 16);
            const b = parseInt(hex.slice(5, 7), 16);
            return 'rgba(' + r + ',' + g + ',' + b + ',' + alpha + ')';
        }

        const marketShareChart = new Chart(document.getElementById('marketShareChart').getContext('2d'), {
            type: 'bar',
            data: {labels: [], datasets: []},
            options: commonOptions
        });

        const subCoverageChart = new Chart(document.getElementById('subCoverageChart').getContext('2d'), {
            type: 'bar',
            data: {labels: [], datasets: []},
            options: {
                ...commonOptions,
                scales: {
                    ...commonOptions.scales,
                    y: {
                        ...commonOptions.scales.y,
                        ticks: {
                            ...commonOptions.scales.y.ticks,
                            callback: value => value + '%'
                        }
                    }
                },
                plugins: {
                    ...commonOptions.plugins,
                    tooltip: {
                        callbacks: {
                            label: context => {
                                let label = context.dataset.label || '';
                                if (label) label += ': ';
                                if (context.parsed.y !== null) label += context.parsed.y.toFixed(2) + '%';
                                return label;
                            }
                        }
                    }
                }
            }
        });

        const genreCoverageChart = new Chart(document.getElementById('genreCoverageChart').getContext('2d'), {
            type: 'radar',
            data: {labels: [], datasets: []},
            options: {
                maintainAspectRatio: false,
                responsive: true,
                scales: {
                    r: {
                        angleLines: {color: '#0c0b09'},
                        grid: {color: '#0c0b09'},
                        pointLabels: {color: '#ffffff'},
                        ticks: {
                            backdropColor: 'transparent',
                            color: '#ffffff',
                            display: false
                        }
                    }
                },
                plugins: {
                    legend: {display: true, position: 'bottom'}
                },
                animation: {duration: 0}
            }
        });

        async function getMarketShare(startYear, endYear) {
            return await fetch('/api/v1/analytics/market-share?startYearParam=' + startYear + '&endYearParam=' + endYear)
                .then(response => response.json())
                .catch(() => []);
        }

        async function getSubCoverage(startYear, endYear) {
            return await fetch('/api/v1/analytics/sub-coverage?startYearParam=' + startYear + '&endYearParam=' + endYear)
                .then(response => response.json())
                .catch(() => []);
        }

        async function getGenreCoverage(startYear, endYear) {
            return await fetch('/api/v1/analytics/genre-coverage?startYearParam=' + startYear + '&endYearParam=' + endYear)
                .then(response => response.json())
                .catch(() => []);
        }

        async function updateCharts(startYear, endYear) {
            const [marketShareData, subCoverageData, genreCoverageData] = await Promise.all([
                getMarketShare(startYear, endYear),
                getSubCoverage(startYear, endYear),
                getGenreCoverage(startYear, endYear)
            ]);

            updateChart(marketShareChart, marketShareData);
            updateChart(subCoverageChart, subCoverageData);
            updateGenreChart(genreCoverageChart, genreCoverageData);
        }

        function updateChart(chart, data) {
            const seasonOrder = {'WINTER': 0, 'SPRING': 1, 'SUMMER': 2, 'AUTUMN': 3};
            const sortedSimulcasts = [...new Set(data.map(d => JSON.stringify(d.simulcast)))]
                .map(s => JSON.parse(s))
                .sort((a, b) => {
                    if (a.year !== b.year) return a.year - b.year;
                    return seasonOrder[a.season] - seasonOrder[b.season];
                });
            const simulcastLabels = sortedSimulcasts.map(s => s.label);
            const platformIds = [...new Set(data.map(d => d.platform.id))].sort();

            chart.data.labels = simulcastLabels;
            chart.data.datasets = platformIds.map(platformId => {
                const name = data.find(d => d.platform.id === platformId).platform.name;

                return {
                    label: name,
                    data: simulcastLabels.map(label => {
                        const d = data.find(pd => pd.simulcast.label === label && pd.platform.id === platformId);
                        return d ? d.value : 0;
                    }),
                    backgroundColor: platformColors[platformId] || '#ffffff',
                    borderRadius: 0,
                };
            });

            chart.update();
        }

        function updateGenreChart(chart, data) {
            const genres = [...new Set(data.map(d => d.genre.name))].sort();
            const seasonOrder = {'WINTER': 0, 'SPRING': 1, 'SUMMER': 2, 'AUTUMN': 3};
            const sortedSimulcasts = [...new Set(data.map(d => JSON.stringify(d.simulcast)))]
                .map(s => JSON.parse(s))
                .sort((a, b) => {
                    if (a.year !== b.year) return a.year - b.year;
                    return seasonOrder[a.season] - seasonOrder[b.season];
                });
            const simulcastLabels = sortedSimulcasts.map(s => s.label);

            chart.data.labels = genres;
            chart.data.datasets = simulcastLabels.map((label, index) => {
                return {
                    label: label,
                    data: genres.map(genreName => {
                        const d = data.find(pd => pd.simulcast.label === label && pd.genre.name === genreName);
                        return d ? d.value : 0;
                    }),
                    backgroundColor: hexToRgba(colorPalette[index % colorPalette.length], 0.2),
                    borderColor: colorPalette[index % colorPalette.length],
                    pointBackgroundColor: colorPalette[index % colorPalette.length],
                    pointBorderColor: '#fff',
                    pointHoverBackgroundColor: '#fff',
                    pointHoverBorderColor: colorPalette[index % colorPalette.length],
                };
            });

            chart.update();
        }
    </script>
</@navigation.display>