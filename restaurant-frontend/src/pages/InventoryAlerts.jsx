import React, { useState, useEffect } from 'react';
import {
    Card,
    Table,
    Button,
    Input,
    Select,
    Space,
    Tag,
    App,
    Row,
    Col,
    Statistic,
    Alert,
    Badge,
    Tabs,
    Empty
} from 'antd';
import {
    WarningOutlined,
    ExclamationCircleOutlined,
    ClockCircleOutlined,
    ReloadOutlined,
    SearchOutlined,
    BellOutlined
} from '@ant-design/icons';
import { inventoryService } from '../services/inventoryService';
import { PAGINATION } from '../constants.js';

const { Option } = Select;
const { Search } = Input;

const InventoryAlerts = () => {
    const { message } = App.useApp();
    const [alerts, setAlerts] = useState([]);
    const [lowStockAlerts, setLowStockAlerts] = useState([]);
    const [expiryAlerts, setExpiryAlerts] = useState([]);
    const [criticalAlerts, setCriticalAlerts] = useState([]);
    const [loading, setLoading] = useState(false);
    const [activeTab, setActiveTab] = useState('all');

    // Load all alerts
    const loadAlerts = async () => {
        setLoading(true);
        try {
            const response = await inventoryService.getAlerts();
            setAlerts(response.data || []);
        } catch (error) {
            message.error('Lỗi khi tải cảnh báo');
            console.error('Error loading alerts:', error);
        } finally {
            setLoading(false);
        }
    };

    // Load low stock alerts
    const loadLowStockAlerts = async () => {
        try {
            const response = await inventoryService.getLowStockAlerts();
            setLowStockAlerts(response.data || []);
        } catch (error) {
            console.error('Error loading low stock alerts:', error);
        }
    };

    // Load expiry alerts
    const loadExpiryAlerts = async () => {
        try {
            const response = await inventoryService.getExpiryAlerts();
            setExpiryAlerts(response.data || []);
        } catch (error) {
            console.error('Error loading expiry alerts:', error);
        }
    };

    // Load critical alerts
    const loadCriticalAlerts = async () => {
        try {
            const response = await inventoryService.getCriticalAlerts();
            setCriticalAlerts(response.data || []);
        } catch (error) {
            console.error('Error loading critical alerts:', error);
        }
    };

    // Load low stock ingredients
    const loadLowStockIngredients = async () => {
        try {
            const response = await inventoryService.getLowStockIngredients();
            // This will be used for the low stock tab
            return response.data || [];
        } catch (error) {
            console.error('Error loading low stock ingredients:', error);
            return [];
        }
    };

    useEffect(() => {
        loadAlerts();
        loadLowStockAlerts();
        loadExpiryAlerts();
        loadCriticalAlerts();
    }, []);

    // Get alert type color
    const getAlertTypeColor = (type) => {
        switch (type) {
            case 'LOW_STOCK': return 'orange';
            case 'EXPIRY': return 'red';
            case 'CRITICAL': return 'red';
            case 'OUT_OF_STOCK': return 'red';
            default: return 'blue';
        }
    };

    // Get alert type text
    const getAlertTypeText = (type) => {
        switch (type) {
            case 'LOW_STOCK': return 'Tồn kho thấp';
            case 'EXPIRY': return 'Sắp hết hạn';
            case 'CRITICAL': return 'Khẩn cấp';
            case 'OUT_OF_STOCK': return 'Hết hàng';
            default: return 'Cảnh báo';
        }
    };

    // Get alert icon
    const getAlertIcon = (type) => {
        switch (type) {
            case 'LOW_STOCK': return <WarningOutlined />;
            case 'EXPIRY': return <ClockCircleOutlined />;
            case 'CRITICAL': return <ExclamationCircleOutlined />;
            case 'OUT_OF_STOCK': return <ExclamationCircleOutlined />;
            default: return <BellOutlined />;
        }
    };

    // Table columns for alerts
    const alertColumns = [
        {
            title: 'Loại cảnh báo',
            dataIndex: 'alertType',
            key: 'alertType',
            render: (type) => (
                <Tag color={getAlertTypeColor(type)} icon={getAlertIcon(type)}>
                    {getAlertTypeText(type)}
                </Tag>
            ),
        },
        {
            title: 'Nguyên liệu',
            dataIndex: 'ingredientName',
            key: 'ingredientName',
            render: (text, record) => (
                <div>
                    <div style={{ fontWeight: '500', fontSize: '14px' }}>{text}</div>
                    <div style={{ fontSize: '12px', color: '#999', marginTop: '2px' }}>{record.ingredientId}</div>
                </div>
            ),
        },
        {
            title: 'Mô tả',
            dataIndex: 'message',
            key: 'message',
        },
        {
            title: 'Tồn kho hiện tại',
            dataIndex: 'currentStock',
            key: 'currentStock',
            render: (stock, record) => (
                <div>
                    <div style={{ fontWeight: '500', fontSize: '14px' }}>{stock} {record.unit}</div>
                    <div style={{ fontSize: '12px', color: '#999', marginTop: '2px' }}>
                        Min: {record.minStockLevel} | Max: {record.maxStockLevel}
                    </div>
                </div>
            ),
        },
        {
            title: 'Ngày tạo',
            dataIndex: 'createdAt',
            key: 'createdAt',
            render: (date) => new Date(date).toLocaleString('vi-VN'),
        },
        {
            title: 'Trạng thái',
            dataIndex: 'isActive',
            key: 'isActive',
            render: (active) => (
                <Tag color={active ? 'red' : 'green'}>
                    {active ? 'Đang hoạt động' : 'Đã xử lý'}
                </Tag>
            ),
        },
    ];

    // Table columns for low stock ingredients
    const lowStockColumns = [
        {
            title: 'Nguyên liệu',
            dataIndex: 'name',
            key: 'name',
            render: (text, record) => (
                <div>
                    <div style={{ fontWeight: '500', fontSize: '14px' }}>{text}</div>
                    <div style={{ fontSize: '12px', color: '#999', marginTop: '2px' }}>{record.description}</div>
                </div>
            ),
        },
        {
            title: 'Danh mục',
            dataIndex: 'category',
            key: 'category',
            render: (category) => <Tag color="blue">{category}</Tag>,
        },
        {
            title: 'Tồn kho hiện tại',
            dataIndex: 'currentStock',
            key: 'currentStock',
            render: (stock, record) => (
                <div>
                    <div className="font-medium text-red-600">{stock} {record.unit}</div>
                    <div className="text-sm text-gray-500">
                        Min: {record.minStockLevel} | Max: {record.maxStockLevel}
                    </div>
                </div>
            ),
        },
        {
            title: 'Mức cảnh báo',
            key: 'alertLevel',
            render: (_, record) => {
                const percentage = (record.currentStock / record.minStockLevel) * 100;
                if (percentage <= 50) {
                    return <Tag color="red">Khẩn cấp ({percentage.toFixed(0)}%)</Tag>;
                } else if (percentage <= 80) {
                    return <Tag color="orange">Cảnh báo ({percentage.toFixed(0)}%)</Tag>;
                } else {
                    return <Tag color="yellow">Sắp hết ({percentage.toFixed(0)}%)</Tag>;
                }
            },
        },
        {
            title: 'Nhà cung cấp',
            dataIndex: 'supplierName',
            key: 'supplierName',
            render: (supplier, record) => (
                <div>
                    <div className="font-medium">{supplier}</div>
                    <div className="text-sm text-gray-500">{record.supplierContact}</div>
                </div>
            ),
        },
    ];

    // Get statistics
    const getStatistics = () => {
        const totalAlerts = alerts.length;
        const activeAlerts = alerts.filter(alert => alert.isActive).length;
        const lowStockCount = lowStockAlerts.length;
        const expiryCount = expiryAlerts.length;
        const criticalCount = criticalAlerts.length;

        return {
            totalAlerts,
            activeAlerts,
            lowStockCount,
            expiryCount,
            criticalCount
        };
    };

    const stats = getStatistics();

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-2xl font-bold text-gray-800 mb-2">Cảnh báo tồn kho</h1>
                    <p className="text-gray-600">Theo dõi và quản lý các cảnh báo tồn kho</p>
                </div>
                <Button
                    icon={<ReloadOutlined />}
                    onClick={() => {
                        loadAlerts();
                        loadLowStockAlerts();
                        loadExpiryAlerts();
                        loadCriticalAlerts();
                    }}
                    className="restaurant-button"
                >
                    Làm mới
                </Button>
            </div>

            {/* Statistics Cards */}
            <Row gutter={[16, 16]}>
                <Col xs={24} sm={6}>
                    <Card className="restaurant-card">
                        <Statistic
                            title="Tổng cảnh báo"
                            value={stats.totalAlerts}
                            prefix={<BellOutlined />}
                            valueStyle={{ color: '#1890ff' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={6}>
                    <Card className="restaurant-card">
                        <Statistic
                            title="Cảnh báo đang hoạt động"
                            value={stats.activeAlerts}
                            prefix={<ExclamationCircleOutlined />}
                            valueStyle={{ color: '#ff4d4f' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={6}>
                    <Card className="restaurant-card">
                        <Statistic
                            title="Tồn kho thấp"
                            value={stats.lowStockCount}
                            prefix={<WarningOutlined />}
                            valueStyle={{ color: '#fa8c16' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={6}>
                    <Card className="restaurant-card">
                        <Statistic
                            title="Sắp hết hạn"
                            value={stats.expiryCount}
                            prefix={<ClockCircleOutlined />}
                            valueStyle={{ color: '#f5222d' }}
                        />
                    </Card>
                </Col>
            </Row>

            {/* Alerts Tabs */}
            <Card className="restaurant-card">
                <Tabs
                    activeKey={activeTab}
                    onChange={setActiveTab}
                    items={[
                        {
                            key: 'all',
                            label: (
                                <span>
                                    <BellOutlined />
                                    Tất cả cảnh báo
                                    {stats.totalAlerts > 0 && (
                                        <Badge count={stats.totalAlerts} style={{ marginLeft: 8 }} />
                                    )}
                                </span>
                            ),
                            children: (
                                <Table
                                    columns={alertColumns}
                                    dataSource={alerts}
                                    rowKey="alertId"
                                    loading={loading}
                                    pagination={{
                                        pageSize: PAGINATION.DEFAULT_PAGE_SIZE,
                                        showSizeChanger: true,
                                        showQuickJumper: true,
                                        showTotal: (total, range) =>
                                            `${range[0]}-${range[1]} của ${total} mục`,
                                    }}
                                    scroll={{ x: 1000 }}
                                />
                            )
                        },
                        {
                            key: 'low-stock',
                            label: (
                                <span>
                                    <WarningOutlined />
                                    Tồn kho thấp
                                    {stats.lowStockCount > 0 && (
                                        <Badge count={stats.lowStockCount} style={{ marginLeft: 8 }} />
                                    )}
                                </span>
                            ),
                            children: (
                                <Table
                                    columns={lowStockColumns}
                                    dataSource={lowStockAlerts}
                                    rowKey="ingredientId"
                                    loading={loading}
                                    pagination={{
                                        pageSize: PAGINATION.DEFAULT_PAGE_SIZE,
                                        showSizeChanger: true,
                                        showQuickJumper: true,
                                        showTotal: (total, range) =>
                                            `${range[0]}-${range[1]} của ${total} mục`,
                                    }}
                                    scroll={{ x: 1000 }}
                                />
                            )
                        },
                        {
                            key: 'expiry',
                            label: (
                                <span>
                                    <ClockCircleOutlined />
                                    Sắp hết hạn
                                    {stats.expiryCount > 0 && (
                                        <Badge count={stats.expiryCount} style={{ marginLeft: 8 }} />
                                    )}
                                </span>
                            ),
                            children: (
                                <Table
                                    columns={alertColumns}
                                    dataSource={expiryAlerts}
                                    rowKey="alertId"
                                    loading={loading}
                                    pagination={{
                                        pageSize: PAGINATION.DEFAULT_PAGE_SIZE,
                                        showSizeChanger: true,
                                        showQuickJumper: true,
                                        showTotal: (total, range) =>
                                            `${range[0]}-${range[1]} của ${total} mục`,
                                    }}
                                    scroll={{ x: 1000 }}
                                />
                            )
                        },
                        {
                            key: 'critical',
                            label: (
                                <span>
                                    <ExclamationCircleOutlined />
                                    Khẩn cấp
                                    {stats.criticalCount > 0 && (
                                        <Badge count={stats.criticalCount} style={{ marginLeft: 8 }} />
                                    )}
                                </span>
                            ),
                            children: (
                                <Table
                                    columns={alertColumns}
                                    dataSource={criticalAlerts}
                                    rowKey="alertId"
                                    loading={loading}
                                    pagination={{
                                        pageSize: PAGINATION.DEFAULT_PAGE_SIZE,
                                        showSizeChanger: true,
                                        showQuickJumper: true,
                                        showTotal: (total, range) =>
                                            `${range[0]}-${range[1]} của ${total} mục`,
                                    }}
                                    scroll={{ x: 1000 }}
                                />
                            )
                        }
                    ]}
                />
            </Card>
        </div>
    );
};

export default InventoryAlerts;