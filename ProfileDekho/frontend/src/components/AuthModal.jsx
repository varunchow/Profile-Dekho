import React, { useState } from 'react';

export default function AuthModal({ isOpen, mode, onClose, onAuthSuccess }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    const handle = username.trim() || 'demo_coder';
    onAuthSuccess(handle);
    onClose();
  };

  const handleDemoInstant = () => {
    onAuthSuccess('alex_coder');
    onClose();
  };

  return (
    <div className="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" 
         style={{ background: 'rgba(0, 0, 0, 0.75)', backdropFilter: 'blur(8px)', zIndex: 2000 }}>
      <div className="glass-card p-4 p-sm-5 text-start position-relative w-100 mx-3" style={{ maxWidth: '420px' }}>
        <button 
          className="position-absolute top-0 end-0 m-3 border-0 bg-transparent text-secondary fs-4"
          onClick={onClose}
        >
          ✕
        </button>

        <h3 className="fw-bold text-white mb-1">
          {mode === 'login' ? 'Welcome Back!' : 'Create ProfileDekho Account'}
        </h3>
        <p className="text-secondary fs-7 mb-4">
          {mode === 'login' ? 'Sign in to sync your competitive coding handles.' : 'Start showcasing your coding achievements to recruiters.'}
        </p>

        <form onSubmit={handleSubmit}>
          <div className="mb-3">
            <label className="form-label fs-7 fw-semibold text-secondary">Username / Handle</label>
            <input 
              type="text" 
              className="form-input-neon" 
              placeholder="e.g. dev_master" 
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </div>

          {mode === 'register' && (
            <div className="mb-3">
              <label className="form-label fs-7 fw-semibold text-secondary">Email Address</label>
              <input 
                type="email" 
                className="form-input-neon" 
                placeholder="dev@example.com" 
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
          )}

          <div className="mb-4">
            <label className="form-label fs-7 fw-semibold text-secondary">Password</label>
            <input 
              type="password" 
              className="form-input-neon" 
              placeholder="••••••••" 
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <button type="submit" className="btn-neon w-100 py-3 justify-content-center mb-3">
            {mode === 'login' ? 'Login' : 'Create Account'}
          </button>
        </form>

        <div className="border-top border-secondary pt-3 mt-3 text-center">
          <button 
            type="button" 
            className="btn btn-sm text-info fw-semibold p-0"
            onClick={handleDemoInstant}
          >
            ⚡ One-Click Demo Login (Instant Preview)
          </button>
        </div>
      </div>
    </div>
  );
}
