import { PAYMENT_STATUS, PAYMENT_METHODS } from "../constants";

/**
 * Get Ant Design color for payment status
 */
export const getPaymentStatusColor = (status) => {
    const colorMap = {
        [PAYMENT_STATUS.PENDING]: "blue",
        [PAYMENT_STATUS.SUCCESS]: "green",
        [PAYMENT_STATUS.FAILED]: "red",
        [PAYMENT_STATUS.REFUNDED]: "orange",
        [PAYMENT_STATUS.PARTIALLY_REFUNDED]: "gold",
    };
    return colorMap[status] || "default";
};

/**
 * Get payment status display text
 */
export const getPaymentStatusText = (status) => {
    const textMap = {
        [PAYMENT_STATUS.PENDING]: "Đang chờ",
        [PAYMENT_STATUS.SUCCESS]: "Thành công",
        [PAYMENT_STATUS.FAILED]: "Thất bại",
        [PAYMENT_STATUS.REFUNDED]: "Đã hoàn tiền",
        [PAYMENT_STATUS.PARTIALLY_REFUNDED]: "Hoàn một phần",
    };
    return textMap[status] || status;
};

/**
 * Get payment method display text
 */
export const getPaymentMethodText = (method) => {
    const textMap = {
        [PAYMENT_METHODS.CASH]: "Tiền mặt",
        [PAYMENT_METHODS.VIETQR]: "VietQR",
        [PAYMENT_METHODS.PAYPAL]: "PayPal",
    };
    return textMap[method] || method;
};

/**
 * Get payment method icon
 */
export const getPaymentMethodIcon = (method) => {
    const iconMap = {
        [PAYMENT_METHODS.CASH]: "💵",
        [PAYMENT_METHODS.VIETQR]: "📱",
        [PAYMENT_METHODS.PAYPAL]: "🅿️",
    };
    return iconMap[method] || "💳";
};

/**
 * Format currency (VND)
 */
export const formatCurrency = (amount) => {
    if (amount == null) return "0 ₫";
    return new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
    }).format(amount);
};

/**
 * Check if payment can be refunded
 */
export const canRefundPayment = (payment) => {
    if (!payment) return false;
    return (
        payment.paymentStatus === PAYMENT_STATUS.SUCCESS ||
        payment.paymentStatus === PAYMENT_STATUS.PARTIALLY_REFUNDED
    );
};

/**
 * Calculate remaining refundable amount
 */
export const getRefundableAmount = (payment) => {
    if (!payment) return 0;
    const refundedAmount = payment.refundedAmount || 0;
    return (payment.amount || 0) - refundedAmount;
};

/**
 * Check if payment is processing
 */
export const isPaymentProcessing = (payment) => {
    return payment && payment.paymentStatus === PAYMENT_STATUS.PENDING;
};

/**
 * Check if payment is completed
 */
export const isPaymentCompleted = (payment) => {
    return (
        payment &&
        (payment.paymentStatus === PAYMENT_STATUS.SUCCESS ||
            payment.paymentStatus === PAYMENT_STATUS.REFUNDED ||
            payment.paymentStatus === PAYMENT_STATUS.PARTIALLY_REFUNDED)
    );
};

/**
 * Check if payment failed
 */
export const isPaymentFailed = (payment) => {
    return payment && payment.paymentStatus === PAYMENT_STATUS.FAILED;
};
