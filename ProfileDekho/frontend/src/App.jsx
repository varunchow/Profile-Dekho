import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import Hero from './components/Hero';
import AuthModal from './components/AuthModal';
import ProfileForm from './components/ProfileForm';
import ShowcaseView from './components/ShowcaseView';

export default function App() {
  const [activeTab, setActiveTab] = useState('home'); // 'home', 'dashboard', 'showcase'
  const [currentUser, setCurrentUser] = useState(localStorage.getItem('pd_user') || null);
  const [authModal, setAuthModal] = useState({ isOpen: false, mode: 'login' });
  const [activeProfile, setActiveProfile] = useState(null);
  const [alertMsg, setAlertMsg] = useState(null);

  // Default seed profile data for instant preview
  const defaultProfiles = {
    tourist: {
      username: 'tourist',
      name: 'Gennady Korotkevich (tourist)',
      title: 'Grandmaster',
      bio: 'Competitive Programming Legend | #1 World Rated',
      totalSolved: 1450,
      easySolved: 300,
      mediumSolved: 650,
      hardSolved: 500,
      totalContests: 142,
      maxRating: 3858,
      currentRating: 3820,
      globalScore: 98500,
      leetcodeHandle: 'tourist',
      codeforcesHandle: 'tourist',
      codechefHandle: 'tourist',
      hackerrankHandle: 'tourist',
      gfgHandle: 'tourist',
      githubHandle: 'tourist',
      leetcodeStats: { solved: 450, easy: 100, medium: 200, hard: 150, rating: 2850 },
      codeforcesStats: { solved: 850, easy: 100, medium: 350, hard: 400, rating: 3820, rankName: 'Legendary Grandmaster' },
      codechefStats: { solved: 150, rating: 2950, stars: '7★' },
      hackerrankStats: { solved: 100, stars: 6 },
      gfgStats: { solved: 200, codingScore: 1400 },
      githubStats: { publicRepos: 18, stars: 450 },
      topicScores: {
        "Data Structures": 99,
        "Dynamic Programming": 98,
        "Algorithms": 100,
        "Graphs & Trees": 97,
        "Math & Bitmask": 99,
        "System Design": 88
      },
      ratingHistory: [
        { month: 'Jan', codeforces: 3600, leetcode: 2700 },
        { month: 'Feb', codeforces: 3680, leetcode: 2750 },
        { month: 'Mar', codeforces: 3720, leetcode: 2790 },
        { month: 'Apr', codeforces: 3790, leetcode: 2810 },
        { month: 'May', codeforces: 3840, leetcode: 2830 },
        { month: 'Jun', codeforces: 3820, leetcode: 2850 }
      ]
    },
    neal_wu: {
      username: 'neal_wu',
      name: 'Neal Wu',
      title: 'Grandmaster',
      bio: 'Software Engineer & Competitive Programmer | IOI Gold Medalist',
      totalSolved: 1120,
      easySolved: 350,
      mediumSolved: 520,
      hardSolved: 250,
      totalContests: 98,
      maxRating: 2950,
      currentRating: 2880,
      globalScore: 76000,
      leetcodeHandle: 'neal_wu',
      codeforcesHandle: 'neal_wu',
      codechefHandle: 'neal_wu',
      hackerrankHandle: 'neal_wu',
      gfgHandle: 'neal_wu',
      githubHandle: 'neal_wu',
      leetcodeStats: { solved: 520, easy: 150, medium: 250, hard: 120, rating: 2650 },
      codeforcesStats: { solved: 600, easy: 200, medium: 270, hard: 130, rating: 2880, rankName: 'International Grandmaster' },
      codechefStats: { solved: 180, rating: 2650, stars: '6★' },
      hackerrankStats: { solved: 110, stars: 6 },
      gfgStats: { solved: 160, codingScore: 1100 },
      githubStats: { publicRepos: 32, stars: 210 },
      topicScores: {
        "Data Structures": 95,
        "Dynamic Programming": 94,
        "Algorithms": 96,
        "Graphs & Trees": 93,
        "Math & Bitmask": 91,
        "System Design": 85
      },
      ratingHistory: [
        { month: 'Jan', codeforces: 2750, leetcode: 2500 },
        { month: 'Feb', codeforces: 2790, leetcode: 2540 },
        { month: 'Mar', codeforces: 2810, leetcode: 2580 },
        { month: 'Apr', codeforces: 2850, leetcode: 2610 },
        { month: 'May', codeforces: 2890, leetcode: 2640 },
        { month: 'Jun', codeforces: 2880, leetcode: 2650 }
      ]
    }
  };

  useEffect(() => {
    // Initial profile load
    setActiveProfile(defaultProfiles.neal_wu);
  }, []);

  const handleSearch = async (username) => {
    const key = username.toLowerCase();
    if (defaultProfiles[key]) {
      setActiveProfile(defaultProfiles[key]);
      setActiveTab('showcase');
      return;
    }

    try {
      const res = await fetch(`/api/profiles/${encodeURIComponent(username)}`);
      if (res.ok) {
        const data = await res.json();
        setActiveProfile(data);
        setActiveTab('showcase');
      } else {
        throw new Error("Profile not found");
      }
    } catch (e) {
      // Generate fallback profile dynamically
      const dynamicProfile = {
        username: username,
        name: username.toUpperCase(),
        title: 'Candidate Master',
        bio: `Competitive Coder @${username} | ProfileDekho`,
        totalSolved: 720,
        easySolved: 290,
        mediumSolved: 340,
        hardSolved: 90,
        totalContests: 38,
        maxRating: 1820,
        currentRating: 1760,
        globalScore: 29800,
        leetcodeHandle: username,
        codeforcesHandle: username,
        codechefHandle: username,
        hackerrankHandle: username,
        gfgHandle: username,
        githubHandle: username,
        leetcodeStats: { solved: 320, easy: 140, medium: 140, hard: 40, rating: 1760 },
        codeforcesStats: { solved: 400, easy: 150, medium: 200, hard: 50, rating: 1690, rankName: 'Expert' },
        topicScores: {
          "Data Structures": 84,
          "Dynamic Programming": 79,
          "Algorithms": 88,
          "Graphs & Trees": 81,
          "Math & Bitmask": 76,
          "System Design": 70
        },
        ratingHistory: [
          { month: 'Jan', codeforces: 1480, leetcode: 1550 },
          { month: 'Feb', codeforces: 1540, leetcode: 1610 },
          { month: 'Mar', codeforces: 1590, leetcode: 1660 },
          { month: 'Apr', codeforces: 1630, leetcode: 1700 },
          { month: 'May', codeforces: 1710, leetcode: 1730 },
          { month: 'Jun', codeforces: 1690, leetcode: 1760 }
        ]
      };
      setActiveProfile(dynamicProfile);
      setActiveTab('showcase');
    }
  };

  const handleLoginSuccess = (username) => {
    setCurrentUser(username);
    localStorage.setItem('pd_user', username);
    showAlert(`Logged in as @${username}!`);
    setActiveTab('dashboard');
  };

  const handleLogout = () => {
    setCurrentUser(null);
    localStorage.removeItem('pd_user');
    showAlert('Logged out successfully.');
    setActiveTab('home');
  };

  const showAlert = (msg) => {
    setAlertMsg(msg);
    setTimeout(() => setAlertMsg(null), 3500);
  };

  return (
    <div className="min-vh-100 d-flex flex-column">
      <Navbar 
        activeTab={activeTab} 
        setActiveTab={setActiveTab} 
        currentUser={currentUser}
        onLogout={handleLogout}
        onSearch={handleSearch}
        openAuth={(mode) => setAuthModal({ isOpen: true, mode })}
      />

      {alertMsg && (
        <div className="alert alert-info border-0 rounded-0 text-center py-2 mb-0 fw-bold" style={{ background: 'linear-gradient(90deg, #6366f1, #a855f7)', color: 'white' }}>
          {alertMsg}
        </div>
      )}

      <main className="flex-grow-1">
        {activeTab === 'home' && (
          <>
            <Hero onSearch={handleSearch} onDemoClick={handleSearch} />
            <ShowcaseView profile={activeProfile || defaultProfiles.neal_wu} />
          </>
        )}

        {activeTab === 'dashboard' && (
          <ProfileForm 
            currentUser={currentUser}
            initialData={activeProfile}
            onSyncSuccess={(updatedProfile) => {
              setActiveProfile(updatedProfile);
              showAlert('Profiles synchronized successfully!');
              setActiveTab('showcase');
            }}
          />
        )}

        {activeTab === 'showcase' && (
          <ShowcaseView profile={activeProfile || defaultProfiles.tourist} />
        )}
      </main>

      <AuthModal 
        isOpen={authModal.isOpen}
        mode={authModal.mode}
        onClose={() => setAuthModal({ ...authModal, isOpen: false })}
        onAuthSuccess={handleLoginSuccess}
      />

      <footer className="glass-nav mt-5 py-4 text-center text-secondary fs-7">
        <div className="container">
          <p className="mb-1">
            <strong>ProfileDekho</strong> • Built with Spring Boot (Java) & React
          </p>
          <p className="mb-0 text-muted">
            Aggregating LeetCode, CodeChef, CodeForces, HackerRank, GeeksforGeeks & GitHub stats into recruiter-ready portfolios.
          </p>
        </div>
      </footer>
    </div>
  );
}
