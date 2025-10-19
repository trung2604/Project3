import React, { useState, useEffect } from 'react';
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
    InputNumber,
    App,
    Row,
    Col,
    Statistic,
    Alert,
    Timeline
} from 'antd';
import {
    SearchOutlined,
    ReloadOutlined,
    WarningOutlined,
    ClockCircleOutlined,
    ExclamationCircleOutlined,
    CheckCircleOutlined
} from '@ant-design/icons';
import { inventoryService } from '../services/inventoryService';
import { ALERT_TYPES, TRANSACTION_TYPES } from '../constants.js';

const { Option } = Select;
const { Search } = Input;

const InventoryAlerts = () => {
    const { message } = App.useApp();
    const [alerts, setAlerts] = useState([]);
    const [loading, setLoading] = useState(false);
    const [pagination, setPagination] = useState({
        current: 1,
        pageSize: 10,
        total: 0
    });
    const [filters, setFilters] = useState({
        alertType: '',
        severity: '',
        search: ''
    });

    // Load alerts data
    const loadAlerts = async (page = 1, size = 10) => {
        setLoading(true);
        try {
            const params = {
                page: page - 1,
                size,
                ...filters
            };

            const response = await inventoryService.getAlerts(params);
            setAlerts(response.data.content || []);
            setPagination(prev => ({
                ...prev,
                current: page,
                total: response.data.totalElements || 0
            }));
        } catch (error) {
            message.error('Lỗi khi tải dữ liệu cảnh báo');
            console.error('Error loading alerts:', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadAlerts();
    }, [filters]);

    // Handle table changes
    const handleTableChange = (paginationInfo) => {
        loadAlerts(paginationInfo.current, paginationInfo.pageSize);
    };

    // Handle search and filters
    const handleSearch = (value) => {
        setFilters(prev => ({ ...prev, search: value }));
    };

    const handleFilterChange = (key, value) => {
        setFilters(prev => ({ ...prev, [key]: value }));
    };

    // Get alert severity color
    const getSeverityColor = (severity) => {
        switch (severity) {
            case 'CRITICAL': return 'red';
            case 'HIGH': return 'orange';
            case 'MEDIUM': return 'yellow';
            case 'LOW': return 'blue';
            default: return 'default';
        }
    };

    // Get alert type icon
    const getAlertIcon = (type) => {
        switch (type) {
            case 'LOW_STOCK': return <WarningOutlined />;
            case 'EXPIRY': return <ClockCircleOutlined />;
            case 'CRITICAL': return <ExclamationCircleOutlined />;
            default: return <CheckCircleOutlined />;
        }
    };

    // Table columns
    const columns = [
        {
            title: 'Loại cảnh báo',
            dataIndex: 'alertType',
            key: 'alertType',
            render: (type) => (
                <Space>
                    {getAlertIcon(type)}
                    <span>{type === 'LOW_STOCK' ? 'Tồn kho thấp' :
                        type === 'EXPIRY' ? 'Hết hạn' :
                            type === 'CRITICAL' ? 'Khẩn cấp' : type}</span>
                </Space>
            ),
        },
        {
            title: 'Nguyên liệu',
            dataIndex: 'ingredientName',
            key: 'ingredientName',
            render: (name, record) => (
                <div>
                    <div className="font-medium">{name}</div>
                    <div className="text-sm text-gray-500">{record.ingredientCategory}</div>
                </div>
            ),
        },
        {
            title: 'Mô tả',
            dataIndex: 'message',
            key: 'message',
        },
        {
            title: 'Mức độ',
            dataIndex: 'severity',
            key: 'severity',
            render: (severity) => (
                <Tag color={getSeverityColor(severity)}>
                    {severity}
                </Tag>
            ),
        },
        {
            title: 'Ngày tạo',
            dataIndex: 'createdAt',
            key: 'createdAt',
            render: (date) => new Date(date).toLocaleDateString('vi-VN'),
        },
        {
            title: 'Trạng thái',
            dataIndex: 'resolved',
            key: 'resolved',
            render: (resolved) => (
                <Tag color={resolved ? 'green' : 'red'}>
                    {resolved ? 'Đã xử lý' : 'Chưa xử lý'}
                </Tag>
            ),
        },
    ];

    // Mock statistics
    const stats = {
        totalAlerts: 15,
        criticalAlerts: 3,
        lowStockAlerts: 8,
        expiryAlerts: 4,
        resolvedAlerts: 7
    };

    return (
        <div className="page-content">
            <div className="page-header">
                <div className="flex justify-between items-center">
                    <div>
                        <h1>Cảnh báo tồn kho</h1>
                        <p>Theo dõi và quản lý các cảnh báo về tồn kho</p>
                    </div>
                    <Button
                        icon={<ReloadOutlined />}
                        onClick={() => loadAlerts()}
                    >
                        Làm mới
                    </Button>
                </div>
            </div>

            {/* Statistics Cards */}
            <div className="page-section">
                <Row gutter={[16, 16]}>
                    <Col xs={24} sm={6}>
                        <Card className="restaurant-card">
                            <Statistic
                                title="Tổng cảnh báo"
                                value={stats.totalAlerts}
                                prefix={<WarningOutlined />}
                                valueStyle={{ color: '#1890ff' }}
                            />
                        </Card>
                    </Col>
                    <Col xs={24} sm={6}>
                        <Card className="restaurant-card">
                            <Statistic
                                title="Khẩn cấp"
                                value={stats.criticalAlerts}
                                prefix={<ExclamationCircleOutlined />}
                                valueStyle={{ color: '#ff4d4f' }}
                            />
                        </Card>
                    </Col>
                    <Col xs={24} sm={6}>
                        <Card className="restaurant-card">
                            <Statistic
                                title="Tồn kho thấp"
                                value={stats.lowStockAlerts}
                                prefix={<WarningOutlined />}
                                valueStyle={{ color: '#faad14' }}
                            />
                        </Card>
                    </Col>
                    <Col xs={24} sm={6}>
                        <Card className="restaurant-card">
                            <Statistic
                                title="Hết hạn"
                                value={stats.expiryAlerts}
                                prefix={<ClockCircleOutlined />}
                                valueStyle={{ color: '#722ed1' }}
                            />
                        </Card>
                    </Col>
                </Row>
            </div>

            {/* Critical Alerts */}
            <div className="page-section">
                <Card title="Cảnh báo khẩn cấp" className="restaurant-card">
                    <div className="alert-container">
                        <Alert
                            message="Cà chua sắp hết hàng"
                            description="Tồn kho chỉ còn 2kg, cần nhập hàng ngay"
                            type="error"
                            showIcon
                            action={
                                <Button size="small" danger>
                                    Xử lý ngay
                                </Button>
                            }
                        />
                        <Alert
                            message="Rau xà lách hết hạn"
                            description="Hết hạn trong 1 ngày, cần kiểm tra và xử lý"
                            type="warning"
                            showIcon
                            action={
                                <Button size="small" type="primary">
                                    Kiểm tra
                                </Button>
                            }
                        />
                    </div>
                </Card>
            </div>

            {/* Filters */}
            <div className="page-section">
                <Card className="restaurant-card search-filter-section">
                    <Row gutter={[16, 16]} align="middle">
                        <Col xs={24} sm={8}>
                            <Search
                                placeholder="Tìm kiếm cảnh báo..."
                                onSearch={handleSearch}
                                enterButton={<SearchOutlined />}
                            />
                        </Col>
                        <Col xs={24} sm={6}>
                            <Select
                                placeholder="Loại cảnh báo"
                                style={{ width: '100%' }}
                                allowClear
                                onChange={(value) => handleFilterChange('alertType', value)}
                            >
                                <Option value="LOW_STOCK">Tồn kho thấp</Option>
                                <Option value="EXPIRY">Hết hạn</Option>
                                <Option value="CRITICAL">Khẩn cấp</Option>
                            </Select>
                        </Col>
                        <Col xs={24} sm={6}>
                            <Select
                                placeholder="Mức độ"
                                style={{ width: '100%' }}
                                allowClear
                                onChange={(value) => handleFilterChange('severity', value)}
                            >
                                <Option value="CRITICAL">Khẩn cấp</Option>
                                <Option value="HIGH">Cao</Option>
                                <Option value="MEDIUM">Trung bình</Option>
                                <Option value="LOW">Thấp</Option>
                            </Select>
                        </Col>
                        <Col xs={24} sm={4}>
                            <Button
                                icon={<ReloadOutlined />}
                                onClick={() => loadAlerts()}
                                className="w-full"
                            >
                                Làm mới
                            </Button>
                        </Col>
                    </Row>
                </Card>
            </div>

            {/* Table */}
            <div className="page-section">
                <Card className="restaurant-card">
                    <Table
                        columns={columns}
                        dataSource={alerts}
                        rowKey="id"
                        loading={loading}
                        pagination={{
                            ...pagination,
                            showSizeChanger: true,
                            showQuickJumper: true,
                            showTotal: (total, range) =>
                                `${range[0]}-${range[1]} của ${total} mục`,
                        }}
                        onChange={handleTableChange}
                    />
                </Card>
            </div>
        </div>
    );
};

export default InventoryAlerts;
