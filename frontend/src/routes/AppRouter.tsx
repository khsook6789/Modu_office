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

import PostListPage from '../features/community/PostListPage';
import PostFormPage from '../features/community/PostFormPage';
import PostDetailPage from '../features/community/PostDetailPage';

import BookingPage from "../features/booking/BookingPage";
import PaymentPage from "../features/booking/PaymentPage";
import BookingSuccessPage from "../features/booking/BookingSuccessPage";
import MyBookingsPage from "../features/booking/MyBookingsPage";

import OperatorDashboard from "../features/operator/OperatorDashboard";
import AdminDashboard from "../features/admin/AdminDashboard";
import MyPage from "../features/user/MyPage";

// Placeholder Components (to be moved to features later)

function ProtectedRoute({ children, role }: { children: React.ReactNode, role?: 'USER' | 'ADMIN' | 'OPERATOR' }) {
  const { user } = useAuth();

  if (!user) return <Navigate to="/login" replace />;

  if (user.role === 'OPERATOR' && user.id.includes('pending')) {
    // Rely on manual redirection or separate logic
  }

  if (role && (user.role as string) !== role) {
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
            <Route path="/" element={<Navigate to="/rooms" replace />} />
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
            <Route path="/payment" element={
              <ProtectedRoute>
                <PaymentPage />
              </ProtectedRoute>
            } />
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

            {/* Community Routes */}
            <Route path="/community" element={<PostListPage />} />
            <Route path="/community/new" element={
              <ProtectedRoute>
                <PostFormPage />
              </ProtectedRoute>
            } />
            <Route path="/community/:id" element={<PostDetailPage />} />
            <Route path="/community/:id/edit" element={
              <ProtectedRoute>
                <PostFormPage />
              </ProtectedRoute>
            } />

            {/* Operator Routes */}
            <Route path="/operator" element={
              <ProtectedRoute role="OPERATOR">
                <OperatorDashboard />
              </ProtectedRoute>
            } />
            <Route path="/office/new" element={
              <ProtectedRoute role="OPERATOR">
                <OfficeFormPage />
              </ProtectedRoute>
            } />
            <Route path="/office/:id/manage" element={
              <ProtectedRoute role="OPERATOR">
                <RoomManagementPage />
              </ProtectedRoute>
            } />
            <Route path="/office/:officeId/rooms/new" element={
              <ProtectedRoute role="OPERATOR">
                <RoomFormPage />
              </ProtectedRoute>
            } />
            <Route path="/rooms/:roomId/edit" element={
              <ProtectedRoute role="OPERATOR">
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
