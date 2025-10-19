import apiClient from './api';
import { API_ENDPOINTS } from '../constants.js';

// Menu Service API
export const menuService = {
    // Menu Item Management
    getMenuItems: (params = {}) => {
        // Sanitize query params: drop undefined/null/empty string; normalize types
        const cleanedEntries = Object.entries(params)
            .filter(([_, v]) => v !== undefined && v !== null && v !== '');

        const normalized = Object.fromEntries(
            cleanedEntries.map(([k, v]) => {
                if (k === 'page' || k === 'size') {
                    const num = typeof v === 'string' ? parseInt(v, 10) : v;
                    return [k, Number.isNaN(num) ? undefined : num];
                }
                if (k === 'categoryId') {
                    // Keep as string if non-numeric ID; otherwise ensure numeric
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

        // Remove keys that normalized to undefined
        const queryParams = Object.fromEntries(
            Object.entries(normalized).filter(([_, v]) => v !== undefined)
        );

        return apiClient.get(API_ENDPOINTS.MENU.ITEMS, { params: queryParams });
    },

    getMenuItemById: (id) => {
        return apiClient.get(`${API_ENDPOINTS.MENU.ITEMS}/${id}`);
    },

    createMenuItem: (data) => {
        return apiClient.post(API_ENDPOINTS.MENU.ITEMS, data);
    },

    updateMenuItem: (id, data) => {
        return apiClient.put(`${API_ENDPOINTS.MENU.ITEMS}/${id}`, data);
    },

    deleteMenuItem: (id) => {
        return apiClient.delete(`${API_ENDPOINTS.MENU.ITEMS}/${id}`);
    },

    toggleMenuItemActive: (id, active) => {
        return apiClient.patch(`${API_ENDPOINTS.MENU.ITEMS}/${id}/active`, null, {
            params: { active }
        });
    },

    updateMenuItemIngredients: (id, ingredients) => {
        return apiClient.patch(`${API_ENDPOINTS.MENU.ITEMS}/${id}/ingredients`, ingredients);
    },

    updateMenuItemPrice: (id, price) => {
        return apiClient.patch(`${API_ENDPOINTS.MENU.ITEMS}/${id}/price`, { price });
    },

    // Category Management
    getCategories: () => {
        return apiClient.get(API_ENDPOINTS.MENU.CATEGORIES);
    },

    getCategoryById: (id) => {
        return apiClient.get(`${API_ENDPOINTS.MENU.CATEGORIES}/${id}`);
    },

    getCategoriesByType: (type) => {
        return apiClient.get(`${API_ENDPOINTS.MENU.CATEGORIES}/type/${type}`);
    },

    createCategory: (data) => {
        return apiClient.post(API_ENDPOINTS.MENU.CATEGORIES, data);
    },

    updateCategory: (id, data) => {
        return apiClient.put(`${API_ENDPOINTS.MENU.CATEGORIES}/${id}`, data);
    },

    deleteCategory: (id) => {
        return apiClient.delete(`${API_ENDPOINTS.MENU.CATEGORIES}/${id}`);
    },

    // Combo Management
    getCombos: (params = {}) => {
        return apiClient.get(API_ENDPOINTS.MENU.COMBOS, { params });
    },

    getComboById: (id) => {
        return apiClient.get(`${API_ENDPOINTS.MENU.COMBOS}/${id}`);
    },

    createCombo: (data) => {
        return apiClient.post(API_ENDPOINTS.MENU.COMBOS, data);
    },

    updateCombo: (id, data) => {
        return apiClient.put(`${API_ENDPOINTS.MENU.COMBOS}/${id}`, data);
    },

    deleteCombo: (id) => {
        return apiClient.delete(`${API_ENDPOINTS.MENU.COMBOS}/${id}`);
    },

    toggleComboActive: (id, active) => {
        return apiClient.patch(`${API_ENDPOINTS.MENU.COMBOS}/${id}/active`, null, {
            params: { active }
        });
    }
};
