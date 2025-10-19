import React from 'react';
import { Row, Col } from 'antd';
import {
    ShoppingCartOutlined,
    BookOutlined,
    DollarOutlined,
    WarningOutlined
} from '@ant-design/icons';
import StatCard from '../Common/StatCard';
import { useDashboardStats } from '../../hooks/useDashboard';

const DashboardStats = () => {
    const { stats, loading, refreshStats } = useDashboardStats();

    const statCards = [
        {
            title: "Tổng nguyên liệu",
            value: stats.totalIngredients,
            icon: <ShoppingCartOutlined />,
            color: '#3f8600',
            trend: 5.2
        },
        {
            title: "Món ăn đang bán",
            value: stats.activeMenuItems,
            icon: <BookOutlined />,
            color: '#3f8600',
            trend: 2.1
        },
        {
            title: "Doanh thu tháng",
            value: stats.monthlyRevenue,
            icon: <DollarOutlined />,
            color: '#cf1322',
            trend: -1.5,
            formatter: (value) => `${(value / 1000000).toFixed(0)}M`,
            suffix: "VNĐ"
        },
        {
            title: "Cảnh báo",
            value: stats.alertCount,
            icon: <WarningOutlined />,
            color: '#cf1322'
        }
    ];

    return (
        <Row gutter={[24, 24]} style={{ marginBottom: '32px' }}>
            {statCards.map((card, index) => (
                <Col xs={24} sm={12} lg={6} key={index}>
                    <StatCard {...card} />
                </Col>
            ))}
        </Row>
    );
};

export default DashboardStats;
