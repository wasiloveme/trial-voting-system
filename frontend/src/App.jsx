import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';

// These are placeholders until we build the real files
const Login = () => <div><h1>This is the Login Page</h1></div>;
const Dashboard = () => <div><h1>Welcome to the Voting Dashboard</h1></div>;

function App() {
  return (
    <Router>
      <Routes>
        {/* If the URL is /login, show the Login component */}
        <Route path="/login" element={<Login />} />

        {/* If the URL is /dashboard, show the Dashboard component */}
        <Route path="/dashboard" element={<Dashboard />} />

        {/* Default: If someone just goes to /, send them to /login */}
        <Route path="/" element={<Navigate to="/login" />} />
      </Routes>
    </Router>
  );
}

export default App;