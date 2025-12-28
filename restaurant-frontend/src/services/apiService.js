import apiClient from "./api";
import { API_ENDPOINTS, API_BASE_URL } from "../constants.js";
import axios from "axios";
import notificationAPI from "./notificationService";
import orderAPI from "./orderService";

const USERS_BASE = "/api/users";

const userAPI = {
  async login({ username, password }) {
    const data = await apiClient.post(`${USERS_BASE}/login`, {
      username,
      password,
    });
    if (data?.accessToken) {
      localStorage.setItem("accessToken", data.accessToken);
      localStorage.setItem("refreshToken", data.refreshToken || "");
      localStorage.setItem("user", JSON.stringify(data.user));
    }
    return data;
  },

  async register(payload) {
    return await apiClient.post(`${USERS_BASE}/register`, payload);
  },

  async exchangeToken(code, redirectUri) {
    return await apiClient.post(`${USERS_BASE}/oauth/token-exchange`, {
      code,
      redirectUri,
    });
  },

  getStoredUser() {
    const raw = localStorage.getItem("user");
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
    if (data) localStorage.setItem("user", JSON.stringify(data));
    return data;
  },

  async changeMyPassword({ currentPassword, newPassword }) {
    return await apiClient.patch(`${USERS_BASE}/me/password`, {
      currentPassword,
      newPassword,
    });
  },

  async updateAvatar(userId, { avatarUrl, avatarPublicId }) {
    const data = await apiClient.patch(`${USERS_BASE}/${userId}/avatar`, {
      avatarUrl,
      avatarPublicId,
    });
    if (data) localStorage.setItem("user", JSON.stringify(data));
    return data;
  },

  async createUser(payload) {
    return await apiClient.post(`${USERS_BASE}`, payload);
  },

  async getAllUsers(params = {}) {
    const queryParams = new URLSearchParams();
    if (params.page !== undefined) queryParams.append("page", params.page);
    if (params.size !== undefined) queryParams.append("size", params.size);
    if (params.search) queryParams.append("search", params.search);
    if (params.role) queryParams.append("role", params.role);
    if (params.status) queryParams.append("status", params.status);

    const queryString = queryParams.toString();
    const url = queryString ? `${USERS_BASE}?${queryString}` : USERS_BASE;
    return await apiClient.get(url);
  },

  async deleteUser(userId) {
    return await apiClient.delete(`${USERS_BASE}/${userId}`);
  },

  async toggleUserStatus(userId, status) {
    return await apiClient.patch(
      `${USERS_BASE}/${userId}/status?status=${status}`
    );
  },

  async updateUser(userId, payload) {
    return await apiClient.put(`${USERS_BASE}/${userId}`, payload);
  },

  async getUserByEmail(email) {
    return await apiClient.get(`${USERS_BASE}/email/${email}`);
  },

  async verifyEmail(userId) {
    return await apiClient.post(`${USERS_BASE}/${userId}/verify-email`);
  },

  // Admin methods
  async initializeRoles() {
    return await apiClient.post(`${USERS_BASE}/admin/initialize-roles`);
  },

  async getRealmRoles() {
    return await apiClient.get(`${USERS_BASE}/admin/realm-roles`);
  },

  async getClientRoles() {
    return await apiClient.get(`${USERS_BASE}/admin/client-roles`);
  },

  async syncUserRole(userId) {
    return await apiClient.post(`${USERS_BASE}/${userId}/sync-role`);
  },
};

