import React, { useState, useEffect } from 'react';
import { Card, Tabs, Modal, Form, Input, InputNumber, Select, DatePicker, Upload, message } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';
import { useCurrentSection } from '../hooks/useDashboard';
import { useAuth } from '../context/AuthContext';
import DashboardStats from '../components/Dashboard/DashboardStats';
import DashboardOverview from '../components/Dashboard/DashboardOverview';
import DashboardQuickActions from '../components/Dashboard/DashboardQuickActions';
import InventoryManagement from './InventoryManagement';
import MenuManagement from './MenuManagement';
import MenuView from './MenuView';
import InventoryAlerts from './InventoryAlerts';
import InventoryTransactions from './InventoryTransactions';
import CategoryManagement from './CategoryManagement';
import CombosManagement from './CombosManagement';
import ErrorPage from '../components/Common/ErrorPage';
import Loading from '../components/Common/Loading';
import { canManageMenu, canViewOverview, getRedirectPathByRole } from '../utils/auth';

const Dashboard = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const { role, loading: authLoading } = useAuth();
    const activeSection = useCurrentSection();
    const [activeInventoryTab, setActiveInventoryTab] = useState('ingredients');
    const [activeMenuTab, setActiveMenuTab] = useState('items');

    // Modal states for quick actions
    const [isAddIngredientModalVisible, setIsAddIngredientModalVisible] = useState(false);
    const [isAddDishModalVisible, setIsAddDishModalVisible] = useState(false);
    const [isInventoryCheckModalVisible, setIsInventoryCheckModalVisible] = useState(false);
    const [isReportModalVisible, setIsReportModalVisible] = useState(false);
    const [loading, setLoading] = useState(false);

    const [form] = Form.useForm();

    // Handle modal actions
    const handleAddIngredient = () => {
        setIsAddIngredientModalVisible(true);
    };

    const handleAddDish = () => {
        setIsAddDishModalVisible(true);
    };

    const handleInventoryCheck = () => {
        setIsInventoryCheckModalVisible(true);
    };

    const handleCreateReport = () => {
        setIsReportModalVisible(true);
    };

    // Modal state mapping
    const modalStateMap = {
        ingredient: setIsAddIngredientModalVisible,
        dish: setIsAddDishModalVisible,
        inventory: setIsInventoryCheckModalVisible,
        report: setIsReportModalVisible
    };

    const handleModalOk = async (modalType) => {
        try {
            setLoading(true);
            await form.validateFields();

            // TODO: API calls here
            await new Promise(resolve => setTimeout(resolve, 1000)); // Simulate API call

            // Close modal and reset form
            const setModalVisible = modalStateMap[modalType];
            if (setModalVisible) {
                setModalVisible(false);
            }
            form.resetFields();
        } catch (error) {
            // Silently handle validation errors (form will show validation messages)
            if (process.env.NODE_ENV === 'development') {
                console.error('Form validation failed:', error);
            }
        } finally {
            setLoading(false);
        }
    };

    const handleModalCancel = (modalType) => {
        const setModalVisible = modalStateMap[modalType];
        if (setModalVisible) {
            setModalVisible(false);
        }
        form.resetFields();
    };

    const handleQuickAction = (actionKey) => {
        const actionMap = {
            'add-ingredient': () => setIsAddIngredientModalVisible(true),
            'add-dish': () => setIsAddDishModalVisible(true),
            'inventory-check': () => setIsInventoryCheckModalVisible(true),
            'create-report': () => setIsReportModalVisible(true)
        };

        const action = actionMap[actionKey];
        if (action) {
            action();
        }
    };

    // Get current section from URL (using hook)
    const currentSection = useCurrentSection();

    // Check if user has permission to view overview
    const hasOverviewPermission = canViewOverview(role);

    // Redirect non-managers away from overview to appropriate page based on role
    useEffect(() => {
        if (!authLoading && currentSection === 'overview' && !hasOverviewPermission) {
            // Redirect to appropriate page based on role
            const redirectPath = getRedirectPathByRole(role);
            navigate(redirectPath, { replace: true });
        }
    }, [authLoading, currentSection, hasOverviewPermission, role, navigate]);

    // Show loading while checking auth
    if (authLoading) {
        return <Loading tip="Đang kiểm tra quyền truy cập..." />;
    }

    // Show 403 error if trying to access overview without permission (fallback, should be redirected)
    if (currentSection === 'overview' && !hasOverviewPermission) {
        return (
            <ErrorPage
                status={403}
                title="403 - Không có quyền truy cập"
                subTitle="Chỉ quản lý nhà hàng mới có thể xem Dashboard tổng quan. Vui lòng sử dụng các chức năng khác."
                showHomeButton={false}
                showReloadButton={false}
                onBack={() => {
                    const redirectPath = getRedirectPathByRole(role);
                    navigate(redirectPath);
                }}
            />
        );
    }

    // Stats and alerts are now loaded from useDashboardStats and useAlerts hooks
    // No more mock data needed

    const inventoryTabs = [
        { key: 'ingredients', label: 'Nguyên liệu', children: <InventoryManagement /> },
        { key: 'transactions', label: 'Giao dịch', children: <InventoryTransactions /> },
        { key: 'alerts', label: 'Cảnh báo', children: <InventoryAlerts /> },
    ];

    const menuTabs = [
        { key: 'items', label: 'Món ăn', children: <MenuManagement /> },
        { key: 'categories', label: 'Danh mục', children: <CategoryManagement /> },
        { key: 'combos', label: 'Combo', children: <CombosManagement /> },
    ];

    const renderOverview = () => (
        <div>
            <DashboardStats />
            <DashboardOverview />
            <DashboardQuickActions onQuickAction={handleQuickAction} />
        </div>
    );

    const renderContent = () => {
        switch (currentSection) {
            case 'overview':
                return renderOverview();
            case 'inventory':
                return (
                    <div>
                        <div style={{ marginBottom: '16px' }}>
                            <h2 style={{ fontSize: '24px', fontWeight: 'bold', margin: 0, color: '#262626' }}>
                                Quản lý kho
                            </h2>
                            <p style={{ color: '#8c8c8c', margin: '8px 0 0 0', fontSize: '14px' }}>
                                Quản lý nguyên liệu, giao dịch và cảnh báo tồn kho
                            </p>
                        </div>
                        <Tabs
                            activeKey={activeInventoryTab}
                            onChange={setActiveInventoryTab}
                            items={inventoryTabs}
                            size="large"
                        />
                    </div>
                );
            case 'menu':
                // Show MenuView for customers, MenuManagement for staff/managers
                if (canManageMenu(role)) {
                    return (
                        <div>
                            <div style={{ marginBottom: '16px' }}>
                                <h2 style={{ fontSize: '24px', fontWeight: 'bold', margin: 0, color: '#262626' }}>
                                    Quản lý Menu
                                </h2>
                                <p style={{ color: '#8c8c8c', margin: '8px 0 0 0', fontSize: '14px' }}>
                                    Quản lý món ăn, danh mục và combo
                                </p>
                            </div>
                            <Tabs
                                activeKey={activeMenuTab}
                                onChange={setActiveMenuTab}
                                items={menuTabs}
                                size="large"
                            />
                        </div>
                    );
                } else {
                    return <MenuView />;
                }
            case 'orders':
                return (
                    <div>
                        <h2 style={{ fontSize: '24px', fontWeight: 'bold', margin: 0, color: '#262626' }}>
                            Quản lý đơn hàng
                        </h2>
                        <p style={{ color: '#8c8c8c', margin: '8px 0 0 0', fontSize: '14px' }}>
                            Tính năng đang được phát triển...
                        </p>
                    </div>
                );
            case 'staff':
                return (
                    <div>
                        <h2 style={{ fontSize: '24px', fontWeight: 'bold', margin: 0, color: '#262626' }}>
                            Quản lý nhân viên
                        </h2>
                        <p style={{ color: '#8c8c8c', margin: '8px 0 0 0', fontSize: '14px' }}>
                            Tính năng đang được phát triển...
                        </p>
                    </div>
                );
            case 'settings':
                return (
                    <div>
                        <h2 style={{ fontSize: '24px', fontWeight: 'bold', margin: 0, color: '#262626' }}>
                            Cài đặt hệ thống
                        </h2>
                        <p style={{ color: '#8c8c8c', margin: '8px 0 0 0', fontSize: '14px' }}>
                            Tính năng đang được phát triển...
                        </p>
                    </div>
                );
            default:
                return renderOverview();
        }
    };

    return (
        <div>
            {/* Content */}
            <Card style={{ borderRadius: '12px', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)' }}>
                {/* Page Header - Only show for overview section */}
                {currentSection === 'overview' && (
                    <div style={{ marginBottom: '24px' }}>
                        <h1 style={{ fontSize: '28px', fontWeight: 'bold', marginBottom: '8px', color: '#262626' }}>
                            Dashboard Tổng Quan
                        </h1>
                        <p style={{ color: '#8c8c8c', margin: 0, fontSize: '16px' }}>
                            Theo dõi tình hình hoạt động và quản lý nhà hàng hiệu quả
                        </p>
                    </div>
                )}

                {renderContent()}
            </Card>

            {/* Modals for Quick Actions */}
            <Modal
                title="Thêm nguyên liệu mới"
                open={isAddIngredientModalVisible}
                onOk={() => handleModalOk('ingredient')}
                onCancel={() => handleModalCancel('ingredient')}
                confirmLoading={loading}
                width={600}
            >
                <Form
                    form={form}
                    layout="vertical"
                    initialValues={{
                        expiryDate: null
                    }}
                    preserve={false}
                >
                    <Form.Item
                        name="name"
                        label="Tên nguyên liệu"
                        rules={[{ required: true, message: 'Vui lòng nhập tên nguyên liệu!' }]}
                    >
                        <Input placeholder="Nhập tên nguyên liệu" />
                    </Form.Item>
                    <Form.Item
                        name="quantity"
                        label="Số lượng"
                        rules={[{ required: true, message: 'Vui lòng nhập số lượng!' }]}
                    >
                        <InputNumber min={0} placeholder="Nhập số lượng" style={{ width: '100%' }} />
                    </Form.Item>
                    <Form.Item
                        name="unit"
                        label="Đơn vị"
                        rules={[{ required: true, message: 'Vui lòng chọn đơn vị!' }]}
                    >
                        <Select placeholder="Chọn đơn vị">
                            <Select.Option value="kg">Kilogram (kg)</Select.Option>
                            <Select.Option value="g">Gram (g)</Select.Option>
                            <Select.Option value="l">Lít (l)</Select.Option>
                            <Select.Option value="ml">Mililit (ml)</Select.Option>
                            <Select.Option value="piece">Cái</Select.Option>
                        </Select>
                    </Form.Item>
                    <Form.Item
                        name="price"
                        label="Giá mua"
                        rules={[{ required: true, message: 'Vui lòng nhập giá mua!' }]}
                    >
                        <InputNumber min={0} placeholder="Nhập giá mua" style={{ width: '100%' }} />
                    </Form.Item>
                    <Form.Item
                        name="expiryDate"
                        label="Ngày hết hạn"
                        rules={[
                            {
                                required: false,
                                message: 'Vui lòng chọn ngày hết hạn!',
                                validator: (_, value) => {
                                    if (!value) return Promise.resolve();
                                    if (value && typeof value.isValid === 'function' && !value.isValid()) {
                                        return Promise.reject(new Error('Ngày không hợp lệ!'));
                                    }
                                    return Promise.resolve();
                                }
                            }
                        ]}
                        normalize={(value) => value || null}
                        getValueProps={(value) => ({ value: value || null })}
                        validateTrigger={[]}
                    >
                        <DatePicker
                            style={{ width: '100%' }}
                            format="YYYY-MM-DD"
                            placeholder="Chọn ngày hết hạn"
                            allowClear
                            getValueFromEvent={(date) => date || null}
                        />
                    </Form.Item>
                </Form>
            </Modal>

            <Modal
                title="Thêm món ăn mới"
                open={isAddDishModalVisible}
                onOk={() => handleModalOk('dish')}
                onCancel={() => handleModalCancel('dish')}
                confirmLoading={loading}
                width={600}
            >
                <Form
                    form={form}
                    layout="vertical"
                    initialValues={{
                        expiryDate: null
                    }}
                    preserve={false}
                >
                    <Form.Item
                        name="name"
                        label="Tên món ăn"
                        rules={[{ required: true, message: 'Vui lòng nhập tên món ăn!' }]}
                    >
                        <Input placeholder="Nhập tên món ăn" />
                    </Form.Item>
                    <Form.Item
                        name="price"
                        label="Giá bán"
                        rules={[{ required: true, message: 'Vui lòng nhập giá bán!' }]}
                    >
                        <InputNumber min={0} placeholder="Nhập giá bán" style={{ width: '100%' }} />
                    </Form.Item>
                    <Form.Item
                        name="category"
                        label="Danh mục"
                        rules={[{ required: true, message: 'Vui lòng chọn danh mục!' }]}
                    >
                        <Select placeholder="Chọn danh mục">
                            <Select.Option value="appetizer">Khai vị</Select.Option>
                            <Select.Option value="main">Món chính</Select.Option>
                            <Select.Option value="dessert">Tráng miệng</Select.Option>
                            <Select.Option value="drink">Đồ uống</Select.Option>
                        </Select>
                    </Form.Item>
                    <Form.Item
                        name="description"
                        label="Mô tả"
                    >
                        <Input.TextArea rows={3} placeholder="Nhập mô tả món ăn" />
                    </Form.Item>
                    <Form.Item
                        name="image"
                        label="Hình ảnh"
                    >
                        <Upload
                            listType="picture-card"
                            maxCount={1}
                            beforeUpload={() => false}
                        >
                            <div>
                                <div style={{ marginTop: 8 }}>Upload</div>
                            </div>
                        </Upload>
                    </Form.Item>
                </Form>
            </Modal>

            <Modal
                title="Kiểm kê kho"
                open={isInventoryCheckModalVisible}
                onOk={() => handleModalOk('inventory')}
                onCancel={() => handleModalCancel('inventory')}
                confirmLoading={loading}
                width={600}
            >
                <Form
                    form={form}
                    layout="vertical"
                    initialValues={{
                        checkDate: null
                    }}
                >
                    <Form.Item
                        name="checkDate"
                        label="Ngày kiểm kê"
                        rules={[
                            {
                                required: true,
                                message: 'Vui lòng chọn ngày kiểm kê!',
                                validator: (_, value) => {
                                    if (!value) return Promise.reject(new Error('Vui lòng chọn ngày kiểm kê!'));
                                    if (value && typeof value.isValid === 'function' && !value.isValid()) {
                                        return Promise.reject(new Error('Ngày không hợp lệ!'));
                                    }
                                    return Promise.resolve();
                                }
                            }
                        ]}
                        normalize={(value) => value || null}
                    >
                        <DatePicker
                            style={{ width: '100%' }}
                            format="YYYY-MM-DD"
                            placeholder="Chọn ngày kiểm kê"
                            allowClear
                            getValueFromEvent={(date) => date || null}
                        />
                    </Form.Item>
                    <Form.Item
                        name="checkType"
                        label="Loại kiểm kê"
                        rules={[{ required: true, message: 'Vui lòng chọn loại kiểm kê!' }]}
                    >
                        <Select placeholder="Chọn loại kiểm kê">
                            <Select.Option value="full">Kiểm kê toàn bộ</Select.Option>
                            <Select.Option value="partial">Kiểm kê một phần</Select.Option>
                            <Select.Option value="expiry">Kiểm tra hạn sử dụng</Select.Option>
                        </Select>
                    </Form.Item>
                    <Form.Item
                        name="notes"
                        label="Ghi chú"
                    >
                        <Input.TextArea rows={3} placeholder="Nhập ghi chú kiểm kê" />
                    </Form.Item>
                </Form>
            </Modal>

            <Modal
                title="Tạo báo cáo"
                open={isReportModalVisible}
                onOk={() => handleModalOk('report')}
                onCancel={() => handleModalCancel('report')}
                confirmLoading={loading}
                width={600}
            >
                <Form
                    form={form}
                    layout="vertical"
                    initialValues={{
                        dateRange: null
                    }}
                >
                    <Form.Item
                        name="reportType"
                        label="Loại báo cáo"
                        rules={[{ required: true, message: 'Vui lòng chọn loại báo cáo!' }]}
                    >
                        <Select placeholder="Chọn loại báo cáo">
                            <Select.Option value="revenue">Báo cáo doanh thu</Select.Option>
                            <Select.Option value="inventory">Báo cáo tồn kho</Select.Option>
                            <Select.Option value="sales">Báo cáo bán hàng</Select.Option>
                            <Select.Option value="expense">Báo cáo chi phí</Select.Option>
                        </Select>
                    </Form.Item>
                    <Form.Item
                        name="dateRange"
                        label="Khoảng thời gian"
                        rules={[
                            {
                                required: true,
                                message: 'Vui lòng chọn khoảng thời gian!',
                                validator: (_, value) => {
                                    if (!value || !Array.isArray(value) || value.length !== 2) {
                                        return Promise.reject(new Error('Vui lòng chọn khoảng thời gian!'));
                                    }
                                    const [start, end] = value;
                                    if (start && typeof start.isValid === 'function' && !start.isValid()) {
                                        return Promise.reject(new Error('Ngày bắt đầu không hợp lệ!'));
                                    }
                                    if (end && typeof end.isValid === 'function' && !end.isValid()) {
                                        return Promise.reject(new Error('Ngày kết thúc không hợp lệ!'));
                                    }
                                    return Promise.resolve();
                                }
                            }
                        ]}
                        normalize={(value) => value || null}
                    >
                        <DatePicker.RangePicker
                            style={{ width: '100%' }}
                            format="YYYY-MM-DD"
                            placeholder={['Từ ngày', 'Đến ngày']}
                            allowClear
                            getValueFromEvent={(dates) => dates || null}
                        />
                    </Form.Item>
                    <Form.Item
                        name="format"
                        label="Định dạng xuất"
                        rules={[{ required: true, message: 'Vui lòng chọn định dạng!' }]}
                    >
                        <Select placeholder="Chọn định dạng">
                            <Select.Option value="pdf">PDF</Select.Option>
                            <Select.Option value="excel">Excel</Select.Option>
                            <Select.Option value="csv">CSV</Select.Option>
                        </Select>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default Dashboard;