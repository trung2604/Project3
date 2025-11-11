import React, { useEffect, useState, useRef } from 'react';
import apiService from '../services/apiService';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { getRedirectPathByRole } from '../utils/auth';
import { getErrorMessage, isTokenError } from '../utils/errorHandler';
import { buildKeycloakAuthUrl } from '../utils/keycloak';

export default function AuthCallback() {
    const [message, setMessage] = useState('Đang xử lý đăng nhập...');
    const [error, setError] = useState(null);
    const { login, loadUser } = useAuth();
    const navigate = useNavigate();
    const processedRef = useRef(false);

    useEffect(() => {
        // Prevent multiple executions
        if (processedRef.current) return;

        const params = new URLSearchParams(window.location.search);
        const code = params.get('code');
        if (!code) {
            setMessage('Thiếu mã xác thực (code)');
            return;
        }

        processedRef.current = true;
        const redirectUri = `${window.location.origin}/auth/callback`;

        (async () => {
            try {
                const data = await apiService.user.exchangeToken(code, redirectUri);
                if (data?.accessToken) {
                    // Login via context
                    login(data.accessToken, data.refreshToken, null);

                    // Load user from API - this will update role in context and return user data
                    const userData = await loadUser();

                    // Get role from returned user data (most reliable)
                    const userRole = userData?.role;

                    // Redirect based on role
                    const redirectPath = getRedirectPathByRole(userRole);
                    navigate(redirectPath, { replace: true });
                } else {
                    setMessage('Không nhận được accessToken');
                }
            } catch (e) {
                setError(e);
                const errorMessage = getErrorMessage(e);
                setMessage(errorMessage);
                processedRef.current = false; // Allow retry on error
            }
        })();
    }, [login, loadUser, navigate]);

    const showRetryButton = error && (isTokenError(error) || message.includes('hết hạn') || message.includes('không hợp lệ'));

    return (
        <div style={{ display: 'grid', placeItems: 'center', minHeight: '100vh', padding: '20px' }}>
            <div style={{ textAlign: 'center' }}>
                <div style={{ marginBottom: '16px', fontSize: '16px' }}>{message}</div>
                {showRetryButton && (
                    <a
                        href={buildKeycloakAuthUrl(`${window.location.origin}/auth/callback`)}
                        style={{
                            display: 'inline-block',
                            padding: '8px 16px',
                            background: '#f59e0b',
                            color: 'white',
                            borderRadius: '8px',
                            textDecoration: 'none',
                            fontWeight: 500
                        }}
                    >
                        Đăng nhập lại
                    </a>
                )}
            </div>
        </div>
    );
}


