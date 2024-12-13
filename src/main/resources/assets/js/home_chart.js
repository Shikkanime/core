const datemenuElement = document.getElementById('datemenu');
const cpuChartElement = document.getElementById('cpuLoadChart').getContext('2d');
const memoryChartElement = document.getElementById('memoryUsageChart').getContext('2d');

const chartConfig = (label, unit) => ({
    type: 'line',
    data: { labels: [], datasets: [{ label, data: [], fill: false, borderColor: 'rgba(33,37,41, 1)', tension: 0.1 }] },
    options: {
        maintainAspectRatio: false,
        scales: { x: { type: 'time', time: { unit } }, y: { beginAtZero: true } },
        elements: { point: { radius: 0 } },
        animation: { duration: 0 },
        plugins: { legend: { display: false } }
    }
});

const cpuChart = new Chart(cpuChartElement, chartConfig('% CPU', 'hour'));
const memoryChart = new Chart(memoryChartElement, chartConfig('RAM in MB', 'hour'));

document.addEventListener('DOMContentLoaded', async () => {
    await setChartData();

    datemenuElement.addEventListener('change', async () => {
        await setChartData();
    });
});

async function getMetrics() {
    return (await axios.get('/api/metrics?hours=' + datemenuElement.value)).data;
}

async function setChartData() {
    const data = await getMetrics();

    const updateChart = (chart, dataKey) => {
        if (datemenuElement.value > 24) {
            chart.options.scales.x.time.unit = 'day';
        } else {
            chart.options.scales.x.time.unit = 'hour';
        }

        chart.data.labels = data.map(metric => metric.date);
        chart.data.datasets[0].data = data.map(metric => metric[dataKey]);
        chart.update();
    };

    updateChart(cpuChart, 'cpuLoad');
    updateChart(memoryChart, 'memoryUsage');
}