import React, { useState, useEffect, useCallback } from "react";
import { Badge, Dropdown, List, Empty, Button, Tag, Spin } from "antd";
import { BellOutlined, CheckOutlined, DeleteOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import notificationAPI from "../../services/notificationService";
import { useAuth } from "../../context/AuthContext";
import { useWebSocketNotifications } from "../../hooks/useWebSocketNotifications";
import NotificationDetailModal from "./NotificationDetailModal";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import "dayjs/locale/vi";
import {
  dispatchDataRefresh,
  DATA_REFRESH_EVENTS,
} from "../../utils/dataRefreshEvents";

dayjs.extend(relativeTime);
dayjs.locale("vi");

const NotificationDropdown = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [dropdownVisible, setDropdownVisible] = useState(false);
  const [selectedNotification, setSelectedNotification] = useState(null);
  const [modalVisible, setModalVisible] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [totalElements, setTotalElements] = useState(0);

  const userId = user?.userId || user?.id || "admin";

  const handleNewNotification = useCallback((notification) => {
    setNotifications((prev) => {
      const exists = prev.some(
        (n) => n.notificationId === notification.notificationId
      );
      if (exists) {
        return prev;
      }
      return [notification, ...prev].slice(0, 10);
    });
    if (notification.status === "UNREAD") {
      setUnreadCount((prev) => prev + 1);
    }

    // Dispatch global refresh event based on notification type to update other components
    if (notification.type === "ORDER_UPDATE") {
      dispatchDataRefresh(DATA_REFRESH_EVENTS.ORDER_UPDATED, {
        orderId: notification.referenceId
      });
    } else if (notification.type === "MENU_ALERT") {
      dispatchDataRefresh(DATA_REFRESH_EVENTS.MENU_ITEM_UPDATED, {});
    }
  }, []);

  const handleUnreadCountUpdate = useCallback((count) => {
    setUnreadCount(count);
  }, []);

  useWebSocketNotifications(handleNewNotification, handleUnreadCountUpdate);

  useEffect(() => {
    if (userId) {
      loadUnreadCount();
      if (dropdownVisible) {
        // Reset pagination when dropdown opens
        setPage(0);
        setHasMore(true);
        setNotifications([]);
        loadNotifications(0, true);
      }
    }
  }, [userId, dropdownVisible]);

  const loadNotifications = async (pageNum = 0, reset = false) => {
    if (reset) {
      setLoading(true);
    } else {
      setLoadingMore(true);
    }

    try {
      const data = await notificationAPI.getNotifications({
        userId: userId,
        page: pageNum,
        size: 10,
      });

      let notificationsList = [];
      let total = 0;

      if (data) {
        // Handle paginated response
        if (data.content && Array.isArray(data.content)) {
          notificationsList = data.content;
          total = data.totalElements || data.total || 0;
        } else if (data.items && Array.isArray(data.items)) {
          notificationsList = data.items;
          total = data.totalElements || data.total || 0;
        } else if (data.notifications && Array.isArray(data.notifications)) {
          notificationsList = data.notifications;
          total = data.totalElements || data.total || notificationsList.length;
        } else if (Array.isArray(data)) {
          notificationsList = data;
          total = data.length;
        } else if (data && typeof data === "object") {
          const keys = Object.keys(data);
          for (const key of keys) {
            if (Array.isArray(data[key])) {
              notificationsList = data[key];
              total =
                data.totalElements || data.total || notificationsList.length;
              break;
            }
          }
        }
      }

      if (reset) {
        setNotifications(notificationsList);
      } else {
        setNotifications((prev) => [...prev, ...notificationsList]);
      }

      setTotalElements(total);
      setHasMore(notificationsList.length === 10 && (pageNum + 1) * 10 < total);
      setPage(pageNum);
    } catch (error) {
      console.error("Error loading notifications:", error);
      if (error.response?.status !== 401 && error.response?.status !== 403) {
        console.warn("Failed to load notifications:", error.message);
      }
      setHasMore(false);
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  };

  const loadMoreNotifications = () => {
    if (!loadingMore && hasMore) {
      loadNotifications(page + 1, false);
    }
  };

  const loadUnreadCount = async () => {
    try {
      const data = await notificationAPI.getUnreadCount(userId);
      let count = 0;
      if (data?.unreadCount !== undefined) {
        count = data.unreadCount;
      } else if (typeof data === "number") {
        count = data;
      } else if (data?.count !== undefined) {
        count = data.count;
      }
      setUnreadCount(count);
    } catch (error) {
      console.error("Error loading unread count:", error);
      setUnreadCount(0);
    }
  };

  const handleMarkAsRead = async (notificationId, e) => {
    if (e) {
      e.stopPropagation();
    }
    try {
      await notificationAPI.markAsRead(notificationId);
      setNotifications((prev) =>
        prev.map((n) =>
          n.notificationId === notificationId ? { ...n, status: "READ" } : n
        )
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (error) {
      console.error("Error marking notification as read:", error);
    }
  };

  const handleArchive = async (notificationId, e) => {
    if (e) {
      e.stopPropagation();
    }
    try {
      await notificationAPI.archive(notificationId);
      setNotifications((prev) =>
        prev.filter((n) => n.notificationId !== notificationId)
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (error) {
      console.error("Error archiving notification:", error);
    }
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

  const notificationItems = [
    {
      key: "notifications",
      label: (
        <div
          style={{ maxHeight: "400px", overflowY: "auto", width: "350px" }}
          onScroll={(e) => {
            const { scrollTop, scrollHeight, clientHeight } = e.target;
            // Load more when scrolled to bottom (with 50px threshold)
            if (
              scrollHeight - scrollTop - clientHeight < 50 &&
              hasMore &&
              !loadingMore
            ) {
              loadMoreNotifications();
            }
          }}
        >
          <div
            style={{
              padding: "12px 16px",
              borderBottom: "1px solid #f0f0f0",
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
            }}
          >
            <strong>Thông báo</strong>
            {unreadCount > 0 && <Tag color="red">{unreadCount} chưa đọc</Tag>}
          </div>
          {loading ? (
            <div style={{ padding: "20px", textAlign: "center" }}>
              <Spin />
            </div>
          ) : notifications.length === 0 ? (
            <Empty
              description="Không có thông báo mới"
              style={{ padding: "20px" }}
            />
          ) : (
            <>
              <List
                dataSource={notifications}
                renderItem={(notification) => (
                  <List.Item
                    style={{
                      padding: "12px 16px",
                      borderBottom: "1px solid #f0f0f0",
                      cursor: "pointer",
                      backgroundColor:
                        notification.status === "UNREAD" ? "#f0f9ff" : "white",
                    }}
                    onClick={(e) => {
                      e.stopPropagation();
                      setSelectedNotification(notification);
                      setModalVisible(true);
                      setDropdownVisible(false);
                    }}
                  >
                    <List.Item.Meta
                      title={
                        <div
                          style={{
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "start",
                          }}
                        >
                          <span
                            style={{
                              fontWeight:
                                notification.status === "UNREAD"
                                  ? "bold"
                                  : "normal",
                            }}
                          >
                            {notification.title}
                          </span>
                          <div style={{ display: "flex", gap: "4px" }}>
                            {notification.severity && (
                              <Tag
                                color={getSeverityColor(notification.severity)}
                                size="small"
                              >
                                {notification.severity}
                              </Tag>
                            )}
                            <Tag size="small">
                              {getTypeLabel(notification.type)}
                            </Tag>
                          </div>
                        </div>
                      }
                      description={
                        <div>
                          <div style={{ marginBottom: "4px" }}>
                            {notification.message}
                          </div>
                          <div
                            style={{
                              fontSize: "12px",
                              color: "#8c8c8c",
                              display: "flex",
                              justifyContent: "space-between",
                              alignItems: "center",
                            }}
                          >
                            <span>
                              {dayjs(notification.createdAt).fromNow()}
                            </span>
                            <div onClick={(e) => e.stopPropagation()}>
                              {notification.status === "UNREAD" && (
                                <Button
                                  type="text"
                                  size="small"
                                  icon={<CheckOutlined />}
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    handleMarkAsRead(
                                      notification.notificationId,
                                      e
                                    );
                                  }}
                                  style={{ marginRight: "4px" }}
                                />
                              )}
                              <Button
                                type="text"
                                size="small"
                                icon={<DeleteOutlined />}
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleArchive(notification.notificationId, e);
                                }}
                                danger
                              />
                            </div>
                          </div>
                        </div>
                      }
                    />
                  </List.Item>
                )}
              />
              {loadingMore && (
                <div style={{ padding: "12px", textAlign: "center" }}>
                  <Spin size="small" />
                  <span style={{ marginLeft: 8, color: "#8c8c8c" }}>
                    Đang tải thêm...
                  </span>
                </div>
              )}
              {!hasMore && notifications.length > 0 && (
                <div
                  style={{
                    padding: "12px",
                    textAlign: "center",
                    color: "#8c8c8c",
                    fontSize: "12px",
                  }}
                >
                  Đã hiển thị tất cả thông báo
                </div>
              )}
            </>
          )}
          {notifications.length > 0 && (
            <div
              style={{
                padding: "12px 16px",
                borderTop: "1px solid #f0f0f0",
                textAlign: "center",
              }}
            >
              <Button
                type="link"
                onClick={() => {
                  setDropdownVisible(false);
                  navigate("/dashboard/notifications");
                }}
              >
                Xem tất cả thông báo
              </Button>
            </div>
          )}
        </div>
      ),
    },
  ];

  return (
    <>
      <Dropdown
        menu={{ items: notificationItems }}
        trigger={["click"]}
        placement="bottomRight"
        open={dropdownVisible}
        onOpenChange={(visible) => {
          setDropdownVisible(visible);
          if (visible) {
            // Reset and load first page
            setPage(0);
            setHasMore(true);
            setNotifications([]);
            loadNotifications(0, true);
          }
        }}
      >
        <Badge count={unreadCount} size="small" offset={[-5, 5]}>
          <BellOutlined
            className="notification-icon"
            style={{
              fontSize: "20px",
              color: "#595959",
              cursor: "pointer",
              padding: "8px",
              display: "block",
            }}
          />
        </Badge>
      </Dropdown>
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
    </>
  );
};

export default NotificationDropdown;
