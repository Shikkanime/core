<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div x-data="{ display: 'CPU' }">
        <div class="d-flex align-items-center mb-3 gap-3 flex-wrap">
            <div class="row g-3 align-items-center">
                <div class="col-auto">
                    <select class="form-select" id="datemenu">
                        <option value="1" selected>Last hour</option>
                        <option value="24">Last 24 hours</option>
                        <option value="168">Last 7 days</option>
                    </select>
                </div>
                <div class="col-auto">
                    <select class="form-select" id="activedaysmenu" title="Active days threshold">
                        <option value="1">Active users: 1+ day</option>
                        <option value="2" selected>Active users: 2+ days</option>
                        <option value="3">Active users: 3+ days</option>
                        <option value="7">Active users: 7+ days</option>
                    </select>
                </div>
                <div class="col-auto">
                    <select class="form-select" id="daysmenu" title="Analytics date range">
                        <option value="7">Last 7 days</option>
                        <option value="30" selected>Last 30 days</option>
                        <option value="90">Last 90 days</option>
                    </select>
                </div>
            </div>
            <div class="ms-auto d-flex gap-2">
                <button class="btn" @click="display = 'CPU'" :class="{ 'btn-dark': display === 'CPU', 'btn-outline-dark': display !== 'CPU' }">CPU</button>
                <button class="btn" @click="display = 'Memory'" :class="{ 'btn-dark': display === 'Memory', 'btn-outline-dark': display !== 'Memory' }">Memory</button>
            </div>
        </div>

        <div class="row g-2">
            <!-- Line charts group -->
            <!-- CPU/Memory time series -->
            <div class="col-12 col-lg-4">
                <div class="card p-3">
                    <div style="height: 28vh">
                        <canvas id="cpuLoadChart" x-show="display === 'CPU'"></canvas>
                        <canvas id="memoryUsageChart" x-show="display === 'Memory'"></canvas>
                    </div>
                </div>
            </div>

            <!-- Attachments cumulative line -->
            <div class="col-12 col-lg-4">
                <div class="card p-3">
                    <div style="height: 28vh">
                        <canvas id="attachmentsChart"></canvas>
                    </div>
                </div>
            </div>

            <!-- Returning users by version over time (stacked area) -->
            <div class="col-12 col-lg-4">
                <div class="card p-3">
                    <div style="height: 28vh">
                        <canvas id="usersByVersionChart"></canvas>
                    </div>
                </div>
            </div>

            <!-- Pie charts group -->
            <!-- Versions pie -->
            <div class="col-12 col-md-4">
                <div class="card p-3">
                    <div style="height: 22vh">
                        <canvas id="versionsPie"></canvas>
                    </div>
                </div>
            </div>

            <!-- Locales pie -->
            <div class="col-12 col-md-4">
                <div class="card p-3">
                    <div style="height: 22vh">
                        <canvas id="localesPie"></canvas>
                    </div>
                </div>
            </div>

            <!-- Devices pie -->
            <div class="col-12 col-md-4">
                <div class="card p-3">
                    <div style="height: 22vh">
                        <canvas id="devicesPie"></canvas>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.7/dist/chart.umd.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/moment@^2"></script>
    <script src="https://cdn.jsdelivr.net/npm/chartjs-adapter-moment@^1"></script>

    <script src="/assets/js/dashboard_chart.js" crossorigin="anonymous"></script>
</@navigation.display>