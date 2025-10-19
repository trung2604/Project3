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
    Upload,
    App,
    Popconfirm,
    Row,
    Col,
    Statistic,
    Image,
    Switch
} from 'antd';
import {
    PlusOutlined,
    EditOutlined,
    DeleteOutlined,
    SearchOutlined,
    ReloadOutlined,
    BookOutlined,
    DollarOutlined,
    EyeOutlined,
    UploadOutlined
} from '@ant-design/icons';
import { menuService } from '../services/menuService';
import { PAGINATION, STATUS } from '../constants.js';

const { Option } = Select;
const { Search } = Input;
const { TextArea } = Input;

const MenuManagement = () => {
    const { message } = App.useApp();
    const [menuItems, setMenuItems] = useState([]);
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(false);
    const [pagination, setPagination] = useState({
        current: 1,
        pageSize: PAGINATION.DEFAULT_PAGE_SIZE,
        total: 0
    });
    const [filters, setFilters] = useState({
        categoryId: undefined,
        active: undefined,
        minPrice: undefined,
        maxPrice: undefined,
        search: ''
    });
    const [modalVisible, setModalVisible] = useState(false);
    const [modalType, setModalType] = useState('create'); // create, edit
    const [selectedMenuItem, setSelectedMenuItem] = useState(null);
    const [form] = Form.useForm();

    // Load menu items data
    const loadMenuItems = async (page = 1, size = PAGINATION.DEFAULT_PAGE_SIZE) => {
        setLoading(true);
        try {
            const params = {
                page: page - 1, // Backend uses 0-based pagination
                size,
                ...filters
            };

            const response = await menuService.getMenuItems(params);
            // Backend returns PagedMenuItemResponse with different structure
            const data = response.data;
            setMenuItems(data.items || []);
            setPagination(prev => ({
                ...prev,
                current: page,
                total: data.totalElements || 0
            }));
        } catch (error) {
            message.error('Lỗi khi tải dữ liệu món ăn');
            console.error('Error loading menu items:', error);
        } finally {
            setLoading(false);
        }
    };

    // Load categories
    const loadCategories = async () => {
        try {
            const response = await menuService.getCategories();
            const raw = Array.isArray(response.data) ? response.data : [];
            // Filter out null/undefined IDs to avoid Select value warnings
            const sanitized = raw.filter(c => c && c.id != null);
            setCategories(sanitized);
        } catch (error) {
            console.error('Error loading categories:', error);
        }
    };

    useEffect(() => {
        loadMenuItems();
        loadCategories();
    }, [filters]);

    // Handle table changes
    const handleTableChange = (paginationInfo) => {
        loadMenuItems(paginationInfo.current, paginationInfo.pageSize);
    };

    // Handle search and filters
    const handleSearch = (value) => {
        setFilters(prev => ({ ...prev, search: value }));
    };

    const handleFilterChange = (key, value) => {
        setFilters(prev => ({ ...prev, [key]: value }));
    };

    // Modal handlers
    const showModal = (type, menuItem = null) => {
        setModalType(type);
        setSelectedMenuItem(menuItem);
        setModalVisible(true);

        if (type === 'edit' && menuItem) {
            form.setFieldsValue({
                ...menuItem,
                ingredients: menuItem.ingredients || [] // Backend expects List<String>
            });
        } else {
            form.resetFields();
        }
    };

    const handleModalOk = async () => {
        try {
            const values = await form.validateFields();

            if (modalType === 'create') {
                await menuService.createMenuItem(values);
                message.success('Tạo món ăn thành công');
            } else if (modalType === 'edit') {
                await menuService.updateMenuItem(selectedMenuItem.id, values);
                message.success('Cập nhật món ăn thành công');
            }

            setModalVisible(false);
            loadMenuItems(pagination.current, pagination.pageSize);
        } catch (error) {
            message.error('Có lỗi xảy ra khi thực hiện thao tác');
            console.error('Error:', error);
        }
    };

    // Delete menu item
    const handleDelete = async (id) => {
        try {
            await menuService.deleteMenuItem(id);
            message.success('Xóa món ăn thành công');
            loadMenuItems(pagination.current, pagination.pageSize);
        } catch (error) {
            message.error('Lỗi khi xóa món ăn');
        }
    };

    // Toggle active status
    const handleToggleActive = async (id, active) => {
        try {
            await menuService.toggleMenuItemActive(id, active);
            message.success(`Đã ${active ? 'kích hoạt' : 'vô hiệu hóa'} món ăn`);
            loadMenuItems(pagination.current, pagination.pageSize);
        } catch (error) {
            message.error('Lỗi khi thay đổi trạng thái');
        }
    };

    // Table columns
    const columns = [
        {
            title: 'Hình ảnh',
            dataIndex: 'imageUrl',
            key: 'imageUrl',
            width: 80,
            render: (imageUrl) => (
                <Image
                    width={60}
                    height={60}
                    src={imageUrl || '/placeholder-food.jpg'}
                    fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAMIAAADDCAYAAADQvc6UAAABRWlDQ1BJQ0MgUHJvZmlsZQAAKJFjYGASSSwoyGFhYGDIzSspCnJ3UoiIjFJgf8LAwSDCIMogwMCcmFxc4BgQ4ANUwgCjUcG3awyMIPqyLsis7PPOq3QdDFcvjV3jOD1boQVTPQrgSkktTgbSf4A4LbmgqISBgTEFyFYuLykAsTuAbJEkoKGAXW3s9AACn97S3xCyIe4u/4uBwQBXhk1Bc0BHTkcdQEBHQl4erFQcnoS1BfADgUeChZiDvIGUwYiGxraWA8QcJ+ZgYFlgI8Bq4FBJx1iCGBYAR4B4p4sMz4B4YQkOAQMFc0aMG6PwgE1JxRQMpC0MFQwJgNYqRYusB2Nwj5Gx0Fi/ws//4/WL2cVt3FiX12XgO+v//f9f///3XQ//fxbDw4BuNAD8VgD0Z4hxUAAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAA7EAAAOxAGVKw4bAAAAB3RJTUUH5wEDCy0b8f3f2QAAABl0RVh0Q29tbWVudABDcmVhdGVkIHdpdGggR0lNUFeBDhcAAAcaSURBVHja7doxAQAwDASx/z/NQYQJQwAAAP//AwDfAAABX3ZhkwAAAABJRU5ErkJggg=="
                    className="rounded object-cover"
                />
            ),
        },
        {
            title: 'Tên món ăn',
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
            dataIndex: 'categoryName',
            key: 'categoryName',
            render: (categoryName) => <Tag color="blue">{categoryName}</Tag>,
        },
        {
            title: 'Giá',
            dataIndex: 'price',
            key: 'price',
            render: (price) => (
                <div className="font-medium text-green-600">
                    {price?.toLocaleString()} VNĐ
                </div>
            ),
        },
        {
            title: 'Thời gian chế biến',
            dataIndex: 'preparationTime',
            key: 'preparationTime',
            render: (time) => `${time} phút`,
        },
        {
            title: 'Độ phổ biến',
            dataIndex: 'popularityScore',
            key: 'popularityScore',
            render: (score) => (
                <div className="flex items-center">
                    <div className="w-16 bg-gray-200 rounded-full h-2 mr-2">
                        <div
                            className="bg-yellow-500 h-2 rounded-full"
                            style={{ width: `${score || 0}%` }}
                        ></div>
                    </div>
                    <span className="text-sm">{score || 0}%</span>
                </div>
            ),
        },
        {
            title: 'Trạng thái',
            dataIndex: 'active',
            key: 'active',
            render: (active, record) => (
                <Switch
                    checked={active}
                    onChange={(checked) => handleToggleActive(record.id, checked)}
                    checkedChildren="Bán"
                    unCheckedChildren="Tạm dừng"
                />
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
                        title="Bạn có chắc muốn xóa món ăn này?"
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
            case 'create': return 'Tạo món ăn mới';
            case 'edit': return 'Chỉnh sửa món ăn';
            case 'view': return 'Chi tiết món ăn';
            default: return 'Thao tác';
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-2xl font-bold text-gray-800 mb-2">Quản lý menu</h1>
                    <p className="text-gray-600">Quản lý món ăn và danh mục</p>
                </div>
                <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={() => showModal('create')}
                    className="restaurant-button"
                >
                    Thêm món ăn
                </Button>
            </div>

            {/* Statistics Cards */}
            <Row gutter={[16, 16]}>
                <Col xs={24} sm={8}>
                    <Card className="restaurant-card">
                        <Statistic
                            title="Tổng món ăn"
                            value={pagination.total}
                            prefix={<BookOutlined />}
                            valueStyle={{ color: '#1890ff' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={8}>
                    <Card className="restaurant-card">
                        <Statistic
                            title="Món đang bán"
                            value={menuItems.filter(item => item.active).length}
                            prefix={<EyeOutlined />}
                            valueStyle={{ color: '#52c41a' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={8}>
                    <Card className="restaurant-card">
                        <Statistic
                            title="Giá trung bình"
                            value={menuItems.reduce((sum, item) => sum + (item.price || 0), 0) / menuItems.length || 0}
                            prefix={<DollarOutlined />}
                            valueStyle={{ color: '#f59e0b' }}
                            formatter={(value) => `${Math.round(value).toLocaleString()} VNĐ`}
                        />
                    </Card>
                </Col>
            </Row>

            {/* Filters */}
            <Card className="restaurant-card">
                <Row gutter={[16, 16]} align="middle">
                    <Col xs={24} sm={6}>
                        <Search
                            placeholder="Tìm kiếm món ăn..."
                            onSearch={handleSearch}
                            enterButton={<SearchOutlined />}
                        />
                    </Col>
                    <Col xs={24} sm={6}>
                        <Select
                            placeholder="Danh mục"
                            style={{ width: '100%' }}
                            allowClear
                            onChange={(value) => handleFilterChange('categoryId', value)}
                        >
                            {categories.map(category => (
                                <Option key={String(category.id)} value={String(category.id)}>
                                    {category.name}
                                </Option>
                            ))}
                        </Select>
                    </Col>
                    <Col xs={24} sm={6}>
                        <Select
                            placeholder="Trạng thái"
                            style={{ width: '100%' }}
                            allowClear
                            onChange={(value) => handleFilterChange('active', value)}
                        >
                            <Option value={true}>Đang bán</Option>
                            <Option value={false}>Tạm dừng</Option>
                        </Select>
                    </Col>
                    <Col xs={24} sm={6}>
                        <Button
                            icon={<ReloadOutlined />}
                            onClick={() => loadMenuItems()}
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
                    dataSource={menuItems}
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
                <Form form={form} layout="vertical">
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item
                                name="name"
                                label="Tên món ăn"
                                rules={[{ required: true, message: 'Vui lòng nhập tên món ăn' }]}
                            >
                                <Input />
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item
                                name="categoryId"
                                label="Danh mục"
                                rules={[{ required: true, message: 'Vui lòng chọn danh mục' }]}
                            >
                                <Select>
                                    {categories.map(category => (
                                        <Option key={String(category.id)} value={String(category.id)}>
                                            {category.name}
                                        </Option>
                                    ))}
                                </Select>
                            </Form.Item>
                        </Col>
                    </Row>

                    <Form.Item
                        name="description"
                        label="Mô tả"
                    >
                        <TextArea rows={3} />
                    </Form.Item>

                    <Form.Item
                        name="price"
                        label="Giá"
                        rules={[{ required: true, message: 'Vui lòng nhập giá' }]}
                    >
                        <InputNumber min={0} style={{ width: '100%' }} />
                    </Form.Item>

                    <Form.Item
                        name="ingredients"
                        label="Nguyên liệu"
                        rules={[{ required: true, message: 'Vui lòng chọn ít nhất một nguyên liệu' }]}
                    >
                        <Select
                            mode="multiple"
                            placeholder="Chọn nguyên liệu"
                            style={{ width: '100%' }}
                        >
                            {/* This would be populated with actual ingredients from API */}
                            <Option value="ingredient1">Cà chua</Option>
                            <Option value="ingredient2">Thịt bò</Option>
                            <Option value="ingredient3">Hành tây</Option>
                            <Option value="ingredient4">Tỏi</Option>
                            <Option value="ingredient5">Gừng</Option>
                        </Select>
                    </Form.Item>

                    <Form.Item
                        name="active"
                        label="Trạng thái"
                        valuePropName="checked"
                        initialValue={true}
                    >
                        <Switch checkedChildren="Đang bán" unCheckedChildren="Tạm dừng" />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default MenuManagement;
