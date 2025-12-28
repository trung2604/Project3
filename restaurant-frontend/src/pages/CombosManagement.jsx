import React, { useState, useEffect, useCallback } from "react";
import {
  Card,
  Table,
  Button,
  Input,
  Select,
  Space,
  Tag,
  Modal,
  Form,
  InputNumber,
  App,
  Popconfirm,
  Row,
  Col,
  Statistic,
  Switch,
  Image,
  Upload,
  Divider,
  List,
  Typography,
} from "antd";
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
  ReloadOutlined,
  ShoppingCartOutlined,
  EyeOutlined,
  UploadOutlined,
  SettingOutlined,
  DollarOutlined,
  MinusOutlined,
} from "@ant-design/icons";
import apiService from "../services/apiService";
import { PAGINATION, STATUS } from "../constants.js";
import {
  listenToDataRefresh,
  dispatchDataRefresh,
  DATA_REFRESH_EVENTS,
} from "../utils/dataRefreshEvents";

const { Option } = Select;
const { Search } = Input;
const { TextArea } = Input;
const { Title, Text } = Typography;

const CombosManagement = () => {
  const { message } = App.useApp();
  const [combos, setCombos] = useState([]);
  const [menuItems, setMenuItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: PAGINATION.DEFAULT_PAGE_SIZE,
    total: 0,
  });
  const [filters, setFilters] = useState({
    active: null,
    search: "",
  });
  const [modalVisible, setModalVisible] = useState(false);
  const [modalType, setModalType] = useState("create");
  const [selectedCombo, setSelectedCombo] = useState(null);
  const [form] = Form.useForm();
  const [imagePreview, setImagePreview] = useState(null);
  const [uploading, setUploading] = useState(false);

  const loadCombos = useCallback(async () => {
    setLoading(true);
    try {
      const response = await apiService.menu.getCombos();
      const combos = Array.isArray(response) ? response : [];
      setCombos(combos);
      setPagination((prev) => ({
        ...prev,
        total: combos.length,
      }));
    } catch (error) {
      message.error("Lỗi khi tải dữ liệu combo");
      console.error("Error loading combos:", error);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadMenuItems = async () => {
    try {
      const response = await apiService.menu.getMenuItems();
      if (response && Array.isArray(response.items)) {
        setMenuItems(response.items);
      } else if (Array.isArray(response)) {
        setMenuItems(response);
      } else {
        setMenuItems([]);
      }
    } catch (error) {
      console.error("Error loading menu items:", error);
      setMenuItems([]);
    }
  };

  useEffect(() => {
    loadCombos();
    loadMenuItems();
  }, []);

  useEffect(() => {}, [filters]);

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

  // Listen to combo changes from other components
  useEffect(() => {
    const eventNames = [
      DATA_REFRESH_EVENTS.COMBO_CREATED,
      DATA_REFRESH_EVENTS.COMBO_UPDATED,
      DATA_REFRESH_EVENTS.COMBO_DELETED,
    ];
    const cleanupFunctions = [];

    eventNames.forEach((eventName) => {
      const cleanup = listenToDataRefresh(eventName, () => {
        loadCombos();
      });
      cleanupFunctions.push(cleanup);
    });

    return () => {
      cleanupFunctions.forEach((cleanup) => cleanup());
    };
  }, [loadCombos]);

  // Handle search and filters
  const handleSearch = (value) => {
    setFilters((prev) => ({ ...prev, search: value }));
  };

  const handleFilterChange = (key, value) => {
    setFilters((prev) => ({ ...prev, [key]: value }));
  };

  // Modal handlers
  const showModal = (type, combo = null) => {
    setModalType(type);
    setSelectedCombo(combo);
    setModalVisible(true);

    if (type === "edit" && combo) {
      form.setFieldsValue({
        ...combo,
        menuItemIds: combo.menuItemIds || [],
      });
      setImagePreview(combo.imageUrl || null);
    } else {
      form.resetFields();
      setImagePreview(null);
    }
  };

  // Handle image upload with progress
  const handleImageUpload = async (file) => {
    setUploading(true);
    try {
      // Validate image first
      const { validateImage } = await import("../utils/imageOptimizer");
      const validation = validateImage(file, 5); // Max 5MB
      if (!validation.valid) {
        message.error(validation.error);
        setUploading(false);
        return false;
      }

      const result = await apiService.cloudinary.uploadImage(
        file,
        "restaurant-combo",
        {
          onProgress: (progress) => {},
          compress: true,
        }
      );

      form.setFieldsValue({
        imageUrl: result.url,
        imagePublicId: result.publicId,
      });
      setImagePreview(result.url);
      message.success("Upload ảnh thành công");
      return false;
    } catch (error) {
      console.error("Upload error:", error);
      message.error(
        "Lỗi khi upload ảnh: " + (error.message || "Vui lòng thử lại")
      );
      return false;
    } finally {
      setUploading(false);
    }
  };

  const handleImagePreview = (file) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      setImagePreview(e.target.result);
    };
    reader.readAsDataURL(file);
    return false;
  };

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields();

      const selectedMenuItemIds = values.menuItemIds || [];
      const originalPrice = calculateComboPrice(selectedMenuItemIds);

      if (values.price > originalPrice) {
        message.error(
          `Giá combo (${values.price.toLocaleString(
            "vi-VN"
          )}đ) không được lớn hơn giá gốc (${originalPrice.toLocaleString(
            "vi-VN"
          )}đ)`
        );
        return;
      }

      if (values.discount && values.discount > 0 && values.discount <= 100) {
        const calculatedPrice = originalPrice * (1 - values.discount / 100);
        values.price = Math.round(calculatedPrice);
      }

      if (modalType === "create") {
        await apiService.menu.createCombo(values);
        message.success("Tạo combo thành công");
        dispatchDataRefresh(DATA_REFRESH_EVENTS.COMBO_CREATED, values);
      } else if (modalType === "edit") {
        const comboId = selectedCombo?.comboId || selectedCombo?.id;
        if (!selectedCombo || !comboId) {
          message.error("Không tìm thấy ID của combo để cập nhật");
          return;
        }
        await apiService.menu.updateCombo(comboId, values);
        message.success("Cập nhật combo thành công");
        dispatchDataRefresh(DATA_REFRESH_EVENTS.COMBO_UPDATED, {
          ...values,
          comboId: comboId,
        });
      }

      setModalVisible(false);
      loadCombos();
    } catch (error) {
      message.error("Có lỗi xảy ra khi thực hiện thao tác");
      console.error("Error:", error);
    }
  };

  // Delete combo
  const handleDelete = async (id) => {
    try {
      await apiService.menu.deleteCombo(id);
      message.success("Xóa combo thành công");
      dispatchDataRefresh(DATA_REFRESH_EVENTS.COMBO_DELETED, { comboId: id });
      loadCombos();
    } catch (error) {
      message.error("Lỗi khi xóa combo");
      console.error("Error deleting combo:", error);
    }
  };

  // Filter combos based on search and filters
  const filteredCombos = combos.filter((combo) => {
    const matchesSearch =
      !filters.search ||
      combo.name.toLowerCase().includes(filters.search.toLowerCase()) ||
      combo.description?.toLowerCase().includes(filters.search.toLowerCase());

    const matchesActive =
      filters.active === null || combo.active === filters.active;

    return matchesSearch && matchesActive;
  });

  const calculateComboPrice = (items) => {
    if (!items || items.length === 0) return 0;
    return items.reduce((total, item) => {
      if (typeof item === "string") {
        const menuItem = menuItems.find(
          (mi) => mi.menuItemId === item || mi.id === item
        );
        return total + (menuItem?.price || 0);
      } else {
        const menuItem = menuItems.find(
          (mi) => mi.menuItemId === item.menuItemId || mi.id === item.menuItemId
        );
        return total + (menuItem?.price || 0) * (item.quantity || 1);
      }
    }, 0);
  };

  const columns = [
    {
      title: "Hình ảnh",
      dataIndex: "imageUrl",
      key: "imageUrl",
      width: 100,
      render: (imageUrl, record) => (
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          {imageUrl ? (
            <Image
              width={60}
              height={60}
              src={imageUrl}
              fallback="/placeholder-food.jpg"
              style={{
                objectFit: "cover",
                borderRadius: 8,
                border: "1px solid #f0f0f0",
              }}
              preview={{
                mask: "Xem ảnh",
              }}
            />
          ) : (
            <div
              style={{
                width: 60,
                height: 60,
                background: "linear-gradient(135deg, #f59e0b 0%, #dc2626 100%)",
                borderRadius: 8,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                color: "white",
                fontSize: "20px",
              }}
            >
              <GiftOutlined />
            </div>
          )}
        </div>
      ),
    },
    {
      title: "Tên combo",
      dataIndex: "name",
      key: "name",
      render: (text, record) => (
        <div>
          <div
            className="font-medium"
            style={{ fontSize: "14px", fontWeight: "600" }}
          >
            {text}
          </div>
          {record.description && (
            <div
              className="text-sm text-gray-500"
              style={{
                marginTop: "4px",
                fontSize: "12px",
                display: "-webkit-box",
                WebkitLineClamp: 2,
                WebkitBoxOrient: "vertical",
                overflow: "hidden",
              }}
            >
              {record.description}
            </div>
          )}
        </div>
      ),
    },
    {
      title: "Số món",
      dataIndex: "menuItemIds",
      key: "itemCount",
      render: (menuItemIds) => {
        if (Array.isArray(menuItemIds)) {
          return menuItemIds.length;
        }
        return 0;
      },
    },
    {
      title: "Giá gốc",
      dataIndex: "menuItemIds",
      key: "originalPrice",
      render: (menuItemIds, record) => {
        // Use menuItemIds if available, otherwise fallback to items
        const items = menuItemIds || record.items || [];
        const price = calculateComboPrice(items);
        return (
          <Text delete className="text-gray-500">
            {price.toLocaleString("vi-VN")}đ
          </Text>
        );
      },
    },
    {
      title: "Giá combo",
      dataIndex: "price",
      key: "price",
      render: (price) => (
        <Text strong className="text-red-600">
          {price?.toLocaleString("vi-VN")}đ
        </Text>
      ),
    },
    {
      title: "Tiết kiệm",
      dataIndex: "menuItemIds",
      key: "savings",
      render: (menuItemIds, record) => {
        const items = menuItemIds || record.items || [];
        const originalPrice = calculateComboPrice(items);
        const savings = originalPrice - (record.price || 0);
        const savingsPercent =
          originalPrice > 0 ? ((savings / originalPrice) * 100).toFixed(0) : 0;
        return (
          <div>
            <Text className="text-green-600 font-medium">
              {savings.toLocaleString("vi-VN")}đ
            </Text>
            <div className="text-xs text-gray-500">({savingsPercent}%)</div>
          </div>
        );
      },
    },
    {
      title: "Trạng thái",
      dataIndex: "active",
      key: "active",
      render: (active) => (
        <Tag color={active ? "green" : "red"}>
          {active ? "Hoạt động" : "Tạm dừng"}
        </Tag>
      ),
    },
    {
      title: "Thao tác",
      key: "actions",
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => showModal("view", record)}
          >
            Xem
          </Button>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => showModal("edit", record)}
          >
            Sửa
          </Button>
          <Popconfirm
            title="Bạn có chắc muốn xóa combo này?"
            onConfirm={() => handleDelete(record.comboId || record.id)}
            okText="Xóa"
            cancelText="Hủy"
          >
            <Button size="small" icon={<DeleteOutlined />} danger>
              Xóa
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  // Get modal title
  const getModalTitle = () => {
    switch (modalType) {
      case "create":
        return "Tạo combo mới";
      case "edit":
        return "Chỉnh sửa combo";
      case "view":
        return "Chi tiết combo";
      default:
        return "Thao tác";
    }
  };

  // Render combo items in modal
  const renderComboItems = (items) => {
    if (!items || items.length === 0) {
      return <Text type="secondary">Chưa có món ăn nào</Text>;
    }

    // Convert menuItemIds array to items array if needed
    const itemsToRender = items.map((item, index) => {
      if (typeof item === "string") {
        // If item is a string (menuItemId), convert to object format
        return { menuItemId: item, quantity: 1 };
      }
      return item;
    });

    return (
      <List
        dataSource={itemsToRender}
        renderItem={(item, index) => {
          const menuItem = menuItems.find(
            (mi) =>
              mi.menuItemId === item.menuItemId || mi.id === item.menuItemId
          );
          return (
            <List.Item key={item.menuItemId || `item-${index}`}>
              <div className="flex justify-between items-center w-full">
                <div>
                  <Text strong>{menuItem?.name || "Món không tồn tại"}</Text>
                  <div className="text-sm text-gray-500">
                    {menuItem?.price?.toLocaleString("vi-VN")}đ x{" "}
                    {item.quantity || 1}
                  </div>
                </div>
                <Text className="text-right">
                  {(
                    (menuItem?.price || 0) * (item.quantity || 1)
                  ).toLocaleString("vi-VN")}
                  đ
                </Text>
              </div>
            </List.Item>
          );
        }}
      />
    );
  };

  return (
    <div className="page-content">
      <div className="flex justify-between items-center page-header">
        <div>
          <h1>Quản lý combo</h1>
          <p>Quản lý các combo món ăn</p>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => showModal("create")}
          className="restaurant-button"
        >
          Thêm combo
        </Button>
      </div>

      {/* Statistics Cards */}
      <div className="page-section">
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={6}>
            <Card className="restaurant-card stat-card">
              <Statistic
                title="Tổng combo"
                value={combos.length}
                prefix={<ShoppingCartOutlined />}
                valueStyle={{ color: "#1890ff" }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={6}>
            <Card className="restaurant-card stat-card">
              <Statistic
                title="Combo hoạt động"
                value={combos.filter((combo) => combo.active).length}
                prefix={<EyeOutlined />}
                valueStyle={{ color: "#52c41a" }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={6}>
            <Card className="restaurant-card stat-card">
              <Statistic
                title="Tổng tiết kiệm"
                value={combos.reduce((total, combo) => {
                  const items = combo.menuItemIds || combo.items || [];
                  const originalPrice = calculateComboPrice(items);
                  return total + (originalPrice - (combo.price || 0));
                }, 0)}
                prefix={<DollarOutlined />}
                valueStyle={{ color: "#f59e0b" }}
                formatter={(value) => `${value.toLocaleString("vi-VN")}đ`}
              />
            </Card>
          </Col>
          <Col xs={24} sm={6}>
            <Card className="restaurant-card stat-card">
              <Statistic
                title="Giá TB combo"
                value={
                  combos.length > 0
                    ? combos.reduce(
                        (sum, combo) => sum + (combo.price || 0),
                        0
                      ) / combos.length
                    : 0
                }
                prefix={<DollarOutlined />}
                valueStyle={{ color: "#dc2626" }}
                formatter={(value) =>
                  `${Math.round(value).toLocaleString("vi-VN")}đ`
                }
              />
            </Card>
          </Col>
        </Row>
      </div>

      {/* Filters */}
      <div className="page-section">
        <Card className="restaurant-card search-filter-section">
          <Row gutter={[16, 16]} align="middle">
            <Col xs={24} sm={8}>
              <Search
                placeholder="Tìm kiếm combo..."
                onSearch={handleSearch}
                enterButton={<SearchOutlined />}
              />
            </Col>
            <Col xs={24} sm={6}>
              <Select
                placeholder="Trạng thái"
                style={{ width: "100%" }}
                allowClear
                onChange={(value) => handleFilterChange("active", value)}
              >
                <Option value={true}>Hoạt động</Option>
                <Option value={false}>Tạm dừng</Option>
              </Select>
            </Col>
            <Col xs={24} sm={4}>
              <Button
                icon={<ReloadOutlined />}
                onClick={() => loadCombos()}
                className="w-full"
              >
                Làm mới
              </Button>
            </Col>
          </Row>
        </Card>
      </div>

      {/* Table */}
      <div className="page-section">
        <Card className="restaurant-card">
          <Table
            columns={columns}
            dataSource={filteredCombos}
            rowKey={(record) => record.comboId || record.id}
            loading={loading}
            pagination={{
              ...pagination,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total, range) =>
                `${range[0]}-${range[1]} của ${total} mục`,
              pageSizeOptions: PAGINATION.PAGE_SIZE_OPTIONS,
            }}
            scroll={{ x: 1100 }}
          />
        </Card>

        {/* Modal */}
        <Modal
          title={getModalTitle()}
          open={modalVisible}
          onOk={modalType !== "view" ? handleModalOk : undefined}
          onCancel={() => {
            setModalVisible(false);
            setImagePreview(null);
            form.resetFields();
          }}
          width={800}
          okText="Lưu"
          cancelText="Hủy"
          footer={
            modalType === "view"
              ? [
                  <Button key="close" onClick={() => setModalVisible(false)}>
                    Đóng
                  </Button>,
                ]
              : undefined
          }
        >
          {modalType === "view" ? (
            <div className="space-y-4">
              <div>
                {selectedCombo?.imageUrl && (
                  <div style={{ marginBottom: "16px", textAlign: "center" }}>
                    <Image
                      src={selectedCombo.imageUrl}
                      alt={selectedCombo.name}
                      width={200}
                      height={200}
                      style={{
                        objectFit: "cover",
                        borderRadius: "12px",
                        border: "1px solid #f0f0f0",
                      }}
                      fallback="/placeholder-food.jpg"
                      preview={{
                        mask: "Xem ảnh",
                      }}
                    />
                  </div>
                )}
                <Title level={4}>{selectedCombo?.name}</Title>
                <Text>{selectedCombo?.description}</Text>
              </div>

              <Divider />

              <div>
                <Title level={5}>Món ăn trong combo:</Title>
                {renderComboItems(
                  selectedCombo?.menuItemIds || selectedCombo?.items || []
                )}
              </div>

              <Divider />

              <Row gutter={16}>
                <Col span={12}>
                  <Statistic
                    title="Giá gốc"
                    value={calculateComboPrice(
                      selectedCombo?.menuItemIds || selectedCombo?.items || []
                    )}
                    formatter={(value) => `${value.toLocaleString("vi-VN")}đ`}
                    valueStyle={{ color: "#8c8c8c" }}
                  />
                </Col>
                <Col span={12}>
                  <Statistic
                    title="Giá combo"
                    value={selectedCombo?.price || 0}
                    formatter={(value) => `${value.toLocaleString("vi-VN")}đ`}
                    valueStyle={{ color: "#dc2626" }}
                  />
                </Col>
              </Row>

              <Row gutter={16}>
                <Col span={12}>
                  <Statistic
                    title="Tiết kiệm"
                    value={
                      calculateComboPrice(
                        selectedCombo?.menuItemIds || selectedCombo?.items || []
                      ) - (selectedCombo?.price || 0)
                    }
                    formatter={(value) => `${value.toLocaleString("vi-VN")}đ`}
                    valueStyle={{ color: "#52c41a" }}
                  />
                </Col>
                <Col span={12}>
                  <Statistic
                    title="Phần trăm tiết kiệm"
                    value={
                      (selectedCombo?.menuItemIds || selectedCombo?.items) &&
                      (selectedCombo?.menuItemIds?.length > 0 ||
                        selectedCombo?.items?.length > 0)
                        ? (
                            ((calculateComboPrice(
                              selectedCombo?.menuItemIds ||
                                selectedCombo?.items ||
                                []
                            ) -
                              (selectedCombo.price || 0)) /
                              calculateComboPrice(
                                selectedCombo?.menuItemIds ||
                                  selectedCombo?.items ||
                                  []
                              )) *
                            100
                          ).toFixed(0)
                        : 0
                    }
                    suffix="%"
                    valueStyle={{ color: "#52c41a" }}
                  />
                </Col>
              </Row>
            </div>
          ) : (
            <Form form={form} layout="vertical">
              <Form.Item
                name="name"
                label="Tên combo"
                rules={[{ required: true, message: "Vui lòng nhập tên combo" }]}
              >
                <Input />
              </Form.Item>

              <Form.Item name="description" label="Mô tả">
                <TextArea rows={3} />
              </Form.Item>

              <div>
                <div style={{ marginBottom: 8 }}>Hình ảnh</div>
                <Upload
                  beforeUpload={handleImageUpload}
                  onPreview={handleImagePreview}
                  showUploadList={false}
                  accept="image/*"
                  listType="picture-card"
                  className="image-uploader"
                >
                  {imagePreview ? (
                    <img
                      src={imagePreview}
                      alt="preview"
                      style={{
                        width: "100%",
                        height: "100%",
                        objectFit: "cover",
                      }}
                    />
                  ) : (
                    <div>
                      <UploadOutlined />
                      <div style={{ marginTop: 8 }}>Upload</div>
                    </div>
                  )}
                </Upload>
              </div>
              {uploading && (
                <div style={{ marginTop: 8, color: "#1890ff" }}>
                  Đang upload...
                </div>
              )}
              {/* Hidden fields for uploaded image metadata */}
              <Form.Item
                name="imageUrl"
                style={{ display: "none" }}
                rules={
                  modalType === "create"
                    ? [{ required: true, message: "Vui lòng upload hình ảnh" }]
                    : []
                }
              >
                <Input />
              </Form.Item>
              <Form.Item name="imagePublicId" style={{ display: "none" }}>
                <Input />
              </Form.Item>

              <Form.Item
                name="menuItemIds"
                label="Món ăn trong combo"
                rules={[
                  {
                    required: true,
                    message: "Vui lòng chọn ít nhất một món ăn",
                  },
                ]}
              >
                <Select
                  mode="multiple"
                  placeholder="Chọn món ăn"
                  showSearch
                  optionFilterProp="children"
                  style={{ width: "100%" }}
                  onChange={(selectedIds) => {
                    // Calculate original price when menu items change
                    const originalPrice = calculateComboPrice(selectedIds);
                    const currentDiscount = form.getFieldValue("discount") || 0;
                    const currentPrice = form.getFieldValue("price");

                    // If discount is set, auto-calculate price
                    if (currentDiscount > 0 && currentDiscount <= 100) {
                      const calculatedPrice =
                        originalPrice * (1 - currentDiscount / 100);
                      form.setFieldsValue({
                        price: Math.round(calculatedPrice),
                      });
                    } else if (!currentPrice || currentPrice === 0) {
                      // If no price set, suggest original price
                      form.setFieldsValue({ price: originalPrice });
                    }

                    // Trigger validation
                    form.validateFields(["price"]);
                  }}
                >
                  {Array.isArray(menuItems) &&
                    menuItems.map((item) => (
                      <Option
                        key={item.id || item.menuItemId}
                        value={item.id || item.menuItemId}
                      >
                        {item.name} - {item.price?.toLocaleString("vi-VN")}đ
                      </Option>
                    ))}
                </Select>
              </Form.Item>

              <Form.Item
                noStyle
                shouldUpdate={(prevValues, currentValues) =>
                  prevValues.menuItemIds !== currentValues.menuItemIds
                }
              >
                {({ getFieldValue }) => {
                  const selectedMenuItemIds =
                    getFieldValue("menuItemIds") || [];
                  const originalPrice =
                    calculateComboPrice(selectedMenuItemIds);

                  return (
                    <div
                      style={{
                        marginBottom: 16,
                        padding: 12,
                        background: "#f5f5f5",
                        borderRadius: 4,
                      }}
                    >
                      <Text strong>Giá gốc: </Text>
                      <Text style={{ color: "#1890ff", fontSize: 16 }}>
                        {originalPrice.toLocaleString("vi-VN")}đ
                      </Text>
                    </div>
                  );
                }}
              </Form.Item>

              <Row gutter={16}>
                <Col span={8}>
                  <Form.Item
                    name="price"
                    label="Giá combo"
                    rules={[
                      { required: true, message: "Vui lòng nhập giá combo" },
                      ({ getFieldValue }) => ({
                        validator(_, value) {
                          const selectedMenuItemIds =
                            getFieldValue("menuItemIds") || [];
                          const originalPrice =
                            calculateComboPrice(selectedMenuItemIds);
                          if (!value || value <= originalPrice) {
                            return Promise.resolve();
                          }
                          return Promise.reject(
                            new Error(
                              `Giá combo không được lớn hơn giá gốc (${originalPrice.toLocaleString(
                                "vi-VN"
                              )}đ)`
                            )
                          );
                        },
                      }),
                    ]}
                  >
                    <InputNumber
                      style={{ width: "100%" }}
                      formatter={(value) =>
                        `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ",")
                      }
                      parser={(value) => value.replace(/\$\s?|(,*)/g, "")}
                      min={0}
                    />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item
                    name="discount"
                    label="Giảm giá (%)"
                    initialValue={0}
                  >
                    <InputNumber
                      style={{ width: "100%" }}
                      min={0}
                      max={100}
                      formatter={(value) => `${value}%`}
                      parser={(value) => value.replace("%", "")}
                      onChange={(discountValue) => {
                        if (
                          discountValue &&
                          discountValue > 0 &&
                          discountValue <= 100
                        ) {
                          const selectedMenuItemIds =
                            form.getFieldValue("menuItemIds") || [];
                          const originalPrice =
                            calculateComboPrice(selectedMenuItemIds);
                          if (originalPrice > 0) {
                            const calculatedPrice =
                              originalPrice * (1 - discountValue / 100);
                            form.setFieldsValue({
                              price: Math.round(calculatedPrice),
                            });
                          }
                        }
                      }}
                    />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item
                    name="active"
                    label="Trạng thái"
                    valuePropName="checked"
                    initialValue={true}
                  >
                    <Switch
                      checkedChildren="Hoạt động"
                      unCheckedChildren="Tạm dừng"
                    />
                  </Form.Item>
                </Col>
              </Row>
            </Form>
          )}
        </Modal>
      </div>
    </div>
  );
};

export default CombosManagement;
