<#import "_layout.ftl" as layout />
<@layout.main>
    <h5>Database current size: <span id="dbSize"></span></h5>

    <div class="row g-3">
        <div class="col-6" style="height: 50vh">
            <canvas id="cpuLoadChart"></canvas>
        </div>
        <div class="col-6">
            <canvas id="memoryUsageChart"></canvas>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script src="/assets/js/admin/home_chart.js"></script>
</@layout.main>