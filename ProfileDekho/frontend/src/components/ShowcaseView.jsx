import React, { useState } from 'react';
import ChartsSection from './ChartsSection';

export default function ShowcaseView({ profile, currentUser, onProfileUpdate }) {
  const [copied, setCopied] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [editName, setEditName] = useState('');
  const [editBio, setEditBio] = useState('');
  const [editTitle, setEditTitle] = useState('');
  const [saving, setSaving] = useState(false);

  if (!profile) return null;

  const shareUrl = `${window.location.origin}/#profile?u=${profile.username}`;

  const displayName = (profile.name && !profile.name.includes('@')) 
    ? profile.name 
    : (profile.username ? profile.username.replace(/[._]/g, ' ').replace(/\b\w/g, c => c.toUpperCase()) : 'Developer');

  const handleCopyLink = () => {
    navigator.clipboard.writeText(shareUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 3000);
  };

  const handleExportJson = () => {
    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(profile, null, 2));
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute("href", dataStr);
    downloadAnchor.setAttribute("download", `${profile.username}_ProfileDekho_Summary.json`);
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  };

  const openEditModal = () => {
    setEditName(displayName);
    setEditBio(profile.bio || '');
    setEditTitle(profile.title || 'Master');
    setShowEditModal(true);
  };

  const handleSaveProfile = async (e) => {
    e.preventDefault();
    setSaving(true);
    const cleanName = (editName && !editName.includes('@')) ? editName.trim() : displayName;
    const cleanBio = editBio.trim() || 'Competitive Programmer | ProfileDekho member';
    const cleanTitle = editTitle.trim() || profile.title || 'Master';

    try {
      const res = await fetch('/api/profiles/update', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: profile.username,
          name: cleanName,
          bio: cleanBio,
          title: cleanTitle
        })
      });
      const updatedProfile = {
        ...profile,
        name: cleanName,
        bio: cleanBio,
        title: cleanTitle
      };
      if (onProfileUpdate) onProfileUpdate(updatedProfile);
      setShowEditModal(false);
    } catch (_) {
      const updatedProfile = {
        ...profile,
        name: cleanName,
        bio: cleanBio,
        title: cleanTitle
      };
      if (onProfileUpdate) onProfileUpdate(updatedProfile);
      setShowEditModal(false);
    }
    setSaving(false);
  };

  return (
    <div className="container py-4" style={{ maxWidth: '1100px' }}>
      {/* Profile Header Glass Banner */}
      <div className="glass-card p-4 p-md-5 mb-4 position-relative overflow-hidden">
        <div className="position-absolute top-0 end-0 p-4 d-none d-md-block opacity-25">
          <div className="fs-1 text-white fw-bold">PROFILEDЕКHO</div>
        </div>

        <div className="row align-items-center g-4">
          <div className="col-md-auto text-center text-md-start">
            <img 
              src={profile.avatar || `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(displayName)}&backgroundColor=4A7FD4&textColor=ffffff`}
              alt={profile.username}
              className="rounded-circle p-1"
              style={{ width: '110px', height: '110px', background: 'linear-gradient(135deg, #6366f1, #a855f7)' }}
            />
          </div>

          <div className="col-md text-center text-md-start">
            <div className="d-flex flex-wrap align-items-center justify-content-center justify-content-md-start gap-2 mb-2">
              <h2 className="fw-bold text-white mb-0">{displayName}</h2>
              <span className="badge bg-indigo-500 px-3 py-2 text-uppercase font-monospace" 
                    style={{ background: 'linear-gradient(135deg, #6366f1, #a855f7)', fontSize: '0.8rem', borderRadius: '12px' }}>
                🏆 {profile.title || 'Master'}
              </span>
            </div>
            <p className="text-secondary mb-3">@{profile.username} • {profile.bio}</p>

            {/* Platform Badges */}
            <div className="d-flex flex-wrap justify-content-center justify-content-md-start gap-2">
              {profile.leetcodeHandle && (
                <a href={`https://leetcode.com/${profile.leetcodeHandle}`} target="_blank" rel="noreferrer" className="platform-badge leetcode text-decoration-none">
                  🟡 LeetCode: @{profile.leetcodeHandle}
                </a>
              )}
              {profile.codeforcesHandle && (
                <a href={`https://codeforces.com/profile/${profile.codeforcesHandle}`} target="_blank" rel="noreferrer" className="platform-badge codeforces text-decoration-none">
                  🔷 Codeforces: @{profile.codeforcesHandle}
                </a>
              )}
              {profile.codechefHandle && (
                <a href={`https://www.codechef.com/users/${profile.codechefHandle}`} target="_blank" rel="noreferrer" className="platform-badge codechef text-decoration-none">
                  🟤 CodeChef: @{profile.codechefHandle}
                </a>
              )}
              {profile.interviewbitHandle && (
                <a href={`https://www.interviewbit.com/profile/${profile.interviewbitHandle}`} target="_blank" rel="noreferrer" className="platform-badge interviewbit text-decoration-none">
                  🟣 InterviewBit: @{profile.interviewbitHandle}
                </a>
              )}
              {profile.githubHandle && (
                <a href={`https://github.com/${profile.githubHandle}`} target="_blank" rel="noreferrer" className="platform-badge github text-decoration-none">
                  🐙 GitHub: @{profile.githubHandle}
                </a>
              )}
            </div>
          </div>

          <div className="col-md-auto text-center text-md-end">
            <div className="d-flex flex-column gap-2">
              {(!currentUser || currentUser.toLowerCase() === profile.username.toLowerCase()) && (
                <button className="btn-neon py-2 px-4 fs-7" onClick={openEditModal}>
                  ✏️ Edit Profile
                </button>
              )}
              <button className="btn-outline-neon py-2 px-4 fs-7" onClick={handleCopyLink}>
                {copied ? '✅ Link Copied!' : '🔗 Share Showcase'}
              </button>
              <button className="btn btn-link text-secondary text-decoration-none fs-8 p-0" onClick={handleExportJson}>
                📥 Download JSON
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Edit Profile Modal */}
      {showEditModal && (
        <div className="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" 
             style={{ background: 'rgba(0, 0, 0, 0.75)', backdropFilter: 'blur(8px)', zIndex: 2000 }}>
          <div className="glass-card p-4 p-sm-5 text-start position-relative w-100 mx-3" style={{ maxWidth: '440px' }}>
            <button 
              className="position-absolute top-0 end-0 m-3 border-0 bg-transparent text-secondary fs-4"
              onClick={() => setShowEditModal(false)}
            >
              ✕
            </button>
            <h4 className="fw-bold text-white mb-2">Edit Profile</h4>
            <p className="text-secondary fs-8 mb-4">Update your profile display name, headline bio, and badge title.</p>

            <form onSubmit={handleSaveProfile}>
              <div className="mb-3">
                <label className="form-label fs-7 fw-semibold text-secondary">👤 Profile Display Name *</label>
                <input 
                  type="text" 
                  className="form-input-neon" 
                  placeholder="e.g. Varun Chow" 
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  required
                  autoFocus
                />
                <span className="fs-8 text-secondary">Displayed on navbar and profile instead of username/email</span>
              </div>

              <div className="mb-3">
                <label className="form-label fs-7 fw-semibold text-secondary">💬 Bio / Tagline</label>
                <input 
                  type="text" 
                  className="form-input-neon" 
                  placeholder="e.g. Competitive Programmer | Full-Stack Dev" 
                  value={editBio}
                  onChange={(e) => setEditBio(e.target.value)}
                />
              </div>

              <div className="mb-4">
                <label className="form-label fs-7 fw-semibold text-secondary">🏆 Title / Badge</label>
                <input 
                  type="text" 
                  className="form-input-neon" 
                  placeholder="e.g. Grandmaster, Master, Candidate Master" 
                  value={editTitle}
                  onChange={(e) => setEditTitle(e.target.value)}
                />
              </div>

              <button type="submit" className="btn-neon w-100 py-2 justify-content-center" disabled={saving}>
                {saving ? 'Saving Changes...' : 'Save Profile Changes'}
              </button>
            </form>
          </div>
        </div>
      )}

      {/* Summary KPI Stat Widgets Grid */}
      <div className="row g-3 mb-4">
        <div className="col-6 col-md-3">
          <div className="stat-widget purple">
            <div className="stat-label">Total Solved</div>
            <div className="stat-value">{profile.totalSolved || 840}</div>
            <div className="d-flex gap-1 mt-2">
              <span className="diff-chip easy">E: {profile.easySolved || 320}</span>
              <span className="diff-chip medium">M: {profile.mediumSolved || 410}</span>
              <span className="diff-chip hard">H: {profile.hardSolved || 110}</span>
            </div>
          </div>
        </div>

        <div className="col-6 col-md-3">
          <div className="stat-widget amber">
            <div className="stat-label">Peak Rating</div>
            <div className="stat-value">{profile.maxRating || 1980}</div>
            <div className="text-secondary fs-7 mt-2 fw-semibold">
              Current: {profile.currentRating || 1890}
            </div>
          </div>
        </div>

        <div className="col-6 col-md-3">
          <div className="stat-widget emerald">
            <div className="stat-label">Contests Attended</div>
            <div className="stat-value">{profile.totalContests || 52}</div>
            <div className="text-success fs-7 mt-2 fw-semibold">
              🔥 Active Competitor
            </div>
          </div>
        </div>

        <div className="col-6 col-md-3">
          <div className="stat-widget cyan">
            <div className="stat-label">Global Dekho Score</div>
            <div className="stat-value">{profile.globalScore || 34500}</div>
            <div className="text-info fs-7 mt-2 fw-semibold">
              ⭐ Top 2.5% Ranked
            </div>
          </div>
        </div>
      </div>

      {/* Interactive Charts Section */}
      <ChartsSection profile={profile} />
    </div>
  );
}
