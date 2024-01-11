<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div class="row g-3 align-items-center mb-3">
        <div class="col-auto">
            <label class="col-form-label" for="hours">Hours</label>
        </div>
        <div class="col-auto">
            <select class="form-select" id="hours">
                <option value="1" selected>1</option>
                <option value="3">3</option>
                <option value="6">6</option>
                <option value="12">12</option>
                <option value="24">24</option>
            </select>
        </div>
    </div>

    <div class="row g-3">
        <div class="col-md-6" style="height: 50vh">
            <canvas id="cpuLoadChart"></canvas>
        </div>
        <div class="col-md-6">
            <canvas id="memoryUsageChart"></canvas>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/chart.js" crossorigin="anonymous"></script>
    <script src="/assets/js/home_chart.js" crossorigin="anonymous"></script>
</@navigation.display>