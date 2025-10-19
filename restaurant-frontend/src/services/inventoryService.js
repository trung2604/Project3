import apiClient from './api';
import { API_ENDPOINTS } from '../constants.js';

// Inventory Service API
export const inventoryService = {
    // Ingredient Management
    getIngredients: (params = {}) => {
        return apiClient.get(API_ENDPOINTS.INVENTORY.INGREDIENTS, { params });
    },

    getIngredientById: (id) => {
        return apiClient.get(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}`);
    },

    createIngredient: (data) => {
        return apiClient.post(API_ENDPOINTS.INVENTORY.INGREDIENTS, data);
    },

    updateIngredient: (id, data) => {
        return apiClient.put(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}`, data);
    },

    deleteIngredient: (id) => {
        return apiClient.delete(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}`);
    },

    toggleIngredientActive: (id, active) => {
        return apiClient.put(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/toggle`, null, {
            params: { active }
        });
    },

    // Stock Operations
    stockIn: (id, data) => {
        return apiClient.post(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/stock-in`, data);
    },

    stockOut: (id, data) => {
        return apiClient.post(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/stock-out`, data);
    },

    adjustStock: (id, data) => {
        return apiClient.post(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/adjust`, data);
    },

    stockTake: (id, data) => {
        return apiClient.post(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/stock-take`, data);
    },

    // Stock Transactions
    getTransactions: (params = {}) => {
        return apiClient.get(API_ENDPOINTS.INVENTORY.TRANSACTIONS, { params });
    },

    getTransactionsByIngredient: (id, params = {}) => {
        return apiClient.get(`${API_ENDPOINTS.INVENTORY.TRANSACTIONS}/ingredient/${id}`, { params });
    },

    getTransactionsByType: (type, params = {}) => {
        return apiClient.get(`${API_ENDPOINTS.INVENTORY.TRANSACTIONS}/type/${type}`, { params });
    },

    // Stock Alerts
    getAlerts: (params = {}) => {
        return apiClient.get(API_ENDPOINTS.INVENTORY.ALERTS, { params });
    },

    getActiveAlerts: () => {
        return apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/active`);
    },

    getLowStockAlerts: () => {
        return apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/low-stock`);
    },

    getExpiryAlerts: () => {
        return apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/expiry`);
    },

    getCriticalAlerts: () => {
        return apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/critical`);
    },

    // Advanced Queries
    getLowStockIngredients: () => {
        return apiClient.get(API_ENDPOINTS.INVENTORY.LOW_STOCK);
    },

    getIngredientsByCategory: (category) => {
        return apiClient.get(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/category/${category}`);
    }
};
