import apiClient from './api';
import { API_ENDPOINTS } from '../constants.js';

const NOTIFICATIONS_BASE = '/api/notifications';

const notificationAPI = {
    // Get all notifications with filters
    async getNotifications(params = {}) {
        const cleanedParams = Object.fromEntries(
            Object.entries(params).filter(([_, v]) => v !== undefined && v !== null && v !== '')
        );
        const res = await apiClient.get(NOTIFICATIONS_BASE, { params: cleanedParams });
        return res.data?.data || res.data;
    },

    // Get notification by ID
    async getNotificationById(id) {
        const res = await apiClient.get(`${NOTIFICATIONS_BASE}/${id}`);
        return res.data?.data || res.data;
    },

    // Get unread notifications
    async getUnreadNotifications(userId, params = {}) {
        const res = await apiClient.get(`${NOTIFICATIONS_BASE}/unread`, {
            params: { userId, ...params }
        });
        return res.data?.data || res.data;
    },

    // Get unread count
    async getUnreadCount(userId) {
        const res = await apiClient.get(`${NOTIFICATIONS_BASE}/unread/count`, {
            params: { userId }
        });
        return res.data?.data || res.data;
    },

    // Mark notification as read
    async markAsRead(notificationId) {
        const res = await apiClient.patch(`${NOTIFICATIONS_BASE}/${notificationId}/read`);
        return res.data?.data || res.data;
    },

    // Archive notification
    async archive(notificationId) {
        const res = await apiClient.patch(`${NOTIFICATIONS_BASE}/${notificationId}/archive`);
        return res.data?.data || res.data;
    },

    // Bulk mark as read
    async bulkMarkAsRead(notificationIds) {
        const res = await apiClient.post(`${NOTIFICATIONS_BASE}/bulk-read`, notificationIds);
        return res.data?.data || res.data;
    },

    // Bulk archive
    async bulkArchive(notificationIds) {
        const res = await apiClient.post(`${NOTIFICATIONS_BASE}/bulk-archive`, notificationIds);
        return res.data?.data || res.data;
    }
};

export default notificationAPI;

