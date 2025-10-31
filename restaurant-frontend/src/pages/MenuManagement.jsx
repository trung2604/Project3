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
    Upload,
    Image,
    Switch,
    Divider
} from 'antd';
import {
    PlusOutlined,
    EditOutlined,
    SearchOutlined,
    ReloadOutlined,
    DeleteOutlined,
    ShoppingCartOutlined,
    DollarOutlined,
    EyeOutlined,
    SettingOutlined,
    UploadOutlined
} from '@ant-design/icons';
import { menuService } from '../services/menuService';
import { inventoryService } from '../services/inventoryService';
import { cloudinaryService } from '../services/cloudinaryService';
import { PAGINATION } from '../constants.js';

const { Option } = Select;
const { Search } = Input;
const { TextArea } = Input;

const MenuManagement = () => {
    const { message } = App.useApp();
    const [menuItems, setMenuItems] = useState([]);
    const [categories, setCategories] = useState([]);
    const [ingredients, setIngredients] = useState([]);
    const [loading, setLoading] = useState(false);
    const [pagination, setPagination] = useState({
        current: 1,
        pageSize: PAGINATION.DEFAULT_PAGE_SIZE,
        total: 0
    });
    const [filters, setFilters] = useState({
        categoryId: '',
        active: null,
        search: ''
    });
    const [modalVisible, setModalVisible] = useState(false);
    const [modalType, setModalType] = useState('create'); // create, edit, ingredients
    const [selectedMenuItem, setSelectedMenuItem] = useState(null);
    const [form] = Form.useForm();
    const [uploading, setUploading] = useState(false);
    const [imagePreview, setImagePreview] = useState(null);

    // Load menu items data
    const loadMenuItems = async (page = 1, size = PAGINATION.DEFAULT_PAGE_SIZE) => {
        setLoading(true);
        try {
            const params = {
                page: page - 1, // Backend uses 0-based pagination
                size,
                // Only include non-empty filters
                ...(filters.categoryId && filters.categoryId.trim() !== '' && { categoryId: filters.categoryId }),
                ...(filters.active !== null && filters.active !== undefined && { active: filters.active }),
                ...(filters.search && filters.search.trim() !== '' && { search: filters.search })
            };

            const response = await menuService.getMenuItems(params);
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
            setCategories(response.data || []);
        } catch (error) {
            console.error('Error loading categories:', error);
        }
    };

    // Load ingredients
    const loadIngredients = async () => {
        try {
            const response = await inventoryService.getIngredients({ size: 1000 });
            setIngredients(response.data.ingredients || []);
        } catch (error) {
            console.error('Error loading ingredients:', error);
        }
    };

    useEffect(() => {
        loadMenuItems();
        loadCategories();
        loadIngredients();
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
        setImagePreview(null);

        if (type === 'edit' && menuItem) {
            form.setFieldsValue({
                ...menuItem,
                categoryId: menuItem.categoryId?.toString(),
                imageUrl: menuItem.imageUrl || undefined,
                imagePublicId: menuItem.imagePublicId || undefined
            });
            if (menuItem.imageUrl) {
                setImagePreview(menuItem.imageUrl);
            }
        } else if (type === 'ingredients' && menuItem) {
            form.setFieldsValue({
                ingredients: menuItem.ingredients || []
            });
        } else {
            form.resetFields();
        }
    };

    const handleModalOk = async () => {
        try {
            const values = await form.validateFields();
            if (modalType === 'create' && !values.imageUrl) {
                message.error('Vui lòng upload hình ảnh trước khi lưu');
                return;
            }
            const payload = {
                ...values,
                categoryId: values.categoryId != null ? String(values.categoryId) : values.categoryId
            };

            if (modalType === 'create') {
                await menuService.createMenuItem(payload);
                message.success('Tạo món ăn thành công');
            } else if (modalType === 'edit') {
                await menuService.updateMenuItem(selectedMenuItem.menuItemId, payload);
                message.success('Cập nhật món ăn thành công');
            } else if (modalType === 'ingredients') {
                await menuService.updateMenuItemIngredients(selectedMenuItem.menuItemId, values.ingredients);
                message.success('Cập nhật nguyên liệu thành công');
            }

            setModalVisible(false);
            loadMenuItems(pagination.current, pagination.pageSize);
        } catch (error) {
            message.error('Có lỗi xảy ra khi thực hiện thao tác');
            console.error('Error:', error);
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

    // Delete menu item
    const handleDeleteMenuItem = async (id) => {
        try {
            await menuService.deleteMenuItem(id);
            message.success('Xóa món ăn thành công');
            loadMenuItems(pagination.current, pagination.pageSize);
        } catch (error) {
            message.error('Lỗi khi xóa món ăn');
        }
    };

    // Update price
    const handleUpdatePrice = async (id, price) => {
        try {
            await menuService.updateMenuItemPrice(id, price);
            message.success('Cập nhật giá thành công');
            loadMenuItems(pagination.current, pagination.pageSize);
        } catch (error) {
            message.error('Lỗi khi cập nhật giá');
        }
    };

    // Handle image upload
    const handleImageUpload = async (file) => {
        setUploading(true);
        try {
            const result = await cloudinaryService.uploadImage(file);

            form.setFieldsValue({
                imageUrl: result.url,
                imagePublicId: result.publicId
            });
            setImagePreview(result.url);
            message.success('Upload ảnh thành công');
            return false; // Prevent default upload
        } catch (error) {
            console.error('Upload error:', error);
            message.error('Lỗi khi upload ảnh: ' + error.message);
            return false;
        } finally {
            setUploading(false);
        }
    };

    // Handle image preview
    const handleImagePreview = (file) => {
        const reader = new FileReader();
        reader.onload = (e) => {
            setImagePreview(e.target.result);
        };
        reader.readAsDataURL(file);
        return false; // Prevent default upload
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
                    fallback="/placeholder-food.jpg"
                    style={{ objectFit: 'cover', borderRadius: 8 }}
                />
            ),
        },
        {
            title: 'Tên món ăn',
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
            dataIndex: 'categoryName',
            key: 'categoryName',
            render: (categoryName) => <Tag color="blue">{categoryName}</Tag>,
        },
        {
            title: 'Giá',
            dataIndex: 'price',
            key: 'price',
            render: (price, record) => (
                <div>
                    <div style={{ fontWeight: '500', fontSize: '14px' }}>{price?.toLocaleString()} VNĐ</div>
                    <div style={{ fontSize: '12px', color: '#999', marginTop: '2px' }}>
                        {record.ingredients?.length || 0} nguyên liệu
                    </div>
                </div>
            ),
        },
        {
            title: 'Công thức',
            dataIndex: 'recipe',
            key: 'recipe',
            width: 200,
            render: (recipe) => (
                <div style={{ maxWidth: '200px' }}>
                    {recipe ? (
                        <div
                            style={{
                                fontSize: '12px',
                                color: '#666',
                                cursor: 'pointer',
                                lineHeight: '1.4'
                            }}
                            title={recipe}
                        >
                            {recipe.length > 50 ? `${recipe.substring(0, 50)}...` : recipe}
                        </div>
                    ) : (
                        <span style={{ color: '#999', fontSize: '12px' }}>Chưa có công thức</span>
                    )}
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
                    onChange={(checked) => handleToggleActive(record.menuItemId, checked)}
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
                        icon={<EditOutlined />}
                        onClick={() => showModal('edit', record)}
                    >
                        Sửa
                    </Button>
                    <Button
                        size="small"
                        icon={<ShoppingCartOutlined />}
                        onClick={() => showModal('ingredients', record)}
                    >
                        Nguyên liệu
                    </Button>
                    {record.recipe && (
                        <Button
                            size="small"
                            icon={<EyeOutlined />}
                            onClick={() => {
                                Modal.info({
                                    title: `Công thức: ${record.name}`,
                                    content: (
                                        <div style={{ whiteSpace: 'pre-wrap', maxHeight: '400px', overflow: 'auto' }}>
                                            {record.recipe}
                                        </div>
                                    ),
                                    width: 600,
                                });
                            }}
                        >
                            Công thức
                        </Button>
                    )}
                    <Button
                        size="small"
                        icon={<DollarOutlined />}
                        onClick={() => {
                            const newPrice = prompt('Nhập giá mới:', record.price);
                            if (newPrice && !isNaN(newPrice)) {
                                handleUpdatePrice(record.menuItemId, parseFloat(newPrice));
                            }
                        }}
                    >
                        Giá
                    </Button>
                    <Popconfirm
                        title="Bạn có chắc muốn xóa món ăn này?"
                        onConfirm={() => handleDeleteMenuItem(record.menuItemId)}
                        okText="Xóa"
                        cancelText="Hủy"
                    >
                        <Button
                            size="small"
                            icon={<DeleteOutlined />}
                            danger
                        >
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
            case 'ingredients': return 'Quản lý nguyên liệu';
            default: return 'Thao tác';
        }
    };

    // Get form fields based on modal type
    const getFormFields = () => {
        if (modalType === 'ingredients') {
            return (
                <>
                    <Form.Item
                        name="ingredients"
                        label="Nguyên liệu"
                        rules={[{ required: true, message: 'Vui lòng chọn ít nhất một nguyên liệu' }]}
                    >
                        <Select
                            mode="multiple"
                            placeholder="Chọn nguyên liệu"
                            style={{ width: '100%' }}
                            optionLabelProp="label"
                        >
                            {ingredients.map(ingredient => (
                                <Option
                                    key={ingredient.ingredientId}
                                    value={ingredient.ingredientId}
                                    label={ingredient.name}
                                >
                                    <div>
                                        <div className="font-medium">{ingredient.name}</div>
                                        <div className="text-sm text-gray-500">
                                            Tồn kho: {ingredient.currentStock} {ingredient.unit}
                                        </div>
                                    </div>
                                </Option>
                            ))}
                        </Select>
                    </Form.Item>
                </>
            );
        }

        return (
            <>
                <Form.Item
                    name="name"
                    label="Tên món ăn"
                    rules={[{ required: true, message: 'Vui lòng nhập tên món ăn' }]}
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
                    <Col span={12}>
                        <Form.Item
                            name="categoryId"
                            label="Danh mục"
                            rules={[{ required: true, message: 'Vui lòng chọn danh mục' }]}
                        >
                            <Select placeholder="Chọn danh mục">
                                {categories.map(category => (
                                    <Option key={category.categoryId} value={category.categoryId}>
                                        {category.name}
                                    </Option>
                                ))}
                            </Select>
                        </Form.Item>
                    </Col>
                    <Col span={12}>
                        <Form.Item
                            name="price"
                            label="Giá (VNĐ)"
                            rules={[{ required: true, message: 'Vui lòng nhập giá' }]}
                        >
                            <InputNumber min={0} style={{ width: '100%' }} />
                        </Form.Item>
                    </Col>
                </Row>
                <div>
                    <div style={{ marginBottom: 8 }}>Hình ảnh</div>
                    <Upload
                        beforeUpload={handleImageUpload}
                        onPreview={handleImagePreview}
                        showUploadList={false}
                        accept="image/*"
                        listType="picture-card"
                        className="image-uploader"
                    >
                        {imagePreview ? (
                            <img src={imagePreview} alt="preview" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                        ) : (
                            <div>
                                <UploadOutlined />
                                <div style={{ marginTop: 8 }}>Upload</div>
                            </div>
                        )}
                    </Upload>
                </div>
                {uploading && <div style={{ marginTop: 8, color: '#1890ff' }}>Đang upload...</div>}
                {/* Hidden fields for uploaded image metadata */}
                <Form.Item name="imageUrl" style={{ display: 'none' }} rules={[{ required: true, message: 'Vui lòng upload hình ảnh' }]}>
                    <Input />
                </Form.Item>
                <Form.Item name="imagePublicId" style={{ display: 'none' }}>
                    <Input />
                </Form.Item>
                <Form.Item
                    name="preparationTime"
                    label="Thời gian chuẩn bị (phút)"
                >
                    <InputNumber min={0} style={{ width: '100%' }} />
                </Form.Item>
                <Form.Item
                    name="recipe"
                    label="Công thức món ăn"
                >
                    <TextArea
                        rows={6}
                        placeholder="Nhập các bước chế biến món ăn..."
                        showCount
                        maxLength={2000}
                    />
                </Form.Item>
                <Form.Item
                    name="active"
                    label="Trạng thái"
                    valuePropName="checked"
                    initialValue={true}
                >
                    <Switch checkedChildren="Bán" unCheckedChildren="Tạm dừng" />
                </Form.Item>
            </>
        );
    };

    // Get statistics
    const getStatistics = () => {
        const totalItems = menuItems.length;
        const activeItems = menuItems.filter(item => item.active).length;
        const inactiveItems = menuItems.filter(item => !item.active).length;
        const avgPrice = menuItems.length > 0
            ? menuItems.reduce((sum, item) => sum + (item.price || 0), 0) / menuItems.length
            : 0;

        return {
            totalItems,
            activeItems,
            inactiveItems,
            avgPrice
        };
    };

    const stats = getStatistics();

    return (
        <div style={{ padding: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
                <div>
                    <h1 style={{ fontSize: '24px', fontWeight: 'bold', color: '#1f2937', marginBottom: '8px' }}>Quản lý thực đơn</h1>
                    <p style={{ color: '#6b7280' }}>Quản lý món ăn và thực đơn</p>
                </div>
                <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={() => showModal('create')}
                >
                    Thêm món ăn
                </Button>
            </div>

            {/* Statistics Cards */}
            <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
                <Col xs={24} sm={6}>
                    <Card>
                        <Statistic
                            title="Tổng món ăn"
                            value={stats.totalItems}
                            prefix={<ShoppingCartOutlined />}
                            valueStyle={{ color: '#1890ff' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={6}>
                    <Card>
                        <Statistic
                            title="Đang bán"
                            value={stats.activeItems}
                            prefix={<EyeOutlined />}
                            valueStyle={{ color: '#52c41a' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={6}>
                    <Card>
                        <Statistic
                            title="Tạm dừng"
                            value={stats.inactiveItems}
                            prefix={<SettingOutlined />}
                            valueStyle={{ color: '#fa8c16' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={6}>
                    <Card>
                        <Statistic
                            title="Giá trung bình"
                            value={stats.avgPrice}
                            prefix={<DollarOutlined />}
                            valueStyle={{ color: '#722ed1' }}
                            formatter={(value) => `${Math.round(value).toLocaleString()} VNĐ`}
                        />
                    </Card>
                </Col>
            </Row>

            {/* Filters */}
            <Card style={{ marginBottom: '24px' }}>
                <Row gutter={[16, 16]} align="middle">
                    <Col xs={24} sm={8}>
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
                                <Option key={category.categoryId} value={category.categoryId}>
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
                    <Col xs={24} sm={4}>
                        <Button
                            icon={<ReloadOutlined />}
                            onClick={() => loadMenuItems()}
                            style={{ width: '100%' }}
                        >
                            Làm mới
                        </Button>
                    </Col>
                </Row>
            </Card>

            {/* Table */}
            <Card>
                <Table
                    columns={columns}
                    dataSource={menuItems}
                    rowKey="menuItemId"
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
                <Form
                    form={form}
                    layout="vertical"
                    preserve={false}
                >
                    {getFormFields()}
                </Form>
            </Modal>
        </div>
    );
};

export default MenuManagement;