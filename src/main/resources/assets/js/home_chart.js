const hoursElement = document.getElementById('hours');
const cpuChartElement = document.getElementById('cpuLoadChart').getContext('2d');
const memoryChartElement = document.getElementById('memoryUsageChart').getContext('2d');

const cpuChart = new Chart(cpuChartElement, {
    type: 'line',
    data: {
        labels: [],
        datasets: [
            {
                label: '% CPU',
                data: [],
                fill: false,
                borderColor: ['rgba(33,37,41, 1)'],
                tension: 0.1
            }
        ]
    },
    options: {
        maintainAspectRatio: false,
        scales: {
            y: {
                beginAtZero: true
            }
        },
        elements: {
            point: {
                radius: 0
            }
        },
        animation: {
            duration: 0
        },
        plugins: {
            legend: {
                display: false
            }
        }
    }
});

const memoryChart = new Chart(memoryChartElement, {
    type: 'line',
    data: {
        labels: [],
        datasets: [
            {
                label: 'RAM in MB',
                data: [],
                fill: false,
                borderColor: ['rgba(33,37,41, 1)'],
                tension: 0.1
            }
        ]
    },
    options: {
        maintainAspectRatio: false,
        scales: {
            y: {
                beginAtZero: true
            }
        },
        elements: {
            point: {
                radius: 0
            }
        },
        animation: {
            duration: 0
        },
        plugins: {
            legend: {
                display: false
            }
        }
    }
});

document.addEventListener('DOMContentLoaded', async () => {
    await setChartData();

    setInterval(async () => {
        await setChartData();
    }, 10000);
});

async function getMetrics() {
    return await axios.get('/api/metrics?hours=' + hoursElement.value)
        .then(response => response.data);
}

async function setChartData() {
    const data = await getMetrics();

    cpuChart.data.labels = data.map(metric => metric.date);
    cpuChart.data.datasets[0].data = data.map(metric => metric.cpuLoad);
    cpuChart.update();

    memoryChart.data.labels = data.map(metric => metric.date);
    memoryChart.data.datasets[0].data = data.map(metric => metric.memoryUsage);
    memoryChart.update();
}