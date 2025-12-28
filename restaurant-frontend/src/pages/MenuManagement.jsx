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
  Upload,
  Image,
  Switch,
  Divider,
} from "antd";
import {
  PlusOutlined,
  EditOutlined,
  SearchOutlined,
  ReloadOutlined,
  DeleteOutlined,
  ShoppingCartOutlined,
  DollarOutlined,
  EyeOutlined,
  SettingOutlined,
  UploadOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import apiService from "../services/apiService";
import { PAGINATION } from "../constants.js";
import { canManageMenu } from "../utils/auth";
import { useAuth } from "../context/AuthContext";
import Loading from "../components/Common/Loading";
import ErrorPage from "../components/Common/ErrorPage";
import {
  listenToDataRefresh,
  dispatchDataRefresh,
  DATA_REFRESH_EVENTS,
} from "../utils/dataRefreshEvents";

const { Option } = Select;
const { Search } = Input;
const { TextArea } = Input;

const MenuManagement = () => {
  const { message } = App.useApp();
  const { role, loading: authLoading } = useAuth();
  const navigate = useNavigate();

  // Check permission
  if (authLoading) {
    return <Loading tip="Đang kiểm tra quyền truy cập..." />;
  }

  if (!canManageMenu(role)) {
    return (
      <ErrorPage
        status={403}
        title="403 - Không có quyền truy cập"
        subTitle="Chỉ nhân viên, quản lý nhà hàng và quản trị viên mới có thể quản lý thực đơn."
        showHomeButton={false}
        showReloadButton={false}
        onBack={() => navigate("/dashboard/menu")}
      />
    );
  }
  const [menuItems, setMenuItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [ingredients, setIngredients] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: PAGINATION.DEFAULT_PAGE_SIZE,
    total: 0,
  });
  const [filters, setFilters] = useState({
    categoryId: "",
    active: null,
    search: "",
  });
  const [modalVisible, setModalVisible] = useState(false);
  const [modalType, setModalType] = useState("create"); // create, edit, ingredients
  const [selectedMenuItem, setSelectedMenuItem] = useState(null);
  const [form] = Form.useForm();
  const [uploading, setUploading] = useState(false);
  const [imagePreview, setImagePreview] = useState(null);
  const [formInitialValues, setFormInitialValues] = useState({});

  const canManage = canManageMenu(role);

  // Load menu items data
  const loadMenuItems = useCallback(
    async (page = 1, size = PAGINATION.DEFAULT_PAGE_SIZE) => {
      setLoading(true);
      try {
        const params = {
          page: page - 1,
          size,
          ...(filters.categoryId &&
            filters.categoryId.trim() !== "" && {
              categoryId: filters.categoryId,
            }),
          ...(filters.active !== null &&
            filters.active !== undefined && { active: filters.active }),
          ...(filters.search &&
            filters.search.trim() !== "" && { search: filters.search }),
        };

        const response = await apiService.menu.getMenuItems(params);
        const items = response?.items || [];

        setMenuItems(items);
        setPagination((prev) => ({
          ...prev,
          current: page,
          total: response?.totalElements || 0,
        }));
      } catch (error) {
        message.error("Lỗi khi tải dữ liệu món ăn");
        console.error("Error loading menu items:", error);
      } finally {
        setLoading(false);
      }
    },
    [filters]
  );

  // Load categories
  const loadCategories = async () => {
    try {
      const response = await apiService.menu.getCategories();
      setCategories(Array.isArray(response) ? response : []);
    } catch (error) {
      console.error("Error loading categories:", error);
    }
  };

  // Load ingredients
  const loadIngredients = async () => {
    try {
      const response = await apiService.inventory.getIngredients({
        size: 1000,
      });
      setIngredients(response?.ingredients || []);
    } catch (error) {
      if (error.response?.status !== 403) {
        console.error("Error loading ingredients:", error);
      }
    }
  };

  useEffect(() => {
    loadCategories();
    if (canManage) {
      apiService.cloudinary.prefetchSignature();
    }
  }, []);

  // Reload menu items when filters or pagination changes
  useEffect(() => {
    loadMenuItems(pagination.current, pagination.pageSize);
  }, [filters, pagination.current, pagination.pageSize, loadMenuItems]);

  // Load ingredients when canManage changes
  useEffect(() => {
    if (canManage) {
      loadIngredients();
    }
  }, [canManage]);

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
      });
      cleanupFunctions.push(cleanup);
    });

    return () => {
      cleanupFunctions.forEach((cleanup) => cleanup());
    };
  }, []);

  // Listen to ingredient changes to reload ingredients
  useEffect(() => {
    const eventNames = [
      DATA_REFRESH_EVENTS.INGREDIENT_CREATED,
      DATA_REFRESH_EVENTS.INGREDIENT_UPDATED,
      DATA_REFRESH_EVENTS.INGREDIENT_DELETED,
    ];
    const cleanupFunctions = [];

    eventNames.forEach((eventName) => {
      const cleanup = listenToDataRefresh(eventName, () => {
        loadIngredients();
      });
      cleanupFunctions.push(cleanup);
    });

    return () => {
      cleanupFunctions.forEach((cleanup) => cleanup());
    };
  }, []);

  // Listen to menu item changes from other components
  useEffect(() => {
    const eventNames = [
      DATA_REFRESH_EVENTS.MENU_ITEM_CREATED,
      DATA_REFRESH_EVENTS.MENU_ITEM_UPDATED,
      DATA_REFRESH_EVENTS.MENU_ITEM_DELETED,
    ];
    const cleanupFunctions = [];

    eventNames.forEach((eventName) => {
      const handler = () => {
        loadMenuItems(pagination.current, pagination.pageSize);
      };
      const cleanup = listenToDataRefresh(eventName, handler);
      cleanupFunctions.push(cleanup);
    });

    return () => {
      cleanupFunctions.forEach((cleanup) => cleanup());
    };
  }, [loadMenuItems, pagination.current, pagination.pageSize]);

  useEffect(() => {
    if (modalVisible && modalType === "ingredients" && selectedMenuItem) {
      let ingredientsData = [];

      if (
        selectedMenuItem.ingredientDetails &&
        selectedMenuItem.ingredientDetails.length > 0
      ) {
        ingredientsData = selectedMenuItem.ingredientDetails.map((detail) => ({
          ingredientId: detail.ingredientId,
          quantity: detail.quantity || 0,
          unit: detail.unit || "kg",
          notes: detail.notes || "",
        }));
      } else if (
        selectedMenuItem.ingredients &&
        selectedMenuItem.ingredients.length > 0
      ) {
        ingredientsData = selectedMenuItem.ingredients.map((ingredientId) => ({
          ingredientId: ingredientId,
          quantity: 1,
          unit: "kg",
          notes: "",
        }));
      }

      setFormInitialValues({
        ingredients: ingredientsData,
      });

      const timer = setTimeout(() => {
        if (modalVisible && modalType === "ingredients") {
          try {
            form.setFieldsValue({
              ingredients: ingredientsData,
            });
          } catch (error) {}
        }
      }, 100);

      return () => clearTimeout(timer);
    } else if (!modalVisible) {
      try {
        form.resetFields();
      } catch (error) {}
      setFormInitialValues({});
    }
  }, [modalVisible, modalType, selectedMenuItem, form]);

  const handleTableChange = (paginationInfo) => {
    loadMenuItems(paginationInfo.current, paginationInfo.pageSize);
  };

  const handleSearch = (value) => {
    setFilters((prev) => ({ ...prev, search: value }));
  };

  const handleFilterChange = (key, value) => {
    setFilters((prev) => ({ ...prev, [key]: value }));
  };

  const showModal = (type, menuItem = null) => {
    setModalType(type);
    setSelectedMenuItem(menuItem);
    setModalVisible(true);

    if (type === "edit" && menuItem) {
      form.resetFields();

      const formValues = {
        name: menuItem.name || "",
        description: menuItem.description || "",
        categoryId: menuItem.categoryId
          ? String(menuItem.categoryId)
          : undefined,
        price:
          menuItem.price !== null && menuItem.price !== undefined
            ? Number(menuItem.price)
            : 0,
        preparationTime:
          menuItem.preparationTime !== null &&
          menuItem.preparationTime !== undefined
            ? Number(menuItem.preparationTime)
            : null,
        recipe: menuItem.recipe || "",
        active: menuItem.active !== undefined ? Boolean(menuItem.active) : true,
        imageUrl: menuItem.imageUrl || "",
        imagePublicId: menuItem.imagePublicId || "",
      };
      setTimeout(() => {
        form.setFieldsValue(formValues);
        form.validateFields().catch(() => {});
        if (menuItem.imageUrl) {
          setImagePreview(menuItem.imageUrl);
        } else {
          setImagePreview(null);
        }
      }, 150);
    } else if (type === "ingredients" && menuItem) {
      let ingredientsData = [];

      if (menuItem.ingredientDetails && menuItem.ingredientDetails.length > 0) {
        ingredientsData = menuItem.ingredientDetails.map((detail) => ({
          ingredientId: detail.ingredientId,
          quantity: detail.quantity || 0,
          unit: detail.unit || "kg",
          notes: detail.notes || "",
        }));
      } else if (menuItem.ingredients && menuItem.ingredients.length > 0) {
        ingredientsData = menuItem.ingredients.map((ingredientId) => ({
          ingredientId: ingredientId,
          quantity: 1,
          unit: "kg",
          notes: "",
        }));
      }

      setFormInitialValues({
        ingredients: ingredientsData,
      });

      form.resetFields();

      setImagePreview(null);
    } else {
      form.resetFields();
      setImagePreview(null);
    }
  };

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields();
      if (modalType === "create" && !values.imageUrl) {
        message.error("Vui lòng upload hình ảnh trước khi lưu");
        return;
      }
      const payload = {
        ...values,
        categoryId:
          values.categoryId != null
            ? String(values.categoryId)
            : values.categoryId,
      };

      if (modalType === "create") {
        await apiService.menu.createMenuItem(payload);
        message.success("Tạo món ăn thành công");
        dispatchDataRefresh(DATA_REFRESH_EVENTS.MENU_ITEM_CREATED, payload);
      } else if (modalType === "edit") {
        if (!payload.imageUrl && selectedMenuItem?.imageUrl) {
          payload.imageUrl = selectedMenuItem.imageUrl;
          payload.imagePublicId = selectedMenuItem.imagePublicId;
        }
        await apiService.menu.updateMenuItem(
          selectedMenuItem.menuItemId,
          payload
        );
        message.success("Cập nhật món ăn thành công");
        dispatchDataRefresh(DATA_REFRESH_EVENTS.MENU_ITEM_UPDATED, {
          ...payload,
          menuItemId: selectedMenuItem.menuItemId,
        });
      } else if (modalType === "ingredients") {
        const ingredientsWithQuantity = values.ingredients.map((item) => ({
          ingredientId: item.ingredientId,
          quantity: item.quantity,
          unit: item.unit || "kg",
          notes: item.notes || "",
        }));

        await apiService.menu.updateMenuItemIngredientsWithQuantity(
          selectedMenuItem.menuItemId,
          ingredientsWithQuantity
        );
        message.success("Cập nhật nguyên liệu thành công");

        dispatchDataRefresh(DATA_REFRESH_EVENTS.MENU_ITEM_UPDATED, {
          menuItemId: selectedMenuItem.menuItemId,
        });

        loadMenuItems(pagination.current, pagination.pageSize);
      }

      setModalVisible(false);
      form.resetFields();
      setImagePreview(null);

      if (modalType !== "ingredients") {
        loadMenuItems(pagination.current, pagination.pageSize);
      }
    } catch (error) {
      message.error("Có lỗi xảy ra khi thực hiện thao tác");
      console.error("Error:", error);
    }
  };

  // Toggle active status
  const handleToggleActive = async (id, active) => {
    try {
      await apiService.menu.toggleMenuItemActive(id, active);
      message.success(`Đã ${active ? "kích hoạt" : "vô hiệu hóa"} món ăn`);
      dispatchDataRefresh(DATA_REFRESH_EVENTS.MENU_ITEM_UPDATED, {
        menuItemId: id,
        active,
      });
      loadMenuItems(pagination.current, pagination.pageSize);
    } catch (error) {
      message.error("Lỗi khi thay đổi trạng thái");
    }
  };

  // Delete menu item
  const handleDeleteMenuItem = async (id) => {
    try {
      await apiService.menu.deleteMenuItem(id);
      message.success("Xóa món ăn thành công");
      dispatchDataRefresh(DATA_REFRESH_EVENTS.MENU_ITEM_DELETED, {
        menuItemId: id,
      });
      loadMenuItems(pagination.current, pagination.pageSize);
    } catch (error) {
      message.error("Lỗi khi xóa món ăn");
    }
  };

  // Delete multiple menu items
  const handleDeleteMultiple = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning("Vui lòng chọn ít nhất một món ăn để xóa");
      return;
    }

    Modal.confirm({
      title: "Xác nhận xóa",
      content: `Bạn có chắc chắn muốn xóa ${selectedRowKeys.length} món ăn đã chọn?`,
      okText: "Xóa",
      okType: "danger",
      cancelText: "Hủy",
      onOk: async () => {
        try {
          setLoading(true);
          const deletePromises = selectedRowKeys.map((id) =>
            apiService.menu.deleteMenuItem(id)
          );
          await Promise.all(deletePromises);

          message.success(`Đã xóa thành công ${selectedRowKeys.length} món ăn`);

          // Dispatch events for each deleted item
          selectedRowKeys.forEach((id) => {
            dispatchDataRefresh(DATA_REFRESH_EVENTS.MENU_ITEM_DELETED, {
              menuItemId: id,
            });
          });

          setSelectedRowKeys([]);
          loadMenuItems(pagination.current, pagination.pageSize);
        } catch (error) {
          message.error("Có lỗi xảy ra khi xóa món ăn");
          console.error("Error deleting menu items:", error);
        } finally {
          setLoading(false);
        }
      },
    });
  };

  // Update price
  const handleUpdatePrice = async (id, price) => {
    try {
      await apiService.menu.updateMenuItemPrice(id, price);
      message.success("Cập nhật giá thành công");
      loadMenuItems(pagination.current, pagination.pageSize);
    } catch (error) {
      message.error("Lỗi khi cập nhật giá");
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
        "restaurant-menu",
        {
          onProgress: (progress) => {
            // You can show progress here if needed
            // console.log(`Upload progress: ${progress}%`);
          },
          compress: true, // Enable compression
        }
      );

      form.setFieldsValue({
        imageUrl: result.url,
        imagePublicId: result.publicId,
      });
      setImagePreview(result.url);
      message.success("Upload ảnh thành công");
      return false; // Prevent default upload
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

  // Handle image preview
  const handleImagePreview = (file) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      setImagePreview(e.target.result);
    };
    reader.readAsDataURL(file);
    return false; // Prevent default upload
  };

  // Table columns
  const columns = [
    {
      title: "Hình ảnh",
      dataIndex: "imageUrl",
      key: "imageUrl",
      width: 80,
      render: (imageUrl) => (
        <Image
          width={60}
          height={60}
          src={imageUrl || "/placeholder-food.jpg"}
          fallback="/placeholder-food.jpg"
          style={{ objectFit: "cover", borderRadius: 8 }}
        />
      ),
    },
    {
      title: "Tên món ăn",
      dataIndex: "name",
      key: "name",
      render: (text, record) => (
        <div>
          <div style={{ fontWeight: "500", fontSize: "14px" }}>{text}</div>
          <div style={{ fontSize: "12px", color: "#999", marginTop: "2px" }}>
            {record.description}
          </div>
        </div>
      ),
    },
    {
      title: "Danh mục",
      dataIndex: "categoryName",
      key: "categoryName",
      render: (categoryName) => <Tag color="blue">{categoryName}</Tag>,
    },
    {
      title: "Giá",
      dataIndex: "price",
      key: "price",
      render: (price, record) => (
        <div>
          <div style={{ fontWeight: "500", fontSize: "14px" }}>
            {price?.toLocaleString()} VNĐ
          </div>
          <div style={{ fontSize: "12px", color: "#999", marginTop: "2px" }}>
            {(() => {
              let ingredientNames = [];
              if (
                record.ingredientDetails &&
                record.ingredientDetails.length > 0
              ) {
                ingredientNames = record.ingredientDetails.map((detail) => {
                  const ingredient = ingredients.find(
                    (ing) => ing.ingredientId === detail.ingredientId
                  );
                  return ingredient ? ingredient.name : detail.ingredientId;
                });
              } else if (record.ingredients && record.ingredients.length > 0) {
                ingredientNames = record.ingredients.map((ingredientId) => {
                  const ingredient = ingredients.find(
                    (ing) => ing.ingredientId === ingredientId
                  );
                  return ingredient ? ingredient.name : ingredientId;
                });
              }

              if (ingredientNames.length === 0) {
                return "Chưa có nguyên liệu";
              }

              return ingredientNames.join(", ");
            })()}
          </div>
        </div>
      ),
    },
    {
      title: "Công thức",
      dataIndex: "recipe",
      key: "recipe",
      width: 200,
      render: (recipe) => (
        <div style={{ maxWidth: "200px" }}>
          {recipe ? (
            <div
              style={{
                fontSize: "12px",
                color: "#666",
                cursor: "pointer",
                lineHeight: "1.4",
              }}
              title={recipe}
            >
              {recipe.length > 50 ? `${recipe.substring(0, 50)}...` : recipe}
            </div>
          ) : (
            <span style={{ color: "#999", fontSize: "12px" }}>
              Chưa có công thức
            </span>
          )}
        </div>
      ),
    },
    {
      title: "Trạng thái",
      dataIndex: "active",
      key: "active",
      render: (active, record) => (
        <Switch
          checked={active}
          onChange={(checked) => handleToggleActive(record.menuItemId, checked)}
          checkedChildren="Bán"
          unCheckedChildren="Tạm dừng"
        />
      ),
    },
    {
      title: "Thao tác",
      key: "actions",
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => showModal("edit", record)}
          >
            Sửa
          </Button>
          <Button
            size="small"
            icon={<ShoppingCartOutlined />}
            onClick={() => showModal("ingredients", record)}
          >
            Nguyên liệu
          </Button>
          {record.recipe && (
            <Button
              size="small"
              icon={<EyeOutlined />}
              onClick={() => {
                Modal.info({
                  title: `Công thức: ${record.name}`,
                  content: (
                    <div
                      style={{
                        whiteSpace: "pre-wrap",
                        maxHeight: "400px",
                        overflow: "auto",
                      }}
                    >
                      {record.recipe}
                    </div>
                  ),
                  width: 600,
                });
              }}
            >
              Công thức
            </Button>
          )}
          <Button
            size="small"
            icon={<DollarOutlined />}
            onClick={() => {
              const newPrice = prompt("Nhập giá mới:", record.price);
              if (newPrice && !isNaN(newPrice)) {
                handleUpdatePrice(record.menuItemId, parseFloat(newPrice));
              }
            }}
          >
            Giá
          </Button>
          <Popconfirm
            title="Bạn có chắc muốn xóa món ăn này?"
            onConfirm={() => handleDeleteMenuItem(record.menuItemId)}
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
        return "Tạo món ăn mới";
      case "edit":
        return "Chỉnh sửa món ăn";
      case "ingredients":
        return "Quản lý nguyên liệu";
      default:
        return "Thao tác";
    }
  };

  // Get form fields based on modal type
  const getFormFields = () => {
    if (modalType === "ingredients") {
      return (
        <>
          <Form.List name="ingredients">
            {(fields, { add, remove }) => (
              <>
                {fields.map(({ key, name, ...restField }) => (
                  <div
                    key={key}
                    style={{
                      display: "flex",
                      marginBottom: 8,
                      gap: 8,
                      alignItems: "flex-start",
                    }}
                  >
                    <Form.Item
                      {...restField}
                      name={[name, "ingredientId"]}
                      rules={[{ required: true, message: "Chọn nguyên liệu" }]}
                      style={{ flex: 2, marginBottom: 0 }}
                    >
                      <Select
                        placeholder="Chọn nguyên liệu"
                        showSearch
                        filterOption={(input, option) =>
                          (option?.label ?? "")
                            .toLowerCase()
                            .includes(input.toLowerCase())
                        }
                      >
                        {ingredients.map((ingredient) => (
                          <Option
                            key={ingredient.ingredientId}
                            value={ingredient.ingredientId}
                            label={ingredient.name}
                          >
                            {ingredient.name}
                          </Option>
                        ))}
                      </Select>
                    </Form.Item>
                    <Form.Item
                      {...restField}
                      name={[name, "quantity"]}
                      rules={[
                        { required: true, message: "Nhập số lượng" },
                        {
                          type: "number",
                          min: 0.001,
                          message: "Số lượng phải > 0",
                        },
                      ]}
                      style={{ flex: 1, marginBottom: 0 }}
                    >
                      <InputNumber
                        placeholder="Số lượng"
                        min={0.001}
                        step={0.1}
                        style={{ width: "100%" }}
                        formatter={(value) =>
                          value
                            ? `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ",")
                            : ""
                        }
                        parser={(value) =>
                          value ? value.replace(/\$\s?|(,*)/g, "") : ""
                        }
                      />
                    </Form.Item>
                    <Form.Item
                      {...restField}
                      name={[name, "unit"]}
                      rules={[{ required: true, message: "Chọn đơn vị" }]}
                      style={{ flex: 0.8, marginBottom: 0 }}
                    >
                      <Select placeholder="Đơn vị">
                        <Option value="kg">kg</Option>
                        <Option value="g">g</Option>
                        <Option value="liter">liter</Option>
                        <Option value="ml">ml</Option>
                        <Option value="piece">cái</Option>
                      </Select>
                    </Form.Item>
                    <Button
                      type="text"
                      danger
                      onClick={() => remove(name)}
                      icon={<DeleteOutlined />}
                      style={{ marginTop: 4 }}
                    >
                      Xóa
                    </Button>
                  </div>
                ))}
                <Form.Item>
                  <Button
                    type="dashed"
                    onClick={() => add()}
                    block
                    icon={<PlusOutlined />}
                  >
                    Thêm nguyên liệu
                  </Button>
                </Form.Item>
              </>
            )}
          </Form.List>
        </>
      );
    }

    return (
      <>
        <Form.Item
          name="name"
          label="Tên món ăn"
          rules={[{ required: true, message: "Vui lòng nhập tên món ăn" }]}
        >
          <Input />
        </Form.Item>
        <Form.Item name="description" label="Mô tả">
          <TextArea rows={3} />
        </Form.Item>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="categoryId"
              label="Danh mục"
              rules={[{ required: true, message: "Vui lòng chọn danh mục" }]}
            >
              <Select placeholder="Chọn danh mục">
                {categories.map((category) => (
                  <Option key={category.categoryId} value={category.categoryId}>
                    {category.name}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="price"
              label="Giá (VNĐ)"
              rules={[{ required: true, message: "Vui lòng nhập giá" }]}
            >
              <InputNumber min={0} style={{ width: "100%" }} />
            </Form.Item>
          </Col>
        </Row>
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
                style={{ width: "100%", height: "100%", objectFit: "cover" }}
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
          <div style={{ marginTop: 8, color: "#1890ff" }}>Đang upload...</div>
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
        <Form.Item name="preparationTime" label="Thời gian chuẩn bị (phút)">
          <InputNumber min={0} style={{ width: "100%" }} />
        </Form.Item>
        <Form.Item name="recipe" label="Công thức món ăn">
          <TextArea
            rows={6}
            placeholder="Nhập các bước chế biến món ăn..."
            showCount
            maxLength={2000}
          />
        </Form.Item>
        <Form.Item name="active" label="Trạng thái" valuePropName="checked">
          <Switch checkedChildren="Bán" unCheckedChildren="Tạm dừng" />
        </Form.Item>
      </>
    );
  };

  // Get statistics
  const getStatistics = () => {
    const totalItems = menuItems.length;
    const activeItems = menuItems.filter((item) => item.active).length;
    const inactiveItems = menuItems.filter((item) => !item.active).length;
    const avgPrice =
      menuItems.length > 0
        ? menuItems.reduce((sum, item) => sum + (item.price || 0), 0) /
          menuItems.length
        : 0;

    return {
      totalItems,
      activeItems,
      inactiveItems,
      avgPrice,
    };
  };

  const stats = getStatistics();

  return (
    <div style={{ padding: "24px" }}>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "24px",
        }}
      >
        <div>
          <h1
            style={{
              fontSize: "24px",
              fontWeight: "bold",
              color: "#1f2937",
              marginBottom: "8px",
            }}
          >
            Quản lý thực đơn
          </h1>
          <p style={{ color: "#6b7280" }}>Quản lý món ăn và thực đơn</p>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => showModal("create")}
        >
          Thêm món ăn
        </Button>
      </div>

      {/* Statistics Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: "24px" }}>
        <Col xs={24} sm={6}>
          <Card>
            <Statistic
              title="Tổng món ăn"
              value={stats.totalItems}
              prefix={<ShoppingCartOutlined />}
              valueStyle={{ color: "#1890ff" }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={6}>
          <Card>
            <Statistic
              title="Đang bán"
              value={stats.activeItems}
              prefix={<EyeOutlined />}
              valueStyle={{ color: "#52c41a" }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={6}>
          <Card>
            <Statistic
              title="Tạm dừng"
              value={stats.inactiveItems}
              prefix={<SettingOutlined />}
              valueStyle={{ color: "#fa8c16" }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={6}>
          <Card>
            <Statistic
              title="Giá trung bình"
              value={stats.avgPrice}
              prefix={<DollarOutlined />}
              valueStyle={{ color: "#722ed1" }}
              formatter={(value) => `${Math.round(value).toLocaleString()} VNĐ`}
            />
          </Card>
        </Col>
      </Row>

      {/* Filters */}
      <Card style={{ marginBottom: "24px" }}>
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} sm={8}>
            <Search
              placeholder="Tìm kiếm món ăn..."
              onSearch={handleSearch}
              enterButton={<SearchOutlined />}
            />
          </Col>
          <Col xs={24} sm={6}>
            <Select
              placeholder="Danh mục"
              style={{ width: "100%" }}
              allowClear
              onChange={(value) => handleFilterChange("categoryId", value)}
            >
              {categories.map((category) => (
                <Option key={category.categoryId} value={category.categoryId}>
                  {category.name}
                </Option>
              ))}
            </Select>
          </Col>
          <Col xs={24} sm={6}>
            <Select
              placeholder="Trạng thái"
              style={{ width: "100%" }}
              allowClear
              onChange={(value) => handleFilterChange("active", value)}
            >
              <Option value={true}>Đang bán</Option>
              <Option value={false}>Tạm dừng</Option>
            </Select>
          </Col>
          <Col xs={24} sm={4}>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => loadMenuItems()}
              style={{ width: "100%" }}
            >
              Làm mới
            </Button>
          </Col>
        </Row>
        {selectedRowKeys.length > 0 && (
          <Row style={{ marginTop: 16 }}>
            <Col span={24}>
              <Space>
                <span>Đã chọn: {selectedRowKeys.length} món ăn</span>
                <Button
                  danger
                  icon={<DeleteOutlined />}
                  onClick={handleDeleteMultiple}
                >
                  Xóa đã chọn ({selectedRowKeys.length})
                </Button>
                <Button onClick={() => setSelectedRowKeys([])}>Bỏ chọn</Button>
              </Space>
            </Col>
          </Row>
        )}
      </Card>

      {/* Table */}
      <Card>
        <Table
          columns={columns}
          dataSource={menuItems}
          rowKey="menuItemId"
          loading={loading}
          rowSelection={{
            selectedRowKeys,
            onChange: setSelectedRowKeys,
            getCheckboxProps: (record) => ({
              name: record.name,
            }),
          }}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) =>
              `${range[0]}-${range[1]} của ${total} mục`,
            pageSizeOptions: PAGINATION.PAGE_SIZE_OPTIONS,
          }}
          onChange={handleTableChange}
          scroll={{ x: 1200 }}
        />
      </Card>

      {/* Modal */}
      <Modal
        key={
          modalType === "ingredients"
            ? `ingredients-${selectedMenuItem?.menuItemId || "new"}`
            : modalType
        }
        title={getModalTitle()}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => {
          setModalVisible(false);
          form.resetFields();
          setSelectedMenuItem(null);
          setFormInitialValues({});
        }}
        width={600}
        okText="Lưu"
        cancelText="Hủy"
        destroyOnHidden={true}
      >
        <Form
          form={form}
          layout="vertical"
          preserve={false}
          initialValues={modalType === "ingredients" ? formInitialValues : {}}
          key={
            modalType === "ingredients"
              ? `form-${selectedMenuItem?.menuItemId || "new"}`
              : modalType
          }
        >
          {getFormFields()}
        </Form>
      </Modal>
    </div>
  );
};

export default MenuManagement;
