import React, { useState } from 'react';

export default function Hero({ onSearch, onDemoClick }) {
  const [handleInput, setHandleInput] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (handleInput.trim()) {
      onSearch(handleInput.trim());
    }
  };

  return (
    <div className="py-5 text-center px-3">
      <div className="container py-4" style={{ maxWidth: '900px' }}>
        <div className="d-inline-flex align-items-center gap-2 px-3 py-1 rounded-pill mb-4" 
             style={{ background: 'rgba(99, 102, 241, 0.12)', border: '1px solid rgba(99, 102, 241, 0.3)', color: '#818cf8' }}>
          <span className="fs-7 fw-bold">✨ ALL-IN-ONE COMPETITIVE CODING PORTFOLIO</span>
        </div>

        <h1 className="display-4 fw-extrabold text-white mb-3 tracking-tight" style={{ fontWeight: 800 }}>
          Showcase Your Coding Journey <br />
          <span style={{ background: 'linear-gradient(135deg, #818cf8, #c084fc, #34d399)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
            Across Every Coding Platform
          </span>
        </h1>

        <p className="lead text-secondary mb-4 mx-auto" style={{ maxWidth: '680px', fontSize: '1.1rem' }}>
          Aggregate your problems solved, contest ratings, streaks, and skill badges from 
          <strong> LeetCode, CodeChef, CodeForces, GeeksforGeeks, and GitHub</strong> into one recruiter-ready dashboard.
        </p>

        <form onSubmit={handleSubmit} className="d-flex flex-column flex-sm-row justify-content-center gap-2 mb-4 mx-auto" style={{ maxWidth: '560px' }}>
          <input 
            type="text" 
            className="form-input-neon py-3 px-4" 
            placeholder="Enter coding handle or username..." 
            value={handleInput}
            onChange={(e) => setHandleInput(e.target.value)}
          />
          <button type="submit" className="btn-neon text-nowrap py-3 px-4">
            Showcase Profile 🚀
          </button>
        </form>

        <div className="d-flex flex-wrap justify-content-center gap-2 mb-5">
          <span className="text-muted fs-7 me-2">Try Demo Profiles:</span>
          {['tourist', 'neal_wu', 'alex_coder', 'coder_pro'].map((sample) => (
            <button 
              key={sample} 
              className="badge bg-dark border border-secondary text-secondary px-3 py-2 cursor-pointer"
              onClick={() => onDemoClick(sample)}
              style={{ cursor: 'pointer', borderRadius: '12px' }}
            >
              @{sample}
            </button>
          ))}
        </div>

        {/* Platform Badges Row */}
        <div className="d-flex flex-wrap justify-content-center gap-3 align-items-center opacity-75">
          <div className="platform-badge leetcode">🟡 LeetCode</div>
          <div className="platform-badge codeforces">🔷 Codeforces</div>
          <div className="platform-badge codechef">🟤 CodeChef</div>
          <div className="platform-badge gfg">🟩 GeeksforGeeks</div>
          <div className="platform-badge github">🐙 GitHub</div>
        </div>
      </div>
    </div>
  );
}
