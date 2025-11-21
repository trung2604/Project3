// API Configuration
// Sử dụng environment variables cho production, fallback về localhost cho development
// API Gateway runs on port 8081 (see apigateway/application.yml)
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081';
export const IDP = {
    URL: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
    REALM: import.meta.env.VITE_KEYCLOAK_REALM || 'project3',
    CLIENT_ID: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'project3',
    CALLBACK_PATH: '/auth/callback',
};

// Restaurant Information
export const RESTAURANT_INFO = {
    name: "Trung's Restaurant",
    owner: 'Đỗ Đình Trung',
    email: 'dodinhtrungthptyv@gmail.com',
    phone: '0383034491',
    address: 'Gia Lâm, Hà Nội',
    description: 'Nhà hàng cao cấp phục vụ các món ăn Á - Âu đặc sắc'
};

// API Endpoints - Updated to match API Gateway routes
export const API_ENDPOINTS = {
    // Inventory Service (Port 8003) - Routed through Gateway
    INVENTORY: {
        INGREDIENTS: '/api/inventory/ingredient',
        TRANSACTIONS: '/api/inventory/transactions',
        ALERTS: '/api/inventory/alerts',
        LOW_STOCK: '/api/inventory/ingredient/low-stock'
    },
    // Menu Service (Port 8002) - Routed through Gateway
    MENU: {
        ITEMS: '/api/restaurant/menu/items',
        CATEGORIES: '/api/restaurant/category',
        COMBOS: '/api/restaurant/combo'
    },
    // Cloudinary Service
    CLOUDINARY: {
        SIGNATURE: '/api/cloudinary/signature'
    },
    // Dashboard Service (if exists)
    DASHBOARD: {
        STATS: '/api/dashboard/stats',
        REVENUE: '/api/dashboard/revenue'
    },
    // Notification Service
    NOTIFICATIONS: {
        BASE: '/api/notifications',
        UNREAD: '/api/notifications/unread',
        UNREAD_COUNT: '/api/notifications/unread/count'
    },
    // Order Service
    ORDER: {
        BASE: '/api/restaurant/order',
        CREATE: '/api/restaurant/order/create',
        BY_ID: (id) => `/api/restaurant/order/${id}`,
        STATUS: (id) => `/api/restaurant/order/${id}/status`,
        CANCEL: (id) => `/api/restaurant/order/${id}/cancel`,
        SPLIT_BILL: (id) => `/api/restaurant/order/${id}/split-bill`,
        START_COOKING: (id) => `/api/restaurant/order/${id}/start-cooking`,
        MARK_READY: (id) => `/api/restaurant/order/${id}/mark-ready`,
        START_DELIVERING: (id) => `/api/restaurant/order/${id}/start-delivering`,
        COMPLETE: (id) => `/api/restaurant/order/${id}/complete`
    }
};

// Common Constants
export const PAGINATION = {
    DEFAULT_PAGE_SIZE: 20,
    PAGE_SIZE_OPTIONS: ['10', '20', '50', '100']
};

export const DATE_FORMAT = 'DD/MM/YYYY';
export const DATETIME_FORMAT = 'DD/MM/YYYY HH:mm:ss';

// Status Constants
export const STATUS = {
    ACTIVE: true,
    INACTIVE: false
};

// Alert Types
export const ALERT_TYPES = {
    LOW_STOCK: 'LOW_STOCK',
    EXPIRY: 'EXPIRY',
    CRITICAL: 'CRITICAL'
};

// Transaction Types
export const TRANSACTION_TYPES = {
    STOCK_IN: 'STOCK_IN',
    STOCK_OUT: 'STOCK_OUT',
    ADJUSTMENT: 'ADJUSTMENT',
    STOCK_TAKE: 'STOCK_TAKE'
};

// Order Types
export const ORDER_TYPES = {
    DINE_IN: 'DINE_IN',
    TAKEAWAY: 'TAKEAWAY',
    DELIVERY: 'DELIVERY'
};

// Order Status
export const ORDER_STATUS = {
    PENDING: 'PENDING',
    COOKING: 'COOKING',
    READY: 'READY',
    DELIVERING: 'DELIVERING',
    COMPLETED: 'COMPLETED',
    CANCELLED: 'CANCELLED'
};