const menuAPI = {
  async getMenuItems(params = {}) {
    // Sanitize query params
    const cleanedEntries = Object.entries(params).filter(
      ([_, v]) => v !== undefined && v !== null && v !== ""
    );

    const normalized = Object.fromEntries(
      cleanedEntries.map(([k, v]) => {
        if (k === "page" || k === "size") {
          const num = typeof v === "string" ? parseInt(v, 10) : v;
          return [k, Number.isNaN(num) ? undefined : num];
        }
        if (k === "categoryId") {
          const maybeNum =
            typeof v === "string" && /^\d+$/.test(v) ? parseInt(v, 10) : v;
          return [k, maybeNum];
        }
        if (k === "active") {
          return [k, v === true || v === false ? v : undefined];
        }
        if (k === "search") {
          return [
            k,
            typeof v === "string" && v.trim().length > 0 ? v.trim() : undefined,
          ];
        }
        return [k, v];
      })
    );

    const queryParams = Object.fromEntries(
      Object.entries(normalized).filter(([_, v]) => v !== undefined)
    );

    return await apiClient.get(API_ENDPOINTS.MENU.ITEMS, {
      params: queryParams,
    });
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
    return await apiClient.patch(
      `${API_ENDPOINTS.MENU.ITEMS}/${id}/active`,
      null,
      {
        params: { active },
      }
    );
  },

  async updateMenuItemIngredients(id, ingredients) {
    return await apiClient.patch(
      `${API_ENDPOINTS.MENU.ITEMS}/${id}/ingredients`,
      ingredients
    );
  },

  async updateMenuItemIngredientsWithQuantity(id, ingredientsWithQuantity) {
    return await apiClient.patch(
      `${API_ENDPOINTS.MENU.ITEMS}/${id}/ingredients-with-quantity`,
      ingredientsWithQuantity
    );
  },

  async updateMenuItemPrice(id, price) {
    return await apiClient.patch(`${API_ENDPOINTS.MENU.ITEMS}/${id}/price`, {
      price,
    });
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
    return await apiClient.patch(
      `${API_ENDPOINTS.MENU.COMBOS}/${id}/active`,
      null,
      {
        params: { active },
      }
    );
  },
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
    return await apiClient.put(
      `${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}`,
      data
    );
  },

  async deleteIngredient(id) {
    return await apiClient.delete(
      `${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}`
    );
  },

  async toggleIngredientActive(id) {
    return await apiClient.put(
      `${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/toggle`
    );
  },

  async stockIn(id, data) {
    return await apiClient.post(
      `${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/stock-in`,
      data
    );
  },

  async stockOut(id, data) {
    return await apiClient.post(
      `${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/stock-out`,
      data
    );
  },

  async adjustStock(id, data) {
    return await apiClient.post(
      `${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/adjust`,
      data
    );
  },

  async stockTake(id, data) {
    return await apiClient.post(
      `${API_ENDPOINTS.INVENTORY.INGREDIENTS}/${id}/stock-take`,
      data
    );
  },

  async getTransactions(params = {}) {
    return await apiClient.get(API_ENDPOINTS.INVENTORY.TRANSACTIONS, {
      params,
    });
  },

  async getTransactionsByIngredient(id, params = {}) {
    return await apiClient.get(
      `${API_ENDPOINTS.INVENTORY.TRANSACTIONS}/ingredient/${id}`,
      { params }
    );
  },

  async getTransactionsByType(type, params = {}) {
    return await apiClient.get(
      `${API_ENDPOINTS.INVENTORY.TRANSACTIONS}/type/${type}`,
      { params }
    );
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
    return await apiClient.get(
      `${API_ENDPOINTS.INVENTORY.INGREDIENTS}/category/${category}`
    );
  },

  // Bulk Operations
  async bulkToggle(ingredientIds) {
    return await apiClient.post(
      `${API_ENDPOINTS.INVENTORY.INGREDIENTS}/bulk-toggle`,
      {
        ingredientIds,
      }
    );
  },

  async bulkDelete(ingredientIds) {
    return await apiClient.post(
      `${API_ENDPOINTS.INVENTORY.INGREDIENTS}/bulk-delete`,
      {
        ingredientIds,
      }
    );
  },
};

const dashboardAPI = {
  async getStats() {
    try {
      const [ingredientsResponse, menuResponse, alertsResponse] =
        await Promise.allSettled([
          apiClient.get(API_ENDPOINTS.INVENTORY.INGREDIENTS, {
            params: { page: 0, size: 1 },
          }),
          apiClient.get(API_ENDPOINTS.MENU.ITEMS, {
            params: { page: 0, size: 1 },
          }),
          apiClient.get(`${API_ENDPOINTS.INVENTORY.ALERTS}/active`),
        ]);

      return {
        totalIngredients:
          ingredientsResponse.status === "fulfilled"
            ? ingredientsResponse.value?.totalElements || 0
            : 0,
        totalMenuItems:
          menuResponse.status === "fulfilled"
            ? menuResponse.value?.totalElements || 0
            : 0,
        alertCount:
          alertsResponse.status === "fulfilled"
            ? Array.isArray(alertsResponse.value)
              ? alertsResponse.value.length
              : 0
            : 0,
      };
    } catch (error) {
      console.error("Error getting dashboard stats:", error);
      return {
        totalIngredients: 0,
        totalMenuItems: 0,
        alertCount: 0,
      };
    }
  },

  async getRevenue(period = "month") {
    try {
      return {
        monthlyRevenue: 0,
        dailyRevenue: 0,
        weeklyRevenue: 0,
      };
    } catch (error) {
      console.error("Error getting revenue data:", error);
      return {
        monthlyRevenue: 0,
        dailyRevenue: 0,
        weeklyRevenue: 0,
      };
    }
  },

  async getLowStockIngredients() {
    try {
      const res = await apiClient.get(API_ENDPOINTS.INVENTORY.LOW_STOCK);
      return Array.isArray(res) ? res : [];
    } catch (error) {
      console.error("Error getting low stock ingredients:", error);
      return [];
    }
  },

  async getActiveAlerts() {
    try {
      const res = await apiClient.get(
        `${API_ENDPOINTS.INVENTORY.ALERTS}/active`
      );
      return Array.isArray(res) ? res : [];
    } catch (error) {
      console.error("Error getting active alerts:", error);
      return [];
    }
  },
};

