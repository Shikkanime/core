<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div class="row g-2">
        <div class="col-12 col-md-6">
            <div class="card p-3">
                <h5 class="card-title">JFR Reports</h5>
                <p class="text-muted">Deep analysis reports that can be opened in IntelliJ IDEA or Java Mission Control.</p>
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                        <tr>
                            <th>Filename</th>
                            <th>Size</th>
                            <th>Last Modified</th>
                            <th>Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        <#list jfrFiles as file>
                            <tr>
                                <td>${file.name}</td>
                                <td>${(file.length() / 1024 / 1024)?string("0.##")} MB</td>
                                <td>${file.lastModified()?number_to_datetime}</td>
                                <td>
                                    <a href="/admin/profiling/download/${file.name}" class="btn btn-sm btn-primary">
                                        <i class="bi bi-download"></i> Download
                                    </a>
                                </td>
                            </tr>
                        <#else>
                            <tr>
                                <td colspan="4" class="text-center">No JFR reports found yet.</td>
                            </tr>
                        </#list>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <div class="col-12 col-md-6">
            <div class="card p-3">
                <h5 class="card-title">Route Performance (Last Hour)</h5>
                <div style="height: 40vh">
                    <canvas id="routePerformanceChart"></canvas>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.7/dist/chart.umd.min.js"></script>
    <script>
        async function loadRouteMetrics() {
            const response = await fetch('/api/v1/profiling/routes');
            const data = await response.json();
            
            const ctx = document.getElementById('routePerformanceChart').getContext('2d');
            new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: data.map(m => m.method + ' ' + m.path),
                    datasets: [{
                        label: 'Avg Duration (ms)',
                        data: data.map(m => m.avgDuration),
                        backgroundColor: 'rgba(54, 162, 235, 0.5)',
                        borderColor: 'rgb(54, 162, 235)',
                        borderWidth: 1
                    }]
                },
                options: {
                    indexAxis: 'y',
                    responsive: true,
                    maintainAspectRatio: false,
                    scales: {
                        x: {
                            beginAtZero: true,
                            title: { display: true, text: 'Milliseconds' }
                        }
                    }
                }
            });
        }
        
        loadRouteMetrics();
    </script>
</@navigation.display>