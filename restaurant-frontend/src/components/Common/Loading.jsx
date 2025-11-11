import React from 'react';
import { Spin } from 'antd';
import { LoadingOutlined } from '@ant-design/icons';

const Loading = ({
    size = 'large',
    tip = 'Đang tải...',
    fullScreen = false,
    style = {}
}) => {
    const antIcon = <LoadingOutlined style={{ fontSize: size === 'large' ? 48 : size === 'small' ? 24 : 32 }} spin />;

    // Avoid Ant Design warning by not using tip prop
    // Instead, render tip text separately for both fullscreen and non-fullscreen
    const containerStyle = fullScreen
        ? {
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: 'rgba(255, 255, 255, 0.8)',
            zIndex: 9999,
            ...style
        }
        : {
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '40px',
            ...style
        };

    return (
        <div style={containerStyle}>
            <Spin indicator={antIcon} size={size} />
            {tip && (
                <div style={{ marginTop: 16, color: 'rgba(0, 0, 0, 0.45)', fontSize: 14 }}>
                    {tip}
                </div>
            )}
        </div>
    );
};

export default Loading;

