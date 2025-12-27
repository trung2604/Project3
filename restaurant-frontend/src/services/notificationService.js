import apiClient from "./api";
import { API_ENDPOINTS } from "../constants.js";

const NOTIFICATIONS_BASE = "/api/notifications";

const notificationAPI = {
  async getNotifications(params = {}) {
    const cleanedParams = Object.fromEntries(
      Object.entries(params).filter(
        ([_, v]) => v !== undefined && v !== null && v !== ""
      )
    );
    const res = await apiClient.get(NOTIFICATIONS_BASE, {
      params: cleanedParams,
    });
    return res;
  },

  async getNotificationById(id) {
    const res = await apiClient.get(`${NOTIFICATIONS_BASE}/${id}`);
    return res;
  },

  async getUnreadNotifications(userId, params = {}) {
    const res = await apiClient.get(`${NOTIFICATIONS_BASE}/unread`, {
      params: { userId, ...params },
    });
    return res;
  },

  async getUnreadCount(userId) {
    const res = await apiClient.get(`${NOTIFICATIONS_BASE}/unread/count`, {
      params: { userId },
    });
    return res;
  },

  async markAsRead(notificationId) {
    const res = await apiClient.patch(
      `${NOTIFICATIONS_BASE}/${notificationId}/read`
    );
    return res;
  },

  async archive(notificationId) {
    const res = await apiClient.patch(
      `${NOTIFICATIONS_BASE}/${notificationId}/archive`
    );
    return res;
  },

  async bulkMarkAsRead(notificationIds) {
    const res = await apiClient.post(
      `${NOTIFICATIONS_BASE}/bulk-read`,
      notificationIds
    );
    return res;
  },

  async bulkArchive(notificationIds) {
    const res = await apiClient.post(
      `${NOTIFICATIONS_BASE}/bulk-archive`,
      notificationIds
    );
    return res;
  },
};

export default notificationAPI;
