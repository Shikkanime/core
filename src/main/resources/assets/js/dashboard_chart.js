const datemenuElement = document.getElementById('datemenu');
const cpuChartElement = document.getElementById('cpuLoadChart').getContext('2d');
const memoryChartElement = document.getElementById('memoryUsageChart').getContext('2d');
const loginCountChartElement = document.getElementById('loginCountChart').getContext('2d');

const createChart = (element, label, unit) => new Chart(element, {
    type: 'line',
    data: { labels: [], datasets: [{ label, data: [], borderColor: 'rgba(33,37,41, 1)', tension: 0.1 }] },
    options: {
        maintainAspectRatio: false,
        scales: { x: { type: 'time', time: { unit } }, y: { beginAtZero: true } },
        elements: { point: { radius: 0 } },
        animation: { duration: 0 },
        plugins: { legend: { display: false } }
    }
});

const cpuChart = createChart(cpuChartElement, '% CPU', 'hour');
const memoryChart = createChart(memoryChartElement, 'RAM in MB', 'hour');

const loginCountChart = new Chart(loginCountChartElement, {
    type: 'line',
    data: { labels: [], datasets: [
            { label: 'Login distinct count', data: [], fill: true, borderColor: 'rgb(255,255,0)', backgroundColor: 'rgba(255,255,0,0.5)', tension: 0.1 },
            { label: 'Login distinct count trending', data: [], fill: false, borderColor: 'rgb(255,0,0)', tension: 0.1 },
            { label: 'Login count', data: [], fill: true, borderColor: 'rgb(255,122,0)', backgroundColor: 'rgba(255,122,0,0.5)', tension: 0.1 },
            { label: 'Login count trending', data: [], fill: false, borderColor: 'rgb(0,255,0)', tension: 0.1 }
        ] },
    options: {
        maintainAspectRatio: false,
        scales: { x: { type: 'time', time: { unit: 'day' } }, y: { beginAtZero: true } },
        elements: { point: { radius: 0 } },
        animation: { duration: 0 },
        plugins: { legend: { display: false } }
    }
});

document.addEventListener('DOMContentLoaded', () => setChartData());
datemenuElement.addEventListener('change', () => setChartData());

const getMetrics = async () => (await fetch('/admin/api/metrics?hours=' + datemenuElement.value)).json();
const getLoginCounts = async () => (await fetch('/admin/api/trace-actions/login-counts?days=30')).json();

const setChartData = async () => {
    const metricsData = await getMetrics();
    const updateChart = (chart, dataKey) => {
        chart.options.scales.x.time.unit = datemenuElement.value > 24 ? 'day' : 'hour';
        chart.data.labels = metricsData.map(metric => metric.date);
        chart.data.datasets[0].data = metricsData.map(metric => metric[dataKey]);
        chart.update();
    };

    updateChart(cpuChart, 'avgCpuLoad');
    updateChart(memoryChart, 'avgMemoryUsage');

    const loginCounts = await getLoginCounts();
    loginCountChart.data.labels = loginCounts.map(loginCount => loginCount.date);
    loginCountChart.data.datasets[0].data = loginCounts.map(loginCount => loginCount.distinctCount);
    loginCountChart.data.datasets[1].data = loginCounts.map((_, i) => {
        const sum = loginCounts.slice(Math.max(0, i - 6), i + 1).reduce((acc, loginCount) => acc + loginCount.distinctCount, 0);
        return sum / Math.min(i + 1, 7);
    });
    loginCountChart.data.datasets[2].data = loginCounts.map(loginCount => loginCount.count);
    loginCountChart.data.datasets[3].data = loginCounts.map((_, i) => {
        const sum = loginCounts.slice(Math.max(0, i - 6), i + 1).reduce((acc, loginCount) => acc + loginCount.count, 0);
        return sum / Math.min(i + 1, 7);
    });
    loginCountChart.update();
};