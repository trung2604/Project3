/**
 * Centralized API Service
 * TẤT CẢ API calls của frontend đều được định nghĩa trong file này
 * Pages/components chỉ cần import và sử dụng functions từ đây
 */

import apiClient from './api';
import { API_ENDPOINTS, API_BASE_URL } from '../constants.js';
import axios from 'axios';

// ============================================
// USER API
// ============================================

const USERS_BASE = '/api/users';

const userAPI = {
    // Authentication
    async login({ username, password }) {
        const res = await apiClient.post(`${USERS_BASE}/login`, { username, password });
        const data = res.data?.data || res.data;
        if (data?.accessToken) {
            localStorage.setItem('accessToken', data.accessToken);
            localStorage.setItem('refreshToken', data.refreshToken || '');
            localStorage.setItem('user', JSON.stringify(data.user));
        }
        return data;
    },

    async register(payload) {
        const res = await apiClient.post(`${USERS_BASE}/register`, payload);
        return res.data?.data || res.data;
    },

    async exchangeToken(code, redirectUri) {
        const res = await apiClient.post(`${USERS_BASE}/oauth/token-exchange`, { code, redirectUri });
        return res.data?.data || res.data;
    },

    // User Profile
    getStoredUser() {
        const raw = localStorage.getItem('user');
        try {
            return raw ? JSON.parse(raw) : null;
        } catch {
            return null;
        }
    },

    async getById(userId) {
        const res = await apiClient.get(`${USERS_BASE}/${userId}`);
        return res.data?.data || res.data;
    },

    async getMe() {
        const res = await apiClient.get(`${USERS_BASE}/me`);
        return res.data?.data || res.data;
    },

    async updateMe(payload) {
        const res = await apiClient.put(`${USERS_BASE}/me`, payload);
        const data = res.data?.data || res.data;
        if (data) localStorage.setItem('user', JSON.stringify(data));
        return data;
    },

    async changeMyPassword({ currentPassword, newPassword }) {
        const res = await apiClient.patch(`${USERS_BASE}/me/password`, { currentPassword, newPassword });
        return res.data?.data || res.data;
    },

    async updateAvatar(userId, { avatarUrl, avatarPublicId }) {
        const res = await apiClient.patch(`${USERS_BASE}/${userId}/avatar`, { avatarUrl, avatarPublicId });
        const data = res.data?.data || res.data;
        if (data) localStorage.setItem('user', JSON.stringify(data));
        return data;
    },

    // Admin User Management
    async createUser(payload) {
        const res = await apiClient.post(`${USERS_BASE}`, payload);
        return res.data?.data || res.data;
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
        const res = await apiClient.get(url);
        return res.data?.data || res.data;
    },

    async deleteUser(userId) {
        const res = await apiClient.delete(`${USERS_BASE}/${userId}`);
        return res.data?.data || res.data;
    },

    async toggleUserStatus(userId, status) {
        const res = await apiClient.patch(`${USERS_BASE}/${userId}/status?status=${status}`);
        return res.data?.data || res.data;
    }
};

// ============================================
// MENU API
// ============================================

