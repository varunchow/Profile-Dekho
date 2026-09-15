import React from 'react';

/**
 * Heatmap component for displaying daily submission/contribution patterns
 * Uses CSS grid and color scales to visualize activity
 */
const Heatmap = ({ data = [], platform = "platform", title = "Activity Heatmap" }) => {
  if (!data || data.length === 0) {
    return (
      <div style={styles.container}>
        <h3 style={styles.title}>{title}</h3>
        <p style={styles.noData}>No activity data available</p>
      </div>
    );
  }

  // Find max value for color scaling
  const maxValue = Math.max(...data.map(d => d.count || 0), 1);
  
  // Get color based on intensity
  const getColor = (count) => {
    if (!count || count === 0) return '#ebedf0';
    const intensity = count / maxValue;
    
    if (intensity < 0.25) return '#c6e48b';
    if (intensity < 0.5) return '#7bc96f';
    if (intensity < 0.75) return '#239a3b';
    return '#196127';
  };

  // Group data by week for calendar view
  const groupedData = {};
  data.forEach(item => {
    const date = new Date(item.date);
    const week = Math.ceil(date.getDate() / 7);
    const month = date.toLocaleString('default', { month: 'short' });
    const key = `${month} W${week}`;
    
    if (!groupedData[key]) {
      groupedData[key] = [];
    }
    groupedData[key].push(item);
  });

  return (
    <div style={styles.container}>
      <h3 style={styles.title}>{title}</h3>
      <div style={styles.heatmapGrid}>
        {data.map((item, idx) => (
          <div
            key={idx}
            style={{
              ...styles.cell,
              backgroundColor: getColor(item.count),
            }}
            title={`${item.date}: ${item.count || 0} submissions`}
          />
        ))}
      </div>
      <div style={styles.legend}>
        <span style={styles.legendLabel}>Less</span>
        {[0, 1, 2, 3, 4].map(i => (
          <div
            key={i}
            style={{
              ...styles.legendCell,
              backgroundColor: getColor(maxValue * (i / 4)),
            }}
          />
        ))}
        <span style={styles.legendLabel}>More</span>
      </div>
    </div>
  );
};

const styles = {
  container: {
    padding: '20px',
    backgroundColor: '#f9fafb',
    borderRadius: '8px',
    marginBottom: '20px',
    border: '1px solid #e5e7eb',
  },
  title: {
    margin: '0 0 16px 0',
    fontSize: '18px',
    fontWeight: '600',
    color: '#1f2937',
  },
  heatmapGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(15px, 1fr))',
    gap: '2px',
    marginBottom: '16px',
  },
  cell: {
    width: '15px',
    height: '15px',
    borderRadius: '2px',
    border: '1px solid #d1d5db',
    cursor: 'pointer',
    transition: 'all 0.2s ease',
  },
  legend: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    fontSize: '12px',
    color: '#6b7280',
  },
  legendLabel: {
    fontSize: '12px',
    color: '#6b7280',
  },
  legendCell: {
    width: '12px',
    height: '12px',
    borderRadius: '2px',
    border: '1px solid #d1d5db',
  },
  noData: {
    color: '#9ca3af',
    fontSize: '14px',
    margin: '0',
  },
};

export default Heatmap;