const CLOUDINARY_URL = "https://api.cloudinary.com/v1_1/dswb2h4ny/image/upload";

let signatureCache = null;
let signatureCacheTime = 0;
const SIGNATURE_CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

const cloudinaryAPI = {
  async getSignature(forceRefresh = false, folder = "restaurant-menu") {
    try {
      // Return cached signature if still valid and same folder
      const now = Date.now();
      if (
        !forceRefresh &&
        signatureCache &&
        signatureCache.folder === folder &&
        now - signatureCacheTime < SIGNATURE_CACHE_DURATION
      ) {
        // Return signature without folder field
        const { folder: _, ...signature } = signatureCache;
        return signature;
      }

      const signature = await apiClient.get(
        API_ENDPOINTS.CLOUDINARY.SIGNATURE,
        {
          params: { folder },
        }
      );

      // Cache the signature with folder info for cache key matching
      signatureCache = { ...signature, folder };
      signatureCacheTime = now;

      // Return signature without folder (Cloudinary doesn't need it in response)
      return signature;
    } catch (error) {
      console.error("Error getting signature:", error);
      throw error;
    }
  },

  /**
   * Upload image to Cloudinary with optimization
   * @param {File} file - Image file to upload
   * @param {string} folder - Cloudinary folder (default: "restaurant-menu")
   * @param {Object} options - Upload options
   * @param {Function} onProgress - Progress callback (progress: 0-100)
   * @param {boolean} compress - Whether to compress image (default: true)
   * @returns {Promise<{url: string, publicId: string}>}
   */
  async uploadImage(file, folder = "restaurant-menu", options = {}) {
    const { onProgress, compress = true } = options;

    try {
      // Pre-fetch signature in parallel with compression (if enabled)
      const signaturePromise = this.getSignature(false, folder);

      // Compress image if enabled
      let fileToUpload = file;
      if (compress) {
        try {
          const { compressImage } = await import("../utils/imageOptimizer");
          fileToUpload = await compressImage(file, 1920, 1920, 0.8);
        } catch (compressionError) {
          console.warn(
            "Image compression failed, using original:",
            compressionError
          );
          // Continue with original file if compression fails
        }
      }

      // Wait for signature
      const signature = await signaturePromise;

      if (!signature.apiKey || !signature.timestamp || !signature.signature) {
        throw new Error("Invalid signature data from backend");
      }

      const formData = new FormData();
      formData.append("file", fileToUpload);
      formData.append("api_key", signature.apiKey);
      formData.append("timestamp", signature.timestamp);
      formData.append("signature", signature.signature);
      formData.append("folder", folder);

      // Add optimization parameters (Cloudinary will auto-optimize)
      // Note: transformations should be in signature if using signed upload
      // For now, we rely on client-side compression and Cloudinary's auto-optimization

      const response = await axios.post(CLOUDINARY_URL, formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
        onUploadProgress: (progressEvent) => {
          if (onProgress && progressEvent.total) {
            const percentCompleted = Math.round(
              (progressEvent.loaded * 100) / progressEvent.total
            );
            onProgress(percentCompleted);
          }
        },
        timeout: 60000, // 60 seconds timeout for large files
      });

      return {
        url: response.data.secure_url,
        publicId: response.data.public_id,
        secure_url: response.data.secure_url,
        public_id: response.data.public_id,
      };
    } catch (error) {
      // If signature error, try refreshing cache and retry once
      if (
        error.response?.status === 401 ||
        error.message?.includes("signature")
      ) {
        signatureCache = null; // Clear cache
        signatureCacheTime = 0;

        // Retry once with fresh signature
        if (!options.retried) {
          return this.uploadImage(file, folder, { ...options, retried: true });
        }
      }

      console.error("Error uploading image:", error);
      throw error;
    }
  },

  async uploadUserAvatar(file, onProgress) {
    return this.uploadImage(file, "restaurant-users", { onProgress });
  },

  /**
   * Pre-fetch signature to reduce latency when user selects file
   */
  async prefetchSignature() {
    try {
      await this.getSignature();
    } catch (error) {
      // Silently fail - signature will be fetched when needed
      console.warn("Failed to prefetch signature:", error);
    }
  },

  /**
   * Clear signature cache (useful for testing or forced refresh)
   */
  clearSignatureCache() {
    signatureCache = null;
    signatureCacheTime = 0;
  },
};

export {
  userAPI,
  menuAPI,
  inventoryAPI,
  dashboardAPI,
  cloudinaryAPI,
  notificationAPI,
};

const apiService = {
  user: userAPI,
  menu: menuAPI,
  inventory: inventoryAPI,
  dashboard: dashboardAPI,
  cloudinary: cloudinaryAPI,
  notification: notificationAPI,
  order: orderAPI,
};

export default apiService;
