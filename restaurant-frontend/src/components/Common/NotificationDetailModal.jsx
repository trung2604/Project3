import React, { useEffect, useState } from "react";
import {
  Modal,
  Descriptions,
  Tag,
  Button,
  Space,
  Spin,
  App,
  Popconfirm,
} from "antd";
import {
  CheckOutlined,
  DeleteOutlined,
  EyeOutlined,
  ShoppingOutlined,
  InboxOutlined,
  GiftOutlined,
  PlayCircleOutlined,
  CheckCircleOutlined,
  TruckOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import notificationAPI from "../../services/notificationService";
import orderAPI from "../../services/orderService";
import { useAuth } from "../../context/AuthContext";
import { ORDER_STATUS } from "../../constants";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import "dayjs/locale/vi";

dayjs.extend(relativeTime);
dayjs.locale("vi");

const NotificationDetailModal = ({
  notification,
  visible,
  onClose,
  onMarkAsRead,
  onArchive,
  onUpdate,
}) => {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [loading, setLoading] = useState(false);
  const [updatingStatus, setUpdatingStatus] = useState(false);
  const [fullNotification, setFullNotification] = useState(notification);

  useEffect(() => {
    if (visible && notification?.notificationId) {
      setFullNotification(notification);
      loadFullNotification();
    }
  }, [visible, notification?.notificationId]);

  const loadFullNotification = async () => {
    if (!notification?.notificationId) return;

    setLoading(true);
    try {
      const data = await notificationAPI.getNotificationById(
        notification.notificationId
      );
      setFullNotification(data);
    } catch (error) {
      console.error("Error loading notification details:", error);
      // Fallback to provided notification if API fails
      setFullNotification(notification);
    } finally {
      setLoading(false);
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

  const getStatusLabel = (status) => {
    const statusMap = {
      UNREAD: "Chưa đọc",
      READ: "Đã đọc",
      ARCHIVED: "Đã lưu",
    };
    return statusMap[status] || status;
  };

  const parseMetadata = () => {
    if (!fullNotification?.metadata) return null;
    try {
      return JSON.parse(fullNotification.metadata);
    } catch (e) {
      return null;
    }
  };

  const handleMarkAsRead = async () => {
    if (!fullNotification?.notificationId) return;
    try {
      await notificationAPI.markAsRead(fullNotification.notificationId);
      const updated = { ...fullNotification, status: "READ" };
      setFullNotification(updated);
      if (onMarkAsRead) onMarkAsRead(fullNotification.notificationId);
      if (onUpdate) onUpdate(updated);
      message.success("Đã đánh dấu đã đọc");
    } catch (error) {
      console.error("Error marking as read:", error);
      message.error("Không thể đánh dấu đã đọc");
    }
  };

  const handleArchive = async () => {
    if (!fullNotification?.notificationId) return;
    try {
      await notificationAPI.archive(fullNotification.notificationId);
      if (onArchive) onArchive(fullNotification.notificationId);
      message.success("Đã lưu vào archive");
      onClose();
    } catch (error) {
      console.error("Error archiving:", error);
      message.error("Không thể lưu vào archive");
    }
  };

  const handleNavigateToRelated = () => {
    const metadata = parseMetadata();
    if (!metadata) return;

    if (fullNotification.type === "ORDER_UPDATE" && metadata.orderId) {
      navigate(`/dashboard/orders?orderId=${metadata.orderId}`);
      onClose();
      message.info(`Đang chuyển đến đơn hàng #${metadata.orderId}`);
    } else if (
      (fullNotification.type === "INVENTORY_ALERT" ||
        fullNotification.type === "MENU_ALERT") &&
      metadata.ingredientId
    ) {
      navigate("/dashboard/inventory/alerts");
      onClose();
      message.info("Đang chuyển đến cảnh báo kho");
    } else if (fullNotification.type === "LOYALTY_UPDATE" && metadata.orderId) {
      navigate("/dashboard/loyalty");
      onClose();
      message.info("Đang chuyển đến quản lý loyalty");
    }
  };

  const handleUpdateOrderStatus = async (orderId, newStatus, statusLabel) => {
    if (!orderId) {
      message.error("Không tìm thấy ID đơn hàng");
      return;
    }

    setUpdatingStatus(true);
    try {
      await orderAPI.updateOrderStatus(
        orderId,
        newStatus,
        user?.userId || user?.id,
        `Cập nhật từ thông báo: ${statusLabel}`
      );
      message.success(`Đã cập nhật trạng thái đơn hàng thành ${statusLabel}`);

      // Reload notification to get updated info
      if (fullNotification?.notificationId) {
        await loadFullNotification();
      }

      // Notify parent component
      if (onUpdate && fullNotification) {
        onUpdate({ ...fullNotification });
      }
    } catch (error) {
      console.error("Error updating order status:", error);
      message.error("Không thể cập nhật trạng thái đơn hàng");
    } finally {
      setUpdatingStatus(false);
    }
  };

  const getOrderStatusButtons = () => {
    const metadata = parseMetadata();
    if (
      !metadata ||
      !metadata.orderId ||
      fullNotification.type !== "ORDER_UPDATE"
    ) {
      return null;
    }

    const orderId = metadata.orderId;
    const currentStatus = metadata.orderStatus || "PENDING";
    const isKitchenStaff =
      user?.role === "KITCHEN_STAFF" || user?.role === "STAFF";

    if (!isKitchenStaff) {
      return (
        <Button
          type="primary"
          icon={<ShoppingOutlined />}
          onClick={handleNavigateToRelated}
        >
          Xem đơn hàng
        </Button>
      );
    }

    // Kitchen staff can update order status
    const buttons = [];

    // Can start cooking if status is PENDING
    if (currentStatus === ORDER_STATUS.PENDING) {
      buttons.push(
        <Popconfirm
          key="start-cooking"
          title="Bắt đầu nấu đơn hàng này?"
          onConfirm={() =>
            handleUpdateOrderStatus(orderId, ORDER_STATUS.COOKING, "Đang nấu")
          }
          okText="Xác nhận"
          cancelText="Hủy"
        >
          <Button
            type="primary"
            icon={<PlayCircleOutlined />}
            loading={updatingStatus}
          >
            Bắt đầu nấu
          </Button>
        </Popconfirm>
      );
    }

    // Can mark ready if status is COOKING
    if (currentStatus === ORDER_STATUS.COOKING) {
      buttons.push(
        <Popconfirm
          key="mark-ready"
          title="Đánh dấu đơn hàng đã sẵn sàng?"
          onConfirm={() =>
            handleUpdateOrderStatus(orderId, ORDER_STATUS.READY, "Sẵn sàng")
          }
          okText="Xác nhận"
          cancelText="Hủy"
        >
          <Button
            type="primary"
            icon={<CheckCircleOutlined />}
            loading={updatingStatus}
          >
            Đánh dấu sẵn sàng
          </Button>
        </Popconfirm>
      );
    }

    // Can start delivering if status is READY
    if (currentStatus === ORDER_STATUS.READY) {
      buttons.push(
        <Popconfirm
          key="start-delivering"
          title="Bắt đầu giao đơn hàng này?"
          onConfirm={() =>
            handleUpdateOrderStatus(
              orderId,
              ORDER_STATUS.DELIVERING,
              "Đang giao"
            )
          }
          okText="Xác nhận"
          cancelText="Hủy"
        >
          <Button
            type="primary"
            icon={<TruckOutlined />}
            loading={updatingStatus}
          >
            Bắt đầu giao
          </Button>
        </Popconfirm>
      );
    }

    // Can complete if status is DELIVERING
    if (currentStatus === ORDER_STATUS.DELIVERING) {
      buttons.push(
        <Popconfirm
          key="complete"
          title="Hoàn thành đơn hàng này?"
          onConfirm={() =>
            handleUpdateOrderStatus(
              orderId,
              ORDER_STATUS.COMPLETED,
              "Hoàn thành"
            )
          }
          okText="Xác nhận"
          cancelText="Hủy"
        >
          <Button
            type="primary"
            icon={<CheckCircleOutlined />}
            loading={updatingStatus}
          >
            Hoàn thành
          </Button>
        </Popconfirm>
      );
    }

    // If no action buttons, show view button
    if (buttons.length === 0) {
      buttons.push(
        <Button
          key="view"
          type="primary"
          icon={<ShoppingOutlined />}
          onClick={handleNavigateToRelated}
        >
          Xem đơn hàng
        </Button>
      );
    }

    return buttons;
  };

  const getActionButton = () => {
    const metadata = parseMetadata();
    if (!metadata) return null;

    // For ORDER_UPDATE, show status update buttons for kitchen staff
    if (fullNotification.type === "ORDER_UPDATE" && metadata.orderId) {
      return getOrderStatusButtons();
    } else if (
      (fullNotification.type === "INVENTORY_ALERT" ||
        fullNotification.type === "MENU_ALERT") &&
      metadata.ingredientId
    ) {
      return (
        <Button
          type="primary"
          icon={<InboxOutlined />}
          onClick={handleNavigateToRelated}
        >
          Xem cảnh báo kho
        </Button>
      );
    } else if (fullNotification.type === "LOYALTY_UPDATE") {
      return (
        <Button
          type="primary"
          icon={<GiftOutlined />}
          onClick={handleNavigateToRelated}
        >
          Xem điểm thưởng
        </Button>
      );
    }
    return null;
  };

  if (!fullNotification) return null;

  const metadata = parseMetadata();

  return (
    <Modal
      title={
        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
          <EyeOutlined />
          <span>Chi tiết thông báo</span>
        </div>
      }
      open={visible}
      onCancel={onClose}
      width={600}
      footer={[
        <Button key="close" onClick={onClose}>
          Đóng
        </Button>,
        fullNotification.status === "UNREAD" && (
          <Button
            key="read"
            icon={<CheckOutlined />}
            onClick={handleMarkAsRead}
          >
            Đánh dấu đã đọc
          </Button>
        ),
        <Button
          key="archive"
          icon={<DeleteOutlined />}
          danger
          onClick={handleArchive}
        >
          Lưu vào archive
        </Button>,
        ...(Array.isArray(getActionButton())
          ? getActionButton().map((btn, idx) =>
              React.isValidElement(btn) && btn.key
                ? btn
                : React.cloneElement(btn, { key: `action-${idx}` })
            )
          : getActionButton()
          ? [React.cloneElement(getActionButton(), { key: "action-0" })]
          : []),
      ].filter(Boolean)}
    >
      <Spin spinning={loading}>
        <Descriptions column={1} bordered>
          <Descriptions.Item label="Tiêu đề">
            <strong>{fullNotification.title}</strong>
          </Descriptions.Item>
          <Descriptions.Item label="Nội dung">
            <div style={{ whiteSpace: "pre-wrap" }}>
              {fullNotification.message}
            </div>
          </Descriptions.Item>
          <Descriptions.Item label="Loại">
            <Tag>{getTypeLabel(fullNotification.type)}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Mức độ">
            {fullNotification.severity && (
              <Tag color={getSeverityColor(fullNotification.severity)}>
                {fullNotification.severity}
              </Tag>
            )}
          </Descriptions.Item>
          <Descriptions.Item label="Trạng thái">
            <Tag
              color={
                fullNotification.status === "UNREAD"
                  ? "blue"
                  : fullNotification.status === "READ"
                  ? "green"
                  : "default"
              }
            >
              {getStatusLabel(fullNotification.status)}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Thời gian tạo">
            {dayjs(fullNotification.createdAt).format("DD/MM/YYYY HH:mm:ss")} (
            {dayjs(fullNotification.createdAt).fromNow()})
          </Descriptions.Item>
          {fullNotification.readAt && (
            <Descriptions.Item label="Thời gian đọc">
              {dayjs(fullNotification.readAt).format("DD/MM/YYYY HH:mm:ss")}
            </Descriptions.Item>
          )}
          {metadata && (
            <Descriptions.Item label="Thông tin bổ sung">
              <div style={{ maxHeight: "200px", overflowY: "auto" }}>
                <pre
                  style={{
                    margin: 0,
                    fontSize: "12px",
                    whiteSpace: "pre-wrap",
                    wordBreak: "break-word",
                  }}
                >
                  {JSON.stringify(metadata, null, 2)}
                </pre>
              </div>
            </Descriptions.Item>
          )}
        </Descriptions>
      </Spin>
    </Modal>
  );
};

export default NotificationDetailModal;
