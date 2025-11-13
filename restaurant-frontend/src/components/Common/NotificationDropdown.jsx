import React, { useState, useEffect } from 'react';
import { Badge, Dropdown, List, Empty, Button, Tag, Spin } from 'antd';
import { BellOutlined, CheckOutlined, DeleteOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import notificationAPI from '../../services/notificationService';
import { useAuth } from '../../context/AuthContext';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/vi';

dayjs.extend(relativeTime);
dayjs.locale('vi');

const NotificationDropdown = () => {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [loading, setLoading] = useState(false);
    const [dropdownVisible, setDropdownVisible] = useState(false);

    const userId = user?.userId || user?.id || 'admin'; // Fallback to 'admin' for now

    useEffect(() => {
        if (userId) {
            loadUnreadCount();
            if (dropdownVisible) {
                loadNotifications();
            }
        }
    }, [userId, dropdownVisible]);

    // Poll for unread count every 30 seconds
    useEffect(() => {
        if (!userId) return;

        const interval = setInterval(() => {
            loadUnreadCount();
        }, 30000); // 30 seconds

        return () => clearInterval(interval);
    }, [userId]);

    const loadNotifications = async () => {
        setLoading(true);
        try {
            const data = await notificationAPI.getUnreadNotifications(userId, { page: 0, size: 10 });
            if (data?.notifications) {
                setNotifications(data.notifications);
            } else if (Array.isArray(data)) {
                setNotifications(data);
            }
        } catch (error) {
            console.error('Error loading notifications:', error);
        } finally {
            setLoading(false);
        }
    };

    const loadUnreadCount = async () => {
        try {
            const data = await notificationAPI.getUnreadCount(userId);
            if (data?.unreadCount !== undefined) {
                setUnreadCount(data.unreadCount);
            } else if (typeof data === 'number') {
                setUnreadCount(data);
            }
        } catch (error) {
            console.error('Error loading unread count:', error);
        }
    };

    const handleMarkAsRead = async (notificationId, e) => {
        e.stopPropagation();
        try {
            await notificationAPI.markAsRead(notificationId);
            setNotifications(prev =>
                prev.map(n => n.notificationId === notificationId
                    ? { ...n, status: 'READ' }
                    : n
                )
            );
            setUnreadCount(prev => Math.max(0, prev - 1));
        } catch (error) {
            console.error('Error marking notification as read:', error);
        }
    };

    const handleArchive = async (notificationId, e) => {
        e.stopPropagation();
        try {
            await notificationAPI.archive(notificationId);
            setNotifications(prev => prev.filter(n => n.notificationId !== notificationId));
            setUnreadCount(prev => Math.max(0, prev - 1));
        } catch (error) {
            console.error('Error archiving notification:', error);
        }
    };

    const getSeverityColor = (severity) => {
        const colorMap = {
            'CRITICAL': 'red',
            'HIGH': 'orange',
            'MEDIUM': 'blue',
            'LOW': 'default'
        };
        return colorMap[severity] || 'default';
    };

    const getTypeLabel = (type) => {
        const typeMap = {
            'INVENTORY_ALERT': 'Cảnh báo kho',
            'MENU_ALERT': 'Cảnh báo menu',
            'ORDER_UPDATE': 'Cập nhật đơn hàng',
            'SYSTEM': 'Hệ thống'
        };
        return typeMap[type] || type;
    };

    const notificationItems = [
        {
            key: 'notifications',
            label: (
                <div style={{ maxHeight: '400px', overflowY: 'auto', width: '350px' }}>
                    <div style={{
                        padding: '12px 16px',
                        borderBottom: '1px solid #f0f0f0',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center'
                    }}>
                        <strong>Thông báo</strong>
                        {unreadCount > 0 && (
                            <Tag color="red">{unreadCount} chưa đọc</Tag>
                        )}
                    </div>
                    {loading ? (
                        <div style={{ padding: '20px', textAlign: 'center' }}>
                            <Spin />
                        </div>
                    ) : notifications.length === 0 ? (
                        <Empty
                            description="Không có thông báo mới"
                            style={{ padding: '20px' }}
                        />
                    ) : (
                        <List
                            dataSource={notifications}
                            renderItem={(notification) => (
                                <List.Item
                                    style={{
                                        padding: '12px 16px',
                                        borderBottom: '1px solid #f0f0f0',
                                        cursor: 'pointer',
                                        backgroundColor: notification.status === 'UNREAD' ? '#f0f9ff' : 'white'
                                    }}
                                    onClick={() => handleMarkAsRead(notification.notificationId, { stopPropagation: () => { } })}
                                >
                                    <List.Item.Meta
                                        title={
                                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
                                                <span style={{ fontWeight: notification.status === 'UNREAD' ? 'bold' : 'normal' }}>
                                                    {notification.title}
                                                </span>
                                                <div style={{ display: 'flex', gap: '4px' }}>
                                                    {notification.severity && (
                                                        <Tag color={getSeverityColor(notification.severity)} size="small">
                                                            {notification.severity}
                                                        </Tag>
                                                    )}
                                                    <Tag size="small">{getTypeLabel(notification.type)}</Tag>
                                                </div>
                                            </div>
                                        }
                                        description={
                                            <div>
                                                <div style={{ marginBottom: '4px' }}>{notification.message}</div>
                                                <div style={{
                                                    fontSize: '12px',
                                                    color: '#8c8c8c',
                                                    display: 'flex',
                                                    justifyContent: 'space-between',
                                                    alignItems: 'center'
                                                }}>
                                                    <span>{dayjs(notification.createdAt).fromNow()}</span>
                                                    <div>
                                                        {notification.status === 'UNREAD' && (
                                                            <Button
                                                                type="text"
                                                                size="small"
                                                                icon={<CheckOutlined />}
                                                                onClick={(e) => handleMarkAsRead(notification.notificationId, e)}
                                                                style={{ marginRight: '4px' }}
                                                            />
                                                        )}
                                                        <Button
                                                            type="text"
                                                            size="small"
                                                            icon={<DeleteOutlined />}
                                                            onClick={(e) => handleArchive(notification.notificationId, e)}
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
                    )}
                    {notifications.length > 0 && (
                        <div style={{
                            padding: '12px 16px',
                            borderTop: '1px solid #f0f0f0',
                            textAlign: 'center'
                        }}>
                            <Button
                                type="link"
                                onClick={() => {
                                    setDropdownVisible(false);
                                    navigate('/dashboard/notifications');
                                }}
                            >
                                Xem tất cả thông báo
                            </Button>
                        </div>
                    )}
                </div>
            )
        }
    ];

    return (
        <Dropdown
            menu={{ items: notificationItems }}
            trigger={['click']}
            placement="bottomRight"
            open={dropdownVisible}
            onOpenChange={(visible) => {
                setDropdownVisible(visible);
                if (visible) {
                    loadNotifications();
                }
            }}
        >
            <Badge count={unreadCount} size="small" offset={[-5, 5]}>
                <BellOutlined
                    className="notification-icon"
                    style={{
                        fontSize: '20px',
                        color: '#595959',
                        cursor: 'pointer',
                        padding: '8px',
                        display: 'block'
                    }}
                />
            </Badge>
        </Dropdown>
    );
};

export default NotificationDropdown;

