import React, { useState, useEffect } from "react";
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
  App,
  Popconfirm,
  Row,
  Col,
  Upload,
  Image,
  DatePicker,
  Avatar,
} from "antd";
import {
  PlusOutlined,
  EditOutlined,
  SearchOutlined,
  ReloadOutlined,
  DeleteOutlined,
  UserOutlined,
  UploadOutlined,
  MailOutlined,
  PhoneOutlined,
  LockOutlined,
  SettingOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import apiService from "../services/apiService";
import { PAGINATION } from "../constants.js";
import { hasRole } from "../utils/auth";
import { useAuth } from "../context/AuthContext";
import Loading from "../components/Common/Loading";
import ErrorPage from "../components/Common/ErrorPage";
import dayjs from "dayjs";
import {
  dispatchDataRefresh,
  DATA_REFRESH_EVENTS,
} from "../utils/dataRefreshEvents";

const { Option } = Select;
const { Search } = Input;
const { TextArea } = Input;

const Staff = () => {
  const { message: antMessage } = App.useApp();
  const { role, loading: authLoading } = useAuth();
  const navigate = useNavigate();

  // Check permission - only RESTAURANT_MANAGER and ADMIN can access
  if (authLoading) {
    return <Loading tip="Đang kiểm tra quyền truy cập..." />;
  }

  if (!hasRole(role, ["RESTAURANT_MANAGER", "ADMIN"])) {
    return (
      <ErrorPage
        status={403}
        title="403 - Không có quyền truy cập"
        subTitle="Chỉ quản lý nhà hàng và quản trị viên mới có thể quản lý nhân viên."
        showHomeButton={false}
        showReloadButton={false}
        onBack={() => navigate("/dashboard")}
      />
    );
  }

  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: PAGINATION.DEFAULT_PAGE_SIZE,
    total: 0,
  });
  const [filters, setFilters] = useState({
    role: "",
    status: "",
    search: "",
  });
  const [modalVisible, setModalVisible] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [form] = Form.useForm();
  const [uploading, setUploading] = useState(false);
  const [imagePreview, setImagePreview] = useState(null);

  // Load users data
  const loadUsers = async (page = 1, size = PAGINATION.DEFAULT_PAGE_SIZE) => {
    setLoading(true);
    try {
      const params = {
        page: page - 1, // Backend uses 0-based pagination
        size,
        ...(filters.role && { role: filters.role }),
        ...(filters.status && { status: filters.status }),
        ...(filters.search &&
          filters.search.trim() !== "" && { search: filters.search }),
      };

      const response = await apiService.user.getAllUsers(params);
      setUsers(response.users || []);
      setPagination((prev) => ({
        ...prev,
        current: page,
        total: response.totalElements || 0,
      }));
    } catch (error) {
      antMessage.error("Lỗi khi tải dữ liệu nhân viên");
      console.error("Error loading users:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, [filters]);

  // Handle table changes
  const handleTableChange = (paginationInfo) => {
    loadUsers(paginationInfo.current, paginationInfo.pageSize);
  };

  // Handle search and filters
  const handleSearch = (value) => {
    setFilters((prev) => ({ ...prev, search: value }));
  };

  const handleFilterChange = (key, value) => {
    setFilters((prev) => ({ ...prev, [key]: value }));
  };

  // Modal handlers
  const showModal = (user = null) => {
    setEditingUser(user);
    setModalVisible(true);
    form.resetFields();
    setImagePreview(null);

    if (user) {
      // Populate form with user data
      form.setFieldsValue({
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.email,
        username: user.username,
        phone: user.phone,
        address: user.address,
        role: user.role,
        dateOfBirth: user.dateOfBirth ? dayjs(user.dateOfBirth) : null,
        avatarUrl: user.avatarUrl,
        avatarPublicId: user.avatarPublicId,
      });
      if (user.avatarUrl) {
        setImagePreview(user.avatarUrl);
      }
    }
  };

  const handleCancel = () => {
    setModalVisible(false);
    setEditingUser(null);
    form.resetFields();
    setImagePreview(null);
  };

  // Handle avatar upload
  const handleAvatarUpload = async (file) => {
    setUploading(true);
    try {
      const result = await apiService.cloudinary.uploadUserAvatar(file);
      form.setFieldsValue({
        avatarUrl: result.url || result.secure_url,
        avatarPublicId: result.publicId || result.public_id,
      });
      setImagePreview(result.url || result.secure_url);
      antMessage.success("Tải ảnh đại diện thành công");
    } catch (error) {
      antMessage.error("Lỗi khi tải ảnh đại diện");
      console.error("Error uploading avatar:", error);
    } finally {
      setUploading(false);
    }
    return false; // Prevent auto upload
  };

  // Handle create/update user
  const handleSubmitUser = async (values) => {
    try {
      setLoading(true);
      const payload = {
        ...values,
        dateOfBirth: values.dateOfBirth
          ? values.dateOfBirth.format("YYYY-MM-DD")
          : null,
      };

      if (editingUser) {
        // Update existing user
        await apiService.user.updateUser(editingUser.userId, payload);
        antMessage.success("Cập nhật nhân viên thành công");
        dispatchDataRefresh(DATA_REFRESH_EVENTS.USER_UPDATED, {
          ...payload,
          userId: editingUser.userId,
        });
      } else {
        // Create new user
        const response = await apiService.user.createUser(payload);
        antMessage.success("Tạo nhân viên thành công");
        dispatchDataRefresh(
          DATA_REFRESH_EVENTS.USER_CREATED,
          response || payload
        );
      }

      handleCancel();
      // Reload users with current pagination
      loadUsers(pagination.current, pagination.pageSize);
    } catch (error) {
      const errorMsg =
        error?.response?.data?.message ||
        (editingUser ? "Lỗi khi cập nhật nhân viên" : "Lỗi khi tạo nhân viên");
      antMessage.error(errorMsg);
      console.error("Error submitting user:", error);
    } finally {
      setLoading(false);
    }
  };

  // Handle verify email
  const handleVerifyEmail = async (userId) => {
    try {
      setLoading(true);
      await apiService.user.verifyEmail(userId);
      antMessage.success("Xác thực email thành công");
      dispatchDataRefresh(DATA_REFRESH_EVENTS.USER_UPDATED, { userId });
      // Reload users with current pagination
      loadUsers(pagination.current, pagination.pageSize);
    } catch (error) {
      const errorMsg =
        error?.response?.data?.message || "Lỗi khi xác thực email";
      antMessage.error(errorMsg);
      console.error("Error verifying email:", error);
    } finally {
      setLoading(false);
    }
  };

  // Handle sync role to Keycloak
  const handleSyncRole = async (userId) => {
    try {
      setLoading(true);
      await apiService.user.syncUserRole(userId);
      antMessage.success(
        "Đã đồng bộ role sang Keycloak. User cần đăng xuất và đăng nhập lại để nhận JWT token mới."
      );
      dispatchDataRefresh(DATA_REFRESH_EVENTS.USER_UPDATED, { userId });
      // Reload users with current pagination
      loadUsers(pagination.current, pagination.pageSize);
    } catch (error) {
      const errorMsg = error?.response?.data?.message || "Lỗi khi đồng bộ role";
      antMessage.error(errorMsg);
      console.error("Error syncing role:", error);
    } finally {
      setLoading(false);
    }
  };

  // Handle delete user
  const handleDeleteUser = async (userId) => {
    try {
      setLoading(true);
      await apiService.user.deleteUser(userId);
      antMessage.success("Xóa nhân viên thành công");
      dispatchDataRefresh(DATA_REFRESH_EVENTS.USER_DELETED, { userId });
      // Reload users with current pagination
      loadUsers(pagination.current, pagination.pageSize);
    } catch (error) {
      const errorMsg =
        error?.response?.data?.message || "Lỗi khi xóa nhân viên";
      antMessage.error(errorMsg);
      console.error("Error deleting user:", error);
    } finally {
      setLoading(false);
    }
  };

  // Handle toggle status
  const handleToggleStatus = async (userId, currentStatus) => {
    try {
      setLoading(true);
      const newStatus = currentStatus === "ACTIVE" ? "INACTIVE" : "ACTIVE";
      await apiService.user.toggleUserStatus(userId, newStatus);
      antMessage.success(
        `Đã ${newStatus === "ACTIVE" ? "kích hoạt" : "vô hiệu hóa"} nhân viên`
      );
      dispatchDataRefresh(DATA_REFRESH_EVENTS.USER_UPDATED, {
        userId,
        status: newStatus,
      });
      // Reload users with current pagination
      loadUsers(pagination.current, pagination.pageSize);
    } catch (error) {
      const errorMsg =
        error?.response?.data?.message || "Lỗi khi thay đổi trạng thái";
      antMessage.error(errorMsg);
      console.error("Error toggling status:", error);
    } finally {
      setLoading(false);
    }
  };

  // Get role label
  const getRoleLabel = (role) => {
    const roleMap = {
      CUSTOMER: "Khách hàng",
      STAFF: "Nhân viên phục vụ",
      KITCHEN_STAFF: "Nhân viên bếp",
      WAREHOUSE_STAFF: "Nhân viên kho",
      RESTAURANT_MANAGER: "Quản lý",
      ADMIN: "Quản trị viên",
    };
    return roleMap[role] || role;
  };

  // Get role color
  const getRoleColor = (role) => {
    const colorMap = {
      CUSTOMER: "blue",
      STAFF: "green",
      KITCHEN_STAFF: "orange",
      WAREHOUSE_STAFF: "cyan",
      RESTAURANT_MANAGER: "purple",
      ADMIN: "red",
    };
    return colorMap[role] || "default";
  };

  // Get status label
  const getStatusLabel = (status) => {
    const statusMap = {
      ACTIVE: "Hoạt động",
      INACTIVE: "Tạm dừng",
      BANNED: "Bị cấm",
    };
    return statusMap[status] || status;
  };

  // Get status color
  const getStatusColor = (status) => {
    const colorMap = {
      ACTIVE: "success",
      INACTIVE: "warning",
      BANNED: "error",
    };
    return colorMap[status] || "default";
  };

  // Table columns
  const columns = [
    {
      title: "Ảnh đại diện",
      dataIndex: "avatarUrl",
      key: "avatarUrl",
      width: 80,
      render: (url) => <Avatar src={url} icon={<UserOutlined />} size={40} />,
    },
    {
      title: "Tên đăng nhập",
      dataIndex: "username",
      key: "username",
      sorter: true,
    },
    {
      title: "Họ và tên",
      key: "fullName",
      render: (_, record) =>
        `${record.firstName || ""} ${record.lastName || ""}`.trim() || "-",
    },
    {
      title: "Email",
      dataIndex: "email",
      key: "email",
    },
    {
      title: "Số điện thoại",
      dataIndex: "phone",
      key: "phone",
      render: (phone) => phone || "-",
    },
    {
      title: "Vai trò",
      dataIndex: "role",
      key: "role",
      render: (role) => (
        <Tag color={getRoleColor(role)}>{getRoleLabel(role)}</Tag>
      ),
      filters: [
        { text: "Khách hàng", value: "CUSTOMER" },
        { text: "Nhân viên phục vụ", value: "STAFF" },
        { text: "Nhân viên bếp", value: "KITCHEN_STAFF" },
        { text: "Nhân viên kho", value: "WAREHOUSE_STAFF" },
        { text: "Quản lý", value: "RESTAURANT_MANAGER" },
        { text: "Quản trị viên", value: "ADMIN" },
      ],
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      render: (status) => (
        <Tag color={getStatusColor(status)}>{getStatusLabel(status)}</Tag>
      ),
      filters: [
        { text: "Hoạt động", value: "ACTIVE" },
        { text: "Tạm dừng", value: "INACTIVE" },
        { text: "Bị cấm", value: "BANNED" },
      ],
    },
    {
      title: "Thao tác",
      key: "actions",
      width: 250,
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => showModal(record)}
          >
            Sửa
          </Button>
          {record.status === "INACTIVE" && (
            <Popconfirm
              title="Xác nhận xác thực email cho nhân viên này?"
              onConfirm={() => handleVerifyEmail(record.userId)}
              okText="Xác nhận"
              cancelText="Hủy"
            >
              <Button type="link" size="small" icon={<MailOutlined />}>
                Xác thực email
              </Button>
            </Popconfirm>
          )}
          <Popconfirm
            title="Đồng bộ role sang Keycloak?"
            description="Role sẽ được gán trong Keycloak. User cần đăng xuất và đăng nhập lại để nhận JWT token mới có role."
            onConfirm={() => handleSyncRole(record.userId)}
            okText="Đồng bộ"
            cancelText="Hủy"
          >
            <Button type="link" size="small" icon={<SettingOutlined />}>
              Đồng bộ role
            </Button>
          </Popconfirm>
          <Popconfirm
            title={`Xác nhận ${
              record.status === "ACTIVE" ? "vô hiệu hóa" : "kích hoạt"
            } nhân viên này?`}
            onConfirm={() => handleToggleStatus(record.userId, record.status)}
            okText="Xác nhận"
            cancelText="Hủy"
          >
            <Button
              type="link"
              size="small"
              danger={record.status === "ACTIVE"}
            >
              {record.status === "ACTIVE" ? "Vô hiệu hóa" : "Kích hoạt"}
            </Button>
          </Popconfirm>
          <Popconfirm
            title="Xác nhận xóa nhân viên này?"
            description="Hành động này không thể hoàn tác. User sẽ bị xóa khỏi cả database và Keycloak."
            onConfirm={() => handleDeleteUser(record.userId)}
            okText="Xóa"
            cancelText="Hủy"
            okButtonProps={{ danger: true }}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              Xóa
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: "24px" }}>
      <Card>
        <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
          <Col xs={24} sm={12} md={8}>
            <Search
              placeholder="Tìm kiếm theo tên, email, username..."
              allowClear
              enterButton={<SearchOutlined />}
              onSearch={handleSearch}
              style={{ width: "100%" }}
            />
          </Col>
          <Col xs={24} sm={12} md={4}>
            <Select
              placeholder="Lọc theo vai trò"
              allowClear
              style={{ width: "100%" }}
              onChange={(value) => handleFilterChange("role", value)}
            >
              <Option value="CUSTOMER">Khách hàng</Option>
              <Option value="STAFF">Nhân viên phục vụ</Option>
              <Option value="KITCHEN_STAFF">Nhân viên bếp</Option>
              <Option value="WAREHOUSE_STAFF">Nhân viên kho</Option>
              <Option value="RESTAURANT_MANAGER">Quản lý</Option>
              <Option value="ADMIN">Quản trị viên</Option>
            </Select>
          </Col>
          <Col xs={24} sm={12} md={4}>
            <Select
              placeholder="Lọc theo trạng thái"
              allowClear
              style={{ width: "100%" }}
              onChange={(value) => handleFilterChange("status", value)}
            >
              <Option value="ACTIVE">Hoạt động</Option>
              <Option value="INACTIVE">Tạm dừng</Option>
              <Option value="BANNED">Bị cấm</Option>
            </Select>
          </Col>
          <Col xs={24} sm={12} md={8} style={{ textAlign: "right" }}>
            <Space>
              <Button icon={<ReloadOutlined />} onClick={() => loadUsers()}>
                Làm mới
              </Button>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => showModal()}
              >
                Tạo nhân viên
              </Button>
            </Space>
          </Col>
        </Row>

        <Table
          columns={columns}
          dataSource={users}
          rowKey="userId"
          loading={loading}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            showSizeChanger: true,
            showTotal: (total) => `Tổng ${total} nhân viên`,
            pageSizeOptions: PAGINATION.PAGE_SIZE_OPTIONS,
          }}
          onChange={handleTableChange}
        />
      </Card>

      {/* Create/Edit User Modal */}
      <Modal
        title={editingUser ? "Chỉnh sửa nhân viên" : "Tạo nhân viên mới"}
        open={modalVisible}
        onCancel={handleCancel}
        footer={null}
        width={600}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmitUser}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="firstName"
                label="Họ"
                rules={[{ required: true, message: "Vui lòng nhập họ" }]}
              >
                <Input placeholder="Nhập họ" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="lastName" label="Tên">
                <Input placeholder="Nhập tên" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="email"
            label="Email"
            rules={[
              { required: true, message: "Vui lòng nhập email" },
              { type: "email", message: "Email không hợp lệ" },
            ]}
          >
            <Input
              prefix={<MailOutlined />}
              placeholder="Nhập email"
              disabled={!!editingUser}
            />
          </Form.Item>

          <Form.Item
            name="username"
            label="Tên đăng nhập"
            rules={[{ required: true, message: "Vui lòng nhập tên đăng nhập" }]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="Nhập tên đăng nhập"
              disabled={!!editingUser}
            />
          </Form.Item>

          {!editingUser && (
            <Form.Item
              name="password"
              label="Mật khẩu"
              rules={[
                { required: true, message: "Vui lòng nhập mật khẩu" },
                { min: 6, message: "Mật khẩu phải có ít nhất 6 ký tự" },
              ]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="Nhập mật khẩu"
              />
            </Form.Item>
          )}

          <Form.Item name="phone" label="Số điện thoại">
            <Input
              prefix={<PhoneOutlined />}
              placeholder="Nhập số điện thoại"
            />
          </Form.Item>

          <Form.Item name="address" label="Địa chỉ">
            <TextArea rows={2} placeholder="Nhập địa chỉ" />
          </Form.Item>

          <Form.Item name="dateOfBirth" label="Ngày sinh">
            <DatePicker
              style={{ width: "100%" }}
              format="DD/MM/YYYY"
              placeholder="Chọn ngày sinh"
            />
          </Form.Item>

          <Form.Item
            name="role"
            label="Vai trò"
            rules={[{ required: true, message: "Vui lòng chọn vai trò" }]}
          >
            <Select placeholder="Chọn vai trò">
              <Option value="STAFF">Nhân viên phục vụ</Option>
              <Option value="KITCHEN_STAFF">Nhân viên bếp</Option>
              <Option value="WAREHOUSE_STAFF">Nhân viên kho</Option>
              <Option value="RESTAURANT_MANAGER">Quản lý nhà hàng</Option>
              <Option value="ADMIN">Quản trị viên</Option>
            </Select>
          </Form.Item>

          <Form.Item label="Ảnh đại diện">
            <Space direction="vertical" style={{ width: "100%" }}>
              {imagePreview && (
                <Image
                  src={imagePreview}
                  alt="Avatar preview"
                  width={100}
                  height={100}
                  style={{ objectFit: "cover", borderRadius: "8px" }}
                />
              )}
              <Upload
                beforeUpload={handleAvatarUpload}
                showUploadList={false}
                accept="image/*"
              >
                <Button icon={<UploadOutlined />} loading={uploading}>
                  {uploading ? "Đang tải..." : "Tải ảnh đại diện"}
                </Button>
              </Upload>
            </Space>
          </Form.Item>

          <Form.Item name="avatarUrl" hidden>
            <Input />
          </Form.Item>

          <Form.Item name="avatarPublicId" hidden>
            <Input />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={loading}>
                {editingUser ? "Cập nhật" : "Tạo nhân viên"}
              </Button>
              <Button onClick={handleCancel}>Hủy</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Staff;
