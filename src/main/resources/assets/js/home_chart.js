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
                backgroundColor: ['rgba(33,37,41, .2)'],
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
                backgroundColor: ['rgba(33,37,41, .2)'],
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
    return await fetch('/api/metrics?hours=' + hoursElement.value, {
        headers: {
            'Content-Type': 'application/json'
        }
    })
        .then(response => response.json())
        .catch(error => console.error(error));
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