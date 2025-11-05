import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Loading from './Common/Loading';

export function ProtectedRoute({ children }) {
    const { isAuthenticated, loading } = useAuth();

    if (loading) {
        return <Loading fullScreen tip="Đang kiểm tra đăng nhập..." />;
    }

    if (!isAuthenticated()) {
        return <Navigate to="/" replace />;
    }

    return children;
}

export function PublicRoute({ children }) {
    const { isAuthenticated, loading } = useAuth();

    if (loading) {
        return <Loading fullScreen tip="Đang kiểm tra đăng nhập..." />;
    }

    if (isAuthenticated()) {
        return <Navigate to="/dashboard" replace />;
    }

    return children;
}

