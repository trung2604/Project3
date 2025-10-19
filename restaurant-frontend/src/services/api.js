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
apiClient.interceptors.request.use(
    (config) => {
        // TODO: Add auth token when API Gateway implements token generation
        // const token = localStorage.getItem('authToken');
        // if (token) {
        //     config.headers.Authorization = `Bearer ${token}`;
        // }
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
            // TODO: Handle unauthorized access when authentication is implemented
            // localStorage.removeItem('authToken');
            // window.location.href = '/login';
            console.error('401 Unauthorized - Authentication not implemented yet');
        } else if (error.response?.status === 403) {
            console.error('403 Forbidden - Check if services are running and accessible');
            console.error('Error details:', error.response?.data);
        } else if (error.response?.status === 404) {
            console.error('404 Not Found - Check API endpoints and service availability');
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
