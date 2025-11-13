import React, { useState, useEffect } from 'react';
import { Layout, Avatar, Dropdown, Badge, Drawer, Button, App } from 'antd';
import {
    HomeOutlined,
    InboxOutlined,
    MenuOutlined,
    ShoppingOutlined,
    TeamOutlined,
    SettingOutlined,
    UserOutlined,
    LogoutOutlined,
    BellOutlined,
    MenuFoldOutlined,
    WarningOutlined,
    HistoryOutlined,
    AppstoreOutlined,
    AppstoreAddOutlined
} from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import { RESTAURANT_INFO } from '../../constants.js';
import { getMenuItemsByRole } from '../../utils/auth';
import { useAuth } from '../../context/AuthContext';
import { buildKeycloakLogoutUrl } from '../../utils/keycloak';
import { IDP } from '../../constants';
import NotificationDropdown from '../Common/NotificationDropdown';

const { Header, Content } = Layout;

const iconMap = {
    home: <HomeOutlined />,
    menu: <MenuOutlined />,
    shopping: <ShoppingOutlined />,
    inbox: <InboxOutlined />,
    warning: <WarningOutlined />,
    history: <HistoryOutlined />,
    appstore: <AppstoreOutlined />,
    'appstore-add': <AppstoreAddOutlined />,
    team: <TeamOutlined />,
    setting: <SettingOutlined />,
};

