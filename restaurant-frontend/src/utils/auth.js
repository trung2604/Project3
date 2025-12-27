// Note: JWT decoding is handled by backend (API Gateway decodes and adds X-User-Id header)
// Frontend only uses role from user data returned by backend API

// Check if user has a specific role
export function hasRole(userRole, requiredRoles) {
  if (!userRole || !requiredRoles) return false;
  return requiredRoles.includes(userRole);
}

// Check if user can manage menu (create/edit/delete)
export function canManageMenu(role) {
  return hasRole(role, [
    "STAFF",
    "KITCHEN_STAFF",
    "RESTAURANT_MANAGER",
    "ADMIN",
  ]);
}

// Check if user can manage inventory
export function canManageInventory(role) {
  return hasRole(role, ["WAREHOUSE_STAFF", "RESTAURANT_MANAGER", "ADMIN"]);
}

// Check if user can view dashboard overview
export function canViewOverview(role) {
  return hasRole(role, ["RESTAURANT_MANAGER", "ADMIN"]);
}

// Check if user is customer
export function isCustomer(role) {
  return role === "CUSTOMER";
}

// Get redirect path based on role after login
export function getRedirectPathByRole(role) {
  switch (role) {
    case "CUSTOMER":
      return "/dashboard/menu"; // Customer goes to menu view
    case "STAFF":
      return "/dashboard/menu"; // Staff goes to menu management
    case "KITCHEN_STAFF":
      return "/dashboard/orders"; // Kitchen staff goes to orders
    case "WAREHOUSE_STAFF":
      return "/dashboard/inventory"; // Warehouse staff goes to inventory
    case "RESTAURANT_MANAGER":
    case "ADMIN":
      return "/dashboard"; // Managers and admins go to overview
    default:
      return "/dashboard/menu"; // Default fallback
  }
}

// Get menu items based on role
export function getMenuItemsByRole(role) {
  const baseItems = [
    {
      icon: "home",
      key: "overview",
      label: "Tổng quan",
      roles: ["RESTAURANT_MANAGER", "ADMIN"],
    },
    {
      icon: "menu",
      key: "menu",
      label: "Thực đơn",
      roles: [
        "CUSTOMER",
        "STAFF",
        "KITCHEN_STAFF",
        "RESTAURANT_MANAGER",
        "ADMIN",
      ],
    },
    {
      icon: "shopping",
      key: "orders",
      label: "Đơn hàng",
      roles: [
        "CUSTOMER",
        "STAFF",
        "KITCHEN_STAFF",
        "RESTAURANT_MANAGER",
        "ADMIN",
      ],
    },
  ];

  const staffItems = [
    {
      icon: "inbox",
      key: "inventory",
      label: "Quản lý kho",
      roles: ["WAREHOUSE_STAFF", "RESTAURANT_MANAGER", "ADMIN"],
    },
    {
      icon: "warning",
      key: "inventory-alerts",
      label: "Cảnh báo kho",
      roles: ["WAREHOUSE_STAFF", "RESTAURANT_MANAGER", "ADMIN"],
    },
    {
      icon: "history",
      key: "inventory-transactions",
      label: "Lịch sử giao dịch",
      roles: ["WAREHOUSE_STAFF", "RESTAURANT_MANAGER", "ADMIN"],
    },
  ];

  const loyaltyItems = [
    {
      icon: "gift",
      key: "loyalty",
      label: "Loyalty",
      roles: ["CUSTOMER", "RESTAURANT_MANAGER", "ADMIN"],
    },
  ];

  const managerItems = [
    {
      icon: "team",
      key: "staff",
      label: "Nhân viên",
      roles: ["RESTAURANT_MANAGER", "ADMIN"],
    },
    {
      icon: "setting",
      key: "settings",
      label: "Cài đặt",
      roles: ["RESTAURANT_MANAGER", "ADMIN"],
    },
  ];

  const allItems = [
    ...baseItems,
    ...staffItems,
    ...loyaltyItems,
    ...managerItems,
  ];
  return allItems.filter((item) => item.roles.includes(role));
}
