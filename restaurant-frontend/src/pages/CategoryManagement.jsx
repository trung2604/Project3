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
    App,
    Popconfirm,
    Row,
    Col,
    Statistic,
    Switch,
    Image,
    Upload
} from 'antd';
import {
    PlusOutlined,
    EditOutlined,
    DeleteOutlined,
    SearchOutlined,
    ReloadOutlined,
    BookOutlined,
    EyeOutlined,
    UploadOutlined,
    SettingOutlined
} from '@ant-design/icons';
import { menuService } from '../services/menuService';
import { PAGINATION, STATUS } from '../constants.js';

const { Option } = Select;
const { Search } = Input;
const { TextArea } = Input;

const CategoryManagement = () => {
    const { message } = App.useApp();
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(false);
    const [pagination, setPagination] = useState({
        current: 1,
        pageSize: PAGINATION.DEFAULT_PAGE_SIZE,
        total: 0
    });
    const [filters, setFilters] = useState({
        type: '',
        active: null,
        search: ''
    });
    const [modalVisible, setModalVisible] = useState(false);
    const [modalType, setModalType] = useState('create'); // create, edit
    const [selectedCategory, setSelectedCategory] = useState(null);
    const [form] = Form.useForm();

    // Load categories data
    const loadCategories = async () => {
        setLoading(true);
        try {
            const response = await menuService.getCategories();
            setCategories(response.data || []);
            setPagination(prev => ({
                ...prev,
                total: response.data?.length || 0
            }));
        } catch (error) {
            message.error('Lỗi khi tải dữ liệu danh mục');
            console.error('Error loading categories:', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadCategories();
    }, []);

    // Handle search and filters
    const handleSearch = (value) => {
        setFilters(prev => ({ ...prev, search: value }));
    };

    const handleFilterChange = (key, value) => {
        setFilters(prev => ({ ...prev, [key]: value }));
    };

    // Modal handlers
    const showModal = (type, category = null) => {
        setModalType(type);
        setSelectedCategory(category);
        setModalVisible(true);

        if (type === 'edit' && category) {
            form.setFieldsValue(category);
        } else {
            form.resetFields();
        }
    };

    const handleModalOk = async () => {
        try {
            const values = await form.validateFields();

            if (modalType === 'create') {
                await menuService.createCategory(values);
                message.success('Tạo danh mục thành công');
            } else if (modalType === 'edit') {
                await menuService.updateCategory(selectedCategory.id, values);
                message.success('Cập nhật danh mục thành công');
            }

            setModalVisible(false);
            loadCategories();
        } catch (error) {
            message.error('Có lỗi xảy ra khi thực hiện thao tác');
            console.error('Error:', error);
        }
    };

    // Delete category
    const handleDelete = async (id) => {
        try {
            await menuService.deleteCategory(id);
            message.success('Xóa danh mục thành công');
            loadCategories();
        } catch (error) {
            message.error('Lỗi khi xóa danh mục');
        }
    };

    // Filter categories based on search and filters
    const filteredCategories = categories.filter(category => {
        const matchesSearch = !filters.search ||
            category.name.toLowerCase().includes(filters.search.toLowerCase()) ||
            category.description?.toLowerCase().includes(filters.search.toLowerCase());

        const matchesType = !filters.type || category.type === filters.type;
        const matchesActive = filters.active === null || category.active === filters.active;

        return matchesSearch && matchesType && matchesActive;
    });

    // Table columns
    const columns = [
        {
            title: 'Tên danh mục',
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
            title: 'Loại',
            dataIndex: 'type',
            key: 'type',
            render: (type) => {
                const typeColors = {
                    'Food': 'blue',
                    'Drink': 'green',
                    'Dessert': 'purple',
                    'Combo': 'orange'
                };
                return <Tag color={typeColors[type] || 'default'}>{type}</Tag>;
            },
        },
        {
            title: 'Số món ăn',
            dataIndex: 'menuItemCount',
            key: 'menuItemCount',
            render: (count) => count || 0,
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
                        title="Bạn có chắc muốn xóa danh mục này?"
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
            case 'create': return 'Tạo danh mục mới';
            case 'edit': return 'Chỉnh sửa danh mục';
            case 'view': return 'Chi tiết danh mục';
            default: return 'Thao tác';
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-2xl font-bold text-gray-800 mb-2">Quản lý danh mục</h1>
                    <p className="text-gray-600">Quản lý các danh mục món ăn</p>
                </div>
                <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={() => showModal('create')}
                    className="restaurant-button"
                >
                    Thêm danh mục
                </Button>
            </div>

            {/* Statistics Cards */}
            <Row gutter={[16, 16]}>
                <Col xs={24} sm={8}>
                    <Card className="restaurant-card">
                        <Statistic
                            title="Tổng danh mục"
                            value={categories.length}
                            prefix={<BookOutlined />}
                            valueStyle={{ color: '#1890ff' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={8}>
                    <Card className="restaurant-card">
                        <Statistic
                            title="Danh mục hoạt động"
                            value={categories.filter(cat => cat.active).length}
                            prefix={<EyeOutlined />}
                            valueStyle={{ color: '#52c41a' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={8}>
                    <Card className="restaurant-card">
                        <Statistic
                            title="Tổng món ăn"
                            value={categories.reduce((sum, cat) => sum + (cat.menuItemCount || 0), 0)}
                            prefix={<BookOutlined />}
                            valueStyle={{ color: '#f59e0b' }}
                        />
                    </Card>
                </Col>
            </Row>

            {/* Filters */}
            <Card className="restaurant-card">
                <Row gutter={[16, 16]} align="middle">
                    <Col xs={24} sm={8}>
                        <Search
                            placeholder="Tìm kiếm danh mục..."
                            onSearch={handleSearch}
                            enterButton={<SearchOutlined />}
                        />
                    </Col>
                    <Col xs={24} sm={6}>
                        <Select
                            placeholder="Loại danh mục"
                            style={{ width: '100%' }}
                            allowClear
                            onChange={(value) => handleFilterChange('type', value)}
                        >
                            <Option value="Food">Món ăn</Option>
                            <Option value="Drink">Đồ uống</Option>
                            <Option value="Dessert">Tráng miệng</Option>
                            <Option value="Combo">Combo</Option>
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
                            onClick={() => loadCategories()}
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
                    dataSource={filteredCategories}
                    rowKey={(record) => record.id || record.categoryId}
                    loading={loading}
                    pagination={{
                        ...pagination,
                        showSizeChanger: true,
                        showQuickJumper: true,
                        showTotal: (total, range) =>
                            `${range[0]}-${range[1]} của ${total} mục`,
                        pageSizeOptions: PAGINATION.PAGE_SIZE_OPTIONS,
                    }}
                    scroll={{ x: 800 }}
                />
            </Card>

            {/* Modal */}
            <Modal
                title={getModalTitle()}
                open={modalVisible}
                onOk={modalType !== 'view' ? handleModalOk : undefined}
                onCancel={() => setModalVisible(false)}
                width={600}
                okText="Lưu"
                cancelText="Hủy"
                footer={modalType === 'view' ? [
                    <Button key="close" onClick={() => setModalVisible(false)}>
                        Đóng
                    </Button>
                ] : undefined}
            >
                <Form form={form} layout="vertical">
                    <Form.Item
                        name="name"
                        label="Tên danh mục"
                        rules={[{ required: true, message: 'Vui lòng nhập tên danh mục' }]}
                    >
                        <Input />
                    </Form.Item>

                    <Form.Item
                        name="description"
                        label="Mô tả"
                    >
                        <TextArea rows={3} />
                    </Form.Item>

                    <Form.Item
                        name="type"
                        label="Loại danh mục"
                        rules={[{ required: true, message: 'Vui lòng chọn loại danh mục' }]}
                    >
                        <Select>
                            <Option value="Food">Món ăn</Option>
                            <Option value="Drink">Đồ uống</Option>
                            <Option value="Dessert">Tráng miệng</Option>
                            <Option value="Combo">Combo</Option>
                        </Select>
                    </Form.Item>

                    <Form.Item
                        name="active"
                        label="Trạng thái"
                        valuePropName="checked"
                        initialValue={true}
                    >
                        <Switch checkedChildren="Hoạt động" unCheckedChildren="Tạm dừng" />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default CategoryManagement;
