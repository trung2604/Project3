import apiClient from './api';
import { API_ENDPOINTS, API_BASE_URL } from '../constants.js';
import axios from 'axios';
import notificationAPI from './notificationService';
import orderAPI from './orderService';

const USERS_BASE = '/api/users';

const userAPI = {
    async login({ username, password }) {
        const data = await apiClient.post(`${USERS_BASE}/login`, { username, password });
        if (data?.accessToken) {
            localStorage.setItem('accessToken', data.accessToken);
            localStorage.setItem('refreshToken', data.refreshToken || '');
            localStorage.setItem('user', JSON.stringify(data.user));
        }
        return data;
    },

    async register(payload) {
        return await apiClient.post(`${USERS_BASE}/register`, payload);
    },

    async exchangeToken(code, redirectUri) {
        return await apiClient.post(`${USERS_BASE}/oauth/token-exchange`, { code, redirectUri });
    },

    getStoredUser() {
        const raw = localStorage.getItem('user');
        try {
            return raw ? JSON.parse(raw) : null;
        } catch {
            return null;
        }
    },

    async getById(userId) {
        return await apiClient.get(`${USERS_BASE}/${userId}`);
    },

    async getMe() {
        return await apiClient.get(`${USERS_BASE}/me`);
    },

    async updateMe(payload) {
        const data = await apiClient.put(`${USERS_BASE}/me`, payload);
        if (data) localStorage.setItem('user', JSON.stringify(data));
        return data;
    },

    async changeMyPassword({ currentPassword, newPassword }) {
        return await apiClient.patch(`${USERS_BASE}/me/password`, { currentPassword, newPassword });
    },

    async updateAvatar(userId, { avatarUrl, avatarPublicId }) {
        const data = await apiClient.patch(`${USERS_BASE}/${userId}/avatar`, { avatarUrl, avatarPublicId });
        if (data) localStorage.setItem('user', JSON.stringify(data));
        return data;
    },

    async createUser(payload) {
        return await apiClient.post(`${USERS_BASE}`, payload);
    },

    async getAllUsers(params = {}) {
        const queryParams = new URLSearchParams();
        if (params.page !== undefined) queryParams.append('page', params.page);
        if (params.size !== undefined) queryParams.append('size', params.size);
        if (params.search) queryParams.append('search', params.search);
        if (params.role) queryParams.append('role', params.role);
        if (params.status) queryParams.append('status', params.status);

        const queryString = queryParams.toString();
        const url = queryString ? `${USERS_BASE}?${queryString}` : USERS_BASE;
        return await apiClient.get(url);
    },

    async deleteUser(userId) {
        return await apiClient.delete(`${USERS_BASE}/${userId}`);
    },

    async toggleUserStatus(userId, status) {
        return await apiClient.patch(`${USERS_BASE}/${userId}/status?status=${status}`);
    }
};

