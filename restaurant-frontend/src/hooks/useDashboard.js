import { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { dashboardService } from '../services/dashboardService';

export const useCurrentSection = () => {
    const location = useLocation();

    const getCurrentSection = () => {
        const path = location.pathname;
        if (path.includes('inventory')) return 'inventory';
        if (path.includes('menu')) return 'menu';
        if (path.includes('orders')) return 'orders';
        if (path.includes('staff')) return 'staff';
        if (path.includes('settings')) return 'settings';
        return 'overview';
    };

    return getCurrentSection();
};

export const useDashboardStats = () => {
    const [stats, setStats] = useState({
        totalIngredients: 0,
        lowStockItems: 0,
        totalMenuItems: 0,
        activeMenuItems: 0,
        monthlyRevenue: 0,
        alertCount: 0
    });

    const [loading, setLoading] = useState(false);

    const loadStats = async () => {
        setLoading(true);
        try {
            const [statsResponse, revenueResponse, lowStockResponse] = await Promise.all([
                dashboardService.getStats(),
                dashboardService.getRevenue(),
                dashboardService.getLowStockIngredients()
            ]);

            const statsData = statsResponse.data;
            const revenueData = revenueResponse.data;
            const lowStockData = lowStockResponse.data || [];

            setStats({
                totalIngredients: statsData.totalIngredients || 0,
                lowStockItems: lowStockData.length || 0,
                totalMenuItems: statsData.totalMenuItems || 0,
                activeMenuItems: statsData.activeMenuItems || 0,
                monthlyRevenue: revenueData.monthlyRevenue || 0,
                alertCount: statsData.alertCount || 0
            });
        } catch (error) {
            console.error('Error loading dashboard stats:', error);
            // Fallback to default values on error
            setStats({
                totalIngredients: 0,
                lowStockItems: 0,
                totalMenuItems: 0,
                activeMenuItems: 0,
                monthlyRevenue: 0,
                alertCount: 0
            });
        } finally {
            setLoading(false);
        }
    };

    const refreshStats = async () => {
        await loadStats();
    };

    useEffect(() => {
        loadStats();
    }, []);

    return { stats, loading, refreshStats };
};

export const useAlerts = () => {
    const [alerts, setAlerts] = useState([]);
    const [loading, setLoading] = useState(false);

    const loadAlerts = async () => {
        setLoading(true);
        try {
            const response = await dashboardService.getActiveAlerts();
            const alertsData = response.data || [];

            // Transform API data to match component expectations
            const transformedAlerts = alertsData.map(alert => ({
                id: alert.id,
                type: alert.alertType?.toLowerCase() || 'info',
                message: alert.message,
                severity: alert.severity?.toLowerCase() || 'warning',
                time: formatTimeAgo(alert.createdAt),
                ingredient: alert.ingredientName,
                currentStock: alert.currentStock ? `${alert.currentStock}${alert.unit || ''}` : null,
                minStock: alert.minStock ? `${alert.minStock}${alert.unit || ''}` : null,
                expiryDate: alert.expiryDate ? formatDate(alert.expiryDate) : null
            }));

            setAlerts(transformedAlerts);
        } catch (error) {
            console.error('Error loading alerts:', error);
            // Fallback to empty array on error
            setAlerts([]);
        } finally {
            setLoading(false);
        }
    };

    const handleAlert = async (alert) => {
        try {
            // TODO: Implement alert handling API call
            console.log('Handling alert:', alert);
            // After handling, refresh alerts
            await loadAlerts();
        } catch (error) {
            console.error('Error handling alert:', error);
        }
    };

    const refreshAlerts = async () => {
        await loadAlerts();
    };

    useEffect(() => {
        loadAlerts();
    }, []);

    return { alerts, loading, handleAlert, refreshAlerts };
};

// Helper functions
const formatTimeAgo = (dateString) => {
    if (!dateString) return 'Không xác định';

    const now = new Date();
    const date = new Date(dateString);
    const diffInMinutes = Math.floor((now - date) / (1000 * 60));

    if (diffInMinutes < 60) {
        return `${diffInMinutes} phút trước`;
    } else if (diffInMinutes < 1440) {
        return `${Math.floor(diffInMinutes / 60)} giờ trước`;
    } else {
        return `${Math.floor(diffInMinutes / 1440)} ngày trước`;
    }
};

const formatDate = (dateString) => {
    if (!dateString) return null;

    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN');
};
