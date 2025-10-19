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
    Popconfirm,
    Row,
    Col,
    Statistic,
    Switch,
    Image,
    Upload,
    Divider,
    List,
    Typography
} from 'antd';
import {
    PlusOutlined,
    EditOutlined,
    DeleteOutlined,
    SearchOutlined,
    ReloadOutlined,
    ShoppingCartOutlined,
    EyeOutlined,
    UploadOutlined,
    SettingOutlined,
    DollarOutlined,
    MinusOutlined
} from '@ant-design/icons';
import { menuService } from '../services/menuService';
import { PAGINATION, STATUS } from '../constants.js';

const { Option } = Select;
const { Search } = Input;
const { TextArea } = Input;
const { Title, Text } = Typography;

const CombosManagement = () => {
    const { message } = App.useApp();
    const [combos, setCombos] = useState([]);
    const [menuItems, setMenuItems] = useState([]);
    const [loading, setLoading] = useState(false);
    const [pagination, setPagination] = useState({
        current: 1,
        pageSize: PAGINATION.DEFAULT_PAGE_SIZE,
        total: 0
    });
    const [filters, setFilters] = useState({
        active: null,
        search: ''
    });
    const [modalVisible, setModalVisible] = useState(false);
    const [modalType, setModalType] = useState('create'); // create, edit, view
    const [selectedCombo, setSelectedCombo] = useState(null);
    const [form] = Form.useForm();

    // Load combos data
    const loadCombos = async () => {
        setLoading(true);
        try {
            const response = await menuService.getCombos();
            setCombos(response.data || []);
            setPagination(prev => ({
                ...prev,
                total: response.data?.length || 0
            }));
        } catch (error) {
            message.error('Lỗi khi tải dữ liệu combo');
            console.error('Error loading combos:', error);
        } finally {
            setLoading(false);
        }
    };

    // Load menu items for combo selection
    const loadMenuItems = async () => {
        try {
            const response = await menuService.getMenuItems();
            // Handle both direct array response and paginated response
            const data = response.data;
            if (Array.isArray(data)) {
                setMenuItems(data);
            } else if (data && Array.isArray(data.items)) {
                setMenuItems(data.items);
            } else {
                setMenuItems([]);
            }
        } catch (error) {
            console.error('Error loading menu items:', error);
            setMenuItems([]);
        }
    };

    useEffect(() => {
        loadCombos();
        loadMenuItems();
    }, []);

    // Handle search and filters
    const handleSearch = (value) => {
        setFilters(prev => ({ ...prev, search: value }));
    };

    const handleFilterChange = (key, value) => {
        setFilters(prev => ({ ...prev, [key]: value }));
    };

    // Modal handlers
    const showModal = (type, combo = null) => {
        setModalType(type);
        setSelectedCombo(combo);
        setModalVisible(true);

        if (type === 'edit' && combo) {
            form.setFieldsValue({
                ...combo,
                menuItemIds: combo.menuItemIds || [] // Backend expects menuItemIds
            });
        } else {
            form.resetFields();
        }
    };

    const handleModalOk = async () => {
        try {
            const values = await form.validateFields();

            if (modalType === 'create') {
                await menuService.createCombo(values);
                message.success('Tạo combo thành công');
            } else if (modalType === 'edit') {
                await menuService.updateCombo(selectedCombo.id, values);
                message.success('Cập nhật combo thành công');
            }

            setModalVisible(false);
            loadCombos();
        } catch (error) {
            message.error('Có lỗi xảy ra khi thực hiện thao tác');
            console.error('Error:', error);
        }
    };

    // Delete combo
    const handleDelete = async (id) => {
        try {
            await menuService.deleteCombo(id);
            message.success('Xóa combo thành công');
            loadCombos();
        } catch (error) {
            message.error('Lỗi khi xóa combo');
        }
    };

    // Filter combos based on search and filters
    const filteredCombos = combos.filter(combo => {
        const matchesSearch = !filters.search ||
            combo.name.toLowerCase().includes(filters.search.toLowerCase()) ||
            combo.description?.toLowerCase().includes(filters.search.toLowerCase());

        const matchesActive = filters.active === null || combo.active === filters.active;

        return matchesSearch && matchesActive;
    });

    // Calculate combo price
    const calculateComboPrice = (items) => {
        if (!items || items.length === 0) return 0;
        return items.reduce((total, item) => {
            const menuItem = menuItems.find(mi => mi.id === item.menuItemId);
            return total + (menuItem?.price || 0) * (item.quantity || 1);
        }, 0);
    };

    // Table columns
    const columns = [
        {
            title: 'Tên combo',
            dataIndex: 'name',
            key: 'name',
            render: (text, record) => (
                <div>
                    <div className="font-medium">{text}</div>
                    <div className="text-sm text-gray-500">{record.description}</div>
                </div>
            ),
        },
        {
            title: 'Số món',
            dataIndex: 'items',
            key: 'itemCount',
            render: (items) => items?.length || 0,
        },
        {
            title: 'Giá gốc',
            dataIndex: 'items',
            key: 'originalPrice',
            render: (items) => {
                const price = calculateComboPrice(items);
                return (
                    <Text delete className="text-gray-500">
                        {price.toLocaleString('vi-VN')}đ
                    </Text>
                );
            },
        },
        {
            title: 'Giá combo',
            dataIndex: 'price',
            key: 'price',
            render: (price) => (
                <Text strong className="text-red-600">
                    {price?.toLocaleString('vi-VN')}đ
                </Text>
            ),
        },
        {
            title: 'Tiết kiệm',
            dataIndex: 'items',
            key: 'savings',
            render: (items, record) => {
                const originalPrice = calculateComboPrice(items);
                const savings = originalPrice - (record.price || 0);
                const savingsPercent = originalPrice > 0 ? (savings / originalPrice * 100).toFixed(0) : 0;
                return (
                    <div>
                        <Text className="text-green-600 font-medium">
                            {savings.toLocaleString('vi-VN')}đ
                        </Text>
                        <div className="text-xs text-gray-500">
                            ({savingsPercent}%)
                        </div>
                    </div>
                );
            },
        },
        {
            title: 'Trạng thái',
            dataIndex: 'active',
            key: 'active',
            render: (active) => (
                <Tag color={active ? 'green' : 'red'}>
                    {active ? 'Hoạt động' : 'Tạm dừng'}
                </Tag>
            ),
        },
        {
            title: 'Thao tác',
            key: 'actions',
            render: (_, record) => (
                <Space>
                    <Button
                        size="small"
                        icon={<EyeOutlined />}
                        onClick={() => showModal('view', record)}
                    >
                        Xem
                    </Button>
                    <Button
                        size="small"
                        icon={<EditOutlined />}
                        onClick={() => showModal('edit', record)}
                    >
                        Sửa
                    </Button>
                    <Popconfirm
                        title="Bạn có chắc muốn xóa combo này?"
                        onConfirm={() => handleDelete(record.id)}
                        okText="Xóa"
                        cancelText="Hủy"
                    >
                        <Button size="small" icon={<DeleteOutlined />} danger>
                            Xóa
                        </Button>
                    </Popconfirm>
                </Space>
            ),
        },
    ];

    // Get modal title
    const getModalTitle = () => {
        switch (modalType) {
            case 'create': return 'Tạo combo mới';
            case 'edit': return 'Chỉnh sửa combo';
            case 'view': return 'Chi tiết combo';
            default: return 'Thao tác';
        }
    };

    // Render combo items in modal
    const renderComboItems = (items) => {
        if (!items || items.length === 0) {
            return <Text type="secondary">Chưa có món ăn nào</Text>;
        }

        return (
            <List
                dataSource={items}
                renderItem={(item) => {
                    const menuItem = menuItems.find(mi => mi.id === item.menuItemId);
                    return (
                        <List.Item>
                            <div className="flex justify-between items-center w-full">
                                <div>
                                    <Text strong>{menuItem?.name || 'Món không tồn tại'}</Text>
                                    <div className="text-sm text-gray-500">
                                        {menuItem?.price?.toLocaleString('vi-VN')}đ x {item.quantity}
                                    </div>
                                </div>
                                <Text className="text-right">
                                    {((menuItem?.price || 0) * (item.quantity || 1)).toLocaleString('vi-VN')}đ
                                </Text>
                            </div>
                        </List.Item>
                    );
                }}
            />
        );
    };

    return (
        <div className="page-content">
            <div className="flex justify-between items-center page-header">
                <div>
                    <h1>Quản lý combo</h1>
                    <p>Quản lý các combo món ăn</p>
                </div>
                <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={() => showModal('create')}
                    className="restaurant-button"
                >
                    Thêm combo
                </Button>
            </div>

            {/* Statistics Cards */}
            <div className="page-section">
                <Row gutter={[16, 16]}>
                    <Col xs={24} sm={6}>
                        <Card className="restaurant-card stat-card">
                            <Statistic
                                title="Tổng combo"
                                value={combos.length}
                                prefix={<ShoppingCartOutlined />}
                                valueStyle={{ color: '#1890ff' }}
                            />
                        </Card>
                    </Col>
                    <Col xs={24} sm={6}>
                        <Card className="restaurant-card stat-card">
                            <Statistic
                                title="Combo hoạt động"
                                value={combos.filter(combo => combo.active).length}
                                prefix={<EyeOutlined />}
                                valueStyle={{ color: '#52c41a' }}
                            />
                        </Card>
                    </Col>
                    <Col xs={24} sm={6}>
                        <Card className="restaurant-card stat-card">
                            <Statistic
                                title="Tổng tiết kiệm"
                                value={combos.reduce((total, combo) => {
                                    const originalPrice = calculateComboPrice(combo.items);
                                    return total + (originalPrice - (combo.price || 0));
                                }, 0)}
                                prefix={<DollarOutlined />}
                                valueStyle={{ color: '#f59e0b' }}
                                formatter={(value) => `${value.toLocaleString('vi-VN')}đ`}
                            />
                        </Card>
                    </Col>
                    <Col xs={24} sm={6}>
                        <Card className="restaurant-card stat-card">
                            <Statistic
                                title="Giá TB combo"
                                value={combos.length > 0 ?
                                    combos.reduce((sum, combo) => sum + (combo.price || 0), 0) / combos.length : 0
                                }
                                prefix={<DollarOutlined />}
                                valueStyle={{ color: '#dc2626' }}
                                formatter={(value) => `${Math.round(value).toLocaleString('vi-VN')}đ`}
                            />
                        </Card>
                    </Col>
                </Row>
            </div>

            {/* Filters */}
            <div className="page-section">
                <Card className="restaurant-card search-filter-section">
                    <Row gutter={[16, 16]} align="middle">
                        <Col xs={24} sm={8}>
                            <Search
                                placeholder="Tìm kiếm combo..."
                                onSearch={handleSearch}
                                enterButton={<SearchOutlined />}
                            />
                        </Col>
                        <Col xs={24} sm={6}>
                            <Select
                                placeholder="Trạng thái"
                                style={{ width: '100%' }}
                                allowClear
                                onChange={(value) => handleFilterChange('active', value)}
                            >
                                <Option value={true}>Hoạt động</Option>
                                <Option value={false}>Tạm dừng</Option>
                            </Select>
                        </Col>
                        <Col xs={24} sm={4}>
                            <Button
                                icon={<ReloadOutlined />}
                                onClick={() => loadCombos()}
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
                        dataSource={filteredCombos}
                        rowKey="id"
                        loading={loading}
                        pagination={{
                            ...pagination,
                            showSizeChanger: true,
                            showQuickJumper: true,
                            showTotal: (total, range) =>
                                `${range[0]}-${range[1]} của ${total} mục`,
                            pageSizeOptions: PAGINATION.PAGE_SIZE_OPTIONS,
                        }}
                        scroll={{ x: 1000 }}
                    />
                </Card>

                {/* Modal */}
                <Modal
                    title={getModalTitle()}
                    open={modalVisible}
                    onOk={modalType !== 'view' ? handleModalOk : undefined}
                    onCancel={() => setModalVisible(false)}
                    width={800}
                    okText="Lưu"
                    cancelText="Hủy"
                    footer={modalType === 'view' ? [
                        <Button key="close" onClick={() => setModalVisible(false)}>
                            Đóng
                        </Button>
                    ] : undefined}
                >
                    {modalType === 'view' ? (
                        <div className="space-y-4">
                            <div>
                                <Title level={4}>{selectedCombo?.name}</Title>
                                <Text>{selectedCombo?.description}</Text>
                            </div>

                            <Divider />

                            <div>
                                <Title level={5}>Món ăn trong combo:</Title>
                                {renderComboItems(selectedCombo?.items)}
                            </div>

                            <Divider />

                            <Row gutter={16}>
                                <Col span={12}>
                                    <Statistic
                                        title="Giá gốc"
                                        value={calculateComboPrice(selectedCombo?.items)}
                                        formatter={(value) => `${value.toLocaleString('vi-VN')}đ`}
                                        valueStyle={{ color: '#8c8c8c' }}
                                    />
                                </Col>
                                <Col span={12}>
                                    <Statistic
                                        title="Giá combo"
                                        value={selectedCombo?.price || 0}
                                        formatter={(value) => `${value.toLocaleString('vi-VN')}đ`}
                                        valueStyle={{ color: '#dc2626' }}
                                    />
                                </Col>
                            </Row>

                            <Row gutter={16}>
                                <Col span={12}>
                                    <Statistic
                                        title="Tiết kiệm"
                                        value={calculateComboPrice(selectedCombo?.items) - (selectedCombo?.price || 0)}
                                        formatter={(value) => `${value.toLocaleString('vi-VN')}đ`}
                                        valueStyle={{ color: '#52c41a' }}
                                    />
                                </Col>
                                <Col span={12}>
                                    <Statistic
                                        title="Phần trăm tiết kiệm"
                                        value={selectedCombo?.items && selectedCombo?.items.length > 0 ?
                                            ((calculateComboPrice(selectedCombo.items) - (selectedCombo.price || 0)) /
                                                calculateComboPrice(selectedCombo.items) * 100).toFixed(0) : 0
                                        }
                                        suffix="%"
                                        valueStyle={{ color: '#52c41a' }}
                                    />
                                </Col>
                            </Row>
                        </div>
                    ) : (
                        <Form form={form} layout="vertical">
                            <Form.Item
                                name="name"
                                label="Tên combo"
                                rules={[{ required: true, message: 'Vui lòng nhập tên combo' }]}
                            >
                                <Input />
                            </Form.Item>

                            <Form.Item
                                name="description"
                                label="Mô tả"
                            >
                                <TextArea rows={3} />
                            </Form.Item>

                            <Row gutter={16}>
                                <Col span={8}>
                                    <Form.Item
                                        name="price"
                                        label="Giá combo"
                                        rules={[{ required: true, message: 'Vui lòng nhập giá combo' }]}
                                    >
                                        <InputNumber
                                            style={{ width: '100%' }}
                                            formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                                            parser={(value) => value.replace(/\$\s?|(,*)/g, '')}
                                            min={0}
                                        />
                                    </Form.Item>
                                </Col>
                                <Col span={8}>
                                    <Form.Item
                                        name="discount"
                                        label="Giảm giá (%)"
                                        initialValue={0}
                                    >
                                        <InputNumber
                                            style={{ width: '100%' }}
                                            min={0}
                                            max={100}
                                            formatter={(value) => `${value}%`}
                                            parser={(value) => value.replace('%', '')}
                                        />
                                    </Form.Item>
                                </Col>
                                <Col span={8}>
                                    <Form.Item
                                        name="active"
                                        label="Trạng thái"
                                        valuePropName="checked"
                                        initialValue={true}
                                    >
                                        <Switch checkedChildren="Hoạt động" unCheckedChildren="Tạm dừng" />
                                    </Form.Item>
                                </Col>
                            </Row>

                            <Form.Item
                                name="menuItemIds"
                                label="Món ăn trong combo"
                                rules={[{ required: true, message: 'Vui lòng chọn ít nhất một món ăn' }]}
                            >
                                <Select
                                    mode="multiple"
                                    placeholder="Chọn món ăn"
                                    showSearch
                                    optionFilterProp="children"
                                    style={{ width: '100%' }}
                                >
                                    {Array.isArray(menuItems) && menuItems.map(item => (
                                        <Option key={item.id || item.menuItemId} value={item.id || item.menuItemId}>
                                            {item.name} - {item.price?.toLocaleString('vi-VN')}đ
                                        </Option>
                                    ))}
                                </Select>
                            </Form.Item>
                        </Form>
                    )}
                </Modal>
            </div>
        </div>
    );
};

export default CombosManagement;
