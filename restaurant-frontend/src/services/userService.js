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
    }
};