const menuAPI = {
    // Menu Items
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

        const res = await apiClient.get(API_ENDPOINTS.MENU.ITEMS, { params: queryParams });
        return res.data?.data || res.data;
    },

    async getMenuItemById(id) {
        const res = await apiClient.get(`${API_ENDPOINTS.MENU.ITEMS}/${id}`);
        return res.data?.data || res.data;
    },

    async createMenuItem(data) {
        const res = await apiClient.post(API_ENDPOINTS.MENU.ITEMS, data);
        return res.data?.data || res.data;
    },

    async updateMenuItem(id, data) {
        const res = await apiClient.put(`${API_ENDPOINTS.MENU.ITEMS}/${id}`, data);
        return res.data?.data || res.data;
    },

    async deleteMenuItem(id) {
        const res = await apiClient.delete(`${API_ENDPOINTS.MENU.ITEMS}/${id}`);
        return res.data?.data || res.data;
    },

    async toggleMenuItemActive(id, active) {
        const res = await apiClient.patch(`${API_ENDPOINTS.MENU.ITEMS}/${id}/active`, null, {
            params: { active }
        });
        return res.data?.data || res.data;
    },

    async updateMenuItemIngredients(id, ingredients) {
        const res = await apiClient.patch(`${API_ENDPOINTS.MENU.ITEMS}/${id}/ingredients`, ingredients);
        return res.data?.data || res.data;
    },

    async updateMenuItemPrice(id, price) {
        const res = await apiClient.patch(`${API_ENDPOINTS.MENU.ITEMS}/${id}/price`, { price });
        return res.data?.data || res.data;
    },

    // Categories
    async getCategories() {
        const res = await apiClient.get(API_ENDPOINTS.MENU.CATEGORIES);
        return res.data?.data || res.data;
    },

    async getCategoryById(id) {
        const res = await apiClient.get(`${API_ENDPOINTS.MENU.CATEGORIES}/${id}`);
        return res.data?.data || res.data;
    },

    async getCategoriesByType(type) {
        const res = await apiClient.get(`${API_ENDPOINTS.MENU.CATEGORIES}/type/${type}`);
        return res.data?.data || res.data;
    },

    async createCategory(data) {
        const res = await apiClient.post(API_ENDPOINTS.MENU.CATEGORIES, data);
        return res.data?.data || res.data;
    },

    async updateCategory(id, data) {
        const res = await apiClient.put(`${API_ENDPOINTS.MENU.CATEGORIES}/${id}`, data);
        return res.data?.data || res.data;
    },

    async deleteCategory(id) {
        const res = await apiClient.delete(`${API_ENDPOINTS.MENU.CATEGORIES}/${id}`);
        return res.data?.data || res.data;
    },

    // Combos
    async getCombos(params = {}) {
        const res = await apiClient.get(API_ENDPOINTS.MENU.COMBOS, { params });
        return res.data?.data || res.data;
    },

    async getComboById(id) {
        const res = await apiClient.get(`${API_ENDPOINTS.MENU.COMBOS}/${id}`);
        return res.data?.data || res.data;
    },

    async createCombo(data) {
        const res = await apiClient.post(API_ENDPOINTS.MENU.COMBOS, data);
        return res.data?.data || res.data;
    },

    async updateCombo(id, data) {
        const res = await apiClient.put(`${API_ENDPOINTS.MENU.COMBOS}/${id}`, data);
        return res.data?.data || res.data;
    },

    async deleteCombo(id) {
        const res = await apiClient.delete(`${API_ENDPOINTS.MENU.COMBOS}/${id}`);
        return res.data?.data || res.data;
    },

    async toggleComboActive(id, active) {
        const res = await apiClient.patch(`${API_ENDPOINTS.MENU.COMBOS}/${id}/active`, null, {
            params: { active }
        });
        return res.data?.data || res.data;
    }
};

// ============================================
// INVENTORY API
// ============================================

const inventoryAPI = {
    // Ingredients
    async getIngredients(params = {}) {
        const res = await apiClient.get(API_ENDPOINTS.INVENTORY.INGREDIENTS, { params });
        return res.data?.data || res.data;
    },

    async getIngredientById(id) {
        const res = await apiClient.get(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}`);
        return res.data?.data || res.data;
    },

    async createIngredient(data) {
        const res = await apiClient.post(API_ENDPOINTS.INVENTORY.INGREDIENTS, data);
        return res.data?.data || res.data;
    },

    async updateIngredient(id, data) {
        const res = await apiClient.put(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}`, data);
        return res.data?.data || res.data;
    },

    async deleteIngredient(id) {
        const res = await apiClient.delete(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}`);
        return res.data?.data || res.data;
    },

    async toggleIngredientActive(id) {
        const res = await apiClient.put(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/toggle`);
        return res.data?.data || res.data;
    },

    // Stock Operations
    async stockIn(id, data) {
        const res = await apiClient.post(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/stock-in`, data);
        return res.data?.data || res.data;
    },

    async stockOut(id, data) {
        const res = await apiClient.post(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/stock-out`, data);
        return res.data?.data || res.data;
    },

    async adjustStock(id, data) {
        const res = await apiClient.post(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/adjust`, data);
        return res.data?.data || res.data;
    },

    async stockTake(id, data) {
        const res = await apiClient.post(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/stock-take`, data);
        return res.data?.data || res.data;
    },

    // Transactions
    async getTransactions(params = {}) {
        const res = await apiClient.get(API_ENDPOINTS.INVENTORY.TRANSACTIONS, { params });
        return res.data?.data || res.data;
    },

    async getTransactionsByIngredient(id, params = {}) {
        const res = await apiClient.get(`${API_ENDPOINTS.INVENTORY.TRANSACTIONS}/ingredient/${id}`, { params });
        return res.data?.data || res.data;
    },

    async getTransactionsByType(type, params = {}) {
        const res = await apiClient.get(`${API_ENDPOINTS.INVENTORY.TRANSACTIONS}/type/${type}`, { params });
        return res.data?.data || res.data;
    },

    // Alerts
    async getAlerts(params = {}) {
        const res = await apiClient.get(API_ENDPOINTS.INVENTORY.ALERTS, { params });
        return res.data?.data || res.data;
    },

    async getActiveAlerts() {
        const res = await apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/active`);
        return res.data?.data || res.data;
    },

    async getLowStockAlerts() {
        const res = await apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/low-stock`);
        return res.data?.data || res.data;
    },

    async getExpiryAlerts() {
        const res = await apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/expiry`);
        return res.data?.data || res.data;
    },

    async getCriticalAlerts() {
        const res = await apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/critical`);
        return res.data?.data || res.data;
    },

    // Advanced Queries
    async getLowStockIngredients() {
        const res = await apiClient.get(API_ENDPOINTS.INVENTORY.LOW_STOCK);
        return res.data?.data || res.data;
    },

    async getIngredientsByCategory(category) {
        const res = await apiClient.get(`${API_ENDPOINTS.INVENTORY.INGREDIENTS}/category/${category}`);
        return res.data?.data || res.data;
    }
};

