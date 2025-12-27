import React, { useState, useEffect } from "react";
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
} from "antd";
import {
  SearchOutlined,
  FireOutlined,
  ShoppingCartOutlined,
  PlusOutlined,
  MinusOutlined,
} from "@ant-design/icons";
import apiService from "../services/apiService";
import { useAuth } from "../context/AuthContext";
import Loading from "../components/Common/Loading";
import { useDataRefresh, DATA_REFRESH_EVENTS, listenToDataRefresh } from "../utils/dataRefreshEvents";

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
  const [selectedItem, setSelectedItem] = useState(null);
  const [orderForm] = Form.useForm();

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

  useEffect(() => {
    loadMenuItems();
    loadCategories();
  }, [filters]);

  // Listen to category changes to reload categories
  useEffect(() => {
    const eventNames = [DATA_REFRESH_EVENTS.CATEGORY_CREATED, DATA_REFRESH_EVENTS.CATEGORY_UPDATED, DATA_REFRESH_EVENTS.CATEGORY_DELETED];
    const cleanupFunctions = [];

    eventNames.forEach(eventName => {
      const cleanup = listenToDataRefresh(eventName, () => {
        loadCategories();
        // Also reload menu items in case category filter is active
        loadMenuItems();
      });
      cleanupFunctions.push(cleanup);
    });

    return () => {
      cleanupFunctions.forEach(cleanup => cleanup());
    };
  }, []);

  // Listen to menu item changes to reload menu items
  useEffect(() => {
    const eventNames = [DATA_REFRESH_EVENTS.MENU_ITEM_CREATED, DATA_REFRESH_EVENTS.MENU_ITEM_UPDATED, DATA_REFRESH_EVENTS.MENU_ITEM_DELETED];
    const cleanupFunctions = [];

    eventNames.forEach(eventName => {
      const cleanup = listenToDataRefresh(eventName, () => {
        loadMenuItems();
      });
      cleanupFunctions.push(cleanup);
    });

    return () => {
      cleanupFunctions.forEach(cleanup => cleanup());
    };
  }, []);

    const handleSearch = (value) => {
    setFilters((prev) => ({ ...prev, search: value }));
    };

    const handleCategoryChange = (value) => {
    setFilters((prev) => ({ ...prev, categoryId: value || "" }));
  };

  const handleOrderClick = (item) => {
    if (!user) {
      antMessage.warning("Vui lòng đăng nhập để đặt món");
      return;
    }
    setSelectedItem(item);
    orderForm.setFieldsValue({
      quantity: 1,
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

  const handleOrderSubmit = async (values) => {
    try {
      const orderData = {
        orderType: values.orderType,
        customerName: values.customerName,
        customerPhone: values.customerPhone,
        orderItems: [
          {
            menuItemId: selectedItem.menuItemId,
            name: selectedItem.name,
            quantity: values.quantity,
            unitPrice: selectedItem.price,
            subtotal: selectedItem.price * values.quantity,
            notes: values.notes || "",
          },
        ],
        discountPercentage: 0,
        discountAmount: 0,
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
      setSelectedItem(null);
    } catch (error) {
      console.error("Error creating order:", error);
      antMessage.error(error.message || "Không thể đặt món. Vui lòng thử lại!");
    }
    };

    if (loading) {
        return <Loading tip="Đang tải thực đơn..." />;
    }

    return (
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
              count={menuItems.length}
              showZero
              style={{ backgroundColor: "#f59e0b" }}
            >
              <Text strong style={{ fontSize: "14px", color: "#262626" }}>
                {menuItems.length} món
              </Text>
            </Badge>
                        </Col>
                    </Row>
            </Card>

            {/* Menu Items Grid */}
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
            ) : (
        <div className="menu-items-grid">
          {menuItems.map((item) => (
                            <Card
              key={item.menuItemId}
                                hoverable
              className="menu-item-card"
              style={{
                height: "100%",
                borderRadius: "20px",
                overflow: "hidden",
                transition: "all 0.4s cubic-bezier(0.4, 0, 0.2, 1)",
                border: "1px solid #e5e7eb",
                background: "#ffffff",
              }}
              styles={{ body: { padding: 0 } }}
                                cover={
                <div
                  style={{
                    height: "240px",
                    overflow: "hidden",
                    backgroundColor: "#f9fafb",
                    position: "relative",
                  }}
                >
                                        <Image
                    src={item.imageUrl || "/placeholder-food.jpg"}
                                            alt={item.name}
                    height={240}
                                            width="100%"
                    style={{
                      objectFit: "cover",
                      transition: "transform 0.5s cubic-bezier(0.4, 0, 0.2, 1)",
                    }}
                                            fallback="/placeholder-food.jpg"
                    preview={{
                      mask: "Xem ảnh",
                    }}
                                        />
                  {item.active && (
                    <div className="menu-item-badge">
                      <span>Đang bán</span>
                    </div>
                  )}
                                    </div>
                                }
            >
              <div className="menu-item-content">
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "flex-start",
                    marginBottom: "10px",
                    flexWrap: "wrap",
                    gap: "8px",
                  }}
                >
                  <h3 className="menu-item-title">{item.name}</h3>
                                        {item.categoryName && (
                    <span className="menu-item-category-tag">
                      {item.categoryName}
                    </span>
                                        )}
                                    </div>
                                    {item.description && (
                  <p className="menu-item-description">{item.description}</p>
                )}
                <div className="menu-item-divider">
                  <div className="menu-item-price">
                    <FireOutlined className="menu-item-price-icon" />
                    <span>{item.price?.toLocaleString("vi-VN")} đ</span>
                                        </div>
                  <Button
                    type="primary"
                    icon={<ShoppingCartOutlined />}
                    onClick={() => handleOrderClick(item)}
                    className="menu-item-order-btn"
                    style={{
                      background:
                        "linear-gradient(135deg, #f59e0b 0%, #dc2626 100%)",
                      border: "none",
                      borderRadius: "12px",
                      height: "40px",
                      fontWeight: "600",
                      boxShadow: "0 4px 12px rgba(245, 158, 11, 0.3)",
                      transition: "all 0.3s ease",
                    }}
                  >
                    Đặt món
                  </Button>
                                    </div>
                                </div>
                            </Card>
          ))}
        </div>
      )}

      {/* Order Modal */}
      <Modal
        title={
          <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
            <ShoppingCartOutlined
              style={{ fontSize: "20px", color: "#f59e0b" }}
            />
            <span>Đặt món: {selectedItem?.name}</span>
          </div>
        }
        open={orderModalVisible}
        onCancel={() => {
          setOrderModalVisible(false);
          orderForm.resetFields();
          setSelectedItem(null);
        }}
        onOk={() => orderForm.submit()}
        okText="Xác nhận đặt món"
        cancelText="Hủy"
        width={600}
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
            quantity: 1,
            orderType: "DINE_IN",
          }}
        >
          {selectedItem && (
            <div
              style={{
                marginBottom: "24px",
                padding: "16px",
                background: "#f9fafb",
                borderRadius: "12px",
                display: "flex",
                gap: "16px",
              }}
            >
              <Image
                src={selectedItem.imageUrl || "/placeholder-food.jpg"}
                alt={selectedItem.name}
                width={100}
                height={100}
                style={{
                  objectFit: "cover",
                  borderRadius: "8px",
                }}
                fallback="/placeholder-food.jpg"
              />
              <div style={{ flex: 1 }}>
                <Typography.Title
                  level={5}
                  style={{ margin: 0, marginBottom: "8px" }}
                >
                  {selectedItem.name}
                </Typography.Title>
                <Typography.Text type="secondary" style={{ fontSize: "14px" }}>
                  {selectedItem.description}
                </Typography.Text>
                <div
                  style={{
                    marginTop: "12px",
                    fontSize: "18px",
                    fontWeight: "700",
                    color: "#f59e0b",
                  }}
                >
                  {selectedItem.price?.toLocaleString("vi-VN")} đ
                </div>
              </div>
            </div>
          )}

          <Form.Item
            name="quantity"
            label="Số lượng"
            rules={[
              { required: true, message: "Vui lòng nhập số lượng" },
              { type: "number", min: 1, message: "Số lượng phải lớn hơn 0" },
            ]}
          >
            <InputNumber
              min={1}
              max={100}
              style={{ width: "100%" }}
              addonBefore={<MinusOutlined />}
              addonAfter={<PlusOutlined />}
              controls={{
                upIcon: <PlusOutlined />,
                downIcon: <MinusOutlined />,
              }}
            />
          </Form.Item>

          <Form.Item
            name="orderType"
            label="Loại đơn hàng"
            rules={[{ required: true, message: "Vui lòng chọn loại đơn hàng" }]}
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
                  rules={[{ required: true, message: "Vui lòng nhập số bàn" }]}
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
            <Input.TextArea rows={3} placeholder="Ghi chú thêm cho món ăn..." />
          </Form.Item>

          <Form.Item
            noStyle
            shouldUpdate={(prevValues, currentValues) =>
              prevValues.quantity !== currentValues.quantity ||
              prevValues.orderType !== currentValues.orderType
            }
          >
            {({ getFieldValue }) => {
              const quantity = getFieldValue("quantity") || 1;
              const total = selectedItem ? selectedItem.price * quantity : 0;
              const vat = total * 0.1;
              const finalTotal = total + vat;

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
                      {total.toLocaleString("vi-VN")} đ
                    </Typography.Text>
                  </div>
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
    );
};

export default MenuView;
