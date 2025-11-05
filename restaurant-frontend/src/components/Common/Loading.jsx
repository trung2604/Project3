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

    if (fullScreen) {
        return (
            <div
                style={{
                    position: 'fixed',
                    top: 0,
                    left: 0,
                    right: 0,
                    bottom: 0,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    backgroundColor: 'rgba(255, 255, 255, 0.8)',
                    zIndex: 9999,
                    ...style
                }}
            >
                <Spin indicator={antIcon} tip={tip} size={size} />
            </div>
        );
    }

    return (
        <div
            style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '40px',
                ...style
            }}
        >
            <Spin indicator={antIcon} tip={tip} size={size} />
        </div>
    );
};

export default Loading;

