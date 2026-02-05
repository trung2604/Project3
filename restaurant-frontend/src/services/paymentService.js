import apiClient from "./api";
import { API_ENDPOINTS } from "../constants.js";

const paymentAPI = {
    /**
     * Get all payments (admin only)
     */
    async getAllPayments() {
        return await apiClient.get(API_ENDPOINTS.PAYMENT.BASE);
    },

    /**
     * Get payment by ID
     */
    async getPaymentById(paymentId) {
        return await apiClient.get(API_ENDPOINTS.PAYMENT.BY_ID(paymentId));
    },

    /**
     * Get payments by order ID
     */
    async getPaymentsByOrderId(orderId) {
        return await apiClient.get(API_ENDPOINTS.PAYMENT.BY_ORDER(orderId));
    },

    /**
     * Get payments by customer ID
     */
    async getPaymentsByCustomerId(customerId) {
        return await apiClient.get(API_ENDPOINTS.PAYMENT.BY_CUSTOMER(customerId));
    },

    /**
     * Create a new payment
     */
    async createPayment(paymentData) {
        return await apiClient.post(API_ENDPOINTS.PAYMENT.CREATE, paymentData);
    },

    /**
     * Process a payment
     */
    async processPayment(paymentId, paymentData) {
        return await apiClient.post(
            API_ENDPOINTS.PAYMENT.PROCESS(paymentId),
            paymentData
        );
    },

    /**
     * Refund a payment (admin/manager only)
     */
    async refundPayment(paymentId, refundData) {
        return await apiClient.post(
            API_ENDPOINTS.PAYMENT.REFUND(paymentId),
            refundData
        );
    },
    /**
     * Complete a payment (manual confirmation)
     */
    async completePayment(paymentId) {
        // Construct URL manually since we haven't updated constants yet
        return await apiClient.post(`/api/payments/${paymentId}/complete`);
    },

    /**
     * Handle PayPal success callback
     */
    async handlePayPalCallback(paymentId, token, payerId) {
        return await apiClient.get(`/api/payments/paypal/success`, {
            params: {
                paymentId,
                token,
                PayerID: payerId
            }
        });
    },
};

export default paymentAPI;
