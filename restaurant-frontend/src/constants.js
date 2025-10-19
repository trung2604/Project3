// API Configuration
export const API_BASE_URL = 'http://localhost:8080';

// Restaurant Information
export const RESTAURANT_INFO = {
    name: 'Golden Dragon Restaurant',
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
    // Dashboard Service (if exists)
    DASHBOARD: {
        STATS: '/api/dashboard/stats',
        REVENUE: '/api/dashboard/revenue'
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
