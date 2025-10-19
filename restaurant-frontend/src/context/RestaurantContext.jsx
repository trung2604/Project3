import React, { createContext, useContext, useState, useEffect } from 'react';

const RestaurantContext = createContext();

export const useRestaurant = () => {
    const context = useContext(RestaurantContext);
    if (!context) {
        throw new Error('useRestaurant must be used within a RestaurantProvider');
    }
    return context;
};

export const RestaurantProvider = ({ children }) => {
    const [user, setUser] = useState({
        name: 'Đỗ Đình Trung',
        role: 'Quản lý nhà hàng',
        avatar: null
    });

    const [notifications, setNotifications] = useState([
        { id: 1, message: 'Cà chua sắp hết hàng', type: 'warning', read: false },
        { id: 2, message: 'Đơn hàng mới #1234', type: 'info', read: false },
        { id: 3, message: 'Báo cáo tháng đã sẵn sàng', type: 'success', read: false }
    ]);

    const [theme, setTheme] = useState({
        primaryColor: '#f59e0b',
        sidebarCollapsed: false,
        darkMode: false
    });

    const updateUser = (userData) => {
        setUser(prev => ({ ...prev, ...userData }));
    };

    const addNotification = (notification) => {
        setNotifications(prev => [...prev, { ...notification, id: Date.now() }]);
    };

    const markNotificationAsRead = (id) => {
        setNotifications(prev =>
            prev.map(notif =>
                notif.id === id ? { ...notif, read: true } : notif
            )
        );
    };

    const updateTheme = (themeData) => {
        setTheme(prev => ({ ...prev, ...themeData }));
    };

    const value = {
        user,
        notifications,
        theme,
        updateUser,
        addNotification,
        markNotificationAsRead,
        updateTheme
    };

    return (
        <RestaurantContext.Provider value={value}>
            {children}
        </RestaurantContext.Provider>
    );
};
