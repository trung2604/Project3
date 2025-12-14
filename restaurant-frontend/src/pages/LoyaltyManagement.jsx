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
  App,
  Row,
  Col,
  Statistic,
  Tabs,
  Descriptions,
  message,
} from "antd";
import {
  PlusOutlined,
  ReloadOutlined,
  GiftOutlined,
  DollarOutlined,
  HistoryOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { canViewOverview } from "../utils/auth";
import Loading from "../components/Common/Loading";
import ErrorPage from "../components/Common/ErrorPage";
import loyaltyService from "../services/loyaltyService";
import dayjs from "dayjs";
import { POINTS_TRANSACTION_TYPE } from "../constants.js";

const { TextArea } = Input;
const { TabPane } = Tabs;

const LoyaltyManagement = () => {
  const { message: messageApi } = App.useApp();
  const { role, loading: authLoading } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [account, setAccount] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [voucherUsage, setVoucherUsage] = useState([]);
  const [activeTab, setActiveTab] = useState("account");
  const [earnPointsModalVisible, setEarnPointsModalVisible] = useState(false);
  const [form] = Form.useForm();

  useEffect(() => {
    if (!authLoading) {
      loadData();
    }
  }, [authLoading]);

  const loadData = async () => {
    setLoading(true);
    try {
      const [accountRes, transactionsRes] = await Promise.all([
        loyaltyService.getMyLoyaltyAccount().catch(() => null),
        loyaltyService.getPointsTransactions().catch(() => null),
      ]);

      if (accountRes?.data) {
        setAccount(accountRes.data);
      }
      if (transactionsRes?.data) {
        setTransactions(transactionsRes.data || []);
      }
      // Voucher usage history - try to load if available
      try {
        const usageRes = await loyaltyService.getVoucherUsageHistory();
        if (usageRes?.data) {
          setVoucherUsage(usageRes.data || []);
        }
      } catch (error) {
        // Voucher usage might not be available, ignore
        console.log("Voucher usage history not available");
      }
    } catch (error) {
      console.error("Error loading loyalty data:", error);
      messageApi.error("Không thể tải dữ liệu loyalty");
    } finally {
      setLoading(false);
    }
  };

  const handleEarnPoints = async (values) => {
    try {
      await loyaltyService.earnPoints({
        points: values.points,
        description: values.description || `Points earned manually`,
        orderId: values.orderId || null,
      });
      messageApi.success("Đã tích điểm thành công");
      setEarnPointsModalVisible(false);
      form.resetFields();
      loadData();
    } catch (error) {
      messageApi.error(error.response?.data?.message || "Không thể tích điểm");
    }
  };

  const transactionColumns = [
    {
      title: "Ngày",
      dataIndex: "createdAt",
      key: "createdAt",
      render: (date) => dayjs(date).format("DD/MM/YYYY HH:mm"),
    },
    {
      title: "Loại",
      dataIndex: "type",
      key: "type",
      render: (type) => (
        <Tag color={type === POINTS_TRANSACTION_TYPE.EARNED ? "green" : "red"}>
          {type === POINTS_TRANSACTION_TYPE.EARNED ? "Tích điểm" : "Đổi điểm"}
        </Tag>
      ),
    },
    {
      title: "Điểm",
      dataIndex: "points",
      key: "points",
      render: (points) => (
        <span
          style={{
            color: points > 0 ? "#52c41a" : "#ff4d4f",
            fontWeight: "bold",
          }}
        >
          {points > 0 ? "+" : ""}
          {points}
        </span>
      ),
    },
    {
      title: "Điểm trước",
      dataIndex: "pointsBefore",
      key: "pointsBefore",
    },
    {
      title: "Điểm sau",
      dataIndex: "pointsAfter",
      key: "pointsAfter",
    },
    {
      title: "Mô tả",
      dataIndex: "description",
      key: "description",
    },
  ];

  const voucherUsageColumns = [
    {
      title: "Ngày sử dụng",
      dataIndex: "usedAt",
      key: "usedAt",
      render: (date) => (date ? dayjs(date).format("DD/MM/YYYY HH:mm") : "-"),
    },
    {
      title: "Mã voucher",
      dataIndex: "voucherId",
      key: "voucherId",
    },
    {
      title: "Điểm đã đổi",
      dataIndex: "pointsRedeemed",
      key: "pointsRedeemed",
      render: (points) => <span style={{ color: "#ff4d4f" }}>-{points}</span>,
    },
    {
      title: "Đơn hàng",
      dataIndex: "orderId",
      key: "orderId",
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      render: (status) => (
        <Tag color={status === "USED" ? "green" : "default"}>
          {status === "USED" ? "Đã sử dụng" : status}
        </Tag>
      ),
    },
  ];

  if (authLoading) {
    return <Loading tip="Đang kiểm tra quyền truy cập..." />;
  }

  if (!canViewOverview(role) && role !== "CUSTOMER") {
    return (
      <ErrorPage
        status={403}
        title="403 - Không có quyền truy cập"
        subTitle="Bạn không có quyền xem thông tin loyalty."
      />
    );
  }

  return (
    <div style={{ padding: "24px" }}>
      <Card
        title={
          <Space>
            <GiftOutlined />
            <span>Quản lý Loyalty</span>
          </Space>
        }
        extra={
          <Space>
            <Button
              icon={<ReloadOutlined />}
              onClick={loadData}
              loading={loading}
            >
              Làm mới
            </Button>
            {canViewOverview(role) && (
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => setEarnPointsModalVisible(true)}
              >
                Tích điểm thủ công
              </Button>
            )}
          </Space>
        }
      >
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          tabBarExtraContent={
            canViewOverview(role) ? (
              <Space>
                <Button onClick={() => navigate("/dashboard/loyalty/vouchers")}>
                  Quản lý Voucher
                </Button>
                <Button
                  onClick={() => navigate("/dashboard/loyalty/promotions")}
                >
                  Quản lý Promotion
                </Button>
              </Space>
            ) : null
          }
        >
          <TabPane tab="Tài khoản Loyalty" key="account">
            {account ? (
              <Row gutter={16}>
                <Col span={24}>
                  <Card>
                    <Descriptions
                      title="Thông tin tài khoản"
                      bordered
                      column={2}
                    >
                      <Descriptions.Item label="ID tài khoản">
                        {account.accountId}
                      </Descriptions.Item>
                      <Descriptions.Item label="User ID">
                        {account.userId}
                      </Descriptions.Item>
                      <Descriptions.Item label="Hạng">
                        <Tag color="gold">{account.tier || "BRONZE"}</Tag>
                      </Descriptions.Item>
                      <Descriptions.Item label="Ngày tạo">
                        {dayjs(account.createdAt).format("DD/MM/YYYY HH:mm")}
                      </Descriptions.Item>
                    </Descriptions>
                  </Card>
                </Col>
                <Col span={24} style={{ marginTop: 16 }}>
                  <Row gutter={16}>
                    <Col span={8}>
                      <Card>
                        <Statistic
                          title="Điểm hiện tại"
                          value={account.currentPoints || 0}
                          prefix={<DollarOutlined />}
                          valueStyle={{ color: "#3f8600" }}
                        />
                      </Card>
                    </Col>
                    <Col span={8}>
                      <Card>
                        <Statistic
                          title="Tổng điểm đã tích"
                          value={account.totalPointsEarned || 0}
                          prefix={<DollarOutlined />}
                          valueStyle={{ color: "#1890ff" }}
                        />
                      </Card>
                    </Col>
                    <Col span={8}>
                      <Card>
                        <Statistic
                          title="Tổng điểm đã đổi"
                          value={account.totalPointsRedeemed || 0}
                          prefix={<DollarOutlined />}
                          valueStyle={{ color: "#cf1322" }}
                        />
                      </Card>
                    </Col>
                  </Row>
                </Col>
              </Row>
            ) : (
              <Card>
                <p>
                  Chưa có tài khoản loyalty. Tài khoản sẽ được tạo tự động khi
                  bạn đặt hàng đầu tiên.
                </p>
              </Card>
            )}
          </TabPane>
          <TabPane tab="Lịch sử giao dịch điểm" key="transactions">
            <Table
              columns={transactionColumns}
              dataSource={transactions}
              rowKey="transactionId"
              loading={loading}
              pagination={{
                pageSize: 10,
                showSizeChanger: true,
                showTotal: (total) => `Tổng ${total} giao dịch`,
              }}
            />
          </TabPane>
          <TabPane tab="Lịch sử sử dụng voucher" key="voucher-usage">
            <Table
              columns={voucherUsageColumns}
              dataSource={voucherUsage}
              rowKey="usageId"
              loading={loading}
              pagination={{
                pageSize: 10,
                showSizeChanger: true,
                showTotal: (total) => `Tổng ${total} voucher đã sử dụng`,
              }}
            />
          </TabPane>
        </Tabs>
      </Card>

      <Modal
        title="Tích điểm thủ công"
        open={earnPointsModalVisible}
        onCancel={() => {
          setEarnPointsModalVisible(false);
          form.resetFields();
        }}
        onOk={() => form.submit()}
        okText="Tích điểm"
        cancelText="Hủy"
      >
        <Form form={form} layout="vertical" onFinish={handleEarnPoints}>
          <Form.Item
            name="points"
            label="Số điểm"
            rules={[{ required: true, message: "Vui lòng nhập số điểm" }]}
          >
            <InputNumber
              min={1}
              style={{ width: "100%" }}
              placeholder="Nhập số điểm"
            />
          </Form.Item>
          <Form.Item name="description" label="Mô tả">
            <TextArea rows={3} placeholder="Mô tả về việc tích điểm" />
          </Form.Item>
          <Form.Item name="orderId" label="Mã đơn hàng (tùy chọn)">
            <Input placeholder="Nhập mã đơn hàng nếu có" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default LoyaltyManagement;
