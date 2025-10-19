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
    DatePicker,
    Timeline
} from 'antd';
import {
    SearchOutlined,
    ReloadOutlined,
    ShoppingCartOutlined,
    MinusOutlined,
    SettingOutlined,
    EyeOutlined,
    DollarOutlined
} from '@ant-design/icons';
import { inventoryService } from '../services/inventoryService';
import { TRANSACTION_TYPES } from '../constants.js';

const { Option } = Select;
const { Search } = Input;
const { RangePicker } = DatePicker;

const InventoryTransactions = () => {
    const { message } = App.useApp();
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(false);
    const [pagination, setPagination] = useState({
        current: 1,
        pageSize: 10,
        total: 0
    });
    const [filters, setFilters] = useState({
        transactionType: '',
        ingredientId: '',
        dateRange: null,
        search: ''
    });

    // Load transactions data
    const loadTransactions = async (page = 1, size = 10) => {
        setLoading(true);
        try {
            const params = {
                page: page - 1,
                size,
                ...filters
            };

            const response = await inventoryService.getTransactions(params);
            setTransactions(response.data.content || []);
            setPagination(prev => ({
                ...prev,
                current: page,
                total: response.data.totalElements || 0
            }));
        } catch (error) {
            message.error('Lỗi khi tải dữ liệu giao dịch');
            console.error('Error loading transactions:', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadTransactions();
    }, [filters]);

    // Handle table changes
    const handleTableChange = (paginationInfo) => {
        loadTransactions(paginationInfo.current, paginationInfo.pageSize);
    };

    // Handle search and filters
    const handleSearch = (value) => {
        setFilters(prev => ({ ...prev, search: value }));
    };

    const handleFilterChange = (key, value) => {
        setFilters(prev => ({ ...prev, [key]: value }));
    };

    // Get transaction type color and icon
    const getTransactionInfo = (type) => {
        switch (type) {
            case 'STOCK_IN':
                return { color: 'green', icon: <ShoppingCartOutlined />, text: 'Nhập hàng' };
            case 'STOCK_OUT':
                return { color: 'red', icon: <MinusOutlined />, text: 'Xuất hàng' };
            case 'ADJUSTMENT':
                return { color: 'blue', icon: <SettingOutlined />, text: 'Điều chỉnh' };
            case 'STOCK_TAKE':
                return { color: 'orange', icon: <EyeOutlined />, text: 'Kiểm kê' };
            default:
                return { color: 'default', icon: null, text: type };
        }
    };

    // Table columns
    const columns = [
        {
            title: 'Loại giao dịch',
            dataIndex: 'transactionType',
            key: 'transactionType',
            render: (type) => {
                const info = getTransactionInfo(type);
                return (
                    <Space>
                        {info.icon}
                        <Tag color={info.color}>{info.text}</Tag>
                    </Space>
                );
            },
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
            title: 'Số lượng',
            dataIndex: 'quantity',
            key: 'quantity',
            render: (quantity, record) => (
                <div className="text-center">
                    <div className="font-medium">{quantity}</div>
                    <div className="text-sm text-gray-500">{record.unit}</div>
                </div>
            ),
        },
        {
            title: 'Tồn kho trước',
            dataIndex: 'previousStock',
            key: 'previousStock',
            render: (stock, record) => `${stock} ${record.unit}`,
        },
        {
            title: 'Tồn kho sau',
            dataIndex: 'newStock',
            key: 'newStock',
            render: (stock, record) => `${stock} ${record.unit}`,
        },
        {
            title: 'Lý do',
            dataIndex: 'reason',
            key: 'reason',
            render: (reason) => (
                <div className="max-w-xs">
                    <div className="truncate">{reason}</div>
                </div>
            ),
        },
        {
            title: 'Người thực hiện',
            dataIndex: 'performedBy',
            key: 'performedBy',
        },
        {
            title: 'Ngày thực hiện',
            dataIndex: 'transactionDate',
            key: 'transactionDate',
            render: (date) => new Date(date).toLocaleString('vi-VN'),
        },
    ];

    // Mock statistics
    const stats = {
        totalTransactions: 245,
        stockInCount: 89,
        stockOutCount: 134,
        adjustmentCount: 22,
        totalValue: 125000000
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-2xl font-bold text-gray-800 mb-2">Lịch sử giao dịch</h1>
                    <p className="text-gray-600">Theo dõi tất cả các giao dịch nhập/xuất kho</p>
                </div>
                <Button
                    icon={<ReloadOutlined />}
                    onClick={() => loadTransactions()}
                >
                    Làm mới
                </Button>
            </div>

            {/* Statistics Cards */}
            <Row gutter={[16, 16]}>
                <Col xs={24} sm={6}>
                    <Card className="restaurant-card">
                        <Statistic
                            title="Tổng giao dịch"
                            value={stats.totalTransactions}
                            prefix={<ShoppingCartOutlined />}
                            valueStyle={{ color: '#1890ff' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={6}>
                    <Card className="restaurant-card">
                        <Statistic
                            title="Nhập hàng"
                            value={stats.stockInCount}
                            prefix={<ShoppingCartOutlined />}
                            valueStyle={{ color: '#52c41a' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={6}>
                    <Card className="restaurant-card">
                        <Statistic
                            title="Xuất hàng"
                            value={stats.stockOutCount}
                            prefix={<MinusOutlined />}
                            valueStyle={{ color: '#ff4d4f' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={6}>
                    <Card className="restaurant-card">
                        <Statistic
                            title="Giá trị giao dịch"
                            value={stats.totalValue}
                            prefix={<DollarOutlined />}
                            valueStyle={{ color: '#f59e0b' }}
                            formatter={(value) => `${(value / 1000000).toFixed(0)}M VNĐ`}
                        />
                    </Card>
                </Col>
            </Row>

            {/* Recent Transactions Timeline */}
            <Card title="Giao dịch gần đây" className="restaurant-card">
                <Timeline>
                    <Timeline.Item color="green">
                        <div className="flex justify-between">
                            <div>
                                <div className="font-medium">Nhập hàng - Cà chua</div>
                                <div className="text-sm text-gray-500">+50kg | Tồn kho: 120kg</div>
                            </div>
                            <div className="text-sm text-gray-400">2 giờ trước</div>
                        </div>
                    </Timeline.Item>
                    <Timeline.Item color="red">
                        <div className="flex justify-between">
                            <div>
                                <div className="font-medium">Xuất hàng - Thịt bò</div>
                                <div className="text-sm text-gray-500">-15kg | Tồn kho: 25kg</div>
                            </div>
                            <div className="text-sm text-gray-400">4 giờ trước</div>
                        </div>
                    </Timeline.Item>
                    <Timeline.Item color="blue">
                        <div className="flex justify-between">
                            <div>
                                <div className="font-medium">Điều chỉnh - Hành tây</div>
                                <div className="text-sm text-gray-500">+5kg | Tồn kho: 30kg</div>
                            </div>
                            <div className="text-sm text-gray-400">6 giờ trước</div>
                        </div>
                    </Timeline.Item>
                </Timeline>
            </Card>

            {/* Filters */}
            <Card className="restaurant-card">
                <Row gutter={[16, 16]} align="middle">
                    <Col xs={24} sm={6}>
                        <Search
                            placeholder="Tìm kiếm giao dịch..."
                            onSearch={handleSearch}
                            enterButton={<SearchOutlined />}
                        />
                    </Col>
                    <Col xs={24} sm={6}>
                        <Select
                            placeholder="Loại giao dịch"
                            style={{ width: '100%' }}
                            allowClear
                            onChange={(value) => handleFilterChange('transactionType', value)}
                        >
                            <Option value="STOCK_IN">Nhập hàng</Option>
                            <Option value="STOCK_OUT">Xuất hàng</Option>
                            <Option value="ADJUSTMENT">Điều chỉnh</Option>
                            <Option value="STOCK_TAKE">Kiểm kê</Option>
                        </Select>
                    </Col>
                    <Col xs={24} sm={6}>
                        <RangePicker
                            style={{ width: '100%' }}
                            onChange={(dates) => handleFilterChange('dateRange', dates)}
                        />
                    </Col>
                    <Col xs={24} sm={6}>
                        <Button
                            icon={<ReloadOutlined />}
                            onClick={() => loadTransactions()}
                            className="w-full"
                        >
                            Làm mới
                        </Button>
                    </Col>
                </Row>
            </Card>

            {/* Table */}
            <Card className="restaurant-card">
                <Table
                    columns={columns}
                    dataSource={transactions}
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
                    scroll={{ x: 1000 }}
                />
            </Card>
        </div>
    );
};

export default InventoryTransactions;