const menuAPI = {
    async getMenuItems(params = {}) {
        // Sanitize query params
        const cleanedEntries = Object.entries(params)
            .filter(([_, v]) => v !== undefined && v !== null && v !== '');

        const normalized = Object.fromEntries(
            cleanedEntries.map(([k, v]) => {
                if (k === 'page' || k === 'size') {
                    const num = typeof v === 'string' ? parseInt(v, 10) : v;
                    return [k, Number.isNaN(num) ? undefined : num];
                }
                if (k === 'categoryId') {
                    const maybeNum = typeof v === 'string' && /^\d+$/.test(v) ? parseInt(v, 10) : v;
                    return [k, maybeNum];
                }
                if (k === 'active') {
                    return [k, v === true || v === false ? v : undefined];
                }
                if (k === 'search') {
                    return [k, typeof v === 'string' && v.trim().length > 0 ? v.trim() : undefined];
                }
                return [k, v];
            })
        );

        const queryParams = Object.fromEntries(
            Object.entries(normalized).filter(([_, v]) => v !== undefined)
        );

        return await apiClient.get(API_ENDPOINTS.MENU.ITEMS, { params: queryParams });
    },

    async getMenuItemById(id) {
        return await apiClient.get(`${API_ENDPOINTS.MENU.ITEMS}/${id}`);
    },

    async createMenuItem(data) {
        return await apiClient.post(API_ENDPOINTS.MENU.ITEMS, data);
    },

    async updateMenuItem(id, data) {
        return await apiClient.put(`${API_ENDPOINTS.MENU.ITEMS}/${id}`, data);
    },

    async deleteMenuItem(id) {
        return await apiClient.delete(`${API_ENDPOINTS.MENU.ITEMS}/${id}`);
    },

    async toggleMenuItemActive(id, active) {
        return await apiClient.patch(`${API_ENDPOINTS.MENU.ITEMS}/${id}/active`, null, {
            params: { active }
        });
    },

    async updateMenuItemIngredients(id, ingredients) {
        return await apiClient.patch(`${API_ENDPOINTS.MENU.ITEMS}/${id}/ingredients`, ingredients);
    },

    async updateMenuItemPrice(id, price) {
        return await apiClient.patch(`${API_ENDPOINTS.MENU.ITEMS}/${id}/price`, { price });
    },

    async getCategories() {
        return await apiClient.get(API_ENDPOINTS.MENU.CATEGORIES);
    },

    async getCategoryById(id) {
        return await apiClient.get(`${API_ENDPOINTS.MENU.CATEGORIES}/${id}`);
    },

    async getCategoriesByType(type) {
        return await apiClient.get(`${API_ENDPOINTS.MENU.CATEGORIES}/type/${type}`);
    },

    async createCategory(data) {
        return await apiClient.post(API_ENDPOINTS.MENU.CATEGORIES, data);
    },

    async updateCategory(id, data) {
        return await apiClient.put(`${API_ENDPOINTS.MENU.CATEGORIES}/${id}`, data);
    },

    async deleteCategory(id) {
        return await apiClient.delete(`${API_ENDPOINTS.MENU.CATEGORIES}/${id}`);
    },

    async getCombos(params = {}) {
        return await apiClient.get(API_ENDPOINTS.MENU.COMBOS, { params });
    },

    async getComboById(id) {
        return await apiClient.get(`${API_ENDPOINTS.MENU.COMBOS}/${id}`);
    },

    async createCombo(data) {
        return await apiClient.post(API_ENDPOINTS.MENU.COMBOS, data);
    },

    async updateCombo(id, data) {
        return await apiClient.put(`${API_ENDPOINTS.MENU.COMBOS}/${id}`, data);
    },

    async deleteCombo(id) {
        return await apiClient.delete(`${API_ENDPOINTS.MENU.COMBOS}/${id}`);
    },

    async toggleComboActive(id, active) {
        return await apiClient.patch(`${API_ENDPOINTS.MENU.COMBOS}/${id}/active`, null, {
            params: { active }
        });
    }
};

