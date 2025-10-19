import React from 'react';
import { Card } from 'antd';
import {
    ShoppingCartOutlined,
    BookOutlined,
    WarningOutlined,
    DollarOutlined
} from '@ant-design/icons';
import QuickActions from '../Common/QuickActions';

const DashboardQuickActions = ({ onQuickAction }) => {
    const quickActions = [
        {
            key: 'add-ingredient',
            icon: <ShoppingCartOutlined />,
            title: 'Thêm nguyên liệu',
            description: 'Nhập nguyên liệu mới',
            backgroundColor: '#f0f8ff',
            borderColor: '#e6f7ff',
            iconColor: '#1890ff'
        },
        {
            key: 'add-dish',
            icon: <BookOutlined />,
            title: 'Thêm món ăn',
            description: 'Tạo món ăn mới',
            backgroundColor: '#f6ffed',
            borderColor: '#d9f7be',
            iconColor: '#52c41a'
        },
        {
            key: 'inventory-check',
            icon: <WarningOutlined />,
            title: 'Kiểm kê kho',
            description: 'Kiểm tra tồn kho',
            backgroundColor: '#fff7e6',
            borderColor: '#ffd591',
            iconColor: '#faad14'
        },
        {
            key: 'create-report',
            icon: <DollarOutlined />,
            title: 'Tạo báo cáo',
            description: 'Xuất báo cáo doanh thu',
            backgroundColor: '#f9f0ff',
            borderColor: '#d3adf7',
            iconColor: '#722ed1'
        }
    ];

    return (
        <Card
            title="Thao tác nhanh"
            style={{
                borderRadius: '12px',
                boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)'
            }}
        >
            <QuickActions
                actions={quickActions}
                onActionClick={onQuickAction}
            />
        </Card>
    );
};

export default DashboardQuickActions;
