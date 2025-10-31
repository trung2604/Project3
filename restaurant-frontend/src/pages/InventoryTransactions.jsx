import React, { useState, useEffect } from 'react';
import dayjs from 'dayjs';
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
    DatePicker,
    Form,
    Modal
} from 'antd';
import {
    ReloadOutlined,
    SearchOutlined,
    FilterOutlined,
    ShoppingCartOutlined,
    MinusOutlined,
    SettingOutlined,
    EyeOutlined,
    DownloadOutlined
} from '@ant-design/icons';
import { inventoryService } from '../services/inventoryService';
import { PAGINATION, TRANSACTION_TYPES } from '../constants.js';

const { Option } = Select;
const { Search } = Input;
const { RangePicker } = DatePicker;

const InventoryTransactions = () => {
    const { message } = App.useApp();
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(false);
    const [pagination, setPagination] = useState({
        current: 1,
        pageSize: PAGINATION.DEFAULT_PAGE_SIZE,
        total: 0
    });
    const [filters, setFilters] = useState({
        transactionType: '',
        ingredientId: '',
        dateRange: null,
        search: ''
    });
    const [modalVisible, setModalVisible] = useState(false);
    const [selectedTransaction, setSelectedTransaction] = useState(null);
    const [form] = Form.useForm();

    // Load transactions data
    const loadTransactions = async (page = 1, size = PAGINATION.DEFAULT_PAGE_SIZE) => {
        setLoading(true);
        try {
            const params = {
                // Backend currently returns a plain list; keep params minimal
                // Only include non-empty filters
                ...(filters.transactionType && filters.transactionType.trim() !== '' && {
                    transactionType: filters.transactionType
                }),
                ...(filters.ingredientId && filters.ingredientId.trim() !== '' && {
                    ingredientId: filters.ingredientId
                }),
                ...(filters.search && filters.search.trim() !== '' && {
                    search: filters.search
                })
            };

            // Add date range if provided
            if (filters.dateRange && filters.dateRange.length === 2) {
                params.fromDate = filters.dateRange[0].format('YYYY-MM-DD');
                params.toDate = filters.dateRange[1].format('YYYY-MM-DD');
            }

            const response = await inventoryService.getTransactions(params);
            const list = Array.isArray(response.data) ? response.data : (response.data?.transactions || []);
            console.log('Transactions API Response:', response);
            console.log('Transactions Data:', response.data);
            console.log('Transactions List:', list);
            setTransactions(list);
            setPagination(prev => ({
                ...prev,
                current: page,
                total: list.length
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
        setPagination(prev => ({
            ...prev,
            current: paginationInfo.current,
            pageSize: paginationInfo.pageSize
        }));
    };

    // Handle search and filters
    const handleSearch = (value) => {
        setFilters(prev => ({ ...prev, search: value }));
    };

    const handleFilterChange = (key, value) => {
        setFilters(prev => ({ ...prev, [key]: value }));
    };

    // Show transaction details
    const showTransactionDetails = (transaction) => {
        setSelectedTransaction(transaction);
        setModalVisible(true);
    };

    // Get transaction type color
    const getTransactionTypeColor = (type) => {
        switch (type) {
            case 'STOCK_IN': return 'green';
            case 'STOCK_OUT': return 'red';
            case 'ADJUSTMENT': return 'blue';
            case 'STOCK_TAKE': return 'orange';
            case 'INITIAL_STOCK': return 'green';
            default: return 'default';
        }
    };

    // Get transaction type text
    const getTransactionTypeText = (type) => {
        switch (type) {
            case 'STOCK_IN': return 'Nhập hàng';
            case 'STOCK_OUT': return 'Xuất hàng';
            case 'ADJUSTMENT': return 'Điều chỉnh';
            case 'STOCK_TAKE': return 'Kiểm kê';
            case 'INITIAL_STOCK': return 'Nhập hàng ban đầu';
            default: return type || 'Không xác định';
        }
    };

    // Get transaction icon
    const getTransactionIcon = (type) => {
        switch (type) {
            case 'STOCK_IN': return <ShoppingCartOutlined />;
            case 'STOCK_OUT': return <MinusOutlined />;
            case 'ADJUSTMENT': return <SettingOutlined />;
            case 'STOCK_TAKE': return <EyeOutlined />;
            case 'INITIAL_STOCK': return <ShoppingCartOutlined />;
            default: return <EyeOutlined />;
        }
    };

    // Table columns
    const columns = [
        {
            title: 'Loại giao dịch',
            dataIndex: 'transactionType',
            key: 'transactionType',
            render: (type) => (
                <Tag color={getTransactionTypeColor(type)} icon={getTransactionIcon(type)}>
                    {getTransactionTypeText(type)}
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
            title: 'Số lượng',
            dataIndex: 'quantity',
            key: 'quantity',
            render: (quantity, record) => (
                <div>
                    <div style={{
                        fontWeight: '500',
                        color: quantity > 0 ? '#52c41a' : '#ff4d4f',
                        fontSize: '14px'
                    }}>
                        {quantity > 0 ? '+' : ''}{quantity} {record.unit}
                    </div>
                    <div style={{ fontSize: '12px', color: '#999', marginTop: '2px' }}>
                        Tồn kho sau: {record.stockAfter} {record.unit}
                    </div>
                </div>
            ),
        },
        {
            title: 'Lý do',
            dataIndex: 'reason',
            key: 'reason',
            ellipsis: true,
            render: (reason) => (
                <div style={{ maxWidth: '200px' }}>
                    {reason ? (
                        <div style={{ fontSize: '12px', color: '#666' }} title={reason}>
                            {reason}
                        </div>
                    ) : (
                        <span style={{ color: '#999', fontSize: '12px' }}>Không có lý do</span>
                    )}
                </div>
            ),
        },
        {
            title: 'Người thực hiện',
            dataIndex: 'createdBy',
            key: 'createdBy',
            render: (createdBy, record) => (
                <div style={{ fontSize: '12px', color: '#666' }}>
                    {createdBy || record.performedBy || 'SYSTEM'}
                </div>
            ),
        },
        {
            title: 'Ngày thực hiện',
            dataIndex: 'transactionDate',
            key: 'transactionDate',
            render: (date) => dayjs(date).format('DD/MM/YYYY HH:mm'),
            sorter: (a, b) => dayjs(a.transactionDate).unix() - dayjs(b.transactionDate).unix(),
        },
        {
            title: 'Thao tác',
            key: 'actions',
            render: (_, record) => (
                <Space>
                    <Button
                        size="small"
                        icon={<EyeOutlined />}
                        onClick={() => showTransactionDetails(record)}
                    >
                        Chi tiết
                    </Button>
                </Space>
            ),
        },
    ];

    // Get statistics
    const getStatistics = () => {
        console.log('All transactions for statistics:', transactions);
        console.log('Transaction types found:', transactions.map(t => t.transactionType));

        const totalTransactions = transactions.length;

        const stockInCount = transactions.filter(t =>
            t.transactionType === 'STOCK_IN' ||
            t.transactionType === 'INITIAL_STOCK' ||
            t.transactionType === 'STOCK_IN' ||
            (t.quantity && t.quantity > 0 && !t.transactionType?.includes('OUT'))
        ).length;

        const stockOutCount = transactions.filter(t =>
            t.transactionType === 'STOCK_OUT' ||
            (t.quantity && t.quantity < 0)
        ).length;

        const adjustmentCount = transactions.filter(t =>
            t.transactionType === 'ADJUSTMENT' ||
            t.transactionType === 'STOCK_TAKE'
        ).length;

        const stockTakeCount = transactions.filter(t => t.transactionType === 'STOCK_TAKE').length;

        console.log('Statistics calculation:', {
            totalTransactions,
            stockInCount,
            stockOutCount,
            adjustmentCount,
            stockTakeCount
        });

        return {
            totalTransactions,
            stockInCount,
            stockOutCount,
            adjustmentCount,
            stockTakeCount
        };
    };

    const stats = getStatistics();

    return (
        <div style={{ padding: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
                <div>
                    <h1 style={{ fontSize: '24px', fontWeight: 'bold', color: '#1f2937', marginBottom: '8px' }}>Lịch sử giao dịch</h1>
                    <p style={{ color: '#6b7280' }}>Theo dõi tất cả các giao dịch tồn kho</p>
                </div>
                <Space>
                    <Button
                        icon={<DownloadOutlined />}
                    >
                        Xuất báo cáo
                    </Button>
                    <Button
                        icon={<ReloadOutlined />}
                        onClick={() => loadTransactions()}
                    >
                        Làm mới
                    </Button>
                </Space>
            </div>

            {/* Statistics Cards */}
            <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
                <Col xs={24} sm={6}>
                    <Card>
                        <Statistic
                            title="Tổng giao dịch"
                            value={stats.totalTransactions}
                            prefix={<EyeOutlined />}
                            valueStyle={{ color: '#1890ff' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={6}>
                    <Card>
                        <Statistic
                            title="Nhập hàng"
                            value={stats.stockInCount}
                            prefix={<ShoppingCartOutlined />}
                            valueStyle={{ color: '#52c41a' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={6}>
                    <Card>
                        <Statistic
                            title="Xuất hàng"
                            value={stats.stockOutCount}
                            prefix={<MinusOutlined />}
                            valueStyle={{ color: '#ff4d4f' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={6}>
                    <Card>
                        <Statistic
                            title="Điều chỉnh"
                            value={stats.adjustmentCount}
                            prefix={<SettingOutlined />}
                            valueStyle={{ color: '#1890ff' }}
                        />
                    </Card>
                </Col>
            </Row>

            {/* Filters */}
            <Card style={{ marginBottom: '24px' }}>
                <Row gutter={[16, 16]} align="middle">
                    <Col xs={24} sm={6}>
                        <Search
                            placeholder="Tìm kiếm giao dịch..."
                            onSearch={handleSearch}
                            enterButton={<SearchOutlined />}
                        />
                    </Col>
                    <Col xs={24} sm={4}>
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
                            placeholder={['Từ ngày', 'Đến ngày']}
                        />
                    </Col>
                    <Col xs={24} sm={4}>
                        <Input
                            placeholder="ID nguyên liệu"
                            onChange={(e) => handleFilterChange('ingredientId', e.target.value)}
                        />
                    </Col>
                    <Col xs={24} sm={4}>
                        <Button
                            icon={<FilterOutlined />}
                            onClick={() => {
                                setFilters({
                                    transactionType: '',
                                    ingredientId: '',
                                    dateRange: null,
                                    search: ''
                                });
                            }}
                            style={{ width: '100%' }}
                        >
                            Xóa bộ lọc
                        </Button>
                    </Col>
                </Row>
            </Card>

            {/* Table */}
            <Card>
                <Table
                    columns={columns}
                    dataSource={transactions}
                    rowKey="transactionId"
                    loading={loading}
                    pagination={{
                        ...pagination,
                        showSizeChanger: true,
                        showQuickJumper: true,
                        showTotal: (total, range) =>
                            `${range[0]}-${range[1]} của ${total} mục`,
                        pageSizeOptions: PAGINATION.PAGE_SIZE_OPTIONS,
                    }}
                    onChange={handleTableChange}
                    scroll={{ x: 1200 }}
                />
            </Card>

            {/* Transaction Details Modal */}
            <Modal
                title="Chi tiết giao dịch"
                open={modalVisible}
                onCancel={() => setModalVisible(false)}
                footer={[
                    <Button key="close" onClick={() => setModalVisible(false)}>
                        Đóng
                    </Button>
                ]}
                width={600}
            >
                {selectedTransaction && (
                    <div style={{ padding: '16px 0' }}>
                        <Row gutter={[16, 16]}>
                            <Col span={12}>
                                <div>
                                    <strong>Loại giao dịch:</strong>
                                    <Tag color={getTransactionTypeColor(selectedTransaction.transactionType)}
                                        icon={getTransactionIcon(selectedTransaction.transactionType)}
                                        style={{ marginLeft: '8px' }}>
                                        {getTransactionTypeText(selectedTransaction.transactionType)}
                                    </Tag>
                                </div>
                            </Col>
                            <Col span={12}>
                                <div>
                                    <strong>Ngày thực hiện:</strong>
                                    <div style={{ marginLeft: '8px' }}>
                                        {dayjs(selectedTransaction.transactionDate).format('DD/MM/YYYY HH:mm')}
                                    </div>
                                </div>
                            </Col>
                        </Row>

                        <Row gutter={[16, 16]} style={{ marginTop: '16px' }}>
                            <Col span={12}>
                                <div>
                                    <strong>Nguyên liệu:</strong>
                                    <div style={{ marginLeft: '8px' }}>{selectedTransaction.ingredientName}</div>
                                </div>
                            </Col>
                            <Col span={12}>
                                <div>
                                    <strong>Người thực hiện:</strong>
                                    <div style={{ marginLeft: '8px' }}>{selectedTransaction.createdBy || selectedTransaction.performedBy}</div>
                                </div>
                            </Col>
                        </Row>

                        <Row gutter={[16, 16]} style={{ marginTop: '16px' }}>
                            <Col span={12}>
                                <div>
                                    <strong>Số lượng:</strong>
                                    <div style={{
                                        marginLeft: '8px',
                                        fontWeight: '500',
                                        color: selectedTransaction.quantity > 0 ? '#52c41a' : '#ff4d4f'
                                    }}>
                                        {selectedTransaction.quantity > 0 ? '+' : ''}{selectedTransaction.quantity} {selectedTransaction.unit}
                                    </div>
                                </div>
                            </Col>
                            <Col span={12}>
                                <div>
                                    <strong>Tồn kho sau:</strong>
                                    <div style={{ marginLeft: '8px', fontWeight: '500' }}>
                                        {selectedTransaction.stockAfter} {selectedTransaction.unit}
                                    </div>
                                </div>
                            </Col>
                        </Row>

                        <div style={{ marginTop: '16px' }}>
                            <strong>Lý do:</strong>
                            <div style={{ marginLeft: '8px' }}>{selectedTransaction.reason || 'Không có lý do'}</div>
                        </div>

                        {selectedTransaction.notes && (
                            <div style={{ marginTop: '16px' }}>
                                <strong>Ghi chú:</strong>
                                <div style={{ marginLeft: '8px' }}>{selectedTransaction.notes}</div>
                            </div>
                        )}
                    </div>
                )}
            </Modal>
        </div>
    );
};

export default InventoryTransactions;