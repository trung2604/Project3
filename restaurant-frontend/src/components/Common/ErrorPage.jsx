import React from 'react';
import { Result, Button } from 'antd';
import { useNavigate } from 'react-router-dom';
import {
    HomeOutlined,
    ReloadOutlined,
    BugOutlined,
    FrownOutlined,
    StopOutlined,
    ExclamationCircleOutlined
} from '@ant-design/icons';

const ErrorPage = ({
    status = 500,
    title,
    subTitle,
    extra,
    icon,
    onBack,
    showHomeButton = true,
    showReloadButton = true
}) => {
    const navigate = useNavigate();

    // Default icons based on status
    const getDefaultIcon = () => {
        if (icon) return icon;
        switch (status) {
            case 403:
                return <StopOutlined style={{ fontSize: 72, color: '#ff4d4f' }} />;
            case 404:
                return <FrownOutlined style={{ fontSize: 72, color: '#faad14' }} />;
            case 500:
                return <BugOutlined style={{ fontSize: 72, color: '#ff4d4f' }} />;
            default:
                return <ExclamationCircleOutlined style={{ fontSize: 72, color: '#faad14' }} />;
        }
    };

    // Default titles based on status
    const getDefaultTitle = () => {
        if (title) return title;
        switch (status) {
            case 403:
                return '403 - Không có quyền truy cập';
            case 404:
                return '404 - Không tìm thấy trang';
            case 500:
                return '500 - Lỗi máy chủ';
            default:
                return 'Đã xảy ra lỗi';
        }
    };

    // Default subtitles based on status
    const getDefaultSubTitle = () => {
        if (subTitle) return subTitle;
        switch (status) {
            case 403:
                return 'Bạn không có quyền truy cập vào trang này. Vui lòng liên hệ quản trị viên nếu bạn cần quyền truy cập.';
            case 404:
                return 'Trang bạn đang tìm kiếm không tồn tại hoặc đã bị di chuyển.';
            case 500:
                return 'Máy chủ đang gặp sự cố. Vui lòng thử lại sau hoặc liên hệ hỗ trợ.';
            default:
                return 'Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.';
        }
    };

    // Default extra buttons
    const getDefaultExtra = () => {
        if (extra) return extra;

        const buttons = [];

        if (showReloadButton) {
            buttons.push(
                <Button
                    key="reload"
                    icon={<ReloadOutlined />}
                    onClick={() => window.location.reload()}
                >
                    Tải lại trang
                </Button>
            );
        }

        if (showHomeButton) {
            buttons.push(
                <Button
                    key="home"
                    type="primary"
                    icon={<HomeOutlined />}
                    onClick={() => navigate('/dashboard')}
                >
                    Về trang chủ
                </Button>
            );
        }

        if (onBack) {
            buttons.push(
                <Button
                    key="back"
                    onClick={onBack}
                >
                    Quay lại
                </Button>
            );
        }

        return buttons.length > 0 ? buttons : null;
    };

    return (
        <Result
            status={status >= 500 ? '500' : status === 404 ? '404' : status === 403 ? '403' : 'error'}
            icon={getDefaultIcon()}
            title={getDefaultTitle()}
            subTitle={getDefaultSubTitle()}
            extra={getDefaultExtra()}
        />
    );
};

export default ErrorPage;

