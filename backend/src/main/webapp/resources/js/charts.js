// Chart.js helpers for results and progress pages

function initRadarChart(canvasId, labels, data) {
    const ctx = document.getElementById(canvasId)
    if (!ctx) return
    new Chart(ctx, {
        type: 'radar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Scores',
                data: data,
                fill: true,
                backgroundColor: 'rgba(59,130,246,0.15)',
                borderColor: '#3B82F6',
                pointBackgroundColor: '#3B82F6',
                pointRadius: 4
            }]
        },
        options: {
            scales: {
                r: {
                    beginAtZero: true,
                    max: 10,
                    ticks: { color: '#94A3B8', backdropColor: 'transparent', stepSize: 2 },
                    grid:  { color: '#1F2937' },
                    pointLabels: { color: '#94A3B8', font: { size: 12 } }
                }
            },
            plugins: { legend: { display: false } },
            responsive: true
        }
    })
}

function initProgressCharts(scores, labels) {
    const lineCtx = document.getElementById('progressChart')
    if (lineCtx && scores.length > 0) {
        new Chart(lineCtx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Overall Score',
                    data: scores,
                    fill: true,
                    borderColor: '#3B82F6',
                    backgroundColor: 'rgba(59,130,246,0.1)',
                    tension: 0.4,
                    pointRadius: 4,
                    pointBackgroundColor: '#3B82F6'
                }]
            },
            options: {
                scales: {
                    y: { min: 0, max: 10, ticks: { color: '#94A3B8' }, grid: { color: '#1F2937' } },
                    x: { ticks: { color: '#94A3B8' }, grid: { color: '#1F2937' } }
                },
                plugins: { legend: { labels: { color: '#94A3B8' } } },
                responsive: true
            }
        })
    }

    const barCtx = document.getElementById('barChart')
    if (barCtx && scores.length > 0) {
        new Chart(barCtx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Session Score',
                    data: scores,
                    backgroundColor: scores.map(s =>
                        s >= 7 ? 'rgba(16,185,129,0.6)' :
                        s >= 5 ? 'rgba(245,158,11,0.6)' : 'rgba(244,63,94,0.6)'),
                    borderRadius: 6
                }]
            },
            options: {
                scales: {
                    y: { min: 0, max: 10, ticks: { color: '#94A3B8' }, grid: { color: '#1F2937' } },
                    x: { ticks: { color: '#94A3B8' }, grid: { display: false } }
                },
                plugins: { legend: { labels: { color: '#94A3B8' } } },
                responsive: true
            }
        })
    }
}

// Auto-init radar chart on results page
document.addEventListener('DOMContentLoaded', function() {
    const canvas = document.getElementById('radarChart')
    if (canvas) {
        initRadarChart('radarChart',
            ['Relevance', 'Clarity', 'Sentiment'],
            [
                parseFloat(canvas.dataset.relevance || 0),
                parseFloat(canvas.dataset.clarity   || 0),
                parseFloat(canvas.dataset.sentiment || 0)
            ])
    }
})
