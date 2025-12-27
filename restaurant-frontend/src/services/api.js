import axios from "axios";
import { API_BASE_URL } from "../constants.js";

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("accessToken");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

apiClient.interceptors.response.use(
  (response) => {
    const apiResponse = response.data;
    if (
      apiResponse &&
      typeof apiResponse === "object" &&
      "data" in apiResponse &&
      "statusCode" in apiResponse
    ) {
      return apiResponse.data;
    }
    return apiResponse;
  },
  (error) => {
    if (error.response?.status === 401) {
      const token = localStorage.getItem("accessToken");
      if (token) {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("user");
        console.error("401 Unauthorized - Session expired. Tokens cleared.");
        window.dispatchEvent(
          new StorageEvent("storage", {
            key: "accessToken",
            newValue: null,
            storageArea: localStorage,
          })
        );
      }
    } else if (error.response?.status === 403) {
      console.error("403 Forbidden - Insufficient permissions");
    } else if (error.response?.status === 404) {
      console.error("404 Not Found - Resource not found");
    } else if (error.code === "ERR_NETWORK") {
      console.error(
        "Network Error - Check if API Gateway and services are running"
      );
    } else if (error.code === "ERR_CANCELED") {
      console.error("Request Canceled - Check CORS configuration");
    }
    return Promise.reject(error);
  }
);

export default apiClient;
