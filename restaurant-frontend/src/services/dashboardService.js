import apiClient from './api';
import { API_ENDPOINTS } from '../constants.js';

// Dashboard Service API
export const dashboardService = {
    // Get dashboard statistics
    getStats: async () => {
        try {
            // Try to aggregate data from other services
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
            // Return fallback data if all services fail
            return {
                data: {
                    totalIngredients: 0,
                    totalMenuItems: 0,
                    alertCount: 0
                }
            };
        }
    },

    // Get revenue data
    getRevenue: async (period = 'month') => {
        try {
            // Since there's no dashboard service, return mock data
            return {
                data: {
                    monthlyRevenue: 125000000,
                    dailyRevenue: 4000000,
                    weeklyRevenue: 28000000
                }
            };
        } catch (error) {
            console.error('Error getting revenue data:', error);
            // Return fallback data
            return {
                data: {
                    monthlyRevenue: 0,
                    dailyRevenue: 0,
                    weeklyRevenue: 0
                }
            };
        }
    },

    // Get low stock ingredients
    getLowStockIngredients: async () => {
        try {
            return await apiClient.get(API_ENDPOINTS.INVENTORY.LOW_STOCK);
        } catch (error) {
            console.error('Error getting low stock ingredients:', error);
            // Return empty array if service is unavailable
            return { data: [] };
        }
    },

    // Get active alerts
    getActiveAlerts: async () => {
        try {
            return await apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/active`);
        } catch (error) {
            console.error('Error getting active alerts:', error);
            // Return empty array if service is unavailable
            return { data: [] };
        }
    }
};

export default dashboardService;
