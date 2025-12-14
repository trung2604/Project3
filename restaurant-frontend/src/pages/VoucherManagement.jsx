import React, { useState, useEffect } from "react";
import {
  Card,
  Table,
  Button,
  Space,
  Tag,
  Modal,
  Form,
  InputNumber,
  Input,
  DatePicker,
  App,
  Popconfirm,
  Select,
  message,
} from "antd";
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  GiftOutlined,
} from "@ant-design/icons";
import { useAuth } from "../context/AuthContext";
import { canViewOverview } from "../utils/auth";
import Loading from "../components/Common/Loading";
import ErrorPage from "../components/Common/ErrorPage";
import loyaltyService from "../services/loyaltyService";
import dayjs from "dayjs";
import { VOUCHER_STATUS } from "../constants.js";

const { TextArea } = Input;
const { Option } = Select;

const VoucherManagement = () => {
  const { message: messageApi } = App.useApp();
  const { role, loading: authLoading } = useAuth();
  const [loading, setLoading] = useState(false);
  const [vouchers, setVouchers] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [modalType, setModalType] = useState("create");
  const [selectedVoucher, setSelectedVoucher] = useState(null);
  const [form] = Form.useForm();
  const [statusFilter, setStatusFilter] = useState(null);

  useEffect(() => {
    if (!authLoading) {
      loadVouchers();
    }
  }, [authLoading, statusFilter]);

  const loadVouchers = async () => {
    setLoading(true);
    try {
      const params = statusFilter ? { status: statusFilter } : {};
      const response = await loyaltyService.getAllVouchers(params);
      if (response?.data) {
        setVouchers(response.data || []);
      }
    } catch (error) {
      console.error("Error loading vouchers:", error);
      messageApi.error("Không thể tải danh sách voucher");
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setModalType("create");
    setSelectedVoucher(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (voucher) => {
    setModalType("edit");
    setSelectedVoucher(voucher);
    form.setFieldsValue({
      ...voucher,
      validFrom: voucher.validFrom ? dayjs(voucher.validFrom) : null,
      validTo: voucher.validTo ? dayjs(voucher.validTo) : null,
    });
    setModalVisible(true);
  };

  const handleDelete = async (voucherId) => {
    try {
      await loyaltyService.deleteVoucher(voucherId);
      messageApi.success("Đã xóa voucher thành công");
      loadVouchers();
    } catch (error) {
      messageApi.error(
        error.response?.data?.message || "Không thể xóa voucher"
      );
    }
  };

  const handleSubmit = async (values) => {
    try {
      const data = {
        ...values,
        validFrom: values.validFrom ? values.validFrom.toISOString() : null,
        validTo: values.validTo ? values.validTo.toISOString() : null,
      };

      if (modalType === "create") {
        await loyaltyService.createVoucher(data);
        messageApi.success("Đã tạo voucher thành công");
      } else {
        await loyaltyService.updateVoucher(selectedVoucher.voucherId, data);
        messageApi.success("Đã cập nhật voucher thành công");
      }
      setModalVisible(false);
      form.resetFields();
      loadVouchers();
    } catch (error) {
      messageApi.error(
        error.response?.data?.message || "Không thể lưu voucher"
      );
    }
  };

  const columns = [
    {
      title: "Mã voucher",
      dataIndex: "code",
      key: "code",
      render: (code) => <Tag color="blue">{code}</Tag>,
    },
    {
      title: "Tên",
      dataIndex: "name",
      key: "name",
    },
    {
      title: "Điểm cần đổi",
      dataIndex: "pointsRequired",
      key: "pointsRequired",
      render: (points) => <span style={{ fontWeight: "bold" }}>{points}</span>,
    },
    {
      title: "Giảm giá",
      key: "discount",
      render: (_, record) => {
        if (record.discountAmount) {
          return `${record.discountAmount.toLocaleString()} VND`;
        }
        if (record.discountPercentage) {
          return `${record.discountPercentage}%`;
        }
        return "-";
      },
    },
    {
      title: "Số lượng",
      key: "quantity",
      render: (_, record) =>
        `${record.remainingQuantity || 0}/${record.totalQuantity || 0}`,
    },
    {
      title: "Hạn sử dụng",
      key: "validity",
      render: (_, record) => {
        if (record.validFrom && record.validTo) {
          return (
            <div>
              <div>{dayjs(record.validFrom).format("DD/MM/YYYY")}</div>
              <div style={{ color: "#999" }}>
                đến {dayjs(record.validTo).format("DD/MM/YYYY")}
              </div>
            </div>
          );
        }
        return "Không giới hạn";
      },
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      render: (status) => {
        const statusMap = {
          [VOUCHER_STATUS.ACTIVE]: { color: "green", text: "Hoạt động" },
          [VOUCHER_STATUS.INACTIVE]: { color: "default", text: "Tạm dừng" },
          [VOUCHER_STATUS.USED_UP]: { color: "orange", text: "Hết hàng" },
          [VOUCHER_STATUS.EXPIRED]: { color: "red", text: "Hết hạn" },
        };
        const statusInfo = statusMap[status] || {
          color: "default",
          text: status,
        };
        return <Tag color={statusInfo.color}>{statusInfo.text}</Tag>;
      },
    },
    {
      title: "Thao tác",
      key: "actions",
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            Sửa
          </Button>
          <Popconfirm
            title="Xác nhận xóa"
            description="Bạn có chắc chắn muốn xóa voucher này?"
            onConfirm={() => handleDelete(record.voucherId)}
            okText="Xóa"
            cancelText="Hủy"
          >
            <Button type="link" danger icon={<DeleteOutlined />}>
              Xóa
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  if (authLoading) {
    return <Loading tip="Đang kiểm tra quyền truy cập..." />;
  }

  if (!canViewOverview(role)) {
    return (
      <ErrorPage
        status={403}
        title="403 - Không có quyền truy cập"
        subTitle="Chỉ quản lý nhà hàng và quản trị viên mới có thể quản lý voucher."
      />
    );
  }

  return (
    <div style={{ padding: "24px" }}>
      <Card
        title={
          <Space>
            <GiftOutlined />
            <span>Quản lý Voucher</span>
          </Space>
        }
        extra={
          <Space>
            <Select
              style={{ width: 150 }}
              placeholder="Lọc theo trạng thái"
              allowClear
              value={statusFilter}
              onChange={setStatusFilter}
            >
              <Option value={VOUCHER_STATUS.ACTIVE}>Hoạt động</Option>
              <Option value={VOUCHER_STATUS.INACTIVE}>Tạm dừng</Option>
              <Option value={VOUCHER_STATUS.USED_UP}>Hết hàng</Option>
              <Option value={VOUCHER_STATUS.EXPIRED}>Hết hạn</Option>
            </Select>
            <Button
              icon={<ReloadOutlined />}
              onClick={loadVouchers}
              loading={loading}
            >
              Làm mới
            </Button>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={handleCreate}
            >
              Tạo voucher mới
            </Button>
          </Space>
        }
      >
        <Table
          columns={columns}
          dataSource={vouchers}
          rowKey="voucherId"
          loading={loading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `Tổng ${total} voucher`,
          }}
        />
      </Card>

      <Modal
        title={modalType === "create" ? "Tạo voucher mới" : "Cập nhật voucher"}
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false);
          form.resetFields();
        }}
        onOk={() => form.submit()}
        okText={modalType === "create" ? "Tạo" : "Cập nhật"}
        cancelText="Hủy"
        width={600}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item
            name="name"
            label="Tên voucher"
            rules={[{ required: true, message: "Vui lòng nhập tên voucher" }]}
          >
            <Input placeholder="Nhập tên voucher" />
          </Form.Item>
          <Form.Item name="description" label="Mô tả">
            <TextArea rows={3} placeholder="Nhập mô tả voucher" />
          </Form.Item>
          <Form.Item
            name="pointsRequired"
            label="Điểm cần đổi"
            rules={[{ required: true, message: "Vui lòng nhập số điểm" }]}
          >
            <InputNumber
              min={1}
              style={{ width: "100%" }}
              placeholder="Nhập số điểm cần đổi"
            />
          </Form.Item>
          <Form.Item name="discountAmount" label="Số tiền giảm (VND)">
            <InputNumber
              min={0}
              style={{ width: "100%" }}
              placeholder="Nhập số tiền giảm"
            />
          </Form.Item>
          <Form.Item name="discountPercentage" label="Phần trăm giảm (%)">
            <InputNumber
              min={0}
              max={100}
              style={{ width: "100%" }}
              placeholder="Nhập phần trăm giảm"
            />
          </Form.Item>
          <Form.Item name="maxDiscountAmount" label="Giảm tối đa (VND)">
            <InputNumber
              min={0}
              style={{ width: "100%" }}
              placeholder="Nhập số tiền giảm tối đa"
            />
          </Form.Item>
          <Form.Item name="minOrderAmount" label="Đơn hàng tối thiểu (VND)">
            <InputNumber
              min={0}
              style={{ width: "100%" }}
              placeholder="Nhập số tiền đơn hàng tối thiểu"
            />
          </Form.Item>
          <Form.Item
            name="totalQuantity"
            label="Tổng số lượng"
            rules={[{ required: true, message: "Vui lòng nhập tổng số lượng" }]}
          >
            <InputNumber
              min={1}
              style={{ width: "100%" }}
              placeholder="Nhập tổng số lượng voucher"
            />
          </Form.Item>
          <Form.Item name="validFrom" label="Ngày bắt đầu">
            <DatePicker
              style={{ width: "100%" }}
              format="DD/MM/YYYY"
              placeholder="Chọn ngày bắt đầu"
            />
          </Form.Item>
          <Form.Item name="validTo" label="Ngày kết thúc">
            <DatePicker
              style={{ width: "100%" }}
              format="DD/MM/YYYY"
              placeholder="Chọn ngày kết thúc"
            />
          </Form.Item>
          <Form.Item
            name="status"
            label="Trạng thái"
            initialValue={VOUCHER_STATUS.ACTIVE}
          >
            <Select>
              <Option value={VOUCHER_STATUS.ACTIVE}>Hoạt động</Option>
              <Option value={VOUCHER_STATUS.INACTIVE}>Tạm dừng</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default VoucherManagement;
