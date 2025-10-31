import React, { useState } from 'react';
import { Layout, Avatar, Dropdown, Badge, Drawer, Button } from 'antd';
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

const { Header, Content } = Layout;

const AppLayout = ({ children }) => {
    const navigate = useNavigate();
    const location = useLocation();
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

    const sidebarIcons = [
        { icon: <HomeOutlined />, key: 'overview', active: currentSection === 'overview', label: 'Tổng quan' },
        { icon: <InboxOutlined />, key: 'inventory', active: currentSection === 'inventory', label: 'Quản lý kho' },
        { icon: <WarningOutlined />, key: 'inventory-alerts', active: currentSection === 'inventory-alerts', label: 'Cảnh báo kho' },
        { icon: <HistoryOutlined />, key: 'inventory-transactions', active: currentSection === 'inventory-transactions', label: 'Lịch sử giao dịch' },
        { icon: <MenuOutlined />, key: 'menu', active: currentSection === 'menu', label: 'Thực đơn' },
        { icon: <AppstoreOutlined />, key: 'menu-categories', active: currentSection === 'menu-categories', label: 'Danh mục' },
        { icon: <AppstoreAddOutlined />, key: 'menu-combos', active: currentSection === 'menu-combos', label: 'Combo' },
        { icon: <ShoppingOutlined />, key: 'orders', active: currentSection === 'orders', label: 'Đơn hàng' },
        { icon: <TeamOutlined />, key: 'staff', active: currentSection === 'staff', label: 'Nhân viên' },
        { icon: <SettingOutlined />, key: 'settings', active: currentSection === 'settings', label: 'Cài đặt' },
    ];

    const userMenuItems = [
        { key: 'profile', icon: <UserOutlined />, label: 'Thông tin cá nhân' },
        { key: 'settings', icon: <SettingOutlined />, label: 'Cài đặt' },
        { type: 'divider' },
        { key: 'logout', icon: <LogoutOutlined />, label: 'Đăng xuất' },
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
        // Close mobile menu after navigation
        setMobileMenuVisible(false);
    };

    const handleUserMenuClick = ({ key }) => {
        if (key === 'logout') {
            // TODO: Implement logout when authentication is implemented
            // localStorage.removeItem('authToken');
            // navigate('/login');
            console.log('Logout clicked - Authentication not implemented yet');
        } else {
            navigate(`/${key}`);
        }
    };

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
                    <h1 className="header-title">Trung's Restaurant Management</h1>
                </div>

                <div className="header-right">
                    <div className="user-info-section">
                        <Badge count={3} size="small">
                            <BellOutlined className="notification-icon" />
                        </Badge>
                        <div className="user-info-text">
                            <div className="user-name">Đỗ Đình Trung</div>
                            <div className="user-role">Quản lý nhà hàng</div>
                        </div>
                        <Dropdown
                            menu={{ items: userMenuItems, onClick: handleUserMenuClick }}
                            placement="bottomRight"
                            trigger={['click']}
                        >
                            <Avatar
                                size={40}
                                icon={<UserOutlined />}
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
                        className={`sidebar-icon ${item.active ? 'active' : ''}`}
                        onClick={() => handleIconClick(item.key)}
                        title={item.label}
                    >
                        {item.icon}
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