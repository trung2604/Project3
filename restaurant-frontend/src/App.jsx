import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider, App as AntApp } from 'antd';
import viVN from 'antd/locale/vi_VN';
import AppLayout from './components/Layout/AppLayout';
import Dashboard from './pages/Dashboard';
import { RestaurantProvider } from './context/RestaurantContext';

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
        <RestaurantProvider>
            <ConfigProvider theme={theme} locale={viVN}>
                <AntApp>
                    <Router>
                        <div className="App">
                            <Routes>
                                <Route path="/" element={<Navigate to="/dashboard" replace />} />
                                <Route path="/dashboard/*" element={
                                    <AppLayout>
                                        <Routes>
                                            <Route path="/" element={<Dashboard />} />
                                            <Route path="/inventory" element={<Dashboard />} />
                                            <Route path="/menu" element={<Dashboard />} />
                                            <Route path="/orders" element={<Dashboard />} />
                                            <Route path="/staff" element={<Dashboard />} />
                                            <Route path="/settings" element={<Dashboard />} />
                                        </Routes>
                                    </AppLayout>
                                } />
                                <Route path="*" element={<Navigate to="/dashboard" replace />} />
                            </Routes>
                        </div>
                    </Router>
                </AntApp>
            </ConfigProvider>
        </RestaurantProvider>
    );
}

export default App;
