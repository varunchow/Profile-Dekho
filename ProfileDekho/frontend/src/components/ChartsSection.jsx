import React, { useEffect, useRef } from 'react';

export default function ChartsSection({ profile }) {
  const solvedCanvasRef = useRef(null);
  const diffCanvasRef = useRef(null);
  const ratingCanvasRef = useRef(null);
  const radarCanvasRef = useRef(null);

  const chartInstancesRef = useRef({});

  useEffect(() => {
    if (!profile || !window.Chart) return;

    const Chart = window.Chart;

    // Destroy existing instances to prevent canvas reuse error
    Object.values(chartInstancesRef.current).forEach(chart => {
      if (chart) chart.destroy();
    });
    chartInstancesRef.current = {};

    // 1. Solved Problems by Platform Chart
    if (solvedCanvasRef.current) {
      const ctx = solvedCanvasRef.current.getContext('2d');
      const platformLabels = ['LeetCode', 'Codeforces', 'CodeChef', 'HackerRank', 'GeeksforGeeks'];
      const platformData = [
        profile.leetcodeStats?.solved || 380,
        profile.codeforcesStats?.solved || 460,
        profile.codechefStats?.solved || 210,
        profile.hackerrankStats?.solved || 140,
        profile.gfgStats?.solved || 230
      ];

      chartInstancesRef.current.solved = new Chart(ctx, {
        type: 'bar',
        data: {
          labels: platformLabels,
          datasets: [{
            label: 'Problems Solved',
            data: platformData,
            backgroundColor: [
              '#ffa116',
              '#3182ce',
              '#b87333',
              '#2ec866',
              '#2f9d58'
            ],
            borderRadius: 8,
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false },
            tooltip: { backgroundColor: '#0f172a', titleColor: '#fff', bodyColor: '#cbd5e1' }
          },
          scales: {
            x: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#94a3b8' } },
            y: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#94a3b8' } }
          }
        }
      });
    }

    // 2. Difficulty Breakdown Doughnut Chart
    if (diffCanvasRef.current) {
      const ctx = diffCanvasRef.current.getContext('2d');
      chartInstancesRef.current.diff = new Chart(ctx, {
        type: 'doughnut',
        data: {
          labels: ['Easy', 'Medium', 'Hard'],
          datasets: [{
            data: [
              profile.easySolved || 320,
              profile.mediumSolved || 410,
              profile.hardSolved || 110
            ],
            backgroundColor: ['#10b981', '#f59e0b', '#f43f5e'],
            borderWidth: 0,
            hoverOffset: 6
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { position: 'bottom', labels: { color: '#cbd5e1', padding: 15, font: { size: 12 } } }
          },
          cutout: '70%'
        }
      });
    }

    // 3. Rating History Line Chart
    if (ratingCanvasRef.current && profile.ratingHistory) {
      const ctx = ratingCanvasRef.current.getContext('2d');
      const labels = profile.ratingHistory.map(h => h.month);
      const cfData = profile.ratingHistory.map(h => h.codeforces);
      const lcData = profile.ratingHistory.map(h => h.leetcode);

      chartInstancesRef.current.rating = new Chart(ctx, {
        type: 'line',
        data: {
          labels: labels,
          datasets: [
            {
              label: 'Codeforces Rating',
              data: cfData,
              borderColor: '#6366f1',
              backgroundColor: 'rgba(99, 102, 241, 0.1)',
              tension: 0.35,
              fill: true
            },
            {
              label: 'LeetCode Rating',
              data: lcData,
              borderColor: '#ffa116',
              backgroundColor: 'rgba(255, 161, 22, 0.05)',
              tension: 0.35,
              fill: false
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { position: 'top', labels: { color: '#cbd5e1' } }
          },
          scales: {
            x: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#94a3b8' } },
            y: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#94a3b8' } }
          }
        }
      });
    }

    // 4. Topic Radar Chart
    if (radarCanvasRef.current && profile.topicScores) {
      const ctx = radarCanvasRef.current.getContext('2d');
      const labels = Object.keys(profile.topicScores);
      const dataValues = Object.values(profile.topicScores);

      chartInstancesRef.current.radar = new Chart(ctx, {
        type: 'radar',
        data: {
          labels: labels,
          datasets: [{
            label: 'Skill Proficiency (%)',
            data: dataValues,
            backgroundColor: 'rgba(168, 85, 247, 0.25)',
            borderColor: '#a855f7',
            pointBackgroundColor: '#c084fc'
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false }
          },
          scales: {
            r: {
              angleLines: { color: 'rgba(255,255,255,0.1)' },
              grid: { color: 'rgba(255,255,255,0.1)' },
              pointLabels: { color: '#cbd5e1', font: { size: 11 } },
              ticks: { display: false }
            }
          }
        }
      });
    }

    return () => {
      Object.values(chartInstancesRef.current).forEach(chart => {
        if (chart) chart.destroy();
      });
    };
  }, [profile]);

  return (
    <div className="row g-4 mt-2">
      <div className="col-lg-6">
        <div className="glass-card p-4 h-100">
          <h5 className="fw-bold text-white mb-3 d-flex align-items-center gap-2">
            📊 Problems Solved Across Platforms
          </h5>
          <div style={{ height: '260px' }}>
            <canvas ref={solvedCanvasRef}></canvas>
          </div>
        </div>
      </div>

      <div className="col-lg-6">
        <div className="glass-card p-4 h-100">
          <h5 className="fw-bold text-white mb-3 d-flex align-items-center gap-2">
            🎯 Overall Difficulty Distribution
          </h5>
          <div style={{ height: '260px' }}>
            <canvas ref={diffCanvasRef}></canvas>
          </div>
        </div>
      </div>

      <div className="col-lg-6">
        <div className="glass-card p-4 h-100">
          <h5 className="fw-bold text-white mb-3 d-flex align-items-center gap-2">
            📈 Contest Rating Progression
          </h5>
          <div style={{ height: '260px' }}>
            <canvas ref={ratingCanvasRef}></canvas>
          </div>
        </div>
      </div>

      <div className="col-lg-6">
        <div className="glass-card p-4 h-100">
          <h5 className="fw-bold text-white mb-3 d-flex align-items-center gap-2">
            🕸️ Topic & Skill Radar
          </h5>
          <div style={{ height: '260px' }}>
            <canvas ref={radarCanvasRef}></canvas>
          </div>
        </div>
      </div>
    </div>
  );
}
