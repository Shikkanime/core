<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div class="row g-3">
        <div class="col-md-6" style="height: 50vh">
            <canvas id="cpuLoadChart"></canvas>
        </div>
        <div class="col-md-6">
            <canvas id="memoryUsageChart"></canvas>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/chart.js" crossorigin="anonymous"></script>
    <script src="/assets/js/admin/home_chart.js" crossorigin="anonymous"></script>
</@navigation.display>