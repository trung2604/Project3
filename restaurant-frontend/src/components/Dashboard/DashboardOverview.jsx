import React from 'react';
import { Row, Col, Card } from 'antd';
import ProgressCard from '../Common/ProgressCard';
import AlertCard from '../Common/AlertCard';
import { useDashboardStats, useAlerts } from '../../hooks/useDashboard';

const DashboardOverview = () => {
    const { stats, refreshStats } = useDashboardStats();
    const { alerts, handleAlert, refreshAlerts } = useAlerts();

    const progressData = [
        {
            label: 'Mức độ tồn kho thấp',
            value: `${stats.lowStockItems} mục`,
            percent: (stats.lowStockItems / stats.totalIngredients) * 100,
            status: 'exception',
            color: '#ff4d4f',
            strokeColor: '#ff4d4f'
        },
        {
            label: 'Tỷ lệ món ăn hoạt động',
            value: `${Math.round(stats.activeMenuItems / stats.totalMenuItems * 100)}%`,
            percent: (stats.activeMenuItems / stats.totalMenuItems) * 100,
            status: 'success',
            color: '#52c41a',
            strokeColor: '#52c41a'
        }
    ];

    return (
        <Row gutter={[24, 24]} style={{ marginBottom: '32px' }}>
            <Col xs={24} lg={12}>
                <ProgressCard
                    title="Tình trạng tồn kho"
                    data={progressData}
                    onRefresh={refreshStats}
                />
            </Col>
            <Col xs={24} lg={12}>
                <Card
                    title="Cảnh báo gần đây"
                    style={{
                        borderRadius: '12px',
                        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)',
                        height: '100%'
                    }}
                >
                    <AlertCard
                        alerts={alerts}
                        onHandleAlert={handleAlert}
                        onViewAll={refreshAlerts}
                    />
                </Card>
            </Col>
        </Row>
    );
};

export default DashboardOverview;
