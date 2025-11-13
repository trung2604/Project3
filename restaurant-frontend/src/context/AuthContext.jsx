import React, { createContext, useContext, useState, useEffect } from 'react';
import apiService from '../services/apiService';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [role, setRole] = useState(null);
    const [loading, setLoading] = useState(true);

    // Load user on mount
    useEffect(() => {
        loadUser();
    }, []);

    // Listen for storage events (when token is cleared by interceptor or other tabs)
    useEffect(() => {
        const handleStorageChange = (e) => {
            // Check if accessToken was removed
            if (e.key === 'accessToken') {
                if (!localStorage.getItem('accessToken')) {
                    // Token was cleared - reset auth state
                    setUser(null);
                    setRole(null);
                    setLoading(false);
                } else if (e.newValue) {
                    // Token was added/updated - reload user
                    loadUser();
                }
            }
        };

        // Listen for storage events (works for cross-tab communication)
        // For same-tab changes, api.js dispatches a StorageEvent manually
        window.addEventListener('storage', handleStorageChange);

        return () => {
            window.removeEventListener('storage', handleStorageChange);
        };
    }, []);

    const loadUser = async () => {
        const token = localStorage.getItem('accessToken');
        const storedUser = apiService.user.getStoredUser();

        if (!token) {
            setLoading(false);
            return null;
        }

        // Fetch user data from API (backend will decode JWT and return user info)
        try {
            const userData = await apiService.user.getMe();
            if (userData) {
                setUser(userData);
                // Set role from user data returned by backend
                setRole(userData.role || null);
                localStorage.setItem('user', JSON.stringify(userData));
                return userData;
            } else if (storedUser) {
                // Fallback to stored user
                setUser(storedUser);
                setRole(storedUser.role || null);
                return storedUser;
            }
        } catch (e) {
            console.warn('Failed to fetch user:', e);
            // Fallback to stored user
            if (storedUser) {
                setUser(storedUser);
                setRole(storedUser.role || null);
                return storedUser;
            }
        } finally {
            setLoading(false);
        }
        return null;
    };

    const login = (token, refreshToken, userData) => {
        localStorage.setItem('accessToken', token);
        if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
        if (userData) {
            localStorage.setItem('user', JSON.stringify(userData));
            setUser(userData);
            setRole(userData.role || null);
        }
    };

    const logout = () => {
        // Clear all authentication data
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');

        // Reset state
        setUser(null);
        setRole(null);
        setLoading(false);

        // Note: With stateless JWT, clearing client-side tokens is sufficient
        // However, to prevent Keycloak SSO from auto-logging in, we should
        // redirect to Keycloak logout endpoint to clear the session
        // This will be handled by AppLayout after logout completes
    };

    const updateUser = (userData) => {
        setUser(userData);
        setRole(userData?.role || null);
        localStorage.setItem('user', JSON.stringify(userData));
    };

    const isAuthenticated = () => {
        const token = localStorage.getItem('accessToken');
        return !!token;
    };

    const value = {
        user,
        role,
        loading,
        login,
        logout,
        updateUser,
        loadUser,
        isAuthenticated
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within AuthProvider');
    }
    return context;
}

