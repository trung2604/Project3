import React from 'react';
import { Button, Card, Progress } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';

const ProgressCard = ({
    title = "Tình trạng tồn kho",
    data = [],
    onRefresh
}) => {
    return (
        <Card
            title={title}
            extra={
                <Button
                    type="text"
                    size="small"
                    icon={<ReloadOutlined />}
                    onClick={onRefresh}
                >
                    Làm mới
                </Button>
            }
            style={{
                borderRadius: '12px',
                boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)',
                height: '100%'
            }}
        >
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                {data.map((item, index) => (
                    <div key={index}>
                        <div style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            marginBottom: '8px'
                        }}>
                            <div style={{ color: '#595959', fontSize: '14px' }}>
                                {item.label}
                            </div>
                            <div style={{
                                color: item.color || '#52c41a',
                                fontWeight: '600',
                                fontSize: '14px'
                            }}>
                                {item.value}
                            </div>
                        </div>
                        <Progress
                            percent={item.percent}
                            status={item.status || 'success'}
                            showInfo={false}
                            strokeColor={item.strokeColor}
                        />
                    </div>
                ))}
            </div>
        </Card>
    );
};

export default ProgressCard;
