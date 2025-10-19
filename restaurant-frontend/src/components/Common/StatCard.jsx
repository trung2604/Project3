import React from 'react';
import { Card, Statistic } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';

const StatCard = ({
    title,
    value,
    icon,
    color = '#1890ff',
    trend,
    formatter,
    suffix,
    precision = 0
}) => {
    return (
        <Card
            style={{
                borderRadius: '12px',
                boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)',
                height: '100%'
            }}
        >
            <Statistic
                title={title}
                value={value}
                valueStyle={{ color }}
                prefix={icon}
                formatter={formatter}
                suffix={suffix}
                precision={precision}
            />
            {trend && (
                <div style={{
                    marginTop: '8px',
                    fontSize: '12px',
                    color: trend > 0 ? '#52c41a' : '#ff4d4f',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px'
                }}>
                    {trend > 0 ? <ArrowUpOutlined /> : <ArrowDownOutlined />}
                    {Math.abs(trend)}%
                </div>
            )}
        </Card>
    );
};

export default StatCard;
