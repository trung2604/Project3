import apiClient from "./api";
import { API_ENDPOINTS } from "../constants.js";

// Loyalty Service API
export const loyaltyService = {
  // Loyalty Account Management
  getMyLoyaltyAccount: () => {
    return apiClient.get(API_ENDPOINTS.LOYALTY.ACCOUNTS_ME);
  },

  getLoyaltyAccountByUserId: (userId) => {
    return apiClient.get(API_ENDPOINTS.LOYALTY.ACCOUNTS_BY_USER(userId));
  },

  createLoyaltyAccount: (data) => {
    return apiClient.post(API_ENDPOINTS.LOYALTY.ACCOUNTS, data);
  },

  // Points Management
  earnPoints: (data) => {
    return apiClient.post(API_ENDPOINTS.LOYALTY.EARN_POINTS, data);
  },

  getPointsTransactions: () => {
    return apiClient.get(API_ENDPOINTS.LOYALTY.POINTS_TRANSACTIONS);
  },

  // Voucher Management
  getAllVouchers: (params = {}) => {
    return apiClient.get(API_ENDPOINTS.LOYALTY.VOUCHERS, { params });
  },

  getVoucherById: (voucherId) => {
    return apiClient.get(API_ENDPOINTS.LOYALTY.VOUCHER_BY_ID(voucherId));
  },

  getVoucherByCode: (code) => {
    return apiClient.get(API_ENDPOINTS.LOYALTY.VOUCHER_BY_CODE(code));
  },

  createVoucher: (data) => {
    return apiClient.post(API_ENDPOINTS.LOYALTY.VOUCHERS, data);
  },

  updateVoucher: (voucherId, data) => {
    return apiClient.put(API_ENDPOINTS.LOYALTY.VOUCHER_BY_ID(voucherId), data);
  },

  deleteVoucher: (voucherId) => {
    return apiClient.delete(API_ENDPOINTS.LOYALTY.VOUCHER_BY_ID(voucherId));
  },

  redeemVoucher: (voucherId, orderId = null) => {
    return apiClient.post(
      `${API_ENDPOINTS.LOYALTY.VOUCHERS}/${voucherId}/redeem`,
      null,
      {
        params: orderId ? { orderId } : {},
      }
    );
  },

  getVoucherUsageHistory: () => {
    return apiClient.get(`${API_ENDPOINTS.LOYALTY.VOUCHERS}/usage`);
  },

  // Promotion Management
  getAllPromotions: (params = {}) => {
    return apiClient.get(API_ENDPOINTS.LOYALTY.PROMOTIONS, { params });
  },

  getPromotionById: (promotionId) => {
    return apiClient.get(API_ENDPOINTS.LOYALTY.PROMOTION_BY_ID(promotionId));
  },

  createPromotion: (data) => {
    return apiClient.post(API_ENDPOINTS.LOYALTY.PROMOTIONS, data);
  },

  updatePromotion: (promotionId, data) => {
    return apiClient.put(
      API_ENDPOINTS.LOYALTY.PROMOTION_BY_ID(promotionId),
      data
    );
  },

  deletePromotion: (promotionId) => {
    return apiClient.delete(API_ENDPOINTS.LOYALTY.PROMOTION_BY_ID(promotionId));
  },
};

export default loyaltyService;
