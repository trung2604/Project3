import axios from 'axios';
import { API_BASE_URL } from '../constants.js';

// Create axios instance
const apiClient = axios.create({
    baseURL: API_BASE_URL,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor
// Note: API Gateway will decode JWT and add X-User-Id header automatically
apiClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('accessToken');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor
apiClient.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        // Enhanced error handling
        if (error.response?.status === 401) {
            // Clear auth data on unauthorized
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('user');
            console.error('401 Unauthorized - Session expired');
        } else if (error.response?.status === 403) {
            console.error('403 Forbidden - Insufficient permissions');
            console.error('Error details:', error.response?.data);
        } else if (error.response?.status === 404) {
            console.error('404 Not Found - Resource not found');
            console.error('Requested URL:', error.config?.url);
        } else if (error.code === 'ERR_NETWORK') {
            console.error('Network Error - Check if API Gateway and services are running');
            console.error('Base URL:', API_BASE_URL);
        } else if (error.code === 'ERR_CANCELED') {
            console.error('Request Canceled - Check CORS configuration');
        }

        return Promise.reject(error);
    }
);

export default apiClient;
