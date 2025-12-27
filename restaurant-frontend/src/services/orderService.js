import apiClient from "./api";
import { API_ENDPOINTS } from "../constants.js";

const orderAPI = {
  async getAllOrders(params = {}) {
    const queryParams = new URLSearchParams();
    if (params.status) queryParams.append("status", params.status);
    if (params.type) queryParams.append("type", params.type);
    if (params.customerId) queryParams.append("customerId", params.customerId);
    if (params.startDate) queryParams.append("startDate", params.startDate);
    if (params.endDate) queryParams.append("endDate", params.endDate);

    const queryString = queryParams.toString();
    const url = queryString
      ? `${API_ENDPOINTS.ORDER.BASE}?${queryString}`
      : API_ENDPOINTS.ORDER.BASE;
    return await apiClient.get(url);
  },

  async getOrderById(orderId) {
    return await apiClient.get(API_ENDPOINTS.ORDER.BY_ID(orderId));
  },

  async createOrder(orderData) {
    return await apiClient.post(API_ENDPOINTS.ORDER.CREATE, orderData);
  },

  async updateOrderStatus(orderId, newStatus, updatedBy, notes) {
    const queryParams = new URLSearchParams();
    queryParams.append("newStatus", newStatus);
    if (updatedBy) queryParams.append("updatedBy", updatedBy);
    if (notes) queryParams.append("notes", notes);

    const queryString = queryParams.toString();
    const url = `${API_ENDPOINTS.ORDER.STATUS(orderId)}?${queryString}`;
    return await apiClient.put(url);
  },

  async cancelOrder(orderId, cancellationReason, allowCancellation = false) {
    return await apiClient.post(API_ENDPOINTS.ORDER.CANCEL(orderId), {
      cancellationReason,
      allowCancellation,
    });
  },

  async splitBill(orderId, splitItems) {
    return await apiClient.post(API_ENDPOINTS.ORDER.SPLIT_BILL(orderId), {
      splitItems,
    });
  },

  async startCooking(orderId, updatedBy) {
    const params = updatedBy ? { updatedBy } : {};
    return await apiClient.post(
      API_ENDPOINTS.ORDER.START_COOKING(orderId),
      null,
      { params }
    );
  },

  async markReady(orderId, updatedBy) {
    const params = updatedBy ? { updatedBy } : {};
    return await apiClient.post(API_ENDPOINTS.ORDER.MARK_READY(orderId), null, {
      params,
    });
  },

  async startDelivering(orderId, updatedBy) {
    const params = updatedBy ? { updatedBy } : {};
    return await apiClient.post(
      API_ENDPOINTS.ORDER.START_DELIVERING(orderId),
      null,
      { params }
    );
  },

  async completeOrder(orderId, updatedBy) {
    const params = updatedBy ? { updatedBy } : {};
    return await apiClient.post(API_ENDPOINTS.ORDER.COMPLETE(orderId), null, {
      params,
    });
  },
};

export default orderAPI;
