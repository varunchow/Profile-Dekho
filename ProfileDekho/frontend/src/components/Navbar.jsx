import React, { useState } from 'react';

export default function Navbar({ activeTab, setActiveTab, currentUser, onLogout, onSearch, openAuth }) {
  const [searchQuery, setSearchQuery] = useState('');

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      onSearch(searchQuery.trim());
    }
  };

  return (
    <nav className="glass-nav py-3 px-4 px-md-5 d-flex align-items-center justify-content-between">
      <div className="d-flex align-items-center gap-3">
        <div 
          className="d-flex align-items-center gap-2 cursor-pointer" 
          onClick={() => setActiveTab('home')}
          style={{ cursor: 'pointer' }}
        >
          <div 
            className="d-flex align-items-center justify-content-center fw-bold text-white rounded-3 fs-4 px-2 py-1"
            style={{ background: 'linear-gradient(135deg, #6366f1, #a855f7)', width: '38px', height: '38px' }}
          >
            PD
          </div>
          <span className="fw-bold fs-4 text-white tracking-tight">
            Profile<span style={{ color: '#6366f1' }}>Dekho</span>
          </span>
        </div>

        <div className="d-none d-md-flex align-items-center gap-1 ms-4">
          <button 
            className={`btn btn-sm ${activeTab === 'home' ? 'text-white fw-bold' : 'text-secondary'}`}
            onClick={() => setActiveTab('home')}
            style={{ border: 'none', background: 'none' }}
          >
            Explore Profiles
          </button>
          <button 
            className={`btn btn-sm ${activeTab === 'dashboard' ? 'text-white fw-bold' : 'text-secondary'}`}
            onClick={() => setActiveTab('dashboard')}
            style={{ border: 'none', background: 'none' }}
          >
            My Profiles
          </button>
          <button 
            className={`btn btn-sm ${activeTab === 'showcase' ? 'text-white fw-bold' : 'text-secondary'}`}
            onClick={() => setActiveTab('showcase')}
            style={{ border: 'none', background: 'none' }}
          >
            Showcase
          </button>
        </div>
      </div>

      <form onSubmit={handleSearchSubmit} className="d-none d-sm-flex align-items-center mx-3" style={{ maxWidth: '320px', flex: '1' }}>
        <div className="position-relative w-100">
          <input 
            type="text" 
            className="form-input-neon py-2 ps-4 pe-4" 
            placeholder="Search handle (e.g. tourist, neal_wu)..." 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ fontSize: '0.88rem' }}
          />
          <button 
            type="submit" 
            className="position-absolute end-0 top-50 translate-middle-y me-2 border-0 bg-transparent text-secondary"
          >
            🔍
          </button>
        </div>
      </form>

      <div className="d-flex align-items-center gap-2">
        {currentUser ? (
          <div className="d-flex align-items-center gap-2">
            <span className="text-light fs-6 fw-semibold d-none d-sm-inline">
              👋 {currentUser}
            </span>
            <button className="btn-outline-neon py-1 px-3 fs-7" onClick={onLogout}>
              Logout
            </button>
          </div>
        ) : (
          <div className="d-flex align-items-center gap-2">
            <button className="btn-outline-neon py-1 px-3" onClick={() => openAuth('login')}>
              Login
            </button>
            <button className="btn-neon py-1 px-3" onClick={() => openAuth('register')}>
              Get Started
            </button>
          </div>
        )}
      </div>
    </nav>
  );
}
