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
    DatePicker,
    Modal,
    Form,
    InputNumber,
    message,
    Descriptions,
    Divider,
    Popconfirm
} from 'antd';
import {
    ReloadOutlined,
    SearchOutlined,
    PlusOutlined,
    EyeOutlined,
    CheckCircleOutlined,
    ClockCircleOutlined,
    CloseCircleOutlined,
    TruckOutlined,
    ShopOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';
import apiService from '../services/apiService';
import { PAGINATION, ORDER_TYPES, ORDER_STATUS, DATETIME_FORMAT } from '../constants.js';
import { useAuth } from '../context/AuthContext';
import { canManageMenu } from '../utils/auth';
import Loading from '../components/Common/Loading';
import ErrorPage from '../components/Common/ErrorPage';

const { Option } = Select;
const { Search } = Input;
const { RangePicker } = DatePicker;

const OrderManagement = () => {
    const { message: antdMessage } = App.useApp();
    const { role, user, loading: authLoading } = useAuth();
    const [form] = Form.useForm();
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(false);
    const [filters, setFilters] = useState({
        status: '',
        type: '',
        startDate: null,
        endDate: null
    });
    const [selectedOrder, setSelectedOrder] = useState(null);
    const [detailModalVisible, setDetailModalVisible] = useState(false);
    const [createModalVisible, setCreateModalVisible] = useState(false);
    const [menuItems, setMenuItems] = useState([]);

    const canManage = canManageMenu(role);
    const isCustomer = role === 'CUSTOMER';

    useEffect(() => {
        // Wait for auth to finish loading before making API calls
        if (authLoading) {
            return;
        }

        // Check if user is authenticated - ProtectedRoute will handle redirect if not
        const token = localStorage.getItem('accessToken');
        if (!token) {
            // Token not found, ProtectedRoute will redirect
            return;
        }

        // Only load orders if we have a valid token
        loadOrders();
        if (canManage) {
            loadMenuItems();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [filters, role, authLoading]);

    const loadOrders = async () => {
        // Check authentication before making request
        const token = localStorage.getItem('accessToken');
        if (!token) {
            console.warn('No access token, cannot load orders');
            return;
        }

        setLoading(true);
        try {
            const params = {
                ...(filters.status && { status: filters.status }),
                ...(filters.type && { type: filters.type }),
                ...(filters.startDate && { startDate: filters.startDate }),
                ...(filters.endDate && { endDate: filters.endDate }),
                ...(isCustomer && user?.userId && { customerId: user.userId })
            };

            const response = await apiService.order.getAllOrders(params);
            setOrders(Array.isArray(response) ? response : []);
        } catch (error) {
            console.error('Error loading orders:', error);
            // Don't show error message if it's a 401 and we're redirecting
            if (error.response?.status !== 401) {
                antdMessage.error('Không thể tải danh sách đơn hàng');
            }
        } finally {
            setLoading(false);
        }
    };

    const loadMenuItems = async () => {
        try {
            const response = await apiService.menu.getMenuItems({ page: 0, size: 1000, active: true });
            setMenuItems(response?.items || []);
        } catch (error) {
            console.error('Error loading menu items:', error);
        }
    };

    const getStatusTag = (status) => {
        const statusConfig = {
            PENDING: { color: 'orange', icon: <ClockCircleOutlined />, text: 'Chờ xử lý' },
            COOKING: { color: 'blue', icon: <ClockCircleOutlined />, text: 'Đang chế biến' },
            READY: { color: 'cyan', icon: <CheckCircleOutlined />, text: 'Sẵn sàng' },
            DELIVERING: { color: 'purple', icon: <TruckOutlined />, text: 'Đang giao' },
            COMPLETED: { color: 'green', icon: <CheckCircleOutlined />, text: 'Hoàn thành' },
            CANCELLED: { color: 'red', icon: <CloseCircleOutlined />, text: 'Đã hủy' }
        };
        const config = statusConfig[status] || { color: 'default', text: status };
        return <Tag color={config.color} icon={config.icon}>{config.text}</Tag>;
    };

    const getTypeTag = (type) => {
        const typeConfig = {
            DINE_IN: { color: 'blue', text: 'Ăn tại chỗ' },
            TAKEAWAY: { color: 'orange', text: 'Mang đi' },
            DELIVERY: { color: 'green', text: 'Giao hàng' }
        };
        const config = typeConfig[type] || { color: 'default', text: type };
        return <Tag color={config.color}>{config.text}</Tag>;
    };

    const handleStatusUpdate = async (orderId, action) => {
        try {
            let response;
            switch (action) {
                case 'start-cooking':
                    response = await apiService.order.startCooking(orderId);
                    break;
                case 'mark-ready':
                    response = await apiService.order.markReady(orderId);
                    break;
                case 'start-delivering':
                    response = await apiService.order.startDelivering(orderId);
                    break;
                case 'complete':
                    response = await apiService.order.completeOrder(orderId);
                    break;
                default:
                    return;
            }
            antdMessage.success('Cập nhật trạng thái thành công');
            loadOrders();
        } catch (error) {
            console.error('Error updating status:', error);
            antdMessage.error('Không thể cập nhật trạng thái');
        }
    };

    const handleCancelOrder = async (orderId, reason) => {
        try {
            await apiService.order.cancelOrder(orderId, reason, canManage);
            antdMessage.success('Hủy đơn hàng thành công');
            loadOrders();
        } catch (error) {
            console.error('Error cancelling order:', error);
            antdMessage.error('Không thể hủy đơn hàng');
        }
    };

    const handleCreateOrder = async (values) => {
        try {
            // Process order items: load prices and calculate subtotals
            const processedOrderItems = values.orderItems.map((item) => {
                const menuItem = menuItems.find(m => m.menuItemId === item.menuItemId);
                if (!menuItem) {
                    throw new Error(`Menu item not found: ${item.menuItemId}`);
                }
                return {
                    menuItemId: item.menuItemId,
                    name: menuItem.name,
                    quantity: item.quantity,
                    unitPrice: menuItem.price,
                    subtotal: menuItem.price * item.quantity,
                    notes: item.notes || ''
                };
            });

            const orderData = {
                orderType: values.orderType,
                customerName: values.customerName,
                customerPhone: values.customerPhone,
                orderItems: processedOrderItems,
                discountPercentage: values.discountPercentage || 0,
                discountAmount: values.discountAmount || 0,
                vatPercentage: values.vatPercentage || 10,
                deliveryAddress: values.deliveryAddress || null,
                tableNumber: values.tableNumber || null,
                notes: values.notes || null
            };

            await apiService.order.createOrder(orderData);
            antdMessage.success('Tạo đơn hàng thành công');
            setCreateModalVisible(false);
            form.resetFields();
            loadOrders();
        } catch (error) {
            console.error('Error creating order:', error);
            antdMessage.error(error.message || 'Không thể tạo đơn hàng');
        }
    };

    const showOrderDetail = async (orderId) => {
        try {
            const order = await apiService.order.getOrderById(orderId);
            setSelectedOrder(order);
            setDetailModalVisible(true);
        } catch (error) {
            console.error('Error loading order detail:', error);
            antdMessage.error('Không thể tải chi tiết đơn hàng');
        }
    };

    const getActionButtons = (order) => {
        const buttons = [];
        const { orderStatus, orderType } = order;

        if (canManage) {
            if (orderStatus === 'PENDING') {
                buttons.push(
                    <Button
                        key="start-cooking"
                        type="primary"
                        size="small"
                        onClick={() => handleStatusUpdate(order.orderId, 'start-cooking')}
                    >
                        Bắt đầu nấu
                    </Button>
                );
            }
            if (orderStatus === 'COOKING') {
                buttons.push(
                    <Button
                        key="mark-ready"
                        type="primary"
                        size="small"
                        onClick={() => handleStatusUpdate(order.orderId, 'mark-ready')}
                    >
                        Đánh dấu sẵn sàng
                    </Button>
                );
            }
            if (orderStatus === 'READY' && orderType === 'DELIVERY') {
                buttons.push(
                    <Button
                        key="start-delivering"
                        type="primary"
                        size="small"
                        onClick={() => handleStatusUpdate(order.orderId, 'start-delivering')}
                    >
                        Bắt đầu giao hàng
                    </Button>
                );
            }
            if (orderStatus === 'READY' || orderStatus === 'DELIVERING') {
                buttons.push(
                    <Button
                        key="complete"
                        type="primary"
                        size="small"
                        onClick={() => handleStatusUpdate(order.orderId, 'complete')}
                    >
                        Hoàn thành
                    </Button>
                );
            }
        }

        if ((orderStatus === 'PENDING' || orderStatus === 'COOKING') && (isCustomer || canManage)) {
            buttons.push(
                <Popconfirm
                    key="cancel"
                    title="Xác nhận hủy đơn hàng"
                    description="Bạn có chắc chắn muốn hủy đơn hàng này?"
                    onConfirm={() => {
                        Modal.confirm({
                            title: 'Lý do hủy đơn',
                            content: (
                                <Input.TextArea
                                    placeholder="Nhập lý do hủy đơn..."
                                    rows={4}
                                    id="cancellation-reason"
                                />
                            ),
                            onOk: () => {
                                const reason = document.getElementById('cancellation-reason').value;
                                handleCancelOrder(order.orderId, reason || 'Không có lý do');
                            }
                        });
                    }}
                    okText="Xác nhận"
                    cancelText="Hủy"
                >
                    <Button danger size="small">Hủy đơn</Button>
                </Popconfirm>
            );
        }

        return buttons.length > 0 ? <Space>{buttons}</Space> : null;
    };

    const columns = [
        {
            title: 'Mã đơn',
            dataIndex: 'orderId',
            key: 'orderId',
            width: 200,
            render: (text) => <strong>{text.substring(0, 8)}...</strong>
        },
        {
            title: 'Khách hàng',
            key: 'customer',
            width: 150,
            render: (_, record) => (
                <div>
                    <div>{record.customerName || 'N/A'}</div>
                    <small style={{ color: '#999' }}>{record.customerPhone || ''}</small>
                </div>
            )
        },
        {
            title: 'Loại',
            dataIndex: 'orderType',
            key: 'orderType',
            width: 100,
            render: getTypeTag
        },
        {
            title: 'Trạng thái',
            dataIndex: 'orderStatus',
            key: 'orderStatus',
            width: 120,
            render: getStatusTag
        },
        {
            title: 'Tổng tiền',
            dataIndex: 'totalAmount',
            key: 'totalAmount',
            width: 120,
            align: 'right',
            render: (amount) => amount ? new Intl.NumberFormat('vi-VN').format(amount) + ' đ' : '0 đ'
        },
        {
            title: 'Ngày tạo',
            dataIndex: 'orderDate',
            key: 'orderDate',
            width: 150,
            render: (date) => date ? dayjs(date).format(DATETIME_FORMAT) : '-'
        },
        {
            title: 'Thao tác',
            key: 'action',
            width: 200,
            fixed: 'right',
            render: (_, record) => (
                <Space>
                    <Button
                        type="link"
                        icon={<EyeOutlined />}
                        onClick={() => showOrderDetail(record.orderId)}
                    >
                        Chi tiết
                    </Button>
                    {getActionButtons(record)}
                </Space>
            )
        }
    ];

    if (authLoading) {
        return <Loading tip="Đang kiểm tra quyền truy cập..." />;
    }

    const totalRevenue = orders
        .filter(o => o.orderStatus === 'COMPLETED')
        .reduce((sum, o) => sum + (o.totalAmount || 0), 0);

    const pendingCount = orders.filter(o => o.orderStatus === 'PENDING').length;
    const cookingCount = orders.filter(o => o.orderStatus === 'COOKING').length;
    const completedCount = orders.filter(o => o.orderStatus === 'COMPLETED').length;

    return (
        <div style={{ padding: '24px' }}>
            <Row gutter={16} style={{ marginBottom: '24px' }}>
                <Col xs={24} sm={12} md={6}>
                    <Card>
                        <Statistic
                            title="Đơn chờ xử lý"
                            value={pendingCount}
                            prefix={<ClockCircleOutlined />}
                            valueStyle={{ color: '#faad14' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={12} md={6}>
                    <Card>
                        <Statistic
                            title="Đang chế biến"
                            value={cookingCount}
                            prefix={<ClockCircleOutlined />}
                            valueStyle={{ color: '#1890ff' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={12} md={6}>
                    <Card>
                        <Statistic
                            title="Đã hoàn thành"
                            value={completedCount}
                            prefix={<CheckCircleOutlined />}
                            valueStyle={{ color: '#52c41a' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={12} md={6}>
                    <Card>
                        <Statistic
                            title="Tổng doanh thu"
                            value={totalRevenue}
                            prefix="đ"
                            precision={0}
                            valueStyle={{ color: '#52c41a' }}
                        />
                    </Card>
                </Col>
            </Row>

            <Card>
                <Space style={{ marginBottom: '16px', width: '100%', justifyContent: 'space-between' }}>
                    <Space>
                        <Search
                            placeholder="Tìm kiếm..."
                            allowClear
                            style={{ width: 300 }}
                            onSearch={(value) => {
                                // Implement search if needed
                            }}
                        />
                        <Select
                            placeholder="Trạng thái"
                            allowClear
                            style={{ width: 150 }}
                            value={filters.status}
                            onChange={(value) => setFilters({ ...filters, status: value })}
                        >
                            {Object.entries(ORDER_STATUS).map(([key, value]) => (
                                <Option key={key} value={value}>{getStatusTag(value).props.children}</Option>
                            ))}
                        </Select>
                        <Select
                            placeholder="Loại đơn"
                            allowClear
                            style={{ width: 150 }}
                            value={filters.type}
                            onChange={(value) => setFilters({ ...filters, type: value })}
                        >
                            {Object.entries(ORDER_TYPES).map(([key, value]) => (
                                <Option key={key} value={value}>{getTypeTag(value).props.children}</Option>
                            ))}
                        </Select>
                        <RangePicker
                            placeholder={['Từ ngày', 'Đến ngày']}
                            onChange={(dates) => {
                                if (dates) {
                                    setFilters({
                                        ...filters,
                                        startDate: dates[0]?.format('YYYY-MM-DDTHH:mm:ss'),
                                        endDate: dates[1]?.format('YYYY-MM-DDTHH:mm:ss')
                                    });
                                } else {
                                    setFilters({
                                        ...filters,
                                        startDate: null,
                                        endDate: null
                                    });
                                }
                            }}
                        />
                    </Space>
                    <Space>
                        {canManage && (
                            <Button
                                type="primary"
                                icon={<PlusOutlined />}
                                onClick={() => setCreateModalVisible(true)}
                            >
                                Tạo đơn hàng
                            </Button>
                        )}
                        <Button icon={<ReloadOutlined />} onClick={loadOrders}>
                            Làm mới
                        </Button>
                    </Space>
                </Space>

                <Table
                    columns={columns}
                    dataSource={orders}
                    rowKey="orderId"
                    loading={loading}
                    scroll={{ x: 1200 }}
                    pagination={{
                        defaultPageSize: PAGINATION.DEFAULT_PAGE_SIZE,
                        pageSizeOptions: PAGINATION.PAGE_SIZE_OPTIONS,
                        showSizeChanger: true,
                        showTotal: (total) => `Tổng cộng: ${total} đơn hàng`
                    }}
                />
            </Card>

            <Modal
                title="Chi tiết đơn hàng"
                open={detailModalVisible}
                onCancel={() => setDetailModalVisible(false)}
                footer={null}
                width={800}
            >
                {selectedOrder && (
                    <div>
                        <Descriptions bordered column={2}>
                            <Descriptions.Item label="Mã đơn">{selectedOrder.orderId}</Descriptions.Item>
                            <Descriptions.Item label="Trạng thái">
                                {getStatusTag(selectedOrder.orderStatus)}
                            </Descriptions.Item>
                            <Descriptions.Item label="Loại đơn">
                                {getTypeTag(selectedOrder.orderType)}
                            </Descriptions.Item>
                            <Descriptions.Item label="Khách hàng">
                                {selectedOrder.customerName} - {selectedOrder.customerPhone}
                            </Descriptions.Item>
                            {selectedOrder.tableNumber && (
                                <Descriptions.Item label="Số bàn">
                                    {selectedOrder.tableNumber}
                                </Descriptions.Item>
                            )}
                            {selectedOrder.deliveryAddress && (
                                <Descriptions.Item label="Địa chỉ giao hàng" span={2}>
                                    {selectedOrder.deliveryAddress}
                                </Descriptions.Item>
                            )}
                            <Descriptions.Item label="Ngày tạo">
                                {selectedOrder.orderDate ? dayjs(selectedOrder.orderDate).format(DATETIME_FORMAT) : '-'}
                            </Descriptions.Item>
                            {selectedOrder.cookingStartTime && (
                                <Descriptions.Item label="Bắt đầu nấu">
                                    {dayjs(selectedOrder.cookingStartTime).format(DATETIME_FORMAT)}
                                </Descriptions.Item>
                            )}
                            {selectedOrder.readyTime && (
                                <Descriptions.Item label="Sẵn sàng">
                                    {dayjs(selectedOrder.readyTime).format(DATETIME_FORMAT)}
                                </Descriptions.Item>
                            )}
                            {selectedOrder.completedTime && (
                                <Descriptions.Item label="Hoàn thành">
                                    {dayjs(selectedOrder.completedTime).format(DATETIME_FORMAT)}
                                </Descriptions.Item>
                            )}
                        </Descriptions>

                        <Divider>Danh sách món ăn</Divider>
                        <Table
                            dataSource={selectedOrder.orderItems || []}
                            rowKey={(record, index) => index}
                            pagination={false}
                            columns={[
                                { title: 'Món ăn', dataIndex: 'name', key: 'name' },
                                { title: 'Số lượng', dataIndex: 'quantity', key: 'quantity', align: 'center' },
                                {
                                    title: 'Đơn giá',
                                    dataIndex: 'unitPrice',
                                    key: 'unitPrice',
                                    align: 'right',
                                    render: (price) => new Intl.NumberFormat('vi-VN').format(price) + ' đ'
                                },
                                {
                                    title: 'Thành tiền',
                                    dataIndex: 'subtotal',
                                    key: 'subtotal',
                                    align: 'right',
                                    render: (subtotal) => new Intl.NumberFormat('vi-VN').format(subtotal) + ' đ'
                                }
                            ]}
                        />

                        <Divider />
                        <Descriptions bordered>
                            <Descriptions.Item label="Tạm tính">
                                {new Intl.NumberFormat('vi-VN').format(selectedOrder.subtotal || 0)} đ
                            </Descriptions.Item>
                            {selectedOrder.discountAmount > 0 && (
                                <Descriptions.Item label="Giảm giá">
                                    -{new Intl.NumberFormat('vi-VN').format(selectedOrder.discountAmount)} đ
                                </Descriptions.Item>
                            )}
                            {selectedOrder.vatAmount > 0 && (
                                <Descriptions.Item label="VAT">
                                    +{new Intl.NumberFormat('vi-VN').format(selectedOrder.vatAmount)} đ
                                </Descriptions.Item>
                            )}
                            <Descriptions.Item label="Tổng tiền" style={{ fontWeight: 'bold', fontSize: '16px' }}>
                                {new Intl.NumberFormat('vi-VN').format(selectedOrder.totalAmount || 0)} đ
                            </Descriptions.Item>
                        </Descriptions>

                        {getActionButtons(selectedOrder) && (
                            <>
                                <Divider />
                                <Space>{getActionButtons(selectedOrder)}</Space>
                            </>
                        )}
                    </div>
                )}
            </Modal>

            <Modal
                title="Tạo đơn hàng mới"
                open={createModalVisible}
                onCancel={() => {
                    setCreateModalVisible(false);
                    form.resetFields();
                }}
                onOk={() => form.submit()}
                width={800}
            >
                <Form
                    form={form}
                    layout="vertical"
                    onFinish={handleCreateOrder}
                >
                    <Form.Item
                        name="orderType"
                        label="Loại đơn"
                        rules={[{ required: true, message: 'Vui lòng chọn loại đơn' }]}
                    >
                        <Select placeholder="Chọn loại đơn">
                            {Object.entries(ORDER_TYPES).map(([key, value]) => (
                                <Option key={key} value={value}>{getTypeTag(value).props.children}</Option>
                            ))}
                        </Select>
                    </Form.Item>

                    <Form.Item
                        name="customerName"
                        label="Tên khách hàng"
                        rules={[{ required: true, message: 'Vui lòng nhập tên khách hàng' }]}
                    >
                        <Input placeholder="Nhập tên khách hàng" />
                    </Form.Item>

                    <Form.Item
                        name="customerPhone"
                        label="Số điện thoại"
                        rules={[{ required: true, message: 'Vui lòng nhập số điện thoại' }]}
                    >
                        <Input placeholder="Nhập số điện thoại" />
                    </Form.Item>

                    <Form.Item
                        noStyle
                        shouldUpdate={(prevValues, currentValues) => prevValues.orderType !== currentValues.orderType}
                    >
                        {({ getFieldValue }) => {
                            const orderType = getFieldValue('orderType');
                            if (orderType === 'DINE_IN') {
                                return (
                                    <Form.Item
                                        name="tableNumber"
                                        label="Số bàn"
                                        rules={[{ required: true, message: 'Vui lòng nhập số bàn' }]}
                                    >
                                        <Input placeholder="Nhập số bàn" />
                                    </Form.Item>
                                );
                            }
                            if (orderType === 'DELIVERY') {
                                return (
                                    <Form.Item
                                        name="deliveryAddress"
                                        label="Địa chỉ giao hàng"
                                        rules={[{ required: true, message: 'Vui lòng nhập địa chỉ giao hàng' }]}
                                    >
                                        <Input.TextArea placeholder="Nhập địa chỉ giao hàng" rows={3} />
                                    </Form.Item>
                                );
                            }
                            return null;
                        }}
                    </Form.Item>

                    <Form.Item
                        name="orderItems"
                        label="Danh sách món ăn"
                        rules={[{ required: true, message: 'Vui lòng thêm ít nhất một món ăn' }]}
                    >
                        <Form.List name="orderItems">
                            {(fields, { add, remove }) => (
                                <>
                                    {fields.map(({ key, name, ...restField }) => (
                                        <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                                            <Form.Item
                                                {...restField}
                                                name={[name, 'menuItemId']}
                                                rules={[{ required: true, message: 'Chọn món ăn' }]}
                                            >
                                                <Select
                                                    style={{ width: 250 }}
                                                    placeholder="Chọn món"
                                                    showSearch
                                                    filterOption={(input, option) =>
                                                        (option?.children?.toLowerCase() || '').includes(input.toLowerCase())
                                                    }
                                                >
                                                    {menuItems.map(item => (
                                                        <Option key={item.menuItemId} value={item.menuItemId}>
                                                            {item.name} - {new Intl.NumberFormat('vi-VN').format(item.price)} đ
                                                        </Option>
                                                    ))}
                                                </Select>
                                            </Form.Item>
                                            <Form.Item
                                                {...restField}
                                                name={[name, 'quantity']}
                                                rules={[{ required: true, message: 'Nhập số lượng' }]}
                                            >
                                                <InputNumber min={1} placeholder="SL" style={{ width: 100 }} />
                                            </Form.Item>
                                            <Form.Item
                                                {...restField}
                                                name={[name, 'notes']}
                                            >
                                                <Input placeholder="Ghi chú (tùy chọn)" style={{ width: 200 }} />
                                            </Form.Item>
                                            <Button onClick={() => remove(name)} danger>Xóa</Button>
                                        </Space>
                                    ))}
                                    <Form.Item>
                                        <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                                            Thêm món ăn
                                        </Button>
                                    </Form.Item>
                                </>
                            )}
                        </Form.List>
                    </Form.Item>

                    <Form.Item name="discountPercentage" label="Giảm giá (%)">
                        <InputNumber min={0} max={100} style={{ width: '100%' }} />
                    </Form.Item>

                    <Form.Item name="discountAmount" label="Giảm giá (đ)">
                        <InputNumber min={0} style={{ width: '100%' }} />
                    </Form.Item>

                    <Form.Item name="vatPercentage" label="VAT (%)" initialValue={10}>
                        <InputNumber min={0} max={100} style={{ width: '100%' }} />
                    </Form.Item>

                    <Form.Item name="notes" label="Ghi chú">
                        <Input.TextArea rows={3} placeholder="Nhập ghi chú (nếu có)" />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default OrderManagement;

