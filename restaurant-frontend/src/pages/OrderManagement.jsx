import React, { useState, useEffect } from "react";
import {
  Card,
  Table,
  Button,
  Input,
  Select,
  Space,
  Tag,
  App,
  Row,
  Col,
  Statistic,
  DatePicker,
  Modal,
  Form,
  InputNumber,
  message,
  Descriptions,
  Divider,
  Popconfirm,
} from "antd";
import {
  ReloadOutlined,
  SearchOutlined,
  PlusOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  TruckOutlined,
  ShopOutlined,
} from "@ant-design/icons";
import dayjs from "dayjs";
import apiService from "../services/apiService";
import {
  PAGINATION,
  ORDER_TYPES,
  ORDER_STATUS,
  DATETIME_FORMAT,
} from "../constants.js";
import { useAuth } from "../context/AuthContext";
import { canManageMenu } from "../utils/auth";
import Loading from "../components/Common/Loading";
import ErrorPage from "../components/Common/ErrorPage";
import {
  listenToDataRefresh,
  dispatchDataRefresh,
  DATA_REFRESH_EVENTS,
} from "../utils/dataRefreshEvents";

const { Option } = Select;
const { Search } = Input;
const { RangePicker } = DatePicker;

const OrderManagement = () => {
  const { message: antdMessage } = App.useApp();
  const { role, user, loading: authLoading } = useAuth();
  const [form] = Form.useForm();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState({
    status: "",
    type: "",
    startDate: null,
    endDate: null,
  });
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [menuItems, setMenuItems] = useState([]);

  const canManage = canManageMenu(role);
  const isCustomer = role === "CUSTOMER";

  useEffect(() => {
    // Wait for auth to finish loading before making API calls
    if (authLoading) {
      return;
    }

    // Check if user is authenticated - ProtectedRoute will handle redirect if not
    const token = localStorage.getItem("accessToken");
    if (!token) {
      // Token not found, ProtectedRoute will redirect
      return;
    }

    // Debug: Log user role
    if (import.meta.env.DEV) {
      console.log("OrderManagement - User role:", role, "User:", user);
    }

    // Only load orders if we have a valid token
    loadOrders();
    if (canManage) {
      loadMenuItems();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters, role, authLoading]);

  // Listen to menu item changes to reload menu items
  useEffect(() => {
    if (!canManage) return;

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
  }, [canManage]);

  const loadOrders = async () => {
    // Check authentication before making request
    const token = localStorage.getItem("accessToken");
    if (!token) {
      console.warn("No access token, cannot load orders");
      return;
    }

    setLoading(true);
    try {
      const params = {
        ...(filters.status && { status: filters.status }),
        ...(filters.type && { type: filters.type }),
        ...(filters.startDate && { startDate: filters.startDate }),
        ...(filters.endDate && { endDate: filters.endDate }),
        // Don't pass customerId for customers - backend will auto-set from X-User-Id header
        // This prevents 403 errors when user info is not yet cached in Order Service
        // Only staff/managers can filter by customerId to view other users' orders
        ...(!isCustomer &&
          filters.customerId && { customerId: filters.customerId }),
      };

      const response = await apiService.order.getAllOrders(params);
      console.log("Orders API response:", response); // Debug
      // Backend returns ApiResponseDTO<List<OrderResponse>>, interceptor extracts .data
      // So response should be the list directly
      const ordersList = Array.isArray(response) ? response : [];
      console.log("Orders list length:", ordersList.length); // Debug
      setOrders(ordersList);
    } catch (error) {
      console.error("Error loading orders:", error);

      // Handle 403 Forbidden - user might not have role in JWT token
      if (error.response?.status === 403) {
        console.warn(
          "403 Forbidden - User role in database:",
          role,
          "but JWT token might not have this role."
        );
        console.warn(
          "Solution: Admin needs to sync role in Staff Management, then user needs to logout and login again."
        );
        antdMessage.warning({
          content:
            "Bạn không có quyền truy cập. Vui lòng liên hệ admin để khởi tạo role KITCHEN_STAFF trong Keycloak (Admin > Initialize Roles) và đồng bộ role, sau đó đăng xuất và đăng nhập lại.",
          duration: 10,
        });
        return;
      }

      // Don't show error message if it's a 401 (authentication issue)
      if (error.response?.status !== 401) {
        antdMessage.error("Không thể tải danh sách đơn hàng");
      }
    } finally {
      setLoading(false);
    }
  };

  const loadMenuItems = async () => {
    try {
      const response = await apiService.menu.getMenuItems({
        page: 0,
        size: 1000,
        active: true,
      });
      setMenuItems(response?.items || []);
    } catch (error) {
      console.error("Error loading menu items:", error);
    }
  };

  const getStatusTag = (status) => {
    const statusConfig = {
      PENDING: {
        color: "orange",
        icon: <ClockCircleOutlined />,
        text: "Chờ xử lý",
      },
      COOKING: {
        color: "blue",
        icon: <ClockCircleOutlined />,
        text: "Đang chế biến",
      },
      READY: { color: "cyan", icon: <CheckCircleOutlined />, text: "Sẵn sàng" },
      DELIVERING: {
        color: "purple",
        icon: <TruckOutlined />,
        text: "Đang giao",
      },
      COMPLETED: {
        color: "green",
        icon: <CheckCircleOutlined />,
        text: "Hoàn thành",
      },
      CANCELLED: {
        color: "red",
        icon: <CloseCircleOutlined />,
        text: "Đã hủy",
      },
    };
    const config = statusConfig[status] || { color: "default", text: status };
    return (
      <Tag color={config.color} icon={config.icon}>
        {config.text}
      </Tag>
    );
  };

  const getTypeTag = (type) => {
    const typeConfig = {
      DINE_IN: { color: "blue", text: "Ăn tại chỗ" },
      TAKEAWAY: { color: "orange", text: "Mang đi" },
      DELIVERY: { color: "green", text: "Giao hàng" },
    };
    const config = typeConfig[type] || { color: "default", text: type };
    return <Tag color={config.color}>{config.text}</Tag>;
  };

  const handleStatusUpdate = async (orderId, action) => {
    try {
      let response;
      switch (action) {
        case "start-cooking":
          response = await apiService.order.startCooking(orderId);
          break;
        case "mark-ready":
          response = await apiService.order.markReady(orderId);
          break;
        case "start-delivering":
          response = await apiService.order.startDelivering(orderId);
          break;
        case "complete":
          response = await apiService.order.completeOrder(orderId);
          break;
        default:
          return;
      }
      antdMessage.success("Cập nhật trạng thái thành công");
      dispatchDataRefresh(DATA_REFRESH_EVENTS.ORDER_UPDATED, {
        orderId,
        action,
      });
      loadOrders();
    } catch (error) {
      console.error("Error updating status:", error);
      antdMessage.error("Không thể cập nhật trạng thái");
    }
  };

  const handleCancelOrder = async (orderId, reason) => {
    try {
      await apiService.order.cancelOrder(orderId, reason, canManage);
      antdMessage.success("Hủy đơn hàng thành công");
      dispatchDataRefresh(DATA_REFRESH_EVENTS.ORDER_CANCELLED, {
        orderId,
        reason,
      });
      loadOrders();
    } catch (error) {
      console.error("Error cancelling order:", error);
      antdMessage.error("Không thể hủy đơn hàng");
    }
  };

  const handleCreateOrder = async (values) => {
    try {
      // Process order items: load prices and calculate subtotals
      const processedOrderItems = values.orderItems.map((item) => {
        const menuItem = menuItems.find(
          (m) => m.menuItemId === item.menuItemId
        );
        if (!menuItem) {
          throw new Error(`Menu item not found: ${item.menuItemId}`);
        }
        return {
          menuItemId: item.menuItemId,
          name: menuItem.name,
          quantity: item.quantity,
          unitPrice: menuItem.price,
          subtotal: menuItem.price * item.quantity,
          notes: item.notes || "",
        };
      });

      const orderData = {
        orderType: values.orderType,
        customerName: values.customerName,
        customerPhone: values.customerPhone,
        orderItems: processedOrderItems,
        discountPercentage: values.discountPercentage || 0,
        discountAmount: values.discountAmount || 0,
        vatPercentage: values.vatPercentage || 10,
        deliveryAddress: values.deliveryAddress || null,
        tableNumber: values.tableNumber || null,
        notes: values.notes || null,
      };

      await apiService.order.createOrder(orderData);
      antdMessage.success("Tạo đơn hàng thành công");
      dispatchDataRefresh(DATA_REFRESH_EVENTS.ORDER_CREATED, orderData);
      setCreateModalVisible(false);
      form.resetFields();
      loadOrders();
    } catch (error) {
      console.error("Error creating order:", error);
      antdMessage.error(error.message || "Không thể tạo đơn hàng");
    }
  };

  const showOrderDetail = async (orderId) => {
    try {
      const order = await apiService.order.getOrderById(orderId);
      setSelectedOrder(order);
      setDetailModalVisible(true);
    } catch (error) {
      console.error("Error loading order detail:", error);
      antdMessage.error("Không thể tải chi tiết đơn hàng");
    }
  };

  const getActionButtons = (order) => {
    const buttons = [];
    const { orderStatus, orderType } = order;

    if (canManage) {
      if (orderStatus === "PENDING") {
        buttons.push(
          <Button
            key="start-cooking"
            type="primary"
            size="small"
            onClick={() => handleStatusUpdate(order.orderId, "start-cooking")}
          >
            Bắt đầu nấu
          </Button>
        );
      }
      if (orderStatus === "COOKING") {
        buttons.push(
          <Button
            key="mark-ready"
            type="primary"
            size="small"
            onClick={() => handleStatusUpdate(order.orderId, "mark-ready")}
          >
            Đánh dấu sẵn sàng
          </Button>
        );
      }
      if (orderStatus === "READY" && orderType === "DELIVERY") {
        buttons.push(
          <Button
            key="start-delivering"
            type="primary"
            size="small"
            onClick={() =>
              handleStatusUpdate(order.orderId, "start-delivering")
            }
          >
            Bắt đầu giao hàng
          </Button>
        );
      }
      if (orderStatus === "READY" || orderStatus === "DELIVERING") {
        buttons.push(
          <Button
            key="complete"
            type="primary"
            size="small"
            onClick={() => handleStatusUpdate(order.orderId, "complete")}
          >
            Hoàn thành
          </Button>
        );
      }
    }

    if (
      (orderStatus === "PENDING" || orderStatus === "COOKING") &&
      (isCustomer || canManage)
    ) {
      buttons.push(
        <Popconfirm
          key="cancel"
          title="Xác nhận hủy đơn hàng"
          description="Bạn có chắc chắn muốn hủy đơn hàng này?"
          onConfirm={() => {
            Modal.confirm({
              title: "Lý do hủy đơn",
              content: (
                <Input.TextArea
                  placeholder="Nhập lý do hủy đơn..."
                  rows={4}
                  id="cancellation-reason"
                />
              ),
              onOk: () => {
                const reason = document.getElementById(
                  "cancellation-reason"
                ).value;
                handleCancelOrder(order.orderId, reason || "Không có lý do");
              },
            });
          }}
          okText="Xác nhận"
          cancelText="Hủy"
        >
          <Button danger size="small">
            Hủy đơn
          </Button>
        </Popconfirm>
      );
    }

    return buttons.length > 0 ? <Space>{buttons}</Space> : null;
  };

  const columns = [
    {
      title: "Mã đơn",
      dataIndex: "orderId",
      key: "orderId",
      width: 200,
      render: (text) => (
        <strong
          style={{
            fontFamily: "monospace",
            color: "#f59e0b",
            fontSize: "14px",
          }}
        >
          {text.substring(0, 8)}...
        </strong>
      ),
    },
    ...(canManage
      ? [
          {
            title: "Khách hàng",
            key: "customer",
            width: 150,
            render: (_, record) => (
              <div>
                <div style={{ fontWeight: "500" }}>
                  {record.customerName || "N/A"}
                </div>
                <small style={{ color: "#8c8c8c", fontSize: "12px" }}>
                  {record.customerPhone || ""}
                </small>
              </div>
            ),
          },
        ]
      : []),
    {
      title: "Loại",
      dataIndex: "orderType",
      key: "orderType",
      width: 100,
      render: getTypeTag,
    },
    {
      title: "Trạng thái",
      dataIndex: "orderStatus",
      key: "orderStatus",
      width: 140,
      render: getStatusTag,
    },
    {
      title: "Tổng tiền",
      dataIndex: "totalAmount",
      key: "totalAmount",
      width: 140,
      align: "right",
      render: (amount) => (
        <span
          style={{
            fontWeight: "600",
            fontSize: "15px",
            color: "#f59e0b",
          }}
        >
          {amount
            ? new Intl.NumberFormat("vi-VN").format(amount) + " đ"
            : "0 đ"}
        </span>
      ),
    },
    {
      title: "Ngày tạo",
      dataIndex: "orderDate",
      key: "orderDate",
      width: 160,
      render: (date) =>
        date ? (
          <div>
            <div>{dayjs(date).format("DD/MM/YYYY")}</div>
            <small style={{ color: "#8c8c8c", fontSize: "12px" }}>
              {dayjs(date).format("HH:mm")}
            </small>
          </div>
        ) : (
          "-"
        ),
    },
    {
      title: "Thao tác",
      key: "action",
      width: isCustomer ? 150 : 200,
      fixed: "right",
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => showOrderDetail(record.orderId)}
            style={{ padding: "4px 8px" }}
          >
            {isCustomer ? "Xem" : "Chi tiết"}
          </Button>
          {getActionButtons(record)}
        </Space>
      ),
    },
  ];

  if (authLoading) {
    return <Loading tip="Đang kiểm tra quyền truy cập..." />;
  }

  const totalRevenue = orders
    .filter((o) => o.orderStatus === "COMPLETED")
    .reduce((sum, o) => sum + (o.totalAmount || 0), 0);

  const pendingCount = orders.filter((o) => o.orderStatus === "PENDING").length;
  const cookingCount = orders.filter((o) => o.orderStatus === "COOKING").length;
  const completedCount = orders.filter(
    (o) => o.orderStatus === "COMPLETED"
  ).length;

  return (
    <div style={{ padding: "24px", maxWidth: "1400px", margin: "0 auto" }}>
      {/* Header */}
      <div style={{ marginBottom: "24px" }}>
        <h1
          style={{
            fontSize: "clamp(20px, 4vw, 28px)",
            fontWeight: "700",
            marginBottom: "8px",
            color: "#262626",
          }}
        >
          {isCustomer ? "Đơn hàng của tôi" : "Quản lý đơn hàng"}
        </h1>
        <p style={{ color: "#8c8c8c", margin: 0, fontSize: "16px" }}>
          {isCustomer
            ? "Theo dõi trạng thái đơn hàng của bạn"
            : "Quản lý và xử lý đơn hàng"}
        </p>
      </div>

      {/* Statistics Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: "24px" }}>
        <Col xs={24} sm={12} md={isCustomer ? 8 : 6}>
          <Card
            style={{
              borderRadius: "12px",
              boxShadow: "0 2px 8px rgba(0, 0, 0, 0.08)",
            }}
          >
            <Statistic
              title={
                <span style={{ fontSize: "14px", color: "#8c8c8c" }}>
                  Đơn chờ xử lý
                </span>
              }
              value={pendingCount}
              prefix={<ClockCircleOutlined style={{ color: "#faad14" }} />}
              valueStyle={{
                color: "#faad14",
                fontSize: "24px",
                fontWeight: "600",
              }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={isCustomer ? 8 : 6}>
          <Card
            style={{
              borderRadius: "12px",
              boxShadow: "0 2px 8px rgba(0, 0, 0, 0.08)",
            }}
          >
            <Statistic
              title={
                <span style={{ fontSize: "14px", color: "#8c8c8c" }}>
                  Đang chế biến
                </span>
              }
              value={cookingCount}
              prefix={<ClockCircleOutlined style={{ color: "#1890ff" }} />}
              valueStyle={{
                color: "#1890ff",
                fontSize: "24px",
                fontWeight: "600",
              }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={isCustomer ? 8 : 6}>
          <Card
            style={{
              borderRadius: "12px",
              boxShadow: "0 2px 8px rgba(0, 0, 0, 0.08)",
            }}
          >
            <Statistic
              title={
                <span style={{ fontSize: "14px", color: "#8c8c8c" }}>
                  Đã hoàn thành
                </span>
              }
              value={completedCount}
              prefix={<CheckCircleOutlined style={{ color: "#52c41a" }} />}
              valueStyle={{
                color: "#52c41a",
                fontSize: "24px",
                fontWeight: "600",
              }}
            />
          </Card>
        </Col>
        {canManage && (
          <Col xs={24} sm={12} md={6}>
            <Card
              style={{
                borderRadius: "12px",
                boxShadow: "0 2px 8px rgba(0, 0, 0, 0.08)",
              }}
            >
              <Statistic
                title={
                  <span style={{ fontSize: "14px", color: "#8c8c8c" }}>
                    Tổng doanh thu
                  </span>
                }
                value={totalRevenue}
                prefix="đ"
                precision={0}
                valueStyle={{
                  color: "#52c41a",
                  fontSize: "24px",
                  fontWeight: "600",
                }}
              />
            </Card>
          </Col>
        )}
        {isCustomer && (
          <Col xs={24} sm={12} md={8}>
            <Card
              style={{
                borderRadius: "12px",
                boxShadow: "0 2px 8px rgba(0, 0, 0, 0.08)",
              }}
            >
              <Statistic
                title={
                  <span style={{ fontSize: "14px", color: "#8c8c8c" }}>
                    Tổng chi tiêu
                  </span>
                }
                value={totalRevenue}
                prefix="đ"
                precision={0}
                valueStyle={{
                  color: "#f59e0b",
                  fontSize: "24px",
                  fontWeight: "600",
                }}
              />
            </Card>
          </Col>
        )}
      </Row>

      <Card
        style={{
          borderRadius: "12px",
          boxShadow: "0 2px 8px rgba(0, 0, 0, 0.08)",
        }}
      >
        <Space
          style={{
            marginBottom: "16px",
            width: "100%",
            justifyContent: "space-between",
            flexWrap: "wrap",
          }}
        >
          <Space wrap>
            <Search
              placeholder="Tìm kiếm đơn hàng..."
              allowClear
              style={{ width: isCustomer ? 200 : 300 }}
              size="large"
              onSearch={(value) => {
                // Implement search if needed
              }}
            />
            <Select
              placeholder="Trạng thái"
              allowClear
              style={{ width: 150 }}
              size="large"
              value={filters.status}
              onChange={(value) => setFilters({ ...filters, status: value })}
            >
              {Object.entries(ORDER_STATUS).map(([key, value]) => (
                <Option key={key} value={value}>
                  {getStatusTag(value).props.children}
                </Option>
              ))}
            </Select>
            {!isCustomer && (
              <Select
                placeholder="Loại đơn"
                allowClear
                style={{ width: 150 }}
                size="large"
                value={filters.type}
                onChange={(value) => setFilters({ ...filters, type: value })}
              >
                {Object.entries(ORDER_TYPES).map(([key, value]) => (
                  <Option key={key} value={value}>
                    {getTypeTag(value).props.children}
                  </Option>
                ))}
              </Select>
            )}
            <RangePicker
              placeholder={["Từ ngày", "Đến ngày"]}
              size="large"
              onChange={(dates) => {
                if (dates) {
                  setFilters({
                    ...filters,
                    startDate: dates[0]?.format("YYYY-MM-DDTHH:mm:ss"),
                    endDate: dates[1]?.format("YYYY-MM-DDTHH:mm:ss"),
                  });
                } else {
                  setFilters({
                    ...filters,
                    startDate: null,
                    endDate: null,
                  });
                }
              }}
            />
          </Space>
          <Space wrap>
            {canManage && (
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => setCreateModalVisible(true)}
                size="large"
                style={{ borderRadius: "8px" }}
              >
                Tạo đơn hàng
              </Button>
            )}
            <Button
              icon={<ReloadOutlined />}
              onClick={loadOrders}
              size="large"
              style={{ borderRadius: "8px" }}
            >
              Làm mới
            </Button>
          </Space>
        </Space>

        <Table
          columns={columns}
          dataSource={orders}
          rowKey="orderId"
          loading={loading}
          scroll={{ x: isCustomer ? 1000 : 1200 }}
          pagination={{
            defaultPageSize: PAGINATION.DEFAULT_PAGE_SIZE,
            pageSizeOptions: PAGINATION.PAGE_SIZE_OPTIONS,
            showSizeChanger: true,
            showTotal: (total) => `Tổng cộng: ${total} đơn hàng`,
            style: { marginTop: "16px" },
          }}
          style={{
            borderRadius: "8px",
          }}
        />
      </Card>

      <Modal
        title="Chi tiết đơn hàng"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={null}
        width={800}
      >
        {selectedOrder && (
          <div>
            <Descriptions bordered column={2}>
              <Descriptions.Item label="Mã đơn">
                {selectedOrder.orderId}
              </Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                {getStatusTag(selectedOrder.orderStatus)}
              </Descriptions.Item>
              <Descriptions.Item label="Loại đơn">
                {getTypeTag(selectedOrder.orderType)}
              </Descriptions.Item>
              <Descriptions.Item label="Khách hàng">
                {selectedOrder.customerName} - {selectedOrder.customerPhone}
              </Descriptions.Item>
              {selectedOrder.tableNumber && (
                <Descriptions.Item label="Số bàn">
                  {selectedOrder.tableNumber}
                </Descriptions.Item>
              )}
              {selectedOrder.deliveryAddress && (
                <Descriptions.Item label="Địa chỉ giao hàng" span={2}>
                  {selectedOrder.deliveryAddress}
                </Descriptions.Item>
              )}
              <Descriptions.Item label="Ngày tạo">
                {selectedOrder.orderDate
                  ? dayjs(selectedOrder.orderDate).format(DATETIME_FORMAT)
                  : "-"}
              </Descriptions.Item>
              {selectedOrder.cookingStartTime && (
                <Descriptions.Item label="Bắt đầu nấu">
                  {dayjs(selectedOrder.cookingStartTime).format(
                    DATETIME_FORMAT
                  )}
                </Descriptions.Item>
              )}
              {selectedOrder.readyTime && (
                <Descriptions.Item label="Sẵn sàng">
                  {dayjs(selectedOrder.readyTime).format(DATETIME_FORMAT)}
                </Descriptions.Item>
              )}
              {selectedOrder.completedTime && (
                <Descriptions.Item label="Hoàn thành">
                  {dayjs(selectedOrder.completedTime).format(DATETIME_FORMAT)}
                </Descriptions.Item>
              )}
            </Descriptions>

            <Divider>Danh sách món ăn</Divider>
            <Table
              dataSource={selectedOrder.orderItems || []}
              rowKey={(record, index) => index}
              pagination={false}
              columns={[
                { title: "Món ăn", dataIndex: "name", key: "name" },
                {
                  title: "Số lượng",
                  dataIndex: "quantity",
                  key: "quantity",
                  align: "center",
                },
                {
                  title: "Đơn giá",
                  dataIndex: "unitPrice",
                  key: "unitPrice",
                  align: "right",
                  render: (price) =>
                    new Intl.NumberFormat("vi-VN").format(price) + " đ",
                },
                {
                  title: "Thành tiền",
                  dataIndex: "subtotal",
                  key: "subtotal",
                  align: "right",
                  render: (subtotal) =>
                    new Intl.NumberFormat("vi-VN").format(subtotal) + " đ",
                },
              ]}
            />

            <Divider />
            <Descriptions bordered>
              <Descriptions.Item label="Tạm tính">
                {new Intl.NumberFormat("vi-VN").format(
                  selectedOrder.subtotal || 0
                )}{" "}
                đ
              </Descriptions.Item>
              {selectedOrder.discountAmount > 0 && (
                <Descriptions.Item label="Giảm giá">
                  -
                  {new Intl.NumberFormat("vi-VN").format(
                    selectedOrder.discountAmount
                  )}{" "}
                  đ
                </Descriptions.Item>
              )}
              {selectedOrder.vatAmount > 0 && (
                <Descriptions.Item label="VAT">
                  +
                  {new Intl.NumberFormat("vi-VN").format(
                    selectedOrder.vatAmount
                  )}{" "}
                  đ
                </Descriptions.Item>
              )}
              <Descriptions.Item
                label="Tổng tiền"
                style={{ fontWeight: "bold", fontSize: "16px" }}
              >
                {new Intl.NumberFormat("vi-VN").format(
                  selectedOrder.totalAmount || 0
                )}{" "}
                đ
              </Descriptions.Item>
            </Descriptions>

            {getActionButtons(selectedOrder) && (
              <>
                <Divider />
                <Space>{getActionButtons(selectedOrder)}</Space>
              </>
            )}
          </div>
        )}
      </Modal>

      <Modal
        title="Tạo đơn hàng mới"
        open={createModalVisible}
        onCancel={() => {
          setCreateModalVisible(false);
          form.resetFields();
        }}
        onOk={() => form.submit()}
        width={800}
      >
        <Form form={form} layout="vertical" onFinish={handleCreateOrder}>
          <Form.Item
            name="orderType"
            label="Loại đơn"
            rules={[{ required: true, message: "Vui lòng chọn loại đơn" }]}
          >
            <Select placeholder="Chọn loại đơn">
              {Object.entries(ORDER_TYPES).map(([key, value]) => (
                <Option key={key} value={value}>
                  {getTypeTag(value).props.children}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="customerName"
            label="Tên khách hàng"
            rules={[
              { required: true, message: "Vui lòng nhập tên khách hàng" },
            ]}
          >
            <Input placeholder="Nhập tên khách hàng" />
          </Form.Item>

          <Form.Item
            name="customerPhone"
            label="Số điện thoại"
            rules={[{ required: true, message: "Vui lòng nhập số điện thoại" }]}
          >
            <Input placeholder="Nhập số điện thoại" />
          </Form.Item>

          <Form.Item
            noStyle
            shouldUpdate={(prevValues, currentValues) =>
              prevValues.orderType !== currentValues.orderType
            }
          >
            {({ getFieldValue }) => {
              const orderType = getFieldValue("orderType");
              if (orderType === "DINE_IN") {
                return (
                  <Form.Item
                    name="tableNumber"
                    label="Số bàn"
                    rules={[
                      { required: true, message: "Vui lòng nhập số bàn" },
                    ]}
                  >
                    <Input placeholder="Nhập số bàn" />
                  </Form.Item>
                );
              }
              if (orderType === "DELIVERY") {
                return (
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
                      placeholder="Nhập địa chỉ giao hàng"
                      rows={3}
                    />
                  </Form.Item>
                );
              }
              return null;
            }}
          </Form.Item>

          <Form.Item
            name="orderItems"
            label="Danh sách món ăn"
            rules={[
              { required: true, message: "Vui lòng thêm ít nhất một món ăn" },
            ]}
          >
            <Form.List name="orderItems">
              {(fields, { add, remove }) => (
                <>
                  {fields.map(({ key, name, ...restField }) => (
                    <Space
                      key={key}
                      style={{ display: "flex", marginBottom: 8 }}
                      align="baseline"
                    >
                      <Form.Item
                        {...restField}
                        name={[name, "menuItemId"]}
                        rules={[{ required: true, message: "Chọn món ăn" }]}
                      >
                        <Select
                          style={{ width: 250 }}
                          placeholder="Chọn món"
                          showSearch
                          filterOption={(input, option) =>
                            (option?.children?.toLowerCase() || "").includes(
                              input.toLowerCase()
                            )
                          }
                        >
                          {menuItems.map((item) => (
                            <Option
                              key={item.menuItemId}
                              value={item.menuItemId}
                            >
                              {item.name} -{" "}
                              {new Intl.NumberFormat("vi-VN").format(
                                item.price
                              )}{" "}
                              đ
                            </Option>
                          ))}
                        </Select>
                      </Form.Item>
                      <Form.Item
                        {...restField}
                        name={[name, "quantity"]}
                        rules={[{ required: true, message: "Nhập số lượng" }]}
                      >
                        <InputNumber
                          min={1}
                          placeholder="SL"
                          style={{ width: 100 }}
                        />
                      </Form.Item>
                      <Form.Item {...restField} name={[name, "notes"]}>
                        <Input
                          placeholder="Ghi chú (tùy chọn)"
                          style={{ width: 200 }}
                        />
                      </Form.Item>
                      <Button onClick={() => remove(name)} danger>
                        Xóa
                      </Button>
                    </Space>
                  ))}
                  <Form.Item>
                    <Button
                      type="dashed"
                      onClick={() => add()}
                      block
                      icon={<PlusOutlined />}
                    >
                      Thêm món ăn
                    </Button>
                  </Form.Item>
                </>
              )}
            </Form.List>
          </Form.Item>

          <Form.Item name="discountPercentage" label="Giảm giá (%)">
            <InputNumber min={0} max={100} style={{ width: "100%" }} />
          </Form.Item>

          <Form.Item name="discountAmount" label="Giảm giá (đ)">
            <InputNumber min={0} style={{ width: "100%" }} />
          </Form.Item>

          <Form.Item name="vatPercentage" label="VAT (%)" initialValue={10}>
            <InputNumber min={0} max={100} style={{ width: "100%" }} />
          </Form.Item>

          <Form.Item name="notes" label="Ghi chú">
            <Input.TextArea rows={3} placeholder="Nhập ghi chú (nếu có)" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default OrderManagement;
