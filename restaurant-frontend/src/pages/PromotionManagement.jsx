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
  TimePicker,
  message,
} from "antd";
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  FireOutlined,
} from "@ant-design/icons";
import { useAuth } from "../context/AuthContext";
import { canViewOverview } from "../utils/auth";
import Loading from "../components/Common/Loading";
import ErrorPage from "../components/Common/ErrorPage";
import loyaltyService from "../services/loyaltyService";
import dayjs from "dayjs";
import { PROMOTION_STATUS, PROMOTION_TYPE } from "../constants.js";

const { TextArea } = Input;
const { Option } = Select;

const PromotionManagement = () => {
  const { message: messageApi } = App.useApp();
  const { role, loading: authLoading } = useAuth();
  const [loading, setLoading] = useState(false);
  const [promotions, setPromotions] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [modalType, setModalType] = useState("create");
  const [selectedPromotion, setSelectedPromotion] = useState(null);
  const [form] = Form.useForm();
  const [statusFilter, setStatusFilter] = useState(null);
  const [typeFilter, setTypeFilter] = useState(null);

  useEffect(() => {
    if (!authLoading) {
      loadPromotions();
    }
  }, [authLoading, statusFilter, typeFilter]);

  const loadPromotions = async () => {
    setLoading(true);
    try {
      const params = {};
      if (statusFilter) params.status = statusFilter;
      if (typeFilter) params.type = typeFilter;
      const response = await loyaltyService.getAllPromotions(params);
      if (response?.data) {
        setPromotions(response.data || []);
      }
    } catch (error) {
      console.error("Error loading promotions:", error);
      messageApi.error("Không thể tải danh sách promotion");
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setModalType("create");
    setSelectedPromotion(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (promotion) => {
    setModalType("edit");
    setSelectedPromotion(promotion);
    form.setFieldsValue({
      ...promotion,
      validFrom: promotion.validFrom ? dayjs(promotion.validFrom) : null,
      validTo: promotion.validTo ? dayjs(promotion.validTo) : null,
      startTime: promotion.startTime
        ? dayjs(promotion.startTime, "HH:mm")
        : null,
      endTime: promotion.endTime ? dayjs(promotion.endTime, "HH:mm") : null,
    });
    setModalVisible(true);
  };

  const handleDelete = async (promotionId) => {
    try {
      await loyaltyService.deletePromotion(promotionId);
      messageApi.success("Đã xóa promotion thành công");
      loadPromotions();
    } catch (error) {
      messageApi.error(
        error.response?.data?.message || "Không thể xóa promotion"
      );
    }
  };

  const handleSubmit = async (values) => {
    try {
      const data = {
        ...values,
        validFrom: values.validFrom ? values.validFrom.toISOString() : null,
        validTo: values.validTo ? values.validTo.toISOString() : null,
        startTime: values.startTime ? values.startTime.format("HH:mm") : null,
        endTime: values.endTime ? values.endTime.format("HH:mm") : null,
      };

      if (modalType === "create") {
        await loyaltyService.createPromotion(data);
        messageApi.success("Đã tạo promotion thành công");
      } else {
        await loyaltyService.updatePromotion(
          selectedPromotion.promotionId,
          data
        );
        messageApi.success("Đã cập nhật promotion thành công");
      }
      setModalVisible(false);
      form.resetFields();
      loadPromotions();
    } catch (error) {
      messageApi.error(
        error.response?.data?.message || "Không thể lưu promotion"
      );
    }
  };

  const columns = [
    {
      title: "Tên",
      dataIndex: "name",
      key: "name",
    },
    {
      title: "Loại",
      dataIndex: "type",
      key: "type",
      render: (type) => {
        const typeMap = {
          [PROMOTION_TYPE.COMBO]: { color: "purple", text: "Combo" },
          [PROMOTION_TYPE.HAPPY_HOUR]: { color: "orange", text: "Happy Hour" },
          [PROMOTION_TYPE.DISCOUNT]: { color: "blue", text: "Giảm giá" },
          [PROMOTION_TYPE.FREE_ITEM]: { color: "green", text: "Tặng món" },
          [PROMOTION_TYPE.POINTS_MULTIPLIER]: {
            color: "gold",
            text: "Nhân điểm",
          },
        };
        const typeInfo = typeMap[type] || { color: "default", text: type };
        return <Tag color={typeInfo.color}>{typeInfo.text}</Tag>;
      },
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
        if (record.pointsMultiplier) {
          return `x${record.pointsMultiplier} điểm`;
        }
        return "-";
      },
    },
    {
      title: "Thời gian",
      key: "time",
      render: (_, record) => {
        if (record.startTime && record.endTime) {
          return `${record.startTime} - ${record.endTime}`;
        }
        return "-";
      },
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
          [PROMOTION_STATUS.ACTIVE]: { color: "green", text: "Hoạt động" },
          [PROMOTION_STATUS.INACTIVE]: { color: "default", text: "Tạm dừng" },
          [PROMOTION_STATUS.EXPIRED]: { color: "red", text: "Hết hạn" },
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
            description="Bạn có chắc chắn muốn xóa promotion này?"
            onConfirm={() => handleDelete(record.promotionId)}
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
        subTitle="Chỉ quản lý nhà hàng và quản trị viên mới có thể quản lý promotion."
      />
    );
  }

  return (
    <div style={{ padding: "24px" }}>
      <Card
        title={
          <Space>
            <FireOutlined />
            <span>Quản lý Promotion</span>
          </Space>
        }
        extra={
          <Space>
            <Select
              style={{ width: 150 }}
              placeholder="Lọc theo loại"
              allowClear
              value={typeFilter}
              onChange={setTypeFilter}
            >
              <Option value={PROMOTION_TYPE.COMBO}>Combo</Option>
              <Option value={PROMOTION_TYPE.HAPPY_HOUR}>Happy Hour</Option>
              <Option value={PROMOTION_TYPE.DISCOUNT}>Giảm giá</Option>
              <Option value={PROMOTION_TYPE.FREE_ITEM}>Tặng món</Option>
              <Option value={PROMOTION_TYPE.POINTS_MULTIPLIER}>
                Nhân điểm
              </Option>
            </Select>
            <Select
              style={{ width: 150 }}
              placeholder="Lọc theo trạng thái"
              allowClear
              value={statusFilter}
              onChange={setStatusFilter}
            >
              <Option value={PROMOTION_STATUS.ACTIVE}>Hoạt động</Option>
              <Option value={PROMOTION_STATUS.INACTIVE}>Tạm dừng</Option>
              <Option value={PROMOTION_STATUS.EXPIRED}>Hết hạn</Option>
            </Select>
            <Button
              icon={<ReloadOutlined />}
              onClick={loadPromotions}
              loading={loading}
            >
              Làm mới
            </Button>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={handleCreate}
            >
              Tạo promotion mới
            </Button>
          </Space>
        }
      >
        <Table
          columns={columns}
          dataSource={promotions}
          rowKey="promotionId"
          loading={loading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `Tổng ${total} promotion`,
          }}
        />
      </Card>

      <Modal
        title={
          modalType === "create" ? "Tạo promotion mới" : "Cập nhật promotion"
        }
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false);
          form.resetFields();
        }}
        onOk={() => form.submit()}
        okText={modalType === "create" ? "Tạo" : "Cập nhật"}
        cancelText="Hủy"
        width={700}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item
            name="name"
            label="Tên promotion"
            rules={[{ required: true, message: "Vui lòng nhập tên promotion" }]}
          >
            <Input placeholder="Nhập tên promotion" />
          </Form.Item>
          <Form.Item name="description" label="Mô tả">
            <TextArea rows={3} placeholder="Nhập mô tả promotion" />
          </Form.Item>
          <Form.Item
            name="type"
            label="Loại promotion"
            rules={[
              { required: true, message: "Vui lòng chọn loại promotion" },
            ]}
          >
            <Select placeholder="Chọn loại promotion">
              <Option value={PROMOTION_TYPE.COMBO}>Combo</Option>
              <Option value={PROMOTION_TYPE.HAPPY_HOUR}>Happy Hour</Option>
              <Option value={PROMOTION_TYPE.DISCOUNT}>Giảm giá</Option>
              <Option value={PROMOTION_TYPE.FREE_ITEM}>Tặng món</Option>
              <Option value={PROMOTION_TYPE.POINTS_MULTIPLIER}>
                Nhân điểm
              </Option>
            </Select>
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
          <Form.Item name="pointsMultiplier" label="Hệ số nhân điểm">
            <InputNumber
              min={1}
              style={{ width: "100%" }}
              placeholder="Nhập hệ số nhân điểm (ví dụ: 2 = x2 điểm)"
            />
          </Form.Item>
          <Form.Item
            name="dayOfWeek"
            label="Ngày trong tuần (0=CN, 1=T2, ..., 6=T7)"
          >
            <InputNumber
              min={0}
              max={6}
              style={{ width: "100%" }}
              placeholder="Nhập ngày trong tuần"
            />
          </Form.Item>
          <Form.Item name="startTime" label="Giờ bắt đầu">
            <TimePicker
              style={{ width: "100%" }}
              format="HH:mm"
              placeholder="Chọn giờ bắt đầu"
            />
          </Form.Item>
          <Form.Item name="endTime" label="Giờ kết thúc">
            <TimePicker
              style={{ width: "100%" }}
              format="HH:mm"
              placeholder="Chọn giờ kết thúc"
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
            initialValue={PROMOTION_STATUS.ACTIVE}
          >
            <Select>
              <Option value={PROMOTION_STATUS.ACTIVE}>Hoạt động</Option>
              <Option value={PROMOTION_STATUS.INACTIVE}>Tạm dừng</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default PromotionManagement;
