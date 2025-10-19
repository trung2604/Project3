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
    DatePicker,
    App,
    Popconfirm,
    Row,
    Col,
    Statistic,
    Alert
} from 'antd';
import {
    PlusOutlined,
    EditOutlined,
    DeleteOutlined,
    SearchOutlined,
    ReloadOutlined,
    WarningOutlined,
    ShoppingCartOutlined,
    MinusOutlined,
    SettingOutlined,
    DollarOutlined
} from '@ant-design/icons';
import { inventoryService } from '../services/inventoryService';
import { PAGINATION, STATUS, TRANSACTION_TYPES } from '../constants.js';

const { Option } = Select;
const { Search } = Input;

const InventoryManagement = () => {
    const { message } = App.useApp();
    const [ingredients, setIngredients] = useState([]);
    const [loading, setLoading] = useState(false);
    const [pagination, setPagination] = useState({
        current: 1,
        pageSize: PAGINATION.DEFAULT_PAGE_SIZE,
        total: 0
    });
    const [filters, setFilters] = useState({
        category: '',
        active: null,
        search: ''
    });
    const [modalVisible, setModalVisible] = useState(false);
    const [modalType, setModalType] = useState('create'); // create, edit, stock-in, stock-out, adjust
    const [selectedIngredient, setSelectedIngredient] = useState(null);
    const [form] = Form.useForm();

    // Load ingredients data
    const loadIngredients = async (page = 1, size = PAGINATION.DEFAULT_PAGE_SIZE) => {
        setLoading(true);
        try {
            const params = {
                page: page - 1, // Backend uses 0-based pagination
                size,
                ...filters
            };

            const response = await inventoryService.getIngredients(params);
            // Backend returns PagedIngredientResponse with different structure
            const data = response.data;
            setIngredients(data.ingredients || []);
            setPagination(prev => ({
                ...prev,
                current: page,
                total: data.totalElements || 0
            }));
        } catch (error) {
            message.error('Lỗi khi tải dữ liệu nguyên liệu');
            console.error('Error loading ingredients:', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadIngredients();
    }, [filters]);

    // Handle table changes
    const handleTableChange = (paginationInfo) => {
        loadIngredients(paginationInfo.current, paginationInfo.pageSize);
    };

    // Handle search and filters
    const handleSearch = (value) => {
        setFilters(prev => ({ ...prev, search: value }));
    };

    const handleFilterChange = (key, value) => {
        setFilters(prev => ({ ...prev, [key]: value }));
    };

    // Modal handlers
    const showModal = (type, ingredient = null) => {
        setModalType(type);
        setSelectedIngredient(ingredient);
        setModalVisible(true);

        if (type === 'edit' && ingredient) {
            form.setFieldsValue(ingredient);
        } else {
            form.resetFields();
        }
    };

    const handleModalOk = async () => {
        try {
            const values = await form.validateFields();

            if (modalType === 'create') {
                await inventoryService.createIngredient(values);
                message.success('Tạo nguyên liệu thành công');
            } else if (modalType === 'edit') {
                await inventoryService.updateIngredient(selectedIngredient.id, values);
                message.success('Cập nhật nguyên liệu thành công');
            } else if (modalType === 'stock-in') {
                await inventoryService.stockIn(selectedIngredient.id, values);
                message.success('Nhập hàng thành công');
            } else if (modalType === 'stock-out') {
                await inventoryService.stockOut(selectedIngredient.id, values);
                message.success('Xuất hàng thành công');
            } else if (modalType === 'adjust') {
                await inventoryService.adjustStock(selectedIngredient.id, values);
                message.success('Điều chỉnh tồn kho thành công');
            }

            setModalVisible(false);
            loadIngredients(pagination.current, pagination.pageSize);
        } catch (error) {
            message.error('Có lỗi xảy ra khi thực hiện thao tác');
            console.error('Error:', error);
        }
    };

    // Delete ingredient
    const handleDelete = async (id) => {
        try {
            await inventoryService.deleteIngredient(id);
            message.success('Xóa nguyên liệu thành công');
            loadIngredients(pagination.current, pagination.pageSize);
        } catch (error) {
            message.error('Lỗi khi xóa nguyên liệu');
        }
    };

    // Toggle active status
    const handleToggleActive = async (id, active) => {
        try {
            await inventoryService.toggleIngredientActive(id, active);
            message.success(`Đã ${active ? 'kích hoạt' : 'vô hiệu hóa'} nguyên liệu`);
            loadIngredients(pagination.current, pagination.pageSize);
        } catch (error) {
            message.error('Lỗi khi thay đổi trạng thái');
        }
    };

    // Table columns
    const columns = [
        {
            title: 'Tên nguyên liệu',
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
            title: 'Danh mục',
            dataIndex: 'category',
            key: 'category',
            render: (category) => <Tag color="blue">{category}</Tag>,
        },
        {
            title: 'Tồn kho',
            dataIndex: 'currentStock',
            key: 'currentStock',
            render: (stock, record) => (
                <div>
                    <div className="font-medium">{stock} {record.unit}</div>
                    <div className="text-sm text-gray-500">
                        Min: {record.minStockLevel} | Max: {record.maxStockLevel}
                    </div>
                </div>
            ),
        },
        {
            title: 'Giá',
            dataIndex: 'unitCost',
            key: 'unitCost',
            render: (cost, record) => (
                <div>
                    <div className="font-medium">{cost?.toLocaleString()} {record.currency}</div>
                    <div className="text-sm text-gray-500">/ {record.unit}</div>
                </div>
            ),
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
                        icon={<EditOutlined />}
                        onClick={() => showModal('edit', record)}
                    >
                        Sửa
                    </Button>
                    <Button
                        size="small"
                        icon={<ShoppingCartOutlined />}
                        onClick={() => showModal('stock-in', record)}
                        type="primary"
                    >
                        Nhập
                    </Button>
                    <Button
                        size="small"
                        icon={<MinusOutlined />}
                        onClick={() => showModal('stock-out', record)}
                        danger
                    >
                        Xuất
                    </Button>
                    <Button
                        size="small"
                        icon={<SettingOutlined />}
                        onClick={() => showModal('adjust', record)}
                    >
                        Điều chỉnh
                    </Button>
                    <Popconfirm
                        title="Bạn có chắc muốn xóa nguyên liệu này?"
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
            case 'create': return 'Tạo nguyên liệu mới';
            case 'edit': return 'Chỉnh sửa nguyên liệu';
            case 'stock-in': return 'Nhập hàng';
            case 'stock-out': return 'Xuất hàng';
            case 'adjust': return 'Điều chỉnh tồn kho';
            default: return 'Thao tác';
        }
    };

    // Get form fields based on modal type
    const getFormFields = () => {
        if (modalType === 'stock-in' || modalType === 'stock-out' || modalType === 'adjust') {
            return (
                <>
                    <Form.Item
                        name="quantity"
                        label="Số lượng"
                        rules={[{ required: true, message: 'Vui lòng nhập số lượng' }]}
                    >
                        <InputNumber min={0} style={{ width: '100%' }} />
                    </Form.Item>
                    <Form.Item
                        name="reason"
                        label="Lý do"
                        rules={[{ required: true, message: 'Vui lòng nhập lý do' }]}
                    >
                        <Input.TextArea rows={3} />
                    </Form.Item>
                    <Form.Item
                        name="notes"
                        label="Ghi chú"
                    >
                        <Input.TextArea rows={2} />
                    </Form.Item>
                </>
            );
        }

        return (
            <>
                <Form.Item
                    name="name"
                    label="Tên nguyên liệu"
                    rules={[{ required: true, message: 'Vui lòng nhập tên nguyên liệu' }]}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    name="description"
                    label="Mô tả"
                >
                    <Input.TextArea rows={3} />
                </Form.Item>
                <Form.Item
                    name="category"
                    label="Danh mục"
                    rules={[{ required: true, message: 'Vui lòng chọn danh mục' }]}
                >
                    <Select>
                        <Option value="Vegetables">Rau củ</Option>
                        <Option value="Meat">Thịt</Option>
                        <Option value="Seafood">Hải sản</Option>
                        <Option value="Spices">Gia vị</Option>
                        <Option value="Dairy">Sữa</Option>
                        <Option value="Grains">Ngũ cốc</Option>
                    </Select>
                </Form.Item>
                <Row gutter={16}>
                    <Col span={12}>
                        <Form.Item
                            name="unit"
                            label="Đơn vị"
                            rules={[{ required: true, message: 'Vui lòng nhập đơn vị' }]}
                        >
                            <Select>
                                <Option value="kg">Kilogram</Option>
                                <Option value="g">Gram</Option>
                                <Option value="l">Lít</Option>
                                <Option value="ml">Mililit</Option>
                                <Option value="piece">Cái</Option>
                                <Option value="box">Hộp</Option>
                            </Select>
                        </Form.Item>
                    </Col>
                    <Col span={12}>
                        <Form.Item
                            name="initialStock"
                            label="Tồn kho ban đầu"
                            rules={[{ required: true, message: 'Vui lòng nhập tồn kho ban đầu' }]}
                        >
                            <InputNumber min={0} style={{ width: '100%' }} />
                        </Form.Item>
                    </Col>
                </Row>
                <Row gutter={16}>
                    <Col span={12}>
                        <Form.Item
                            name="minStockLevel"
                            label="Mức tồn kho tối thiểu"
                            rules={[{ required: true, message: 'Vui lòng nhập mức tối thiểu' }]}
                        >
                            <InputNumber min={0} style={{ width: '100%' }} />
                        </Form.Item>
                    </Col>
                    <Col span={12}>
                        <Form.Item
                            name="maxStockLevel"
                            label="Mức tồn kho tối đa"
                            rules={[{ required: true, message: 'Vui lòng nhập mức tối đa' }]}
                        >
                            <InputNumber min={0} style={{ width: '100%' }} />
                        </Form.Item>
                    </Col>
                </Row>
                <Form.Item
                    name="unitCost"
                    label="Giá đơn vị"
                    rules={[{ required: true, message: 'Vui lòng nhập giá đơn vị' }]}
                >
                    <InputNumber min={0} style={{ width: '100%' }} />
                </Form.Item>
                <Form.Item
                    name="currency"
                    label="Tiền tệ"
                    initialValue="VND"
                >
                    <Select>
                        <Option value="VND">VND</Option>
                        <Option value="USD">USD</Option>
                    </Select>
                </Form.Item>
                <Row gutter={16}>
                    <Col span={12}>
                        <Form.Item
                            name="supplierName"
                            label="Tên nhà cung cấp"
                            rules={[{ required: true, message: 'Vui lòng nhập tên nhà cung cấp' }]}
                        >
                            <Input />
                        </Form.Item>
                    </Col>
                    <Col span={12}>
                        <Form.Item
                            name="supplierContact"
                            label="Liên hệ nhà cung cấp"
                        >
                            <Input />
                        </Form.Item>
                    </Col>
                </Row>
                <Form.Item
                    name="expiryDate"
                    label="Ngày hết hạn"
                >
                    <DatePicker style={{ width: '100%' }} />
                </Form.Item>
            </>
        );
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-2xl font-bold text-gray-800 mb-2">Quản lý kho hàng</h1>
                    <p className="text-gray-600">Quản lý nguyên liệu và tồn kho</p>
                </div>
                <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={() => showModal('create')}
                    className="restaurant-button"
                >
                    Thêm nguyên liệu
                </Button>
            </div>

            {/* Statistics Cards */}
            <Row gutter={[16, 16]}>
                <Col xs={24} sm={8}>
                    <Card className="restaurant-card">
                        <Statistic
                            title="Tổng nguyên liệu"
                            value={pagination.total}
                            prefix={<ShoppingCartOutlined />}
                            valueStyle={{ color: '#1890ff' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={8}>
                    <Card className="restaurant-card">
                        <Statistic
                            title="Tồn kho thấp"
                            value={12}
                            prefix={<WarningOutlined />}
                            valueStyle={{ color: '#ff4d4f' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={8}>
                    <Card className="restaurant-card">
                        <Statistic
                            title="Giá trị tồn kho"
                            value={45000000}
                            prefix={<DollarOutlined />}
                            valueStyle={{ color: '#52c41a' }}
                            formatter={(value) => `${(value / 1000000).toFixed(0)}M VNĐ`}
                        />
                    </Card>
                </Col>
            </Row>

            {/* Filters */}
            <Card className="restaurant-card">
                <Row gutter={[16, 16]} align="middle">
                    <Col xs={24} sm={8}>
                        <Search
                            placeholder="Tìm kiếm nguyên liệu..."
                            onSearch={handleSearch}
                            enterButton={<SearchOutlined />}
                        />
                    </Col>
                    <Col xs={24} sm={6}>
                        <Select
                            placeholder="Danh mục"
                            style={{ width: '100%' }}
                            allowClear
                            onChange={(value) => handleFilterChange('category', value)}
                        >
                            <Option value="Vegetables">Rau củ</Option>
                            <Option value="Meat">Thịt</Option>
                            <Option value="Seafood">Hải sản</Option>
                            <Option value="Spices">Gia vị</Option>
                            <Option value="Dairy">Sữa</Option>
                            <Option value="Grains">Ngũ cốc</Option>
                        </Select>
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
                            onClick={() => loadIngredients()}
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
                    dataSource={ingredients}
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
                    onChange={handleTableChange}
                    scroll={{ x: 1200 }}
                />
            </Card>

            {/* Modal */}
            <Modal
                title={getModalTitle()}
                open={modalVisible}
                onOk={handleModalOk}
                onCancel={() => setModalVisible(false)}
                width={600}
                okText="Lưu"
                cancelText="Hủy"
            >
                <Form form={form} layout="vertical">
                    {getFormFields()}
                </Form>
            </Modal>
        </div>
    );
};

export default InventoryManagement;
