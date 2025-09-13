import React from 'react'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import './App.css'

function App() {
  return (
    <Router>
      <div className="App">
        <header className="App-header">
          <h1>Lab Signoff Application</h1>
          <p>Welcome to the Laboratory Signoff System</p>
        </header>
        <main>
          <Routes>
            <Route path="/" element={<div>Dashboard - Coming Soon</div>} />
            <Route path="/labs" element={<div>Labs - Coming Soon</div>} />
            <Route path="/signoffs" element={<div>Signoffs - Coming Soon</div>} />
          </Routes>
        </main>
      </div>
    </Router>
  )
}

export default App