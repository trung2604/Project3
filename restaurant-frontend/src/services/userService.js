import apiClient from './api';
import { API_ENDPOINTS } from '../constants';

const USERS_BASE = '/api/users';

export const userService = {
    async login({ username, password }) {
        const res = await apiClient.post(`${USERS_BASE}/login`, { username, password });
        const data = res.data?.data || res.data; // ApiResponseDTO wrapper
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

    getStoredUser() {
        const raw = localStorage.getItem('user');
        try { return raw ? JSON.parse(raw) : null; } catch { return null; }
    },

    async getById(userId) {
        const res = await apiClient.get(`${USERS_BASE}/${userId}`);
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

    // Admin methods
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