const inventoryAPI = {
    async getIngredients(params = {}) {
        return await apiClient.get(API_ENDPOINTS.INVENTORY.INGREDIENTS, { params });
    },

    async getIngredientById(id) {
        return await apiClient.get(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}`);
    },

    async createIngredient(data) {
        return await apiClient.post(API_ENDPOINTS.INVENTORY.INGREDIENTS, data);
    },

    async updateIngredient(id, data) {
        return await apiClient.put(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}`, data);
    },

    async deleteIngredient(id) {
        return await apiClient.delete(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}`);
    },

    async toggleIngredientActive(id) {
        return await apiClient.put(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/toggle`);
    },

    async stockIn(id, data) {
        return await apiClient.post(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/stock-in`, data);
    },

    async stockOut(id, data) {
        return await apiClient.post(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/stock-out`, data);
    },

    async adjustStock(id, data) {
        return await apiClient.post(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/adjust`, data);
    },

    async stockTake(id, data) {
        return await apiClient.post(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/stock-take`, data);
    },

    async getTransactions(params = {}) {
        return await apiClient.get(API_ENDPOINTS.INVENTORY.TRANSACTIONS, { params });
    },

    async getTransactionsByIngredient(id, params = {}) {
        return await apiClient.get(`${API_ENDPOINTS.INVENTORY.TRANSACTIONS}/ingredient/${id}`, { params });
    },

    async getTransactionsByType(type, params = {}) {
        return await apiClient.get(`${API_ENDPOINTS.INVENTORY.TRANSACTIONS}/type/${type}`, { params });
    },

    async getAlerts(params = {}) {
        return await apiClient.get(API_ENDPOINTS.INVENTORY.ALERTS, { params });
    },

    async getActiveAlerts() {
        return await apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/active`);
    },

    async getLowStockAlerts() {
        return await apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/low-stock`);
    },

    async getExpiryAlerts() {
        return await apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/expiry`);
    },

    async getCriticalAlerts() {
        return await apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/critical`);
    },

    async getLowStockIngredients() {
        return await apiClient.get(API_ENDPOINTS.INVENTORY.LOW_STOCK);
    },

    async getIngredientsByCategory(category) {
        return await apiClient.get(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/category/${category}`);
    }
};

const dashboardAPI = {
    async getStats() {
        try {
            const [ingredientsResponse, menuResponse, alertsResponse] = await Promise.allSettled([
                apiClient.get(API_ENDPOINTS.INVENTORY.INGREDIENTS, { params: { page: 0, size: 1 } }),
                apiClient.get(API_ENDPOINTS.MENU.ITEMS, { params: { page: 0, size: 1 } }),
                apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/active`)
            ]);

            return {
                totalIngredients: ingredientsResponse.status === 'fulfilled' ? (ingredientsResponse.value?.totalElements || 0) : 0,
                totalMenuItems: menuResponse.status === 'fulfilled' ? (menuResponse.value?.totalElements || 0) : 0,
                alertCount: alertsResponse.status === 'fulfilled' ? (Array.isArray(alertsResponse.value) ? alertsResponse.value.length : 0) : 0
            };
        } catch (error) {
            console.error('Error getting dashboard stats:', error);
            return {
                totalIngredients: 0,
                totalMenuItems: 0,
                alertCount: 0
            };
        }
    },

    async getRevenue(period = 'month') {
        try {
            return {
                monthlyRevenue: 0,
                dailyRevenue: 0,
                weeklyRevenue: 0
            };
        } catch (error) {
            console.error('Error getting revenue data:', error);
            return {
                monthlyRevenue: 0,
                dailyRevenue: 0,
                weeklyRevenue: 0
            };
        }
    },

    async getLowStockIngredients() {
        try {
            const res = await apiClient.get(API_ENDPOINTS.INVENTORY.LOW_STOCK);
            return Array.isArray(res) ? res : [];
        } catch (error) {
            console.error('Error getting low stock ingredients:', error);
            return [];
        }
    },

    async getActiveAlerts() {
        try {
            const res = await apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/active`);
            return Array.isArray(res) ? res : [];
        } catch (error) {
            console.error('Error getting active alerts:', error);
            return [];
        }
    }
};

const CLOUDINARY_URL = 'https://api.cloudinary.com/v1_1/dswb2h4ny/image/upload';

const cloudinaryAPI = {
    async getSignature() {
        try {
            return await apiClient.get(API_ENDPOINTS.CLOUDINARY.SIGNATURE);
        } catch (error) {
            console.error('Error getting signature:', error);
            throw error;
        }
    },

    async uploadImage(file, folder = 'restaurant-menu') {
        try {
            const signature = await this.getSignature();

            if (!signature.apiKey || !signature.timestamp || !signature.signature) {
                throw new Error('Invalid signature data from backend');
            }

            const formData = new FormData();
            formData.append('file', file);
            formData.append('api_key', signature.apiKey);
            formData.append('timestamp', signature.timestamp);
            formData.append('signature', signature.signature);
            formData.append('folder', folder);

            const response = await axios.post(CLOUDINARY_URL, formData, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            });

            return {
                url: response.data.secure_url,
                publicId: response.data.public_id,
                secure_url: response.data.secure_url,
                public_id: response.data.public_id
            };
        } catch (error) {
            console.error('Error uploading image:', error);
            throw error;
        }
    },

    async uploadUserAvatar(file) {
        return this.uploadImage(file, 'restaurant-users');
    }
};

export { userAPI, menuAPI, inventoryAPI, dashboardAPI, cloudinaryAPI, notificationAPI };

const apiService = {
    user: userAPI,
    menu: menuAPI,
    inventory: inventoryAPI,
    dashboard: dashboardAPI,
    cloudinary: cloudinaryAPI,
    notification: notificationAPI,
    order: orderAPI
};

export default apiService;
