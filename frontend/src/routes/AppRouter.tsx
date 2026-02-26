import { Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider, useAuth } from "../contexts/AuthContext";
import { RoomProvider } from "../contexts/RoomContext";
import MainLayout from "../layouts/MainLayout";
import AuthLayout from "../layouts/AuthLayout";

import LoginPage from "../features/auth/LoginPage";
import SignupPage from "../features/auth/SignupPage";
import AdminSignupPage from "../features/auth/AdminSignupPage";
import PendingApprovalPage from "../features/auth/PendingApprovalPage";

import RoomsListPage from "../features/rooms/RoomsListPage";
import RoomDetailPage from "../features/rooms/RoomDetailPage";

import OfficeFormPage from "../features/office/OfficeFormPage";
import RoomManagementPage from "../features/rooms/RoomManagementPage";
import RoomFormPage from "../features/rooms/RoomFormPage";

import BookingPage from "../features/booking/BookingPage";
import BookingSuccessPage from "../features/booking/BookingSuccessPage";
import MyBookingsPage from "../features/booking/MyBookingsPage";

import OperatorDashboard from "../features/operator/OperatorDashboard";
import AdminDashboard from "../features/admin/AdminDashboard";
import MyPage from "../features/user/MyPage";
import LandingPage from "../features/landing/LandingPage";

function ProtectedRoute({ children, role }: { children: React.ReactNode, role?: 'USER' | 'ADMIN' | 'MANAGER' }) {
  const { user } = useAuth();

  if (!user) return <Navigate to="/login" replace />;

  if (role && user.role !== role) {
    if (user.role === 'ADMIN') return children;
    return <Navigate to="/" replace />;
  }

  return children;
}

export default function AppRouter() {
  return (
    <AuthProvider>
      <RoomProvider>
        <Routes>
          <Route element={<AuthLayout />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/signup" element={<SignupPage />} />
            <Route path="/admin/signup" element={<AdminSignupPage />} />
            <Route path="/pending-approval" element={<PendingApprovalPage />} />
          </Route>

          <Route element={<MainLayout />}>
            <Route path="/intro" element={<LandingPage />} />
            {/* Redirect / to /intro as default public landing */}
            <Route path="/" element={<Navigate to="/intro" replace />} />
            
            <Route path="/rooms" element={
              <ProtectedRoute>
                <RoomsListPage />
              </ProtectedRoute>
            } />
            <Route path="/rooms/:id" element={
              <ProtectedRoute>
                <RoomDetailPage />
              </ProtectedRoute>
            } />

            {/* Booking Routes */}
            <Route path="/rooms/:roomId/book" element={
              <ProtectedRoute>
                <BookingPage />
              </ProtectedRoute>
            } />
            {/* PaymentPage Removed */}
            <Route path="/booking/success/:id" element={
              <ProtectedRoute>
                <BookingSuccessPage />
              </ProtectedRoute>
            } />
            <Route path="/my-bookings" element={
              <ProtectedRoute>
                <MyBookingsPage />
              </ProtectedRoute>
            } />

            <Route path="/mypage" element={
              <ProtectedRoute>
                <MyPage />
              </ProtectedRoute>
            } />

            {/* Community Routes Removed */}

            {/* Operator Routes */}
            <Route path="/operator" element={
              <ProtectedRoute role="MANAGER">
                <OperatorDashboard />
              </ProtectedRoute>
            } />
            <Route path="/office/new" element={
              <ProtectedRoute role="MANAGER">
                <OfficeFormPage />
              </ProtectedRoute>
            } />
            <Route path="/office/:id/edit" element={
              <ProtectedRoute role="MANAGER">
                <OfficeFormPage />
              </ProtectedRoute>
            } />
            <Route path="/office/:id/manage" element={
              <ProtectedRoute role="MANAGER">
                <RoomManagementPage />
              </ProtectedRoute>
            } />
            <Route path="/office/:officeId/rooms/new" element={
              <ProtectedRoute role="MANAGER">
                <RoomFormPage />
              </ProtectedRoute>
            } />
            <Route path="/rooms/:roomId/edit" element={
              <ProtectedRoute role="MANAGER">
                <RoomFormPage />
              </ProtectedRoute>
            } />

            <Route path="/admin" element={
              <ProtectedRoute role="ADMIN">
                <AdminDashboard />
              </ProtectedRoute>
            } />
          </Route>
        </Routes>
      </RoomProvider>
    </AuthProvider>
  );
}
