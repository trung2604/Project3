import React from 'react';
import { Row, Col } from 'antd';

const QuickActionCard = ({
    icon,
    title,
    description,
    onClick,
    backgroundColor = '#f0f8ff',
    borderColor = '#e6f7ff',
    iconColor = '#1890ff'
}) => {
    return (
        <Col xs={24} sm={12} md={6}>
            <div
                onClick={onClick}
                className="quick-action-card"
                style={{
                    textAlign: 'center',
                    padding: '20px',
                    background: backgroundColor,
                    borderRadius: '12px',
                    cursor: 'pointer',
                    transition: 'all 0.3s ease',
                    border: `1px solid ${borderColor}`,
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'center'
                }}
            >
                <div
                    className="quick-action-icon"
                    style={{
                        fontSize: '32px',
                        color: iconColor,
                        marginBottom: '12px'
                    }}
                >
                    {icon}
                </div>
                <div
                    className="quick-action-title"
                    style={{
                        fontWeight: '600',
                        color: '#262626',
                        marginBottom: '4px',
                        fontSize: '14px'
                    }}
                >
                    {title}
                </div>
                <div
                    className="quick-action-desc"
                    style={{
                        fontSize: '12px',
                        color: '#8c8c8c'
                    }}
                >
                    {description}
                </div>
            </div>
        </Col>
    );
};

const QuickActions = ({ actions = [], onActionClick }) => {
    return (
        <Row gutter={[16, 16]}>
            {actions.map((action, index) => (
                <QuickActionCard
                    key={index}
                    icon={action.icon}
                    title={action.title}
                    description={action.description}
                    onClick={() => onActionClick?.(action.key)}
                    backgroundColor={action.backgroundColor}
                    borderColor={action.borderColor}
                    iconColor={action.iconColor}
                />
            ))}
        </Row>
    );
};

export default QuickActions;
