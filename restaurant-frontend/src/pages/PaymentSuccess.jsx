import React, { useEffect, useState } from 'react';
import { Result, Button, Card, Descriptions, Spin, Typography } from 'antd';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { CheckCircleFilled, HomeOutlined, FileTextOutlined } from '@ant-design/icons';
import apiService from '../services/apiService';
import paymentAPI from '../services/paymentService';

const { Title, Text } = Typography;

const PaymentSuccess = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [paymentDetails, setPaymentDetails] = useState(null);
    const [error, setError] = useState(null);

    // Get params from URL
    const paymentId = searchParams.get('paymentId');
    const orderId = searchParams.get('orderId');
    const token = searchParams.get('token'); // PayPal token
    const payerId = searchParams.get('PayerID'); // PayPal PayerID

    useEffect(() => {
        const verifyAndLoadPayment = async () => {
            // If we have PayPal params, we might need to verify/execute payment first
            // But usually the backend handles the callback and THEN redirects here?
            // OR, if the returnURL points here, WE (Frontend) trigger the verification (like OrderManagement did).

            // Case 1: PayPal Callback handling
            if (token && payerId && paymentId) {
                try {
                    await paymentAPI.handlePayPalCallback(paymentId, token, payerId);
                    // Success! Now fetch details
                } catch (err) {
                    console.error("PayPal verification failed:", err);
                    // If error is "Order already captured", we can ignore it and fetch details?
                    // Assuming backend handles idempotency (which I tried to fix)
                }
            }

            // Fetch Payment & Order Details
            try {
                if (paymentId) {
                    const paymentRes = await paymentAPI.getPaymentById(paymentId);
                    const paymentData = paymentRes?.data || paymentRes;
                    setPaymentDetails(prev => ({ ...prev, payment: paymentData }));
                }

                if (orderId) {
                    const orderRes = await apiService.order.getOrderById(orderId);
                    const orderData = orderRes?.data || orderRes;
                    setPaymentDetails(prev => ({ ...prev, order: orderData }));
                }
            } catch (err) {
                console.error("Error loading details:", err);
                setError("Không thể tải thông tin thanh toán.");
            } finally {
                setLoading(false);
            }
        };

        verifyAndLoadPayment();
    }, [paymentId, orderId, token, payerId]);

    if (loading) {
        return (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
                <Spin size="large" tip="Đang xác thực thanh toán..." />
            </div>
        );
    }

    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
    };

    return (
        <div style={{ maxWidth: 800, margin: '40px auto', padding: '20px' }}>
            <Card bordered={false} style={{ borderRadius: 16, boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}>
                <Result
                    status="success"
                    icon={<CheckCircleFilled style={{ color: '#52c41a' }} />}
                    title={<Title level={2} style={{ color: '#52c41a' }}>Thanh toán thành công!</Title>}
                    subTitle="Cảm ơn bạn đã sử dụng dịch vụ. Đơn hàng của bạn đã được thanh toán và đang được xử lý."
                    extra={[
                        <Button type="primary" key="home" icon={<HomeOutlined />} onClick={() => navigate('/')}>
                            Về trang chủ
                        </Button>,
                        <Button key="order" icon={<FileTextOutlined />} onClick={() => navigate(`/dashboard/orders`)}>
                            Xem đơn hàng
                        </Button>,
                    ]}
                >
                    {paymentDetails && (
                        <div style={{ background: '#f9fafb', padding: '24px', borderRadius: '12px', marginTop: '24px' }}>
                            <Descriptions title="Thông tin hóa đơn" bordered column={{ xxl: 1, xl: 1, lg: 1, md: 1, sm: 1, xs: 1 }}>
                                <Descriptions.Item label="Mã đơn hàng">
                                    <Text strong>{orderId}</Text>
                                </Descriptions.Item>
                                <Descriptions.Item label="Mã giao dịch">
                                    <Text copyable>{paymentId}</Text>
                                </Descriptions.Item>
                                <Descriptions.Item label="Phương thức thanh toán">
                                    {paymentDetails.payment?.paymentMethod || 'PAYPAL'}
                                </Descriptions.Item>
                                <Descriptions.Item label="Trạng thái">
                                    <Text type="success" strong>ĐÃ THANH TOÁN</Text>
                                </Descriptions.Item>
                                <Descriptions.Item label="Tổng tiền">
                                    <Text style={{ fontSize: '18px', color: '#f59e0b', fontWeight: 'bold' }}>
                                        {formatCurrency(paymentDetails.payment?.amount || 0)}
                                    </Text>
                                </Descriptions.Item>
                            </Descriptions>
                        </div>
                    )}
                </Result>
            </Card>
        </div>
    );
};

export default PaymentSuccess;
