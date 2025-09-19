const datemenuElement = document.getElementById('datemenu');
const activedaysmenuElement = document.getElementById('activedaysmenu');
const daysmenuElement = document.getElementById('daysmenu');
const cpuChartElement = document.getElementById('cpuLoadChart').getContext('2d');
const memoryChartElement = document.getElementById('memoryUsageChart').getContext('2d');
const attachmentsChartElement = document.getElementById('attachmentsChart').getContext('2d');
const versionsPieElement = document.getElementById('versionsPie').getContext('2d');
const localesPieElement = document.getElementById('localesPie').getContext('2d');
const devicesPieElement = document.getElementById('devicesPie').getContext('2d');
const usersByVersionChartElement = document.getElementById('usersByVersionChart').getContext('2d');
const simulcastsBarChartElement = document.getElementById('simulcastsBarChart') ? document.getElementById('simulcastsBarChart').getContext('2d') : null;

const colorPalette = [
    '#4e79a7','#f28e2c','#e15759','#76b7b2','#59a14f','#edc949','#af7aa1','#ff9da7','#9c755f','#bab0ab',
    '#1f77b4','#ff7f0e','#2ca02c','#d62728','#9467bd','#8c564b','#e377c2','#7f7f7f','#bcbd22','#17becf'
];

const createLineChart = (element, label, unit) => new Chart(element, {
    type: 'line',
    data: { labels: [], datasets: [{ label, data: [], borderColor: 'rgba(33,37,41, 1)', backgroundColor: 'rgba(33,37,41,0.1)', tension: 0.1, fill: true }] },
    options: {
        maintainAspectRatio: false,
        scales: { x: { type: 'time', time: { unit } }, y: { beginAtZero: true } },
        elements: { point: { radius: 0 } },
        animation: { duration: 0 },
        plugins: { legend: { display: false } }
    }
});

const createBarChart = (element, label) => new Chart(element, {
    type: 'bar',
    data: { labels: [], datasets: [{ label, data: [], backgroundColor: 'rgba(33,37,41,0.6)' }] },
    options: {
        maintainAspectRatio: false,
        scales: { x: { type: 'time', time: { unit: 'day' } }, y: { beginAtZero: true } },
        animation: { duration: 0 },
        plugins: { legend: { display: false } }
    }
});

const createPieChart = (element, label) => new Chart(element, {
    type: 'doughnut',
    data: { labels: [], datasets: [{ label, data: [], backgroundColor: colorPalette }] },
    options: {
        maintainAspectRatio: false,
        plugins: { legend: { position: 'bottom' } }
    }
});

const cpuChart = createLineChart(cpuChartElement, '% CPU', 'hour');
const memoryChart = createLineChart(memoryChartElement, 'RAM in MB', 'hour');
const attachmentsChart = createLineChart(attachmentsChartElement, 'Attachments', 'day');
const versionsPie = createPieChart(versionsPieElement, 'Versions');
const localesPie = createPieChart(localesPieElement, 'Locales');
const devicesPie = createPieChart(devicesPieElement, 'Devices');

const simulcastsBarChart = simulcastsBarChartElement ? new Chart(simulcastsBarChartElement, {
    type: 'bar',
    data: { labels: [], datasets: [{ label: "Animés", data: [], backgroundColor: '#4e79a7' }] },
    options: {
        maintainAspectRatio: false,
        scales: { y: { beginAtZero: true } },
        plugins: { legend: { display: false } }
    }
}) : null;

const usersByVersionChart = new Chart(usersByVersionChartElement, {
    type: 'line',
    data: { labels: [], datasets: [] },
    options: {
        maintainAspectRatio: false,
        scales: { x: { type: 'time', time: { unit: 'day' } }, y: { beginAtZero: true, stacked: false } },
        elements: { point: { radius: 0 } },
        interaction: { mode: 'index', intersect: false },
        plugins: { legend: { position: 'bottom' } },
        animation: { duration: 0 }
    }
});

const hexToRgba = (hex, alpha=0.3) => {
    const h = hex.replace('#','');
    const bigint = parseInt(h, 16);
    const r = (bigint >> 16) & 255;
    const g = (bigint >> 8) & 255;
    const b = bigint & 255;
    return `rgba(${r},${g},${b},${alpha})`;
};

