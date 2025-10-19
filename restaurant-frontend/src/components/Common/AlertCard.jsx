import React from 'react';
import { Alert, Button, Space } from 'antd';
import {
    EyeOutlined,
    WarningOutlined,
    ClockCircleOutlined,
    ExclamationCircleOutlined,
    InfoCircleOutlined,
    ReloadOutlined
} from '@ant-design/icons';

const AlertCard = ({
    alerts = [],
    title = "Cảnh báo gần đây",
    maxDisplay = 3,
    onViewAll,
    onHandleAlert
}) => {
    const getAlertIcon = (type) => {
        switch (type) {
            case 'low_stock': return <WarningOutlined />;
            case 'expiry': return <ClockCircleOutlined />;
            case 'critical': return <ExclamationCircleOutlined />;
            default: return <InfoCircleOutlined />;
        }
    };

    const getAlertColor = (severity) => {
        switch (severity) {
            case 'error': return 'error';
            case 'warning': return 'warning';
            case 'success': return 'success';
            default: return 'info';
        }
    };

    return (
        <div>
            <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: '16px'
            }}>
                <h3 style={{ margin: 0, fontSize: '16px', fontWeight: '600' }}>
                    {title}
                </h3>
                <Button
                    type="text"
                    size="small"
                    icon={<ReloadOutlined />}
                    onClick={onViewAll}
                >
                    Làm mới
                </Button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {alerts.slice(0, maxDisplay).map((alert) => (
                    <Alert
                        key={alert.id}
                        message={alert.message}
                        description={
                            <div>
                                <div style={{ fontSize: '12px', marginTop: '4px' }}>
                                    {alert.ingredient && (
                                        <strong>{alert.ingredient}</strong>
                                    )}
                                    {alert.currentStock && (
                                        <span style={{ marginLeft: '8px' }}>
                                            Tồn kho: {alert.currentStock}
                                            {alert.minStock && ` (Min: ${alert.minStock})`}
                                        </span>
                                    )}
                                    {alert.expiryDate && (
                                        <span style={{ marginLeft: '8px' }}>
                                            Hết hạn: {alert.expiryDate}
                                        </span>
                                    )}
                                </div>
                                <div style={{
                                    fontSize: '10px',
                                    color: '#8c8c8c',
                                    marginTop: '2px'
                                }}>
                                    {alert.time}
                                </div>
                            </div>
                        }
                        type={getAlertColor(alert.severity)}
                        showIcon
                        icon={getAlertIcon(alert.type)}
                        action={
                            <Space>
                                <Button
                                    size="small"
                                    type="primary"
                                    ghost
                                    onClick={() => onHandleAlert?.(alert)}
                                >
                                    Xử lý
                                </Button>
                            </Space>
                        }
                        style={{ borderRadius: '8px' }}
                    />
                ))}

                {alerts.length > maxDisplay && (
                    <div style={{ textAlign: 'center', paddingTop: '8px' }}>
                        <Button type="link" size="small" onClick={onViewAll}>
                            Xem tất cả ({alerts.length} cảnh báo)
                        </Button>
                    </div>
                )}
            </div>
        </div>
    );
};

export default AlertCard;