const AppLayout = ({ children }) => {
    const navigate = useNavigate();
    const location = useLocation();
    const { user, role, logout } = useAuth();
    const { modal } = App.useApp();
    const [mobileMenuVisible, setMobileMenuVisible] = useState(false);

    // Get current section from URL or default to overview
    const getCurrentSection = () => {
        const path = location.pathname;
        if (path.includes('inventory/alerts')) return 'inventory-alerts';
        if (path.includes('inventory/transactions')) return 'inventory-transactions';
        if (path.includes('inventory')) return 'inventory';
        if (path.includes('menu/categories')) return 'menu-categories';
        if (path.includes('menu/combos')) return 'menu-combos';
        if (path.includes('menu')) return 'menu';
        if (path.includes('orders')) return 'orders';
        if (path.includes('staff')) return 'staff';
        if (path.includes('settings')) return 'settings';
        return 'overview';
    };

    const currentSection = getCurrentSection();

    // Get page title based on current section
    const getPageTitle = () => {
        const pageTitleMap = {
            'overview': 'Dashboard Tổng Quan',
            'menu': 'Quản lý Thực đơn',
            'menu-categories': 'Quản lý Danh mục',
            'menu-combos': 'Quản lý Combo',
            'orders': 'Quản lý Đơn hàng',
            'inventory': 'Quản lý Kho',
            'inventory-alerts': 'Cảnh báo Kho',
            'inventory-transactions': 'Lịch sử Giao dịch',
            'staff': 'Quản lý Nhân viên',
            'settings': 'Cài đặt Hệ thống'
        };
        return pageTitleMap[currentSection] || 'Dashboard';
    };

    const pageTitle = getPageTitle();

    // Get menu items based on user role
    const menuItems = getMenuItemsByRole(role || 'CUSTOMER');
    const sidebarIcons = menuItems.map(item => ({
        icon: iconMap[item.icon] || <HomeOutlined />,
        key: item.key,
        active: currentSection === item.key,
        label: item.label
    }));

    const userMenuItems = [
        { key: 'profile', icon: <UserOutlined />, label: 'Thông tin cá nhân' },
        { type: 'divider' },
        { key: 'logout', icon: <LogoutOutlined />, label: 'Đăng xuất', danger: true },
    ];

    const handleIconClick = (key) => {
        if (key === 'overview') {
            navigate('/dashboard');
        } else if (key === 'inventory-alerts') {
            navigate('/dashboard/inventory/alerts');
        } else if (key === 'inventory-transactions') {
            navigate('/dashboard/inventory/transactions');
        } else if (key === 'menu-categories') {
            navigate('/dashboard/menu/categories');
        } else if (key === 'menu-combos') {
            navigate('/dashboard/menu/combos');
        } else {
            navigate(`/dashboard/${key}`);
        }
        setMobileMenuVisible(false);
    };

    const handleUserMenuClick = ({ key }) => {
        if (key === 'logout') {
            modal.confirm({
                title: 'Xác nhận đăng xuất',
                content: 'Bạn có chắc chắn muốn đăng xuất không?',
                okText: 'Đăng xuất',
                cancelText: 'Hủy',
                okType: 'danger',
                onOk: () => {
                    logout();
                    // Redirect to Keycloak logout to clear session, then redirect back to landing page
                    const logoutRedirectUri = `${window.location.origin}/`;
                    const keycloakLogoutUrl = buildKeycloakLogoutUrl(logoutRedirectUri);
                    window.location.href = keycloakLogoutUrl;
                }
            });
        } else if (key === 'profile') {
            navigate('/dashboard/profile');
        }
    };

    const getRoleLabel = (role) => {
        const roleMap = {
            'ADMIN': 'Quản trị viên',
            'RESTAURANT_MANAGER': 'Quản lý nhà hàng',
            'WAREHOUSE_STAFF': 'Nhân viên kho',
            'STAFF': 'Nhân viên',
            'CUSTOMER': 'Khách hàng'
        };
        return roleMap[role] || 'Khách hàng';
    };

    const userName = user?.firstName && user?.lastName
        ? `${user.firstName} ${user.lastName}`
        : user?.username || 'User';

    return (
        <Layout>
            <Header className="modern-header">
                <div className="header-left">
                    {/* Mobile Menu Button */}
                    <Button
                        type="text"
                        icon={<MenuFoldOutlined />}
                        onClick={() => setMobileMenuVisible(true)}
                        className="mobile-menu-button"
                        style={{
                            display: 'none',
                            fontSize: '18px',
                            color: '#262626',
                            marginRight: '12px'
                        }}
                    />

                    <img
                        src="/LogoRestaurant.png"
                        alt="Restaurant Logo"
                        className="header-logo"
                        style={{ width: '80px', height: '80px', marginRight: '20px' }}
                    />
                    <div className="header-title-section">
                        <h1 className="header-title">Trung's Restaurant Management</h1>
                        <div className="header-page-title">{pageTitle}</div>
                    </div>
                </div>

                <div className="header-right">
                    <div className="user-info-section">
                        <NotificationDropdown />
                        <div className="user-info-text">
                            <div className="user-name">{userName}</div>
                            <div className="user-role">{getRoleLabel(role)}</div>
                        </div>
                        <Dropdown
                            menu={{ items: userMenuItems, onClick: handleUserMenuClick }}
                            placement="bottomRight"
                            trigger={['click']}
                        >
                            <Avatar
                                size={40}
                                src={user?.avatarUrl}
                                icon={!user?.avatarUrl && <UserOutlined />}
                                className="user-avatar"
                                style={{ backgroundColor: '#ff6b35', cursor: 'pointer' }}
                            />
                        </Dropdown>
                    </div>
                </div>
            </Header>

            <div className="modern-sidebar">
                {sidebarIcons.map((item, index) => (
                    <div
                        key={index}
                        className={`sidebar-item ${item.active ? 'active' : ''}`}
                        onClick={() => handleIconClick(item.key)}
                        title={item.label}
                    >
                        <div className="sidebar-icon-wrapper">
                            {item.active && <div className="sidebar-indicator" />}
                            <div className="sidebar-icon">
                                {item.icon}
                            </div>
                        </div>
                        <span className="sidebar-label">{item.label}</span>
                    </div>
                ))}
            </div>

            <Content className="main-content">
                {children}
            </Content>

            {/* Mobile Navigation Drawer */}
            <Drawer
                title="Menu"
                placement="left"
                onClose={() => setMobileMenuVisible(false)}
                open={mobileMenuVisible}
                width={280}
                className="mobile-navigation-drawer"
            >
                <div className="mobile-sidebar-content">
                    {sidebarIcons.map((item, index) => (
                        <div
                            key={index}
                            className={`mobile-sidebar-item ${item.active ? 'active' : ''}`}
                            onClick={() => handleIconClick(item.key)}
                        >
                            <span className="mobile-sidebar-icon">{item.icon}</span>
                            <span className="mobile-sidebar-label">{item.label}</span>
                        </div>
                    ))}
                </div>
            </Drawer>
        </Layout>
    );
};

export default AppLayout;