const getAnalytics = async () => (await fetch('/admin/api/analytics?hours=' + datemenuElement.value + '&activeDays=' + activedaysmenuElement.value + '&days=' + daysmenuElement.value)).json();

const setChartData = async () => {
    const data = await getAnalytics();

    // Metrics charts (CPU/Memory)
    const metricsData = data.metrics || [];
    const timeUnit = parseInt(datemenuElement.value, 10) > 24 ? 'day' : 'hour';
    const labels = metricsData.map(m => m.date);

    const toNumberArray = (arr, key) => arr.map(m => parseFloat(m[key]));

    [cpuChart, memoryChart].forEach(c => c.options.scales.x.time.unit = timeUnit);

    cpuChart.data.labels = labels;
    cpuChart.data.datasets[0].data = toNumberArray(metricsData, 'avgCpuLoad');
    cpuChart.update();

    memoryChart.data.labels = labels;
    memoryChart.data.datasets[0].data = toNumberArray(metricsData, 'avgMemoryUsage');
    memoryChart.update();

    // Attachments chart (line) - cumulative
    const atts = (data.attachments || []).sort((a,b) => a.date.localeCompare(b.date));
    attachmentsChart.data.labels = atts.map(a => a.date);
    const cumulative = atts.reduce((acc, cur, idx) => {
        const val = Number(cur.count) || 0;
        acc.push((idx > 0 ? acc[idx - 1] : 0) + val);
        return acc;
    }, []);
    attachmentsChart.data.datasets[0].data = cumulative;
    attachmentsChart.update();

    // Pie charts from analytics key-counts
    const applyPie = (chart, arr) => {
        const labels = arr.map(x => x.key);
        const values = arr.map(x => x.count);
        chart.data.labels = labels;
        chart.data.datasets[0].data = values;
        chart.update();
    };

    const analytics = data.analytics || {};
    applyPie(versionsPie, analytics.versionCounts || []);
    applyPie(localesPie, analytics.localeCounts || []);
    applyPie(devicesPie, analytics.deviceCounts || []);

    // Stacked area: dailyUserVersionCounts
    const duvc = analytics.dailyUserVersionCounts || [];
    const uniqueDates = Array.from(new Set(duvc.map(x => x.date))).sort((a,b) => a.localeCompare(b));
    const versions = Array.from(new Set(duvc.map(x => x.version))).sort((a,b) => a.localeCompare(b));

    const seriesMap = new Map();
    versions.forEach((v, idx) => {
        seriesMap.set(v, {
            label: v,
            data: new Array(uniqueDates.length).fill(0),
            borderColor: colorPalette[idx % colorPalette.length],
            fill: false,
            tension: 0.1
        });
    });

    duvc.forEach(row => {
        const dateIndex = uniqueDates.indexOf(row.date);
        const dataset = seriesMap.get(row.version);
        if (dataset && dateIndex >= 0) dataset.data[dateIndex] = row.count;
    });


    usersByVersionChart.options.scales.x.time.unit = 'day';
    usersByVersionChart.data.labels = uniqueDates;
    usersByVersionChart.data.datasets = Array.from(seriesMap.values());
    usersByVersionChart.update();

    if (simulcastsBarChart) {
        const simulcasts = await fetch('/api/v1/simulcasts').then(r => r.json());
        const seasonOrder = { WINTER: 0, SPRING: 1, SUMMER: 2, AUTUMN: 3 };
        const sorted = simulcasts.slice().sort((a, b) => {
            if (a.year === b.year) return seasonOrder[a.season] - seasonOrder[b.season];
            return a.year - b.year;
        });
        simulcastsBarChart.data.labels = sorted.map(s => s.season + ' ' + s.year);
        simulcastsBarChart.data.datasets[0].data = sorted.map(s => s.animesCount || 0);
        simulcastsBarChart.update();
    }
};

document.addEventListener('DOMContentLoaded', () => setChartData());
datemenuElement.addEventListener('change', () => setChartData());
activedaysmenuElement.addEventListener('change', () => setChartData());
daysmenuElement.addEventListener('change', () => setChartData());