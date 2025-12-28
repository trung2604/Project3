import React, { useState, useEffect, useMemo } from "react";
import { createPortal } from "react-dom";
import {
  Card,
  Row,
  Col,
  Input,
  Select,
  Image,
  Tag,
  Space,
  Badge,
  Empty,
  Typography,
  Button,
  Modal,
  Form,
  InputNumber,
  Radio,
  message,
  App,
  Divider,
} from "antd";
import {
  SearchOutlined,
  FireOutlined,
  ShoppingCartOutlined,
  PlusOutlined,
  MinusOutlined,
  GiftOutlined,
  CheckOutlined,
} from "@ant-design/icons";
import apiService from "../services/apiService";
import { useAuth } from "../context/AuthContext";
import Loading from "../components/Common/Loading";
import {
  useDataRefresh,
  DATA_REFRESH_EVENTS,
  listenToDataRefresh,
} from "../utils/dataRefreshEvents";

const { Option } = Select;
const { Search } = Input;
const { Title, Text } = Typography;

const MenuView = () => {
  const { role, user } = useAuth();
  const { message: antMessage } = App.useApp();
  const [menuItems, setMenuItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState({
    categoryId: "",
    search: "",
  });
  const [orderModalVisible, setOrderModalVisible] = useState(false);
  const [orderForm] = Form.useForm();
  const [combos, setCombos] = useState([]);
  const [activeTab, setActiveTab] = useState("menu"); // "menu" or "combos"
  const [selectedItems, setSelectedItems] = useState(new Map()); // Map of itemId -> { item, quantity, type: 'menu' | 'combo' }

  // Load menu items (only active items)
  const loadMenuItems = async () => {
    setLoading(true);
    try {
      const params = {
        page: 0,
        size: 1000, // Get all items for customer view
        active: true, // Only show active items
        ...(filters.categoryId &&
          filters.categoryId.trim() !== "" && {
            categoryId: filters.categoryId,
          }),
        ...(filters.search &&
          filters.search.trim() !== "" && { search: filters.search }),
      };

      const response = await apiService.menu.getMenuItems(params);
      // Response interceptor đã extract data, response là PagedMenuItemResponse trực tiếp
      setMenuItems(response?.items || []);
    } catch (error) {
      console.error("Error loading menu items:", error);
    } finally {
      setLoading(false);
    }
  };

  // Load categories
  const loadCategories = async () => {
    try {
      const response = await apiService.menu.getCategories();
      // Response interceptor đã extract data, response là list trực tiếp
      const activeCategories = Array.isArray(response)
        ? response.filter((cat) => cat.active !== false)
        : [];
      setCategories(activeCategories);
    } catch (error) {
      console.error("Error loading categories:", error);
    }
  };

  // Load combos
  const loadCombos = async () => {
    try {
      const response = await apiService.menu.getCombos({
        page: 0,
        size: 100,
        active: true,
      });
      setCombos(response?.items || response || []);
    } catch (error) {
      console.error("Error loading combos:", error);
    }
  };

  useEffect(() => {
    loadMenuItems();
    loadCategories();
    loadCombos();
  }, [filters]);

  // Listen to category changes to reload categories
  useEffect(() => {
    const eventNames = [
      DATA_REFRESH_EVENTS.CATEGORY_CREATED,
      DATA_REFRESH_EVENTS.CATEGORY_UPDATED,
      DATA_REFRESH_EVENTS.CATEGORY_DELETED,
    ];
    const cleanupFunctions = [];

    eventNames.forEach((eventName) => {
      const cleanup = listenToDataRefresh(eventName, () => {
        loadCategories();
        // Also reload menu items in case category filter is active
        loadMenuItems();
      });
      cleanupFunctions.push(cleanup);
    });

    return () => {
      cleanupFunctions.forEach((cleanup) => cleanup());
    };
  }, []);

  // Listen to menu item changes to reload menu items
  useEffect(() => {
    const eventNames = [
      DATA_REFRESH_EVENTS.MENU_ITEM_CREATED,
      DATA_REFRESH_EVENTS.MENU_ITEM_UPDATED,
      DATA_REFRESH_EVENTS.MENU_ITEM_DELETED,
    ];
    const cleanupFunctions = [];

    eventNames.forEach((eventName) => {
      const cleanup = listenToDataRefresh(eventName, () => {
        loadMenuItems();
      });
      cleanupFunctions.push(cleanup);
    });

    return () => {
      cleanupFunctions.forEach((cleanup) => cleanup());
    };
  }, []);

  const handleSearch = (value) => {
    setFilters((prev) => ({ ...prev, search: value }));
  };

  const handleCategoryChange = (value) => {
    setFilters((prev) => ({ ...prev, categoryId: value || "" }));
  };

  // Group menu items by category
  const menuItemsByCategory = useMemo(() => {
    const grouped = {};
    menuItems.forEach((item) => {
      const categoryId = item.categoryId || "other";
      const categoryName = item.categoryName || "Khác";
      if (!grouped[categoryId]) {
        grouped[categoryId] = {
          categoryId,
          categoryName,
          items: [],
        };
      }
      grouped[categoryId].items.push(item);
    });
    return Object.values(grouped);
  }, [menuItems]);

  // Toggle item selection
  const toggleItemSelection = (item, type = "menu") => {
    if (!user) {
      antMessage.warning("Vui lòng đăng nhập để chọn món");
      return;
    }
    const itemId = type === "menu" ? item.menuItemId : item.comboId || item.id;
    setSelectedItems((prev) => {
      const newMap = new Map(prev);
      if (newMap.has(itemId)) {
        newMap.delete(itemId);
      } else {
        newMap.set(itemId, { item, quantity: 1, type });
      }
      return newMap;
    });
  };

  // Update item quantity
  const updateItemQuantity = (itemId, delta) => {
    setSelectedItems((prev) => {
      const newMap = new Map(prev);
      const selected = newMap.get(itemId);
      if (selected) {
        const newQuantity = Math.max(1, selected.quantity + delta);
        newMap.set(itemId, { ...selected, quantity: newQuantity });
      }
      return newMap;
    });
  };

  // Calculate total price from selected items
  const calculateTotalPrice = () => {
    let total = 0;
    selectedItems.forEach((selectedData) => {
      const { item, quantity, type } = selectedData;
      if (type === "combo") {
        total += (item.price || 0) * quantity;
      } else {
        total += (item.price || 0) * quantity;
      }
    });
    return total;
  };

  // Render Menu Item Card
  const renderMenuItemCard = (item) => {
    const itemId = item.menuItemId;
    const isSelected = selectedItems.has(itemId);
    const selectedData = selectedItems.get(itemId);

    return (
      <Card
        hoverable
        style={{
          height: "100%",
          borderRadius: "20px",
          overflow: "hidden",
          transition: "all 0.3s ease",
          border: isSelected ? "2px solid #f59e0b" : "1px solid #e5e7eb",
          background: "#ffffff",
          display: "flex",
          flexDirection: "column",
          position: "relative",
        }}
        styles={{
          body: {
            padding: 0,
            flex: 1,
            display: "flex",
            flexDirection: "column",
          },
        }}
        cover={
          <div
            style={{
              height: "200px",
              overflow: "hidden",
              backgroundColor: "#f9fafb",
              position: "relative",
            }}
          >
            <Image
              src={item.imageUrl || "/placeholder-food.jpg"}
              alt={item.name}
              height={200}
              width="100%"
              style={{
                objectFit: "cover",
              }}
              fallback="/placeholder-food.jpg"
              preview={{
                mask: "Xem ảnh",
              }}
            />
            {item.active && (
              <Tag
                color="green"
                style={{
                  position: "absolute",
                  top: "8px",
                  right: "8px",
                  margin: 0,
                }}
              >
                Đang bán
              </Tag>
            )}
            {isSelected && (
              <div
                style={{
                  position: "absolute",
                  top: "8px",
                  left: "8px",
                  background: "#f59e0b",
                  borderRadius: "50%",
                  width: "32px",
                  height: "32px",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  color: "white",
                  fontSize: "16px",
                }}
              >
                <CheckOutlined />
              </div>
            )}
          </div>
        }
      >
        <div
          style={{
            padding: "16px",
            flex: 1,
            display: "flex",
            flexDirection: "column",
          }}
        >
          <div style={{ marginBottom: "8px" }}>
            <Title
              level={5}
              style={{
                margin: 0,
                marginBottom: "4px",
                fontSize: "16px",
                fontWeight: "600",
                lineHeight: "1.4",
                minHeight: "44px",
              }}
              ellipsis={{ rows: 2 }}
            >
              {item.name}
            </Title>
            {item.categoryName && (
              <Tag color="blue" style={{ marginTop: "4px" }}>
                {item.categoryName}
              </Tag>
            )}
          </div>
          {item.description && (
            <Text
              type="secondary"
              style={{
                fontSize: "13px",
                display: "-webkit-box",
                WebkitLineClamp: 2,
                WebkitBoxOrient: "vertical",
                overflow: "hidden",
                marginBottom: "12px",
                flex: 1,
              }}
            >
              {item.description}
            </Text>
          )}
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginTop: "auto",
              paddingTop: "12px",
              borderTop: "1px solid #f0f0f0",
            }}
          >
            <Text
              strong
              style={{
                fontSize: "18px",
                color: "#f59e0b",
                fontWeight: "700",
              }}
            >
              {item.price?.toLocaleString("vi-VN")} đ
            </Text>
          </div>
          {isSelected ? (
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                marginTop: "12px",
                gap: "8px",
              }}
            >
              <Button
                size="small"
                icon={<MinusOutlined />}
                onClick={(e) => {
                  e.stopPropagation();
                  updateItemQuantity(itemId, -1);
                }}
                style={{ flex: 1 }}
              />
              <Text
                strong
                style={{
                  minWidth: "50px",
                  textAlign: "center",
                  fontSize: "16px",
                }}
              >
                {selectedData.quantity}
              </Text>
              <Button
                size="small"
                icon={<PlusOutlined />}
                onClick={(e) => {
                  e.stopPropagation();
                  updateItemQuantity(itemId, 1);
                }}
                style={{ flex: 1 }}
              />
              <Button
                type="primary"
                danger
                size="small"
                onClick={(e) => {
                  e.stopPropagation();
                  toggleItemSelection(item, "menu");
                }}
                style={{ marginLeft: "8px" }}
              >
                Xóa
              </Button>
            </div>
          ) : (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                toggleItemSelection(item, "menu");
              }}
              style={{
                marginTop: "12px",
                width: "100%",
                background: "linear-gradient(135deg, #f59e0b 0%, #dc2626 100%)",
                border: "none",
                height: "40px",
                fontWeight: "600",
              }}
            >
              Thêm vào giỏ
            </Button>
          )}
        </div>
      </Card>
    );
  };

  // Render Combo Card
  const renderComboCard = (combo) => {
    const itemId = combo.comboId || combo.id;
    const isSelected = selectedItems.has(itemId);
    const selectedData = selectedItems.get(itemId);
    const items = combo.menuItemIds || combo.items || [];

    // Calculate original price - handle both string IDs and object format
    const originalPrice = items.reduce((sum, item) => {
      let menuItemId,
        quantity = 1;

      // Check if item is a string (ID) or object
      if (typeof item === "string") {
        menuItemId = item;
      } else if (item && typeof item === "object") {
        menuItemId = item.menuItemId || item.id || item;
        quantity = item.quantity || 1;
      } else {
        menuItemId = item;
      }

      const menuItem = menuItems.find(
        (mi) => mi.menuItemId === menuItemId || mi.id === menuItemId
      );
      return sum + (menuItem?.price || 0) * quantity;
    }, 0);

    const savings = originalPrice - (combo.price || 0);
    const savingsPercent =
      originalPrice > 0 ? ((savings / originalPrice) * 100).toFixed(0) : 0;

    return (
      <Card
        hoverable
        className="combo-card"
        style={{
          width: "100%",
          height: "100%",
          borderRadius: "20px",
          overflow: "hidden",
          transition: "all 0.3s ease",
          border: isSelected ? "2px solid #f59e0b" : "1px solid #e5e7eb",
          background: "#ffffff",
          display: "flex",
          flexDirection: "column",
          position: "relative",
          alignSelf: "stretch",
        }}
        styles={{
          body: {
            padding: 0,
            flex: 1,
            display: "flex",
            flexDirection: "column",
            minHeight: "auto",
            height: "auto",
          },
        }}
        cover={
          <div
            style={{
              height: "280px",
              overflow: "hidden",
              backgroundColor: "#f9fafb",
              position: "relative",
            }}
          >
            {combo.imageUrl ? (
              <Image
                src={combo.imageUrl}
                alt={combo.name}
                height={280}
                width="100%"
                style={{
                  objectFit: "cover",
                }}
                fallback="/placeholder-food.jpg"
                preview={{
                  mask: "Xem ảnh",
                }}
              />
            ) : (
              <div
                style={{
                  height: "100%",
                  background:
                    "linear-gradient(135deg, #f59e0b 0%, #dc2626 100%)",
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  justifyContent: "center",
                  color: "white",
                }}
              >
                <GiftOutlined
                  style={{ fontSize: "64px", marginBottom: "12px" }}
                />
                <Text
                  style={{
                    color: "white",
                    fontSize: "16px",
                    fontWeight: "600",
                  }}
                >
                  Combo
                </Text>
              </div>
            )}
            {combo.active && (
              <Tag
                color="green"
                style={{
                  position: "absolute",
                  top: "8px",
                  right: "8px",
                  margin: 0,
                }}
              >
                Đang bán
              </Tag>
            )}
            {isSelected && (
              <div
                style={{
                  position: "absolute",
                  top: "8px",
                  left: "8px",
                  background: "#f59e0b",
                  borderRadius: "50%",
                  width: "32px",
                  height: "32px",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  color: "white",
                  fontSize: "16px",
                }}
              >
                <CheckOutlined />
              </div>
            )}
            <Tag
              color="orange"
              style={{
                position: "absolute",
                bottom: "8px",
                left: "8px",
                margin: 0,
                fontSize: "12px",
                fontWeight: "600",
              }}
            >
              <GiftOutlined style={{ marginRight: "4px" }} />
              Combo
            </Tag>
          </div>
        }
      >
        <div
          className="combo-card-content"
          style={{
            padding: "24px",
            flex: 1,
            display: "flex",
            flexDirection: "column",
            minHeight: "auto",
            width: "100%",
          }}
        >
          <div style={{ marginBottom: "16px", flexShrink: 0 }}>
            <Title
              level={4}
              style={{
                margin: 0,
                marginBottom: "10px",
                fontSize: "22px",
                fontWeight: "700",
                lineHeight: "1.4",
                minHeight: "60px",
                color: "#1f2937",
              }}
              ellipsis={{ rows: 2 }}
            >
              {combo.name}
            </Title>
            <div style={{ marginTop: "10px" }}>
              <Tag
                color="blue"
                style={{
                  fontSize: "14px",
                  padding: "6px 12px",
                  borderRadius: "8px",
                  fontWeight: "500",
                }}
              >
                <GiftOutlined style={{ marginRight: "6px" }} />
                Bao gồm {items.length} món
              </Tag>
            </div>
          </div>
          {combo.description && (
            <Text
              type="secondary"
              style={{
                fontSize: "15px",
                display: "-webkit-box",
                WebkitLineClamp: 3,
                WebkitBoxOrient: "vertical",
                overflow: "hidden",
                marginBottom: "20px",
                flex: "1 1 auto",
                minHeight: "60px",
                lineHeight: "1.7",
                color: "#6b7280",
              }}
            >
              {combo.description}
            </Text>
          )}
          <div
            style={{
              marginTop: "auto",
              paddingTop: "20px",
              borderTop: "2px solid #f0f0f0",
              flexShrink: 0,
            }}
          >
            <div style={{ marginBottom: "16px" }}>
              {originalPrice > 0 && originalPrice > (combo.price || 0) && (
                <Text
                  delete
                  type="secondary"
                  style={{
                    fontSize: "15px",
                    display: "block",
                    marginBottom: "8px",
                    color: "#9ca3af",
                    fontWeight: "500",
                  }}
                >
                  {originalPrice.toLocaleString("vi-VN")} đ
                </Text>
              )}
              <Text
                strong
                style={{
                  fontSize: "26px",
                  color: "#f59e0b",
                  fontWeight: "700",
                  display: "block",
                  marginBottom: "10px",
                  lineHeight: "1.2",
                }}
              >
                {combo.price?.toLocaleString("vi-VN")} đ
              </Text>
              {savings > 0 && (
                <Tag
                  color="green"
                  style={{
                    fontSize: "14px",
                    fontWeight: "600",
                    padding: "6px 12px",
                    borderRadius: "8px",
                    display: "inline-block",
                  }}
                >
                  Tiết kiệm {savings.toLocaleString("vi-VN")} đ (
                  {savingsPercent}%)
                </Tag>
              )}
            </div>
          </div>
          {isSelected ? (
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                marginTop: "12px",
                gap: "8px",
              }}
            >
              <Button
                size="small"
                icon={<MinusOutlined />}
                onClick={(e) => {
                  e.stopPropagation();
                  updateItemQuantity(itemId, -1);
                }}
                style={{ flex: 1 }}
              />
              <Text
                strong
                style={{
                  minWidth: "50px",
                  textAlign: "center",
                  fontSize: "16px",
                }}
              >
                {selectedData.quantity}
              </Text>
              <Button
                size="small"
                icon={<PlusOutlined />}
                onClick={(e) => {
                  e.stopPropagation();
                  updateItemQuantity(itemId, 1);
                }}
                style={{ flex: 1 }}
              />
              <Button
                type="primary"
                danger
                size="small"
                onClick={(e) => {
                  e.stopPropagation();
                  toggleItemSelection(combo, "combo");
                }}
                style={{ marginLeft: "8px" }}
              >
                Xóa
              </Button>
            </div>
          ) : (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                toggleItemSelection(combo, "combo");
              }}
              style={{
                marginTop: "20px",
                width: "100%",
                background: "linear-gradient(135deg, #f59e0b 0%, #dc2626 100%)",
                border: "none",
                height: "48px",
                fontWeight: "600",
                fontSize: "16px",
                borderRadius: "12px",
              }}
            >
              Thêm vào giỏ
            </Button>
          )}
        </div>
      </Card>
    );
  };

  // Handle order button click
  const handleOrderClick = () => {
    if (!user) {
      antMessage.warning("Vui lòng đăng nhập để đặt món");
      return;
    }
    if (selectedItems.size === 0) {
      antMessage.warning("Vui lòng chọn ít nhất một món");
      return;
    }
    orderForm.setFieldsValue({
      orderType: "DINE_IN",
      notes: "",
      customerName:
        user.firstName && user.lastName
          ? `${user.firstName} ${user.lastName}`
          : user.username || "",
      customerPhone: user.phone || "",
      deliveryAddress: "",
      tableNumber: "",
    });
    setOrderModalVisible(true);
  };

  // Handle order submit
  const handleOrderSubmit = async (values) => {
    try {
      let orderItems = [];
      let totalDiscount = 0;
      let totalDiscountAmount = 0;

      // Process all selected items
      selectedItems.forEach((selectedData) => {
        const { item, quantity, type } = selectedData;

        if (type === "combo") {
          // Handle combo order
          const comboItems = item.menuItemIds || item.items || [];
          const originalPrice = comboItems.reduce((sum, comboItem) => {
            let menuItemId,
              itemQuantity = 1;

            // Check if comboItem is a string (ID) or object
            if (typeof comboItem === "string") {
              menuItemId = comboItem;
            } else if (comboItem && typeof comboItem === "object") {
              menuItemId = comboItem.menuItemId || comboItem.id || comboItem;
              itemQuantity = comboItem.quantity || 1;
            } else {
              menuItemId = comboItem;
            }

            const menuItem = menuItems.find(
              (mi) => mi.menuItemId === menuItemId || mi.id === menuItemId
            );
            return sum + (menuItem?.price || 0) * itemQuantity;
          }, 0);

          const comboDiscount = originalPrice - (item.price || 0);
          const comboDiscountPercent =
            originalPrice > 0 ? (comboDiscount / originalPrice) * 100 : 0;

          comboItems.forEach((comboItem) => {
            let menuItemId,
              itemQuantity = 1;

            // Check if comboItem is a string (ID) or object
            if (typeof comboItem === "string") {
              menuItemId = comboItem;
            } else if (comboItem && typeof comboItem === "object") {
              menuItemId = comboItem.menuItemId || comboItem.id || comboItem;
              itemQuantity = comboItem.quantity || 1;
            } else {
              menuItemId = comboItem;
            }

            const menuItem = menuItems.find(
              (mi) => mi.menuItemId === menuItemId || mi.id === menuItemId
            );
            const finalQuantity = itemQuantity * quantity;
            orderItems.push({
              menuItemId: menuItemId,
              name: menuItem?.name || "Món không tồn tại",
              quantity: finalQuantity,
              unitPrice: menuItem?.price || 0,
              subtotal: (menuItem?.price || 0) * finalQuantity,
              notes: values.notes || "",
            });
          });

          totalDiscount += comboDiscount * quantity;
          totalDiscountAmount += comboDiscount * quantity;
        } else {
          // Handle menu item order
          orderItems.push({
            menuItemId: item.menuItemId,
            name: item.name,
            quantity: quantity,
            unitPrice: item.price,
            subtotal: item.price * quantity,
            notes: values.notes || "",
          });
        }
      });

      const subtotal = orderItems.reduce((sum, item) => sum + item.subtotal, 0);
      const discountPercentage =
        subtotal > 0 ? (totalDiscount / subtotal) * 100 : 0;

      const orderData = {
        orderType: values.orderType,
        customerName: values.customerName,
        customerPhone: values.customerPhone,
        orderItems: orderItems,
        discountPercentage: discountPercentage,
        discountAmount: totalDiscountAmount,
        vatPercentage: 10,
        deliveryAddress:
          values.orderType === "DELIVERY" ? values.deliveryAddress : null,
        tableNumber: values.orderType === "DINE_IN" ? values.tableNumber : null,
        notes: values.notes || null,
      };

      await apiService.order.createOrder(orderData);
      antMessage.success("Đặt món thành công!");
      setOrderModalVisible(false);
      orderForm.resetFields();
      setSelectedItems(new Map());
    } catch (error) {
      console.error("Error creating order:", error);
      antMessage.error(error.message || "Không thể đặt món. Vui lòng thử lại!");
    }
  };

  if (loading) {
    return <Loading tip="Đang tải thực đơn..." />;
  }

  return (
    <>
      <div style={{ padding: "0", maxWidth: "1400px", margin: "0 auto" }}>
        {/* Header Section */}
        <div
          style={{
            marginBottom: "32px",
            textAlign: "center",
            paddingTop: "24px",
          }}
        >
          <Title
            level={1}
            style={{
              fontSize: "clamp(24px, 5vw, 36px)",
              fontWeight: "700",
              marginBottom: "12px",
              color: "#262626",
              background: "linear-gradient(135deg, #f59e0b 0%, #dc2626 100%)",
              WebkitBackgroundClip: "text",
              WebkitTextFillColor: "transparent",
              backgroundClip: "text",
            }}
          >
            Thực đơn nhà hàng
          </Title>
          <Text style={{ fontSize: "16px", color: "#8c8c8c" }}>
            Khám phá các món ăn ngon miệng của chúng tôi
          </Text>
        </div>

        {/* Tabs */}
        <Card
          style={{
            marginBottom: "24px",
            borderRadius: "16px",
            boxShadow: "0 4px 12px rgba(0, 0, 0, 0.08)",
          }}
          styles={{ body: { padding: "16px" } }}
        >
          <Space
            size="large"
            style={{ width: "100%", justifyContent: "center" }}
          >
            <Button
              type={activeTab === "menu" ? "primary" : "default"}
              size="large"
              onClick={() => setActiveTab("menu")}
              style={{
                minWidth: "150px",
                height: "48px",
                fontSize: "16px",
                fontWeight: "600",
              }}
            >
              <FireOutlined /> Thực đơn ({menuItems.length})
            </Button>
            <Button
              type={activeTab === "combos" ? "primary" : "default"}
              size="large"
              onClick={() => setActiveTab("combos")}
              style={{
                minWidth: "150px",
                height: "48px",
                fontSize: "16px",
                fontWeight: "600",
              }}
            >
              <GiftOutlined /> Combo ({combos.length})
            </Button>
          </Space>
        </Card>

        {/* Filters */}
        <Card
          style={{
            marginBottom: "32px",
            borderRadius: "16px",
            boxShadow: "0 4px 12px rgba(0, 0, 0, 0.08)",
          }}
          styles={{ body: { padding: "20px" } }}
        >
          <Row gutter={[16, 16]} align="middle">
            <Col xs={24} sm={12} md={10}>
              <Search
                placeholder="Tìm kiếm món ăn..."
                allowClear
                onSearch={handleSearch}
                size="large"
                prefix={<SearchOutlined style={{ color: "#8c8c8c" }} />}
                style={{ width: "100%" }}
              />
            </Col>
            <Col xs={24} sm={12} md={10}>
              <Select
                placeholder="Chọn danh mục"
                allowClear
                onChange={handleCategoryChange}
                size="large"
                style={{ width: "100%" }}
              >
                {categories.map((category) => (
                  <Option key={category.categoryId} value={category.categoryId}>
                    {category.name}
                  </Option>
                ))}
              </Select>
            </Col>
            <Col xs={24} md={4} style={{ textAlign: "right" }}>
              <Badge
                count={activeTab === "menu" ? menuItems.length : combos.length}
                showZero
                style={{ backgroundColor: "#f59e0b" }}
              >
                <Text strong style={{ fontSize: "14px", color: "#262626" }}>
                  {activeTab === "menu"
                    ? `${menuItems.length} món`
                    : `${combos.length} combo`}
                </Text>
              </Badge>
            </Col>
          </Row>
        </Card>

        {/* Menu Items Container */}
        {activeTab === "menu" && (
          <div className="menu-items-container" style={{ width: "100%" }}>
            {menuItems.length === 0 ? (
              <Card style={{ borderRadius: "16px", minHeight: "400px" }}>
                <Empty
                  description={
                    <div>
                      <Text style={{ fontSize: "16px", color: "#8c8c8c" }}>
                        Không tìm thấy món ăn nào
                      </Text>
                      <br />
                      <Text type="secondary" style={{ fontSize: "14px" }}>
                        Thử thay đổi bộ lọc để tìm kiếm
                      </Text>
                    </div>
                  }
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                />
              </Card>
            ) : filters.categoryId ? (
              // Show filtered items in grid
              <div className="menu-items-grid">
                <Row gutter={[24, 24]}>
                  {menuItems.map((item) => (
                    <Col key={item.menuItemId} xs={24} sm={12} md={8} lg={6}>
                      {renderMenuItemCard(item)}
                    </Col>
                  ))}
                </Row>
              </div>
            ) : (
              // Show items grouped by category
              <div className="menu-items-by-category">
                {menuItemsByCategory.map((category) => (
                  <div
                    key={category.categoryId}
                    className="menu-category-section"
                    style={{ marginBottom: "48px" }}
                  >
                    <Title
                      level={3}
                      style={{
                        marginBottom: "24px",
                        paddingBottom: "12px",
                        borderBottom: "2px solid #f59e0b",
                        fontSize: "24px",
                        fontWeight: "700",
                        color: "#262626",
                      }}
                    >
                      {category.categoryName}
                      <Badge
                        count={category.items.length}
                        style={{
                          marginLeft: "12px",
                          backgroundColor: "#f59e0b",
                        }}
                      />
                    </Title>
                    <Row gutter={[24, 24]}>
                      {category.items.map((item) => (
                        <Col
                          key={item.menuItemId}
                          xs={24}
                          sm={12}
                          md={8}
                          lg={6}
                        >
                          {renderMenuItemCard(item)}
                        </Col>
                      ))}
                    </Row>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Combos Container */}
        {activeTab === "combos" && (
          <div className="combos-container" style={{ width: "100%" }}>
            {combos.length === 0 ? (
              <Card style={{ borderRadius: "16px", minHeight: "400px" }}>
                <Empty
                  description={
                    <div>
                      <Text style={{ fontSize: "16px", color: "#8c8c8c" }}>
                        Không có combo nào
                      </Text>
                    </div>
                  }
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                />
              </Card>
            ) : (
              <div className="combos-grid">
                <Row gutter={[32, 32]} align="stretch">
                  {combos.map((combo) => (
                    <Col
                      key={combo.comboId || combo.id}
                      xs={24}
                      sm={24}
                      md={12}
                      lg={8}
                      style={{
                        display: "flex",
                        alignItems: "stretch",
                      }}
                    >
                      <div
                        style={{
                          width: "100%",
                          display: "flex",
                          flexDirection: "column",
                        }}
                      >
                        {renderComboCard(combo)}
                      </div>
                    </Col>
                  ))}
                </Row>
              </div>
            )}
          </div>
        )}

        {/* Order Modal */}
        <Modal
          title={
            <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
              <ShoppingCartOutlined
                style={{ fontSize: "20px", color: "#f59e0b" }}
              />
              <span>Đặt hàng ({selectedItems.size} món)</span>
            </div>
          }
          open={orderModalVisible}
          onCancel={() => {
            setOrderModalVisible(false);
            orderForm.resetFields();
          }}
          onOk={() => orderForm.submit()}
          okText="Xác nhận đặt hàng"
          cancelText="Hủy"
          width={700}
          okButtonProps={{
            style: {
              background: "linear-gradient(135deg, #f59e0b 0%, #dc2626 100%)",
              border: "none",
            },
          }}
        >
          <Form
            form={orderForm}
            layout="vertical"
            onFinish={handleOrderSubmit}
            initialValues={{
              orderType: "DINE_IN",
            }}
          >
            {/* Display selected items */}
            <div style={{ marginBottom: "24px" }}>
              <Text
                strong
                style={{
                  fontSize: "16px",
                  marginBottom: "12px",
                  display: "block",
                }}
              >
                Các món đã chọn:
              </Text>
              <div
                style={{
                  maxHeight: "300px",
                  overflowY: "auto",
                  border: "1px solid #f0f0f0",
                  borderRadius: "8px",
                  padding: "12px",
                }}
              >
                {Array.from(selectedItems.entries()).map(
                  ([itemId, selectedData]) => {
                    const { item, quantity, type } = selectedData;
                    const isCombo = type === "combo";
                    const displayPrice = isCombo ? item.price : item.price;
                    const totalPrice = displayPrice * quantity;

                    return (
                      <div
                        key={itemId}
                        style={{
                          display: "flex",
                          gap: "12px",
                          padding: "12px",
                          marginBottom: "8px",
                          background: "#f9fafb",
                          borderRadius: "8px",
                        }}
                      >
                        <Image
                          src={item.imageUrl || "/placeholder-food.jpg"}
                          alt={item.name}
                          width={60}
                          height={60}
                          style={{
                            objectFit: "cover",
                            borderRadius: "6px",
                          }}
                          fallback="/placeholder-food.jpg"
                        />
                        <div style={{ flex: 1 }}>
                          <div
                            style={{
                              display: "flex",
                              justifyContent: "space-between",
                              alignItems: "start",
                            }}
                          >
                            <div>
                              <Text strong style={{ fontSize: "14px" }}>
                                {item.name}
                              </Text>
                              {isCombo && (
                                <Tag
                                  color="orange"
                                  style={{ marginLeft: "8px" }}
                                >
                                  Combo
                                </Tag>
                              )}
                            </div>
                            <Text
                              strong
                              style={{ color: "#f59e0b", fontSize: "14px" }}
                            >
                              {displayPrice?.toLocaleString("vi-VN")} đ
                            </Text>
                          </div>
                          <div
                            style={{
                              display: "flex",
                              justifyContent: "space-between",
                              marginTop: "4px",
                            }}
                          >
                            <Text type="secondary" style={{ fontSize: "12px" }}>
                              Số lượng: {quantity}
                            </Text>
                            <Text strong style={{ fontSize: "14px" }}>
                              Tổng: {totalPrice.toLocaleString("vi-VN")} đ
                            </Text>
                          </div>
                        </div>
                      </div>
                    );
                  }
                )}
              </div>
            </div>

            <Form.Item
              name="orderType"
              label="Loại đơn hàng"
              rules={[
                { required: true, message: "Vui lòng chọn loại đơn hàng" },
              ]}
            >
              <Radio.Group>
                <Radio.Button value="DINE_IN">Ăn tại chỗ</Radio.Button>
                <Radio.Button value="TAKEOUT">Mang đi</Radio.Button>
                <Radio.Button value="DELIVERY">Giao hàng</Radio.Button>
              </Radio.Group>
            </Form.Item>

            <Form.Item
              noStyle
              shouldUpdate={(prevValues, currentValues) =>
                prevValues.orderType !== currentValues.orderType
              }
            >
              {({ getFieldValue }) =>
                getFieldValue("orderType") === "DELIVERY" ? (
                  <Form.Item
                    name="deliveryAddress"
                    label="Địa chỉ giao hàng"
                    rules={[
                      {
                        required: true,
                        message: "Vui lòng nhập địa chỉ giao hàng",
                      },
                    ]}
                  >
                    <Input.TextArea
                      rows={3}
                      placeholder="Nhập địa chỉ giao hàng"
                    />
                  </Form.Item>
                ) : getFieldValue("orderType") === "DINE_IN" ? (
                  <Form.Item
                    name="tableNumber"
                    label="Số bàn"
                    rules={[
                      { required: true, message: "Vui lòng nhập số bàn" },
                    ]}
                  >
                    <Input placeholder="Nhập số bàn" />
                  </Form.Item>
                ) : null
              }
            </Form.Item>

            <Form.Item name="customerName" label="Tên khách hàng">
              <Input disabled />
            </Form.Item>

            <Form.Item name="customerPhone" label="Số điện thoại">
              <Input disabled />
            </Form.Item>

            <Form.Item name="notes" label="Ghi chú (tùy chọn)">
              <Input.TextArea
                rows={3}
                placeholder="Ghi chú thêm cho món ăn..."
              />
            </Form.Item>

            <Form.Item
              noStyle
              shouldUpdate={(prevValues, currentValues) =>
                prevValues.orderType !== currentValues.orderType
              }
            >
              {({ getFieldValue }) => {
                // Calculate totals from selected items
                let subtotal = 0;
                let totalDiscount = 0;

                selectedItems.forEach((selectedData) => {
                  const { item, quantity, type } = selectedData;
                  if (type === "combo") {
                    const comboItems = item.menuItemIds || item.items || [];
                    const originalPrice = comboItems.reduce(
                      (sum, comboItem) => {
                        let menuItemId,
                          itemQuantity = 1;

                        // Check if comboItem is a string (ID) or object
                        if (typeof comboItem === "string") {
                          menuItemId = comboItem;
                        } else if (comboItem && typeof comboItem === "object") {
                          menuItemId =
                            comboItem.menuItemId || comboItem.id || comboItem;
                          itemQuantity = comboItem.quantity || 1;
                        } else {
                          menuItemId = comboItem;
                        }

                        const menuItem = menuItems.find(
                          (mi) =>
                            mi.menuItemId === menuItemId || mi.id === menuItemId
                        );
                        return sum + (menuItem?.price || 0) * itemQuantity;
                      },
                      0
                    );
                    subtotal += originalPrice * quantity;
                    totalDiscount +=
                      (originalPrice - (item.price || 0)) * quantity;
                  } else {
                    subtotal += (item.price || 0) * quantity;
                  }
                });

                const discountAmount = totalDiscount;
                const totalAfterDiscount = subtotal - discountAmount;
                const vat = totalAfterDiscount * 0.1;
                const finalTotal = totalAfterDiscount + vat;

                return (
                  <div
                    style={{
                      padding: "16px",
                      background: "#f9fafb",
                      borderRadius: "12px",
                      marginTop: "16px",
                    }}
                  >
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        marginBottom: "8px",
                      }}
                    >
                      <Typography.Text>Tạm tính:</Typography.Text>
                      <Typography.Text strong>
                        {subtotal.toLocaleString("vi-VN")} đ
                      </Typography.Text>
                    </div>
                    {discountAmount > 0 && (
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          marginBottom: "8px",
                        }}
                      >
                        <Typography.Text>Giảm giá:</Typography.Text>
                        <Typography.Text strong style={{ color: "#52c41a" }}>
                          -{discountAmount.toLocaleString("vi-VN")} đ
                        </Typography.Text>
                      </div>
                    )}
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        marginBottom: "8px",
                      }}
                    >
                      <Typography.Text>VAT (10%):</Typography.Text>
                      <Typography.Text strong>
                        {vat.toLocaleString("vi-VN")} đ
                      </Typography.Text>
                    </div>
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        paddingTop: "12px",
                        borderTop: "2px solid #e5e7eb",
                      }}
                    >
                      <Typography.Text strong style={{ fontSize: "16px" }}>
                        Tổng cộng:
                      </Typography.Text>
                      <Typography.Text
                        strong
                        style={{
                          fontSize: "18px",
                          color: "#f59e0b",
                        }}
                      >
                        {finalTotal.toLocaleString("vi-VN")} đ
                      </Typography.Text>
                    </div>
                  </div>
                );
              }}
            </Form.Item>
          </Form>
        </Modal>
      </div>

      {/* Fixed Order Button - Always visible at bottom right of viewport */}
      {selectedItems.size > 0 &&
        createPortal(
          <div
            style={{
              position: "fixed",
              bottom: "24px",
              right: "24px",
              zIndex: 9999,
              pointerEvents: "auto",
            }}
          >
            <Badge count={selectedItems.size} showZero>
              <Button
                type="primary"
                size="large"
                icon={<ShoppingCartOutlined />}
                onClick={handleOrderClick}
                style={{
                  background:
                    "linear-gradient(135deg, #f59e0b 0%, #dc2626 100%)",
                  border: "none",
                  borderRadius: "50px",
                  height: "56px",
                  padding: "0 32px",
                  fontSize: "16px",
                  fontWeight: "600",
                  boxShadow: "0 4px 12px rgba(245, 158, 11, 0.4)",
                  transition: "all 0.3s ease",
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = "scale(1.05)";
                  e.currentTarget.style.boxShadow =
                    "0 6px 16px rgba(245, 158, 11, 0.5)";
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = "scale(1)";
                  e.currentTarget.style.boxShadow =
                    "0 4px 12px rgba(245, 158, 11, 0.4)";
                }}
              >
                Đặt hàng ({selectedItems.size} món)
              </Button>
            </Badge>
          </div>,
          document.body
        )}
    </>
  );
};

export default MenuView;