// ============================================
// DASHBOARD API
// ============================================

const dashboardAPI = {
    async getStats() {
        try {
            const [ingredientsResponse, menuResponse, alertsResponse] = await Promise.allSettled([
                apiClient.get(API_ENDPOINTS.INVENTORY.INGREDIENTS, { params: { page: 0, size: 1 } }),
                apiClient.get(API_ENDPOINTS.MENU.ITEMS, { params: { page: 0, size: 1 } }),
                apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/active`)
            ]);

            return {
                totalIngredients: ingredientsResponse.status === 'fulfilled' ? (ingredientsResponse.value.data?.data?.totalElements || 0) : 0,
                totalMenuItems: menuResponse.status === 'fulfilled' ? (menuResponse.value.data?.data?.totalElements || 0) : 0,
                alertCount: alertsResponse.status === 'fulfilled' ? (alertsResponse.value.data?.data?.length || 0) : 0
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
            // TODO: Implement real revenue API when available
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
            return res.data?.data || res.data || [];
        } catch (error) {
            console.error('Error getting low stock ingredients:', error);
            return [];
        }
    },

    async getActiveAlerts() {
        try {
            const res = await apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/active`);
            return res.data?.data || res.data || [];
        } catch (error) {
            console.error('Error getting active alerts:', error);
            return [];
        }
    }
};

// ============================================
// CLOUDINARY API
// ============================================

const CLOUDINARY_URL = 'https://api.cloudinary.com/v1_1/dswb2h4ny/image/upload';

const cloudinaryAPI = {
    async getSignature() {
        try {
            const url = `${API_BASE_URL}${API_ENDPOINTS.CLOUDINARY.SIGNATURE}`;
            const response = await axios.get(url);
            return response.data?.data || response.data;
        } catch (error) {
            console.error('Error getting signature:', error);
            throw error;
        }
    },

    async uploadImage(file, folder = 'restaurant-menu') {
        try {
            // Get signature from backend
            const signature = await this.getSignature();

            // Validate signature data
            if (!signature.apiKey || !signature.timestamp || !signature.signature) {
                throw new Error('Invalid signature data from backend');
            }

            // Create form data with signature
            const formData = new FormData();
            formData.append('file', file);
            formData.append('api_key', signature.apiKey);
            formData.append('timestamp', signature.timestamp);
            formData.append('signature', signature.signature);
            formData.append('folder', folder);

            // Upload to Cloudinary
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

// ============================================
// EXPORT
// ============================================

// Named exports for individual APIs
export { userAPI, menuAPI, inventoryAPI, dashboardAPI, cloudinaryAPI };

// Default export with all APIs grouped
const apiService = {
    user: userAPI,
    menu: menuAPI,
    inventory: inventoryAPI,
    dashboard: dashboardAPI,
    cloudinary: cloudinaryAPI
};

export default apiService;
