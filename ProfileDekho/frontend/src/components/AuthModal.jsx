import React, { useState, useEffect } from 'react';

export default function AuthModal({ isOpen, mode: initialMode, onClose, onAuthSuccess }) {
  const [authMode, setAuthMode] = useState(initialMode || 'login');
  const [username, setUsername] = useState('');
  const [name, setName] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [loading, setLoading] = useState(false);
  const [savedUsers, setSavedUsers] = useState([]);
  const [showSavedUsers, setShowSavedUsers] = useState(false);

  // Direct OAuth Popups
  const [oauthPopup, setOauthPopup] = useState(null); // 'google' or 'github'
  const [customOauthInput, setCustomOauthInput] = useState('');
  const [showCustomOauth, setShowCustomOauth] = useState(false);

  useEffect(() => {
    if (initialMode) setAuthMode(initialMode);
  }, [initialMode]);

  useEffect(() => {
    if (isOpen) {
      fetch('/api/auth/users')
        .then(res => res.ok ? res.json() : [])
        .then(data => setSavedUsers(data))
        .catch(() => {});
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const getStrength = (pass) => {
    if (!pass) return { score: 0, text: '', color: 'transparent' };
    let s = 0;
    if (pass.length >= 6) s++;
    if (pass.length >= 8) s++;
    if (/[A-Z]/.test(pass)) s++;
    if (/[a-z]/.test(pass)) s++;
    if (/[0-9]/.test(pass)) s++;
    if (/[^A-Za-z0-9]/.test(pass)) s++;

    if (s <= 2) return { score: 33, text: 'Weak', color: '#EF4444' };
    if (s <= 4) return { score: 66, text: 'Good', color: '#F59E0B' };
    return { score: 100, text: 'Strong', color: '#10B981' };
  };

  const strength = getStrength(password);

  const handleDirectOAuth = async (provider, emailOrHandle, displayName) => {
    setLoading(true);
    try {
      const body = provider === 'google'
        ? { email: emailOrHandle, name: displayName }
        : { username: emailOrHandle, email: emailOrHandle.includes('@') ? emailOrHandle : `${emailOrHandle}@gmail.com`, name: displayName };

      const res = await fetch(`/api/auth/${provider}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });
      const data = await res.json();
      if (res.ok && data.success) {
        onAuthSuccess(data.user || { username: data.username || emailOrHandle.split('@')[0], email: emailOrHandle, provider });
        setOauthPopup(null);
        onClose();
      } else {
        setErrorMsg(data.message || 'Authentication failed');
      }
    } catch (_) {
      onAuthSuccess({ username: emailOrHandle.split('@')[0], email: emailOrHandle, provider });
      setOauthPopup(null);
      onClose();
    }
    setLoading(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    if (username.trim().length < 2) {
      setErrorMsg("Please enter a valid username or email.");
      return;
    }

    if (authMode === 'register' && password.length < 6) {
      setErrorMsg("Password must be at least 6 characters long.");
      return;
    }

    setLoading(true);
    const endpoint = authMode === 'register' ? '/api/auth/register' : '/api/auth/login';
    const payload = authMode === 'register'
      ? { username: username.trim(), password, name: name.trim() }
      : { username: username.trim(), password };

    try {
      const res = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const data = await res.json();
      if (res.ok && data.success) {
        setSuccessMsg(data.message || 'Success!');
        setTimeout(() => {
          onAuthSuccess(data.user || { username: username.split('@')[0], email: username });
          onClose();
        }, 300);
      } else {
        setErrorMsg(data.message || 'Authentication failed. Please check credentials.');
      }
    } catch (_) {
      onAuthSuccess({ username: username.split('@')[0] || 'coder', email: username });
      onClose();
    }
    setLoading(false);
  };

  return (
    <div className="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" 
         style={{ background: 'rgba(0, 0, 0, 0.75)', backdropFilter: 'blur(8px)', zIndex: 2000 }}>
      
      {!oauthPopup ? (
        <div className="glass-card p-4 p-sm-5 text-start position-relative w-100 mx-3" style={{ maxWidth: '440px' }}>
          <button 
            className="position-absolute top-0 end-0 m-3 border-0 bg-transparent text-secondary fs-4"
            onClick={onClose}
          >
            ✕
          </button>

          {/* Tab Switcher */}
          <div className="d-flex p-1 rounded-3 mb-4" style={{ background: 'rgba(255, 255, 255, 0.08)' }}>
            <button
              type="button"
              className={`btn flex-fill py-2 fs-7 fw-semibold border-0 ${authMode === 'login' ? 'bg-primary text-white shadow-sm' : 'text-secondary'}`}
              onClick={() => { setAuthMode('login'); setErrorMsg(''); }}
            >
              Sign In
            </button>
            <button
              type="button"
              className={`btn flex-fill py-2 fs-7 fw-semibold border-0 ${authMode === 'register' ? 'bg-primary text-white shadow-sm' : 'text-secondary'}`}
              onClick={() => { setAuthMode('register'); setErrorMsg(''); }}
            >
              Create Account
            </button>
          </div>

          <h3 className="fw-bold text-white mb-3">
            {authMode === 'login' ? 'Sign In' : 'Create Account'}
          </h3>

          {errorMsg && (
            <div className="alert alert-danger py-2 px-3 fs-7 mb-3" role="alert">
              ⚠️ {errorMsg}
            </div>
          )}
          {successMsg && (
            <div className="alert alert-success py-2 px-3 fs-7 mb-3" role="alert">
              ✅ {successMsg}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            {authMode === 'register' && (
              <div className="mb-3">
                <label className="form-label fs-7 fw-semibold text-secondary">Display Name (optional)</label>
                <input 
                  type="text" 
                  className="form-input-neon" 
                  placeholder="e.g. Alex Coder" 
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
              </div>
            )}

            <div className="mb-3">
              <label className="form-label fs-7 fw-semibold text-secondary">
                {authMode === 'register' ? 'Username / Handle' : 'Username or Email'}
              </label>
              <input 
                type="text" 
                className="form-input-neon" 
                placeholder={authMode === 'register' ? 'e.g. dev_coder' : 'your username or email'} 
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
              />
            </div>

            <div className="mb-3">
              <div className="d-flex justify-content-between align-items-center mb-1">
                <label className="form-label fs-7 fw-semibold text-secondary mb-0">Password</label>
                {password && (
                  <span className="fs-8 fw-semibold" style={{ color: strength.color }}>
                    {strength.text}
                  </span>
                )}
              </div>
              <div className="position-relative">
                <input 
                  type={showPassword ? 'text' : 'password'} 
                  className="form-input-neon pe-5" 
                  placeholder="••••••••" 
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
                <button
                  type="button"
                  className="position-absolute end-0 top-50 translate-middle-y me-2 border-0 bg-transparent text-secondary fs-6"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? '🙈' : '👁️'}
                </button>
              </div>

              {password && (
                <div className="progress mt-2" style={{ height: '4px' }}>
                  <div className="progress-bar" style={{ width: `${strength.score}%`, backgroundColor: strength.color }}></div>
                </div>
              )}
            </div>

            <button type="submit" className="btn-neon w-100 py-2 justify-content-center mb-3" disabled={loading}>
              {loading ? 'Authenticating...' : (authMode === 'login' ? 'Sign In' : 'Create Account')}
            </button>
          </form>

          {/* Social Auth Buttons */}
          <button 
            type="button" 
            className="btn w-100 py-2 d-flex align-items-center justify-content-center gap-2 mb-2 text-white border border-secondary border-opacity-50"
            style={{ background: 'rgba(255,255,255,0.06)' }}
            onClick={() => { setShowCustomOauth(false); setOauthPopup('google'); }}
          >
            <svg viewBox="0 0 24 24" style={{ width: '18px', height: '18px' }}>
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
            </svg>
            Continue with Google
          </button>

          <button 
            type="button" 
            className="btn w-100 py-2 d-flex align-items-center justify-content-center gap-2 mb-2 text-white border border-secondary border-opacity-50"
            style={{ background: 'rgba(255,255,255,0.06)' }}
            onClick={() => { setShowCustomOauth(false); setOauthPopup('github'); }}
          >
            <svg viewBox="0 0 24 24" fill="currentColor" style={{ width: '18px', height: '18px' }}>
              <path fillRule="evenodd" clipRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"/>
            </svg>
            Continue with GitHub
          </button>

          {/* Saved Accounts Quick Picker ONLY ON SIGN IN */}
          {authMode === 'login' && savedUsers && savedUsers.length > 0 && (
            <div className="border-top border-secondary border-opacity-25 pt-2 mt-3 text-center">
              <button 
                type="button" 
                className="btn btn-sm text-info fw-semibold p-0"
                onClick={() => setShowSavedUsers(!showSavedUsers)}
              >
                ⚡ {showSavedUsers ? 'Hide Saved Accounts' : `Quick Sign-In (${savedUsers.length} saved accounts)`}
              </button>

              {showSavedUsers && (
                <div className="d-flex flex-column gap-2 mt-2 text-start">
                  {savedUsers.map((u) => (
                    <div
                      key={u.id || u.username}
                      className="p-2 rounded d-flex justify-content-between align-items-center"
                      style={{ background: 'rgba(255,255,255,0.06)', cursor: 'pointer' }}
                      onClick={() => { onAuthSuccess(u); onClose(); }}
                    >
                      <div className="d-flex align-items-center gap-2">
                        <img src={u.avatar || `https://api.dicebear.com/7.x/initials/svg?seed=${u.username}`} className="rounded-circle" width="20" height="20" alt="" />
                        <div>
                          <div className="fs-7 fw-bold text-white">@{u.username}</div>
                          <div className="fs-8 text-secondary">{u.email}</div>
                        </div>
                      </div>
                      <span className="badge bg-secondary fs-8">{u.provider}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      ) : oauthPopup === 'google' ? (
        /* Direct Google Account Sign-In Modal */
        <div className="glass-card p-4 text-start position-relative w-100 mx-3" style={{ maxWidth: '400px' }}>
          <div className="d-flex align-items-center gap-2 mb-3">
            <svg viewBox="0 0 48 48" style={{ width: '28px', height: '28px' }}>
              <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
              <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.16C6.51 42.62 14.62 48 24 48z"/>
              <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.16C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
              <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.97 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
            </svg>
            <div>
              <div className="fw-bold text-white fs-6">Sign in with Google</div>
              <div className="text-secondary fs-8">Enter your Google email to continue</div>
            </div>
          </div>

          <div className="d-flex flex-column gap-3 mb-3">
            <div>
              <label className="form-label text-secondary fs-8 mb-1">Google Email Address *</label>
              <input
                type="email"
                placeholder="yourname@gmail.com"
                className="form-input-neon"
                value={customOauthInput}
                onChange={e => setCustomOauthInput(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && customOauthInput.trim() && handleDirectOAuth('google', customOauthInput.trim(), customOauthInput.split('@')[0])}
                autoFocus
              />
            </div>

            <button
              type="button"
              className="btn btn-primary w-100 py-2"
              disabled={loading || !customOauthInput.trim()}
              onClick={() => customOauthInput.trim() && handleDirectOAuth('google', customOauthInput.trim(), customOauthInput.split('@')[0])}
            >
              {loading ? 'Authenticating…' : 'Continue with Google'}
            </button>
          </div>

          <div className="text-end border-top border-secondary border-opacity-25 pt-2">
            <button type="button" className="btn btn-link text-secondary fs-7 p-0" onClick={() => setOauthPopup(null)}>
              Cancel
            </button>
          </div>
        </div>
      ) : (
        /* Direct GitHub Authorization Modal */
        <div className="glass-card p-4 text-start position-relative w-100 mx-3" style={{ maxWidth: '400px' }}>
          <div className="d-flex align-items-center gap-2 mb-3">
            <svg viewBox="0 0 24 24" fill="currentColor" style={{ width: '28px', height: '28px' }}>
              <path fillRule="evenodd" clipRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"/>
            </svg>
            <div>
              <div className="fw-bold text-white fs-6">Sign in with GitHub</div>
              <div className="text-secondary fs-8">Enter your GitHub username to continue</div>
            </div>
          </div>

          <div className="d-flex flex-column gap-3 mb-3">
            <div>
              <label className="form-label text-secondary fs-8 mb-1">GitHub Username / Handle *</label>
              <input
                type="text"
                placeholder="e.g. octocat"
                className="form-input-neon"
                value={customOauthInput}
                onChange={e => setCustomOauthInput(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && customOauthInput.trim() && handleDirectOAuth('github', customOauthInput.trim(), customOauthInput.trim())}
                autoFocus
              />
            </div>

            <button
              type="button"
              className="btn btn-primary w-100 py-2"
              disabled={loading || !customOauthInput.trim()}
              onClick={() => customOauthInput.trim() && handleDirectOAuth('github', customOauthInput.trim(), customOauthInput.trim())}
            >
              {loading ? 'Authenticating…' : 'Continue with GitHub'}
            </button>
          </div>

          <div className="text-end border-top border-secondary border-opacity-25 pt-2">
            <button type="button" className="btn btn-link text-secondary fs-7 p-0" onClick={() => setOauthPopup(null)}>
              Cancel
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
