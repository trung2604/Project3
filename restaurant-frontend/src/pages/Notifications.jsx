import React, { useState, useEffect } from "react";
import {
  Card,
  List,
  Tag,
  Button,
  Empty,
  Select,
  Input,
  Pagination,
  Spin,
  Checkbox,
} from "antd";
import {
  CheckOutlined,
  DeleteOutlined,
  SearchOutlined,
} from "@ant-design/icons";
import notificationAPI from "../services/notificationService";
import { useAuth } from "../context/AuthContext";
import NotificationDetailModal from "../components/Common/NotificationDetailModal";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import "dayjs/locale/vi";

dayjs.extend(relativeTime);
dayjs.locale("vi");

const { Search } = Input;
const { Option } = Select;

const Notifications = () => {
  const { user } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [filters, setFilters] = useState({
    status: undefined,
    type: undefined,
    severity: undefined,
    search: undefined,
  });
  const [selectedIds, setSelectedIds] = useState([]);
  const [selectedNotification, setSelectedNotification] = useState(null);
  const [modalVisible, setModalVisible] = useState(false);

  const userId = user?.userId || user?.id || "admin";

  useEffect(() => {
    if (userId) {
      loadNotifications();
    }
  }, [userId, page, size, filters]);

  const loadNotifications = async () => {
    setLoading(true);
    try {
      const data = await notificationAPI.getNotifications({
        userId,
        ...filters,
        page,
        size,
      });

      if (data?.notifications) {
        setNotifications(data.notifications);
        setTotal(data.totalElements || 0);
      } else if (Array.isArray(data)) {
        setNotifications(data);
        setTotal(data.length);
      }
    } catch (error) {
      console.error("Error loading notifications:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleMarkAsRead = async (notificationId) => {
    try {
      await notificationAPI.markAsRead(notificationId);
      setNotifications((prev) =>
        prev.map((n) =>
          n.notificationId === notificationId ? { ...n, status: "READ" } : n
        )
      );
    } catch (error) {
      console.error("Error marking notification as read:", error);
    }
  };

  const handleArchive = async (notificationId) => {
    try {
      await notificationAPI.archive(notificationId);
      setNotifications((prev) =>
        prev.filter((n) => n.notificationId !== notificationId)
      );
      loadNotifications();
    } catch (error) {
      console.error("Error archiving notification:", error);
    }
  };

  const handleBulkMarkAsRead = async () => {
    if (selectedIds.length === 0) return;
    try {
      await notificationAPI.bulkMarkAsRead(selectedIds);
      setSelectedIds([]);
      loadNotifications();
    } catch (error) {
      console.error("Error bulk marking as read:", error);
    }
  };

  const handleBulkArchive = async () => {
    if (selectedIds.length === 0) return;
    try {
      await notificationAPI.bulkArchive(selectedIds);
      setSelectedIds([]);
      loadNotifications();
    } catch (error) {
      console.error("Error bulk archiving:", error);
    }
  };

  const handleFilterChange = (key, value) => {
    setFilters((prev) => ({ ...prev, [key]: value }));
    setPage(0);
  };

  const handleSearch = (value) => {
    setFilters((prev) => ({ ...prev, search: value }));
    setPage(0);
  };

  const getSeverityColor = (severity) => {
    const colorMap = {
      CRITICAL: "red",
      HIGH: "orange",
      MEDIUM: "blue",
      LOW: "default",
    };
    return colorMap[severity] || "default";
  };

  const getTypeLabel = (type) => {
    const typeMap = {
      INVENTORY_ALERT: "Cảnh báo kho",
      MENU_ALERT: "Cảnh báo menu",
      ORDER_UPDATE: "Cập nhật đơn hàng",
      LOYALTY_UPDATE: "Cập nhật điểm thưởng",
      SYSTEM: "Hệ thống",
    };
    return typeMap[type] || type;
  };

  const getStatusLabel = (status) => {
    const statusMap = {
      UNREAD: "Chưa đọc",
      READ: "Đã đọc",
      ARCHIVED: "Đã lưu",
    };
    return statusMap[status] || status;
  };

  return (
    <div style={{ padding: "24px" }}>
      <Card>
        <div style={{ marginBottom: "16px" }}>
          <h2 style={{ marginBottom: "16px" }}>Thông báo</h2>

          {/* Filters */}
          <div
            style={{
              display: "flex",
              gap: "12px",
              marginBottom: "16px",
              flexWrap: "wrap",
            }}
          >
            <Search
              placeholder="Tìm kiếm thông báo..."
              allowClear
              onSearch={handleSearch}
              style={{ width: "300px" }}
              enterButton={<SearchOutlined />}
            />
            <Select
              placeholder="Trạng thái"
              allowClear
              style={{ width: "150px" }}
              onChange={(value) => handleFilterChange("status", value)}
            >
              <Option value="UNREAD">Chưa đọc</Option>
              <Option value="READ">Đã đọc</Option>
              <Option value="ARCHIVED">Đã lưu</Option>
            </Select>
            <Select
              placeholder="Loại"
              allowClear
              style={{ width: "150px" }}
              onChange={(value) => handleFilterChange("type", value)}
            >
              <Option value="INVENTORY_ALERT">Cảnh báo kho</Option>
              <Option value="MENU_ALERT">Cảnh báo menu</Option>
              <Option value="ORDER_UPDATE">Cập nhật đơn hàng</Option>
              <Option value="LOYALTY_UPDATE">Cập nhật điểm thưởng</Option>
              <Option value="SYSTEM">Hệ thống</Option>
            </Select>
            <Select
              placeholder="Mức độ"
              allowClear
              style={{ width: "150px" }}
              onChange={(value) => handleFilterChange("severity", value)}
            >
              <Option value="CRITICAL">Khẩn cấp</Option>
              <Option value="HIGH">Cao</Option>
              <Option value="MEDIUM">Trung bình</Option>
              <Option value="LOW">Thấp</Option>
            </Select>
          </div>

          {/* Bulk Actions */}
          {selectedIds.length > 0 && (
            <div
              style={{
                marginBottom: "16px",
                padding: "12px",
                backgroundColor: "#f0f9ff",
                borderRadius: "8px",
                display: "flex",
                gap: "8px",
                alignItems: "center",
              }}
            >
              <span>Đã chọn {selectedIds.length} thông báo</span>
              <Button icon={<CheckOutlined />} onClick={handleBulkMarkAsRead}>
                Đánh dấu đã đọc
              </Button>
              <Button
                icon={<DeleteOutlined />}
                danger
                onClick={handleBulkArchive}
              >
                Lưu vào archive
              </Button>
              <Button type="link" onClick={() => setSelectedIds([])}>
                Bỏ chọn
              </Button>
            </div>
          )}
        </div>

        {loading ? (
          <div style={{ textAlign: "center", padding: "40px" }}>
            <Spin size="large" />
          </div>
        ) : notifications.length === 0 ? (
          <Empty description="Không có thông báo" />
        ) : (
          <>
            <List
              dataSource={notifications}
              renderItem={(notification) => (
                <List.Item
                  style={{
                    padding: "16px",
                    borderBottom: "1px solid #f0f0f0",
                    backgroundColor:
                      notification.status === "UNREAD" ? "#f0f9ff" : "white",
                    borderRadius: "8px",
                    marginBottom: "8px",
                    cursor: "pointer",
                  }}
                  onClick={(e) => {
                    // Only open modal if click is not on button or checkbox
                    const target = e.target;
                    if (
                      target.closest("button") ||
                      target.closest("input[type='checkbox']") ||
                      target.closest(".ant-checkbox")
                    ) {
                      return; // Let button/checkbox handle their own clicks
                    }
                    setSelectedNotification(notification);
                    setModalVisible(true);
                    // Auto mark as read when clicking to view details
                    if (notification.status === "UNREAD") {
                      handleMarkAsRead(notification.notificationId);
                    }
                  }}
                >
                  <div style={{ width: "100%", display: "flex", gap: "12px" }}>
                    <Checkbox
                      checked={selectedIds.includes(
                        notification.notificationId
                      )}
                      onChange={(e) => {
                        e.stopPropagation();
                        if (e.target.checked) {
                          setSelectedIds([
                            ...selectedIds,
                            notification.notificationId,
                          ]);
                        } else {
                          setSelectedIds(
                            selectedIds.filter(
                              (id) => id !== notification.notificationId
                            )
                          );
                        }
                      }}
                      onClick={(e) => e.stopPropagation()}
                    />
                    <div style={{ flex: 1 }}>
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "start",
                          marginBottom: "8px",
                        }}
                      >
                        <h4
                          style={{
                            margin: 0,
                            fontWeight:
                              notification.status === "UNREAD"
                                ? "bold"
                                : "normal",
                          }}
                        >
                          {notification.title}
                        </h4>
                        <div
                          style={{
                            display: "flex",
                            gap: "4px",
                            flexWrap: "wrap",
                          }}
                        >
                          {notification.severity && (
                            <Tag
                              color={getSeverityColor(notification.severity)}
                            >
                              {notification.severity}
                            </Tag>
                          )}
                          <Tag>{getTypeLabel(notification.type)}</Tag>
                          <Tag>{getStatusLabel(notification.status)}</Tag>
                        </div>
                      </div>
                      <p style={{ marginBottom: "8px", color: "#595959" }}>
                        {notification.message}
                      </p>
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "center",
                          fontSize: "12px",
                          color: "#8c8c8c",
                        }}
                      >
                        <span>
                          {dayjs(notification.createdAt).format(
                            "DD/MM/YYYY HH:mm"
                          )}{" "}
                          ({dayjs(notification.createdAt).fromNow()})
                        </span>
                        <div onClick={(e) => e.stopPropagation()}>
                          {notification.status === "UNREAD" && (
                            <Button
                              type="link"
                              size="small"
                              icon={<CheckOutlined />}
                              onClick={(e) => {
                                e.stopPropagation();
                                handleMarkAsRead(notification.notificationId);
                              }}
                              style={{ marginRight: "8px" }}
                            >
                              Đánh dấu đã đọc
                            </Button>
                          )}
                          <Button
                            type="link"
                            size="small"
                            icon={<DeleteOutlined />}
                            danger
                            onClick={(e) => {
                              e.stopPropagation();
                              handleArchive(notification.notificationId);
                            }}
                          >
                            Lưu vào archive
                          </Button>
                        </div>
                      </div>
                    </div>
                  </div>
                </List.Item>
              )}
            />
            <div style={{ marginTop: "16px", textAlign: "right" }}>
              <Pagination
                current={page + 1}
                pageSize={size}
                total={total}
                showSizeChanger
                showTotal={(total, range) =>
                  `${range[0]}-${range[1]} của ${total} thông báo`
                }
                onChange={(pageNum, pageSize) => {
                  setPage(pageNum - 1);
                  setSize(pageSize);
                }}
                pageSizeOptions={["10", "20", "50", "100"]}
                defaultPageSize={10}
              />
            </div>
          </>
        )}
      </Card>
      <NotificationDetailModal
        notification={selectedNotification}
        visible={modalVisible}
        onClose={() => {
          setModalVisible(false);
          setSelectedNotification(null);
        }}
        onMarkAsRead={(notificationId) => {
          handleMarkAsRead(notificationId);
        }}
        onArchive={(notificationId) => {
          handleArchive(notificationId);
        }}
        onUpdate={(updatedNotification) => {
          setNotifications((prev) =>
            prev.map((n) =>
              n.notificationId === updatedNotification.notificationId
                ? updatedNotification
                : n
            )
          );
        }}
      />
    </div>
  );
};

export default Notifications;
