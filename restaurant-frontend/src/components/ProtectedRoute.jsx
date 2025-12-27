import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import Loading from "./Common/Loading";

export function ProtectedRoute({ children }) {
  let authContext;
  try {
    authContext = useAuth();
  } catch (error) {
    // If AuthContext is not available, show loading
    console.error("AuthContext error:", error);
    return <Loading fullScreen tip="Đang khởi tạo..." />;
  }

  const { isAuthenticated, loading } = authContext;

  if (loading) {
    return <Loading fullScreen tip="Đang kiểm tra đăng nhập..." />;
  }

  if (!isAuthenticated()) {
    return <Navigate to="/" replace />;
  }

  return children;
}

export function PublicRoute({ children }) {
  let authContext;
  try {
    authContext = useAuth();
  } catch (error) {
    // If AuthContext is not available, show loading
    console.error("AuthContext error:", error);
    return <Loading fullScreen tip="Đang khởi tạo..." />;
  }

  const { isAuthenticated, loading } = authContext;

  if (loading) {
    return <Loading fullScreen tip="Đang kiểm tra đăng nhập..." />;
  }

  if (isAuthenticated()) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
}
