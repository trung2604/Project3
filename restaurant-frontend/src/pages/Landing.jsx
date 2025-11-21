import React, { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { IDP, RESTAURANT_INFO } from "../constants";
import { useAuth } from "../context/AuthContext";
import { getRedirectPathByRole } from "../utils/auth";
import {
  buildKeycloakAuthUrl,
  buildKeycloakRegisterUrl,
} from "../utils/keycloak";

export default function Landing() {
  const navigate = useNavigate();
  const { isAuthenticated, loading, role } = useAuth();
  const redirectUri = `${window.location.origin}${IDP.CALLBACK_PATH}`;
  const loginUrl = buildKeycloakAuthUrl(redirectUri, true);
  const registerUrl = buildKeycloakRegisterUrl(redirectUri);

  // Redirect if already logged in
  useEffect(() => {
    if (!loading && isAuthenticated() && role) {
      const redirectPath = getRedirectPathByRole(role);
      navigate(redirectPath, { replace: true });
    }
  }, [loading, isAuthenticated, role, navigate]);

  const features = [
    {
      icon: "📊",
      title: "Quản lý Menu",
      description:
        "Quản lý món ăn, danh mục và combo một cách dễ dàng và trực quan",
    },
    {
      icon: "📦",
      title: "Quản lý Kho",
      description: "Theo dõi tồn kho, nhập xuất và cảnh báo hết hàng tự động",
    },
    {
      icon: "📝",
      title: "Quản lý Đơn hàng",
      description: "Xử lý đơn hàng nhanh chóng, theo dõi trạng thái real-time",
    },
    {
      icon: "👥",
      title: "Quản lý Nhân viên",
      description: "Quản lý nhân sự, phân quyền và theo dõi hiệu suất làm việc",
    },
    {
      icon: "📈",
      title: "Báo cáo & Thống kê",
      description: "Dashboard trực quan với các chỉ số kinh doanh quan trọng",
    },
    {
      icon: "🔔",
      title: "Thông báo Thông minh",
      description: "Nhận thông báo kịp thời về đơn hàng, kho hàng và hoạt động",
    },
  ];

  return (
    <div className="landing-page">
      {/* Hero Section */}
      <section className="hero-section">
        <div className="hero-background">
          <div className="hero-overlay"></div>
        </div>

        {/* Main Content - Centered */}
        <div className="hero-content">
          {/* Logo - Prominent */}
          <div className="hero-logo">
            <div className="logo-container">
              <img
                src="/LogoRestaurant.png"
                alt="Trung's Restaurant Logo"
                className="hero-logo-img"
              />
            </div>
          </div>

          <h1 className="hero-title">{RESTAURANT_INFO.name}</h1>
          <p className="hero-subtitle">
            Hệ thống quản lý nhà hàng chuyên nghiệp
          </p>
          <p className="hero-description">{RESTAURANT_INFO.description}</p>

          <div className="hero-actions">
            <a href={loginUrl} className="btn btn-primary">
              <span>Đăng nhập</span>
              <svg
                width="20"
                height="20"
                viewBox="0 0 20 20"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  d="M7.5 15L12.5 10L7.5 5"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </a>
            <a href={registerUrl} className="btn btn-secondary">
              <span>Tạo tài khoản</span>
            </a>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="features-section">
        <div className="container">
          <div className="section-header">
            <h2 className="section-title">Tính năng nổi bật</h2>
            <p className="section-subtitle">
              Giải pháp quản lý toàn diện cho nhà hàng của bạn
            </p>
          </div>

          <div className="features-grid">
            {features.map((feature, index) => (
              <div key={index} className="feature-card">
                <div className="feature-icon">{feature.icon}</div>
                <h3 className="feature-title">{feature.title}</h3>
                <p className="feature-description">{feature.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="cta-section">
        <div className="container">
          <div className="cta-content">
            <h2 className="cta-title">Sẵn sàng bắt đầu?</h2>
            <p className="cta-description">
              Tham gia cùng hàng trăm nhà hàng đã tin tưởng sử dụng hệ thống của
              chúng tôi
            </p>
            <div className="cta-actions">
              <a href={loginUrl} className="btn btn-primary btn-large">
                <span>Đăng nhập ngay</span>
                <svg
                  width="20"
                  height="20"
                  viewBox="0 0 20 20"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path
                    d="M7.5 15L12.5 10L7.5 5"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              </a>
              <a href={registerUrl} className="btn btn-outline btn-large">
                <span>Tạo tài khoản miễn phí</span>
              </a>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="landing-footer">
        <div className="container">
          <div className="footer-content">
            <div className="footer-info">
              <h3 className="footer-title">{RESTAURANT_INFO.name}</h3>
              <p className="footer-description">
                {RESTAURANT_INFO.description}
              </p>
            </div>
            <div className="footer-contact">
              <p className="footer-item">
                <strong>Chủ sở hữu:</strong> {RESTAURANT_INFO.owner}
              </p>
              <p className="footer-item">
                <strong>Email:</strong> {RESTAURANT_INFO.email}
              </p>
              <p className="footer-item">
                <strong>Điện thoại:</strong> {RESTAURANT_INFO.phone}
              </p>
              <p className="footer-item">
                <strong>Địa chỉ:</strong> {RESTAURANT_INFO.address}
              </p>
            </div>
          </div>
          <div className="footer-bottom">
            <p>
              &copy; {new Date().getFullYear()} {RESTAURANT_INFO.name}. Tất cả
              quyền được bảo lưu.
            </p>
          </div>
        </div>
      </footer>

      <style>{`
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }

                body {
                    margin: 0;
                    padding: 0;
                    overflow-x: hidden;
                }

                .landing-page {
                    width: 100%;
                    min-height: 100vh;
                    overflow-x: hidden;
                    margin: 0;
                    padding: 0;
                }

                /* ============================================
                   Hero Section
                   ============================================ */
                .hero-section {
                    position: relative;
                    min-height: 100vh;
                    display: flex;
                    flex-direction: column;
                    padding: 0;
                    margin: 0;
                    overflow: hidden;
                }

                .hero-background {
                    position: absolute;
                    inset: 0;
                    background: 
                        linear-gradient(135deg, rgba(249, 115, 22, 0.85) 0%, rgba(220, 38, 38, 0.8) 100%),
                        url('https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1920&q=80')
                        center/cover no-repeat;
                    z-index: 0;
                }

                .hero-overlay {
                    position: absolute;
                    inset: 0;
                    background: linear-gradient(180deg, rgba(0, 0, 0, 0.2) 0%, rgba(0, 0, 0, 0.4) 100%);
                    z-index: 1;
                }

                /* ============================================
                   Main Content - Centered
                   ============================================ */
                .hero-content {
                    position: relative;
                    z-index: 2;
                    flex: 1;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    text-align: center;
                    max-width: 800px;
                    width: 100%;
                    margin: 0 auto;
                    padding: 2rem;
                }

                /* ============================================
                   Hero Logo - Prominent & Attractive
                   ============================================ */
                .hero-logo {
                    margin-bottom: 2rem;
                    animation: fadeInDown 0.8s ease-out;
                }

                .logo-container {
                    width: 180px;
                    height: 180px;
                    background: rgba(255, 255, 255, 0.95);
                    border-radius: 32px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    padding: 12px;
                    box-shadow: 
                        0 20px 60px rgba(0, 0, 0, 0.3),
                        0 0 0 4px rgba(255, 255, 255, 0.2),
                        inset 0 0 20px rgba(249, 115, 22, 0.1);
                    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
                    position: relative;
                    overflow: hidden;
                }

                .logo-container::before {
                    content: '';
                    position: absolute;
                    inset: -2px;
                    border-radius: 28px;
                    padding: 2px;
                    background: linear-gradient(135deg, rgba(249, 115, 22, 0.3), rgba(220, 38, 38, 0.3));
                    -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
                    -webkit-mask-composite: xor;
                    mask-composite: exclude;
                    opacity: 0;
                    transition: opacity 0.4s ease;
                }

                .logo-container:hover {
                    transform: translateY(-8px) scale(1.05);
                    box-shadow: 
                        0 30px 80px rgba(0, 0, 0, 0.4),
                        0 0 0 4px rgba(249, 115, 22, 0.3),
                        inset 0 0 30px rgba(249, 115, 22, 0.15);
                }

                .logo-container:hover::before {
                    opacity: 1;
                }

                .hero-logo-img {
                    width: 100%;
                    height: 100%;
                    object-fit: contain;
                    filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.2));
                    transition: transform 0.4s ease;
                    transform: scale(1.1);
                }

                .logo-container:hover .hero-logo-img {
                    transform: scale(1.1);
                }

                .hero-title {
                    font-size: clamp(2.5rem, 5vw, 4rem);
                    font-weight: 700;
                    color: #ffffff;
                    margin: 0 0 1rem;
                    text-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
                    letter-spacing: -0.02em;
                    line-height: 1.2;
                }

                .hero-subtitle {
                    font-size: clamp(1.125rem, 2.5vw, 1.5rem);
                    color: rgba(255, 255, 255, 0.95);
                    margin: 0 0 0.75rem;
                    font-weight: 500;
                    line-height: 1.4;
                }

                .hero-description {
                    font-size: clamp(1rem, 2vw, 1.125rem);
                    color: rgba(255, 255, 255, 0.9);
                    margin: 0 0 2.5rem;
                    line-height: 1.6;
                    max-width: 600px;
                }

                /* ============================================
                   Buttons
                   ============================================ */
                .hero-actions {
                    display: flex;
                    gap: 1rem;
                    justify-content: center;
                    flex-wrap: wrap;
                }

                .btn {
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    gap: 0.5rem;
                    padding: 0.875rem 2rem;
                    font-size: 1.125rem;
                    font-weight: 600;
                    text-decoration: none;
                    border-radius: 12px;
                    transition: all 0.3s ease;
                    border: none;
                    cursor: pointer;
                    white-space: nowrap;
                }

                .btn-primary {
                    background: #ffffff;
                    color: #f97316;
                    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
                }

                .btn-primary:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.3);
                    background: #fef3f2;
                }

                .btn-secondary {
                    background: rgba(255, 255, 255, 0.15);
                    color: #ffffff;
                    border: 2px solid rgba(255, 255, 255, 0.3);
                    backdrop-filter: blur(10px);
                }

                .btn-secondary:hover {
                    background: rgba(255, 255, 255, 0.25);
                    border-color: rgba(255, 255, 255, 0.5);
                    transform: translateY(-2px);
                }

                .btn-outline {
                    background: transparent;
                    color: #ffffff;
                    border: 2px solid rgba(255, 255, 255, 0.3);
                }

                .btn-outline:hover {
                    background: rgba(255, 255, 255, 0.1);
                    border-color: rgba(255, 255, 255, 0.5);
                }

                .btn-large {
                    padding: 1.125rem 2.5rem;
                    font-size: 1.25rem;
                }

                /* ============================================
                   Features Section
                   ============================================ */
                .features-section {
                    padding: 5rem 2rem;
                    background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
                }

                .container {
                    max-width: 1200px;
                    margin: 0 auto;
                }

                .section-header {
                    text-align: center;
                    margin-bottom: 3.5rem;
                }

                .section-title {
                    font-size: clamp(2rem, 4vw, 2.75rem);
                    font-weight: 700;
                    color: #0f172a;
                    margin: 0 0 1rem;
                    letter-spacing: -0.02em;
                    line-height: 1.2;
                }

                .section-subtitle {
                    font-size: 1.125rem;
                    color: #64748b;
                    margin: 0;
                    max-width: 600px;
                    margin-left: auto;
                    margin-right: auto;
                    line-height: 1.6;
                }

                .features-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
                    gap: 2rem;
                }

                .feature-card {
                    background: #ffffff;
                    border-radius: 16px;
                    padding: 2rem;
                    text-align: center;
                    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
                    transition: all 0.3s ease;
                    border: 1px solid #f1f5f9;
                }

                .feature-card:hover {
                    transform: translateY(-4px);
                    box-shadow: 0 12px 32px rgba(249, 115, 22, 0.12);
                    border-color: rgba(249, 115, 22, 0.2);
                }

                .feature-icon {
                    font-size: 3rem;
                    margin-bottom: 1.25rem;
                    display: block;
                }

                .feature-title {
                    font-size: 1.375rem;
                    font-weight: 600;
                    color: #0f172a;
                    margin: 0 0 0.75rem;
                    line-height: 1.3;
                }

                .feature-description {
                    font-size: 0.95rem;
                    color: #64748b;
                    margin: 0;
                    line-height: 1.6;
                }

                /* ============================================
                   CTA Section
                   ============================================ */
                .cta-section {
                    padding: 5rem 2rem;
                    background: linear-gradient(135deg, #f97316 0%, #ea580c 100%);
                    position: relative;
                    overflow: hidden;
                }

                .cta-content {
                    position: relative;
                    z-index: 1;
                    text-align: center;
                    max-width: 700px;
                    margin: 0 auto;
                }

                .cta-title {
                    font-size: clamp(2rem, 4vw, 2.75rem);
                    font-weight: 700;
                    color: #ffffff;
                    margin: 0 0 1rem;
                    text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
                    line-height: 1.2;
                }

                .cta-description {
                    font-size: 1.125rem;
                    color: rgba(255, 255, 255, 0.95);
                    margin: 0 0 2.5rem;
                    line-height: 1.6;
                }

                .cta-actions {
                    display: flex;
                    gap: 1rem;
                    justify-content: center;
                    flex-wrap: wrap;
                }

                .cta-section .btn-primary {
                    background: #ffffff;
                    color: #f97316;
                }

                .cta-section .btn-primary:hover {
                    background: #fef3f2;
                }

                .cta-section .btn-outline {
                    color: #ffffff;
                    border-color: rgba(255, 255, 255, 0.5);
                }

                .cta-section .btn-outline:hover {
                    background: rgba(255, 255, 255, 0.1);
                    border-color: rgba(255, 255, 255, 0.8);
                }

                /* ============================================
                   Footer
                   ============================================ */
                .landing-footer {
                    background: #0f172a;
                    color: #94a3b8;
                    padding: 3.5rem 2rem 1.5rem;
                }

                .footer-content {
                    max-width: 1200px;
                    margin: 0 auto;
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                    gap: 2.5rem;
                    margin-bottom: 2.5rem;
                }

                .footer-title {
                    font-size: 1.375rem;
                    font-weight: 700;
                    color: #ffffff;
                    margin: 0 0 0.75rem;
                }

                .footer-description {
                    font-size: 0.95rem;
                    line-height: 1.6;
                    margin: 0;
                }

                .footer-item {
                    font-size: 0.9rem;
                    margin: 0.5rem 0;
                    line-height: 1.6;
                }

                .footer-item strong {
                    color: #ffffff;
                    font-weight: 600;
                }

                .footer-bottom {
                    max-width: 1200px;
                    margin: 0 auto;
                    padding-top: 1.5rem;
                    border-top: 1px solid rgba(255, 255, 255, 0.1);
                    text-align: center;
                    font-size: 0.875rem;
                }

                /* ============================================
                   Responsive Design
                   ============================================ */
                @media (max-width: 768px) {
                    .hero-content {
                        padding: 1.5rem;
                    }

                    .logo-container {
                        width: 150px;
                        height: 150px;
                        border-radius: 28px;
                        padding: 10px;
                    }

                    .hero-logo-img {
                        transform: scale(1.15);
                    }

                    .features-section,
                    .cta-section {
                        padding: 4rem 1.5rem;
                    }

                    .features-grid {
                        grid-template-columns: 1fr;
                        gap: 1.5rem;
                    }

                    .feature-card {
                        padding: 1.75rem;
                    }

                    .hero-actions,
                    .cta-actions {
                        flex-direction: column;
                        width: 100%;
                    }

                    .hero-actions .btn,
                    .cta-actions .btn {
                        width: 100%;
                    }

                    .footer-content {
                        grid-template-columns: 1fr;
                        gap: 2rem;
                    }
                }

                @media (max-width: 480px) {
                    .hero-title {
                        font-size: 2rem;
                    }

                    .hero-subtitle {
                        font-size: 1rem;
                    }

                    .section-title,
                    .cta-title {
                        font-size: 1.75rem;
                    }
                }

                /* ============================================
                   Animations
                   ============================================ */
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
            `}</style>
    </div>
  );
}
