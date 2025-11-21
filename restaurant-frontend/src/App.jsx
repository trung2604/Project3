import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider, App as AntApp } from 'antd';
import viVN from 'antd/locale/vi_VN';
import AppLayout from './components/Layout/AppLayout';
import Landing from './pages/Landing';
import Profile from './pages/Profile';
import AuthCallback from './pages/AuthCallback';
import { ProtectedRoute, PublicRoute } from './components/ProtectedRoute';
import ErrorPage from './components/Common/ErrorPage';
import Dashboard from './pages/Dashboard';
import InventoryManagement from './pages/InventoryManagement';
import InventoryAlerts from './pages/InventoryAlerts';
import InventoryTransactions from './pages/InventoryTransactions';
import MenuManagement from './pages/MenuManagement';
import MenuCategories from './pages/MenuCategories';
import MenuCombos from './pages/MenuCombos';
import Staff from './pages/Staff';
import Notifications from './pages/Notifications';
import OrderManagement from './pages/OrderManagement';
import { RestaurantProvider } from './context/RestaurantContext';
import { AuthProvider } from './context/AuthContext';

// Ant Design theme configuration
const theme = {
    token: {
        colorPrimary: '#f59e0b',
        colorSuccess: '#52c41a',
        colorWarning: '#faad14',
        colorError: '#ff4d4f',
        colorInfo: '#1890ff',
        borderRadius: 8,
        fontFamily: 'Inter, system-ui, sans-serif',
    },
    components: {
        Layout: {
            headerBg: 'linear-gradient(135deg, #f59e0b 0%, #dc2626 100%)',
            siderBg: '#ffffff',
        },
        Menu: {
            itemBg: 'transparent',
            itemSelectedBg: '#f0f9ff',
            itemHoverBg: '#f0f9ff',
        },
        Card: {
            borderRadiusLG: 12,
        },
        Button: {
            borderRadius: 8,
        },
        Table: {
            headerBg: '#f8fafc',
            headerColor: '#374151',
        },
    },
};

function App() {
    return (
        <AuthProvider>
            <RestaurantProvider>
                <ConfigProvider theme={theme} locale={viVN}>
                    <AntApp>
                        <Router>
                            <div className="App">
                                <Routes>
                                    <Route path="/" element={
                                        <PublicRoute>
                                            <Landing />
                                        </PublicRoute>
                                    } />
                                    <Route path="/auth/callback" element={<AuthCallback />} />
                                    <Route path="/dashboard/*" element={
                                        <ProtectedRoute>
                                            <AppLayout>
                                                <Routes>
                                                    <Route path="/" element={<Dashboard />} />
                                                    <Route path="/inventory" element={<InventoryManagement />} />
                                                    <Route path="/inventory/alerts" element={<InventoryAlerts />} />
                                                    <Route path="/inventory/transactions" element={<InventoryTransactions />} />
                                                    <Route path="/menu" element={<Dashboard />} />
                                                    {/* <Route path="/menu/categories" element={<MenuCategories />} /> */}
                                                    {/* <Route path="/menu/combos" element={<MenuCombos />} /> */}
                                                    <Route path="/orders" element={<OrderManagement />} />
                                                    <Route path="/staff" element={<Staff />} />
                                                    <Route path="/settings" element={<Dashboard />} />
                                                    <Route path="/profile" element={<Profile />} />
                                                    <Route path="/notifications" element={<Notifications />} />
                                                </Routes>
                                            </AppLayout>
                                        </ProtectedRoute>
                                    } />
                                    <Route path="*" element={<ErrorPage status={404} />} />
                                </Routes>
                            </div>
                        </Router>
                    </AntApp>
                </ConfigProvider>
            </RestaurantProvider>
        </AuthProvider>
    );
}

export default App;
