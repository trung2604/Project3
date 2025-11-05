/**
 * Extract error message from various error types
 * @param {Error|AxiosError} error - The error object
 * @returns {string} Human-readable error message
 */
export function getErrorMessage(error) {
    if (!error) return 'Đã xảy ra lỗi không xác định';

    // Network errors
    if (error.code === 'ERR_NETWORK') {
        return 'Lỗi kết nối mạng. Vui lòng kiểm tra API Gateway có đang chạy không.';
    }

    if (error.code === 'ERR_CANCELED') {
        return 'Request bị hủy. Có thể do CORS hoặc timeout.';
    }

    // HTTP response errors
    if (error.response) {
        const responseData = error.response.data;
        const errorMessage = responseData?.message ||
            responseData?.error ||
            `HTTP ${error.response.status}: ${error.response.statusText}`;

        // Special handling for OAuth errors
        if (responseData?.error === 'invalid_grant' ||
            responseData?.error_description?.includes('Code not valid')) {
            return 'Mã xác thực đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.';
        }

        return errorMessage;
    }

    // Generic error message
    return error.message || 'Đã xảy ra lỗi không xác định';
}

/**
 * Check if error is related to authentication/authorization
 * @param {Error|AxiosError} error - The error object
 * @returns {boolean} True if error is auth-related
 */
export function isAuthError(error) {
    if (!error?.response) return false;
    const status = error.response.status;
    return status === 401 || status === 403;
}

/**
 * Check if error indicates token expiration or invalid grant
 * @param {Error|AxiosError} error - The error object
 * @returns {boolean} True if error indicates token/grant issue
 */
export function isTokenError(error) {
    if (!error?.response?.data) return false;
    const responseData = error.response.data;
    return responseData?.error === 'invalid_grant' ||
        responseData?.error_description?.includes('Code not valid') ||
        responseData?.error === 'invalid_token';
}

