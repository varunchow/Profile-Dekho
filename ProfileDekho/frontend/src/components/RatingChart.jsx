import React from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  BarChart,
  Bar,
} from 'recharts';

/**
 * RatingChart component for displaying contest rating progression
 * Supports single platform or multi-platform comparison
 */
const RatingChart = ({ 
  data = [], 
  platform = "Codeforces",
  height = 400,
  type = "line"
}) => {
  if (!data || data.length === 0) {
    return (
      <div style={styles.container}>
        <h3 style={styles.title}>{platform} Rating Progression</h3>
        <p style={styles.noData}>No rating data available</p>
      </div>
    );
  }

  const renderChart = () => {
    if (type === 'bar') {
      return (
        <ResponsiveContainer width="100%" height={height}>
          <BarChart data={data}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
            <XAxis 
              dataKey="date" 
              stroke="#6b7280"
              tick={{ fontSize: 12 }}
            />
            <YAxis 
              stroke="#6b7280"
              tick={{ fontSize: 12 }}
              domain={['dataMin - 50', 'dataMax + 50']}
            />
            <Tooltip 
              contentStyle={{
                backgroundColor: '#ffffff',
                border: '1px solid #e5e7eb',
                borderRadius: '6px',
              }}
              cursor={{ fill: 'rgba(59, 130, 246, 0.1)' }}
            />
            <Legend />
            <Bar 
              dataKey="rating" 
              fill="#3b82f6" 
              name={platform}
              radius={[4, 4, 0, 0]}
            />
          </BarChart>
        </ResponsiveContainer>
      );
    }

    // Default: LineChart
    return (
      <ResponsiveContainer width="100%" height={height}>
        <LineChart data={data}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
          <XAxis 
            dataKey="date" 
            stroke="#6b7280"
            tick={{ fontSize: 12 }}
          />
          <YAxis 
            stroke="#6b7280"
            tick={{ fontSize: 12 }}
            domain={['dataMin - 50', 'dataMax + 50']}
          />
          <Tooltip 
            contentStyle={{
              backgroundColor: '#ffffff',
              border: '1px solid #e5e7eb',
              borderRadius: '6px',
            }}
            cursor={{ fill: 'rgba(59, 130, 246, 0.1)' }}
          />
          <Legend />
          <Line 
            type="monotone" 
            dataKey="rating" 
            stroke="#3b82f6" 
            dot={false}
            isAnimationActive={true}
            name={platform}
            strokeWidth={2}
          />
        </LineChart>
      </ResponsiveContainer>
    );
  };

  return (
    <div style={styles.container}>
      <h3 style={styles.title}>{platform} Rating Progression</h3>
      {renderChart()}
    </div>
  );
};

/**
 * Multi-platform rating comparison chart
 */
export const RatingComparison = ({ data = {}, height = 400 }) => {
  if (!data || Object.keys(data).length === 0) {
    return (
      <div style={styles.container}>
        <h3 style={styles.title}>Rating Comparison</h3>
        <p style={styles.noData}>No comparison data available</p>
      </div>
    );
  }

  const colors = {
    codeforces: '#ef4444',
    leetcode: '#f59e0b',
    codechef: '#8b5cf6',
    interviewbit: '#06b6d4',
  };

  return (
    <div style={styles.container}>
      <h3 style={styles.title}>Rating Comparison Across Platforms</h3>
      <ResponsiveContainer width="100%" height={height}>
        <LineChart data={transformComparisonData(data)}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
          <XAxis 
            dataKey="date" 
            stroke="#6b7280"
            tick={{ fontSize: 12 }}
          />
          <YAxis 
            stroke="#6b7280"
            tick={{ fontSize: 12 }}
          />
          <Tooltip 
            contentStyle={{
              backgroundColor: '#ffffff',
              border: '1px solid #e5e7eb',
              borderRadius: '6px',
            }}
            cursor={{ fill: 'rgba(59, 130, 246, 0.1)' }}
          />
          <Legend />
          {Object.entries(data).map(([platform, _]) => (
            <Line 
              key={platform}
              type="monotone" 
              dataKey={platform} 
              stroke={colors[platform] || '#666'}
              dot={false}
              name={platform.charAt(0).toUpperCase() + platform.slice(1)}
              strokeWidth={2}
            />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};

/**
 * Summary card showing key rating statistics
 */
export const RatingSummary = ({ stats = {} }) => {
  return (
    <div style={styles.summaryGrid}>
      {Object.entries(stats).map(([platform, data]) => (
        <div key={platform} style={styles.summaryCard}>
          <h4 style={styles.summaryTitle}>
            {platform.charAt(0).toUpperCase() + platform.slice(1)}
          </h4>
          <div style={styles.statRow}>
            <span style={styles.statLabel}>Current Rating:</span>
            <span style={styles.statValue}>{data.current || 0}</span>
          </div>
          <div style={styles.statRow}>
            <span style={styles.statLabel}>Max Rating:</span>
            <span style={styles.statValue}>{data.max || 0}</span>
          </div>
          <div style={styles.statRow}>
            <span style={styles.statLabel}>Contests:</span>
            <span style={styles.statValue}>{data.contests || 0}</span>
          </div>
          <div style={styles.statRow}>
            <span style={styles.statLabel}>Rank:</span>
            <span style={styles.statValue}>{data.rank || 'N/A'}</span>
          </div>
        </div>
      ))}
    </div>
  );
};

const transformComparisonData = (data) => {
  const allDates = new Set();
  Object.values(data).forEach(platformData => {
    platformData.forEach(item => allDates.add(item.date));
  });

  return Array.from(allDates)
    .sort()
    .map(date => {
      const entry = { date };
      Object.entries(data).forEach(([platform, platformData]) => {
        const rating = platformData.find(d => d.date === date)?.rating || null;
        entry[platform] = rating;
      });
      return entry;
    });
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
  noData: {
    color: '#9ca3af',
    fontSize: '14px',
    margin: '0',
  },
  summaryGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
    gap: '16px',
    marginTop: '16px',
  },
  summaryCard: {
    backgroundColor: '#ffffff',
    padding: '16px',
    borderRadius: '8px',
    border: '1px solid #e5e7eb',
  },
  summaryTitle: {
    margin: '0 0 12px 0',
    fontSize: '14px',
    fontWeight: '600',
    color: '#1f2937',
  },
  statRow: {
    display: 'flex',
    justifyContent: 'space-between',
    paddingBottom: '8px',
    borderBottom: '1px solid #f3f4f6',
  },
  statLabel: {
    fontSize: '12px',
    color: '#6b7280',
  },
  statValue: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#1f2937',
  },
};

export default RatingChart;
