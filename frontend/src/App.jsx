import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import ProtectedLayout from './components/ProtectedLayout';

// Public Pages
import HomePage from './pages/HomePage';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage';
import ResetPasswordPage from './pages/auth/ResetPasswordPage';

// Citizen Pages
import ReportWastePage from './pages/citizen/ReportWastePage';
import MyComplaintsPage from './pages/citizen/MyComplaintsPage';
import ComplaintDetailPage from './pages/citizen/ComplaintDetailPage';

// Admin Pages
import AdminDashboardPage from './pages/admin/AdminDashboardPage';
import AdminComplaintListPage from './pages/admin/AdminComplaintListPage';
import AdminComplaintReviewPage from './pages/admin/AdminComplaintReviewPage';
import AdminMapPage from './pages/admin/AdminMapPage';
import AdminVolunteerListPage from './pages/admin/AdminVolunteerListPage';

// Volunteer Pages
import VolunteerDashboardPage from './pages/volunteer/VolunteerDashboardPage';
import VolunteerTaskListPage from './pages/volunteer/VolunteerTaskListPage';
import VolunteerTaskDetailPage from './pages/volunteer/VolunteerTaskDetailPage';

export default function App() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Navbar />
      <main style={{ flex: 1 }}>
        <Routes>
          {/* Public Routes */}
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />

          {/* Citizen & Public Reporting Protected Routes */}
          <Route element={<ProtectedLayout allowedRoles={['CITIZEN', 'VOLUNTEER', 'ADMIN']} />}>
            <Route path="/citizen/report" element={<ReportWastePage />} />
            <Route path="/citizen/complaints" element={<MyComplaintsPage />} />
            <Route path="/citizen/complaints/:id" element={<ComplaintDetailPage />} />
          </Route>

          {/* Admin Protected Routes */}
          <Route element={<ProtectedLayout allowedRoles={['ADMIN']} />}>
            <Route path="/admin" element={<AdminDashboardPage />} />
            <Route path="/admin/complaints" element={<AdminComplaintListPage />} />
            <Route path="/admin/complaints/:id" element={<AdminComplaintReviewPage />} />
            <Route path="/admin/map" element={<AdminMapPage />} />
            <Route path="/admin/volunteers" element={<AdminVolunteerListPage />} />
          </Route>

          {/* Volunteer Protected Routes */}
          <Route element={<ProtectedLayout allowedRoles={['VOLUNTEER', 'ADMIN']} />}>
            <Route path="/volunteer" element={<VolunteerDashboardPage />} />
            <Route path="/volunteer/tasks" element={<VolunteerTaskListPage />} />
            <Route path="/volunteer/tasks/:id" element={<VolunteerTaskDetailPage />} />
          </Route>

          {/* Fallback route */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <Footer />
    </div>
  );
}
