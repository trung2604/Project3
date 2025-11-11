import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { IDP, RESTAURANT_INFO } from '../constants';
import { useAuth } from '../context/AuthContext';
import { getRedirectPathByRole } from '../utils/auth';
import { buildKeycloakAuthUrl, buildKeycloakRegisterUrl } from '../utils/keycloak';

export default function Landing() {
    const navigate = useNavigate();
    const { isAuthenticated, loading, role } = useAuth();
    const redirectUri = `${window.location.origin}${IDP.CALLBACK_PATH}`;
    // Force login prompt to prevent SSO auto-login after logout
    const loginUrl = buildKeycloakAuthUrl(redirectUri, true);
    const registerUrl = buildKeycloakRegisterUrl(redirectUri);

    // Redirect if already logged in
    useEffect(() => {
        if (!loading && isAuthenticated() && role) {
            // Redirect based on role
            const redirectPath = getRedirectPathByRole(role);
            navigate(redirectPath, { replace: true });
        }
    }, [loading, isAuthenticated, role, navigate]);

    return (
        <div style={{
            minHeight: '100vh',
            background: 'linear-gradient(135deg, #f59e0b 0%, #dc2626 100%)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '2rem',
            position: 'relative',
            overflow: 'hidden'
        }}>
            {/* Decorative elements */}
            <div style={{
                position: 'absolute',
                top: '-50%',
                right: '-50%',
                width: '800px',
                height: '800px',
                background: 'rgba(255, 255, 255, 0.05)',
                borderRadius: '50%',
                pointerEvents: 'none'
            }} />
            <div style={{
                position: 'absolute',
                bottom: '-30%',
                left: '-30%',
                width: '600px',
                height: '600px',
                background: 'rgba(255, 255, 255, 0.03)',
                borderRadius: '50%',
                pointerEvents: 'none'
            }} />

            {/* Main Content */}
            <div style={{
                position: 'relative',
                zIndex: 1,
                textAlign: 'center',
                maxWidth: '800px',
                width: '100%'
            }}>
                {/* Logo */}
                <div style={{
                    marginBottom: '3rem',
                    animation: 'fadeInDown 0.8s ease-out'
                }}>
                    <div style={{
                        width: '120px',
                        height: '120px',
                        borderRadius: '50%',
                        background: 'white',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        margin: '0 auto 1.5rem',
                        fontSize: '3rem',
                        boxShadow: '0 10px 30px rgba(0, 0, 0, 0.2)'
                    }}>
                        🍽️
                    </div>
                    <h1 style={{
                        margin: 0,
                        color: 'white',
                        fontSize: '3rem',
                        fontWeight: 700,
                        textShadow: '0 2px 10px rgba(0, 0, 0, 0.2)',
                        marginBottom: '0.5rem'
                    }}>
                        {RESTAURANT_INFO.name}
                    </h1>
                    <p style={{
                        margin: 0,
                        color: 'rgba(255, 255, 255, 0.9)',
                        fontSize: '1.25rem',
                        fontWeight: 400
                    }}>
                        Hệ thống quản lý nhà hàng chuyên nghiệp
                    </p>
                </div>

                {/* Description */}
                <div style={{
                    background: 'rgba(255, 255, 255, 0.95)',
                    borderRadius: '16px',
                    padding: '2rem',
                    marginBottom: '2rem',
                    boxShadow: '0 10px 40px rgba(0, 0, 0, 0.2)',
                    animation: 'fadeInUp 0.8s ease-out 0.2s both'
                }}>
                    <p style={{
                        color: '#374151',
                        fontSize: '1.125rem',
                        lineHeight: '1.8',
                        maxWidth: '600px',
                        margin: '0 auto'
                    }}>
                        {RESTAURANT_INFO.description}
                    </p>
                </div>

                {/* Action Buttons */}
                <div style={{
                    display: 'flex',
                    gap: '1rem',
                    justifyContent: 'center',
                    flexWrap: 'wrap',
                    animation: 'fadeInUp 0.8s ease-out 0.4s both'
                }}>
                    <a
                        href={loginUrl}
                        style={{
                            display: 'inline-block',
                            padding: '1rem 2.5rem',
                            background: 'white',
                            color: '#f59e0b',
                            borderRadius: '8px',
                            textDecoration: 'none',
                            fontWeight: 600,
                            fontSize: '1.125rem',
                            transition: 'all 0.3s',
                            boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
                            minWidth: '200px'
                        }}
                        onMouseEnter={(e) => {
                            e.currentTarget.style.transform = 'translateY(-3px)';
                            e.currentTarget.style.boxShadow = '0 6px 20px rgba(0, 0, 0, 0.2)';
                        }}
                        onMouseLeave={(e) => {
                            e.currentTarget.style.transform = 'translateY(0)';
                            e.currentTarget.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.15)';
                        }}
                    >
                        Đăng nhập
                    </a>
                    <a
                        href={registerUrl}
                        style={{
                            display: 'inline-block',
                            padding: '1rem 2.5rem',
                            background: 'rgba(255, 255, 255, 0.2)',
                            color: 'white',
                            borderRadius: '8px',
                            textDecoration: 'none',
                            fontWeight: 600,
                            fontSize: '1.125rem',
                            transition: 'all 0.3s',
                            border: '2px solid white',
                            boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
                            minWidth: '200px'
                        }}
                        onMouseEnter={(e) => {
                            e.currentTarget.style.background = 'rgba(255, 255, 255, 0.3)';
                            e.currentTarget.style.transform = 'translateY(-3px)';
                            e.currentTarget.style.boxShadow = '0 6px 20px rgba(0, 0, 0, 0.2)';
                        }}
                        onMouseLeave={(e) => {
                            e.currentTarget.style.background = 'rgba(255, 255, 255, 0.2)';
                            e.currentTarget.style.transform = 'translateY(0)';
                            e.currentTarget.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.15)';
                        }}
                    >
                        Đăng ký
                    </a>
                </div>

                {/* Features */}
                <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                    gap: '1.5rem',
                    marginTop: '4rem',
                    animation: 'fadeInUp 0.8s ease-out 0.6s both'
                }}>
                    {[
                        { icon: '📊', title: 'Quản lý Menu', desc: 'Quản lý món ăn và danh mục' },
                        { icon: '📦', title: 'Quản lý Kho', desc: 'Theo dõi tồn kho và nhập xuất' },
                        { icon: '📝', title: 'Quản lý Đơn hàng', desc: 'Xử lý đơn hàng hiệu quả' },
                        { icon: '👥', title: 'Quản lý Nhân viên', desc: 'Quản lý nhân sự và phân quyền' }
                    ].map((feature, idx) => (
                        <div
                            key={idx}
                            style={{
                                background: 'rgba(255, 255, 255, 0.1)',
                                borderRadius: '12px',
                                padding: '1.5rem',
                                backdropFilter: 'blur(10px)',
                                border: '1px solid rgba(255, 255, 255, 0.2)',
                                transition: 'all 0.3s'
                            }}
                            onMouseEnter={(e) => {
                                e.currentTarget.style.background = 'rgba(255, 255, 255, 0.15)';
                                e.currentTarget.style.transform = 'translateY(-5px)';
                            }}
                            onMouseLeave={(e) => {
                                e.currentTarget.style.background = 'rgba(255, 255, 255, 0.1)';
                                e.currentTarget.style.transform = 'translateY(0)';
                            }}
                        >
                            <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>{feature.icon}</div>
                            <h3 style={{ margin: '0 0 0.5rem', color: 'white', fontSize: '1rem', fontWeight: 600 }}>
                                {feature.title}
                            </h3>
                            <p style={{ margin: 0, color: 'rgba(255, 255, 255, 0.8)', fontSize: '0.875rem' }}>
                                {feature.desc}
                            </p>
                        </div>
                    ))}
                </div>
            </div>

            {/* CSS Animations */}
            <style>{`
                @keyframes fadeInDown {
                    from {
                        opacity: 0;
                        transform: translateY(-30px);
                    }
                    to {
                        opacity: 1;
                        transform: translateY(0);
                    }
                }
                @keyframes fadeInUp {
                    from {
                        opacity: 0;
                        transform: translateY(30px);
                    }
                    to {
                        opacity: 1;
                        transform: translateY(0);
                    }
                }
            `}</style>
        </div>
    );
}

