import apiClient from './api';
import { API_ENDPOINTS } from '../constants.js';

export const dashboardService = {
    getStats: async () => {
        try {
            const [ingredientsResponse, menuResponse, alertsResponse] = await Promise.allSettled([
                apiClient.get(API_ENDPOINTS.INVENTORY.INGREDIENTS, { params: { page: 0, size: 1 } }),
                apiClient.get(API_ENDPOINTS.MENU.ITEMS, { params: { page: 0, size: 1 } }),
                apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/active`)
            ]);

            return {
                data: {
                    totalIngredients: ingredientsResponse.status === 'fulfilled' ? (ingredientsResponse.value.data.totalElements || 0) : 0,
                    totalMenuItems: menuResponse.status === 'fulfilled' ? (menuResponse.value.data.totalElements || 0) : 0,
                    alertCount: alertsResponse.status === 'fulfilled' ? (alertsResponse.value.data?.length || 0) : 0
                }
            };
        } catch (error) {
            console.error('Error getting dashboard stats:', error);
            return {
                data: {
                    totalIngredients: 0,
                    totalMenuItems: 0,
                    alertCount: 0
                }
            };
        }
    },

    getRevenue: async (period = 'month') => {
        try {
            // TODO: Implement real revenue API when available
            // For now, return zero values since there's no revenue service
            return {
                data: {
                    monthlyRevenue: 0,
                    dailyRevenue: 0,
                    weeklyRevenue: 0
                }
            };
        } catch (error) {
            console.error('Error getting revenue data:', error);
            return {
                data: {
                    monthlyRevenue: 0,
                    dailyRevenue: 0,
                    weeklyRevenue: 0
                }
            };
        }
    },

    getLowStockIngredients: async () => {
        try {
            return await apiClient.get(API_ENDPOINTS.INVENTORY.LOW_STOCK);
        } catch (error) {
            console.error('Error getting low stock ingredients:', error);
            return { data: [] };
        }
    },

    getActiveAlerts: async () => {
        try {
            return await apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/active`);
        } catch (error) {
            console.error('Error getting active alerts:', error);
            return { data: [] };
        }
    }
};

export default dashboardService;
