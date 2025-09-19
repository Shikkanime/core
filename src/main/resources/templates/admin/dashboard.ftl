<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div class="d-flex align-items-center mb-3 gap-3 flex-wrap">
        <div class="ms-0 me-auto">
            <select class="form-select" id="datemenu">
                <option value="1" selected>Last hour</option>
                <option value="24">Last 24 hours</option>
                <option value="168">Last 7 days</option>
            </select>
        </div>
        <div class="ms-auto d-flex gap-2">
            <select class="form-select" id="activedaysmenu" title="Active days threshold">
                <option value="1">Active users: 1+ day</option>
                <option value="2" selected>Active users: 2+ days</option>
                <option value="3">Active users: 3+ days</option>
                <option value="7">Active users: 7+ days</option>
            </select>

            <select class="form-select" id="daysmenu" title="Analytics date range">
                <option value="7">Last 7 days</option>
                <option value="30" selected>Last 30 days</option>
                <option value="90">Last 90 days</option>
            </select>
        </div>
    </div>

    <div class="row g-2">
        <!-- Line charts group -->
        <!-- CPU time series -->
        <div class="col-12 col-md-3">
            <div class="card p-3">
                <div style="height: 36vh">
                    <canvas id="cpuLoadChart"></canvas>
                </div>
            </div>
        </div>

        <!-- Memory time series -->
        <div class="col-12 col-md-3">
            <div class="card p-3">
                <div style="height: 36vh">
                    <canvas id="memoryUsageChart"></canvas>
                </div>
            </div>
        </div>

        <!-- Simulcasts bar chart -->
        <div class="col-12 col-md-6">
            <div class="card p-3">
                <div class="d-flex align-items-end justify-content-end">
                    <div class="dropdown">
                        <button class="btn btn-light" type="button" id="actionsMenuButton" data-bs-toggle="dropdown" aria-expanded="false">
                            <i class="bi bi-three-dots-vertical"></i>
                        </button>
                        <ul class="dropdown-menu" aria-labelledby="actionsMenuButton">
                            <li>
                                <a class="dropdown-item" href="/admin/simulcasts-invalidate">
                                    Invalid simulcasts
                                </a>
                            </li>
                        </ul>
                    </div>
                </div>

                <div style="height: 32vh">
                    <canvas id="simulcastsBarChart"></canvas>
                </div>
            </div>
        </div>

        <!-- Attachments cumulative line -->
        <div class="col-12 col-md-4">
            <div class="card p-3">
                <div style="height: 32vh">
                    <canvas id="attachmentsChart"></canvas>
                </div>
            </div>
        </div>

        <!-- Returning users by version over time (stacked area) -->
        <div class="col-12 col-md-5">
            <div class="card p-3">
                <div style="height: 32vh">
                    <canvas id="usersByVersionChart"></canvas>
                </div>
            </div>
        </div>

        <!-- Pie charts group -->

        <!-- Versions pie -->
        <div class="col-12 col-md-1">
            <div class="card p-3">
                <div style="height: 32vh">
                    <canvas id="versionsPie"></canvas>
                </div>
            </div>
        </div>

        <!-- Locales pie -->
        <div class="col-12 col-md-1">
            <div class="card p-3">
                <div style="height: 32vh">
                    <canvas id="localesPie"></canvas>
                </div>
            </div>
        </div>

        <!-- Devices pie -->
        <div class="col-12 col-md-1">
            <div class="card p-3">
                <div style="height: 32vh">
                    <canvas id="devicesPie"></canvas>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.7/dist/chart.umd.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/moment@^2"></script>
    <script src="https://cdn.jsdelivr.net/npm/chartjs-adapter-moment@^1"></script>

    <script src="/assets/js/dashboard_chart.js" crossorigin="anonymous"></script>
</@navigation.display>