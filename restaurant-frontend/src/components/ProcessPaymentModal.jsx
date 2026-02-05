import React, { useState, useEffect } from "react";
import { Modal, Button, Spin, message, Alert, Divider } from "antd";
import {
    CheckCircleOutlined,
    ClockCircleOutlined,
    DollarOutlined,
    QrcodeOutlined
} from "@ant-design/icons";
import { QRCodeCanvas as QRCode } from "qrcode.react";
import paymentAPI from "../services/paymentService";
import { PAYMENT_METHODS, PAYMENT_STATUS } from "../constants";
import { formatCurrency, getPaymentMethodText, getPaymentMethodIcon } from "../utils/paymentHelpers";

const ProcessPaymentModal = ({ visible, payment, order, onClose, onSuccess }) => {
    const [processing, setProcessing] = useState(false);
    const [polling, setPolling] = useState(false);
    const [qrData, setQrData] = useState(null);
    const [paymentUrl, setPaymentUrl] = useState(null);
    const [currentStatus, setCurrentStatus] = useState(null);

    useEffect(() => {
        if (visible && payment) {
            setCurrentStatus(payment.paymentStatus);
            setQrData(null);
            setPaymentUrl(null);
            setPolling(false);

            // Auto-trigger VietQR logic if applicable
            if (payment.paymentMethod === PAYMENT_METHODS.VIETQR && payment.paymentStatus === PAYMENT_STATUS.PENDING) {
                // Determine if we need to call API (if we don't store QR data in payment object)
                // Since we don't have QR URL in the payment list response, we must process/re-generate it.
                handleVietQRPayment();
            }
        }
    }, [visible, payment]);

    // Auto-polling for VietQR and PayPal
    useEffect(() => {
        if (!polling || !payment) return;

        const interval = setInterval(async () => {
            try {
                const response = await paymentAPI.getPaymentById(payment.paymentId);
                // Handle response unwrapping (interceptor might return data directly)
                const paymentData = response?.data || response;

                if (paymentData?.paymentStatus) {
                    setCurrentStatus(paymentData.paymentStatus);

                    if (paymentData.paymentStatus !== PAYMENT_STATUS.PENDING) {
                        setPolling(false);
                        clearInterval(interval);

                        if (paymentData.paymentStatus === PAYMENT_STATUS.SUCCESS) {
                            message.success("Thanh toán thành công!");
                            setTimeout(() => {
                                onClose();
                                if (onSuccess) onSuccess();
                            }, 1500);
                        } else if (paymentData.paymentStatus === PAYMENT_STATUS.FAILED) {
                            message.error("Thanh toán thất bại!");
                        }
                    }
                }
            } catch (error) {
                console.error("Error polling payment status:", error);
            }
        }, 5000); // Poll every 5 seconds

        return () => clearInterval(interval);
    }, [polling, payment, onClose, onSuccess]);

    const handleCashPayment = async () => {
        Modal.confirm({
            title: "Xác nhận thanh toán tiền mặt",
            content: "Bạn xác nhận đã thanh toán tiền mặt cho nhân viên?",
            onOk: async () => {
                setProcessing(true);
                try {
                    const response = await paymentAPI.processPayment(payment.paymentId, {
                        orderId: order.orderId,
                        customerId: order.customerId,
                        amount: payment.amount,
                        paymentMethod: PAYMENT_METHODS.CASH,
                        returnUrl: window.location.href,
                        cancelUrl: window.location.href,
                    });

                    if (response?.success || response?.data?.success) {
                        message.success("Xác nhận thanh toán thành công!");
                        setTimeout(() => {
                            onClose();
                            if (onSuccess) onSuccess();
                        }, 1000);
                    } else {
                        message.error(response?.message || "Xác nhận thanh toán thất bại");
                    }
                } catch (error) {
                    console.error("Error processing cash payment:", error);
                    message.error(error?.response?.data?.message || "Có lỗi xảy ra khi xử lý thanh toán");
                } finally {
                    setProcessing(false);
                }
            },
        });
    };

    const handleVietQRPayment = async () => {
        setProcessing(true);
        try {
            const response = await paymentAPI.processPayment(payment.paymentId, {
                orderId: order.orderId,
                customerId: order.customerId,
                amount: payment.amount,
                paymentMethod: PAYMENT_METHODS.VIETQR,
                returnUrl: window.location.href,
                cancelUrl: window.location.href,
            });

            // Interceptor unwraps response, so response is the PaymentResult object
            const result = response?.data || response;
            if (result?.qrCodeData) {
                setQrData(result.qrCodeData);
                setPolling(true);
                message.info("Vui lòng quét mã QR để thanh toán");
            } else {
                message.error("Không thể tạo mã QR");
            }
        } catch (error) {
            console.error("Error processing VietQR payment:", error);
            message.error(error?.response?.data?.message || "Có lỗi xảy ra khi tạo mã QR");
        } finally {
            setProcessing(false);
        }
    };

    const handlePayPalPayment = async () => {
        setProcessing(true);
        try {
            const response = await paymentAPI.processPayment(payment.paymentId, {
                orderId: order.orderId,
                customerId: order.customerId,
                amount: payment.amount,
                paymentMethod: PAYMENT_METHODS.PAYPAL,
                returnUrl: `${window.location.origin}/payment/success?paymentId=${payment.paymentId}&orderId=${order.orderId}`,
                cancelUrl: `${window.location.origin}/dashboard/orders?payment=cancelled&orderId=${order.orderId}`,
            });

            // Response structure: { statusCode: 200, data: { redirectUrl: "..." } }
            // Interceptor usually unwraps to response.data or response directly.
            // If response is the Axios response: response.data.data.redirectUrl
            // If response is the API response DTO: response.data.redirectUrl

            const result = response?.data || response;

            if (result?.redirectUrl) {
                setPaymentUrl(result.redirectUrl);
                message.info("Chuyển hướng đến PayPal...");
                setTimeout(() => {
                    window.location.href = result.redirectUrl;
                }, 1000);
            } else {
                message.error("Không thể tạo link thanh toán PayPal");
            }
        } catch (error) {
            console.error("Error processing PayPal payment:", error);
            message.error(error?.response?.data?.message || "Có lỗi xảy ra khi tạo link PayPal");
        } finally {
            setProcessing(false);
        }
    };

    const renderPaymentContent = () => {
        if (!payment) return null;

        const method = payment.paymentMethod;

        // CASH Payment
        if (method === PAYMENT_METHODS.CASH) {
            return (
                <div style={{ textAlign: "center", padding: "20px" }}>
                    <DollarOutlined style={{ fontSize: "64px", color: "#52c41a" }} />
                    <h3 style={{ marginTop: "16px" }}>Thanh toán tiền mặt</h3>
                    <p style={{ color: "#8c8c8c" }}>
                        Vui lòng thanh toán tiền mặt cho nhân viên khi nhận hàng
                    </p>
                    <Divider />
                    <Button
                        type="primary"
                        size="large"
                        onClick={handleCashPayment}
                        loading={processing}
                        block
                    >
                        Xác nhận đã thanh toán
                    </Button>
                </div>
            );
        }

        // VietQR Payment
        if (method === PAYMENT_METHODS.VIETQR) {
            if (qrData) {
                return (
                    <div style={{ textAlign: "center", padding: "20px" }}>
                        <p style={{ marginBottom: "16px", fontSize: "16px" }}>Vui lòng quét mã QR để thanh toán</p>
                        <div style={{
                            padding: "20px",
                            backgroundColor: "#fff",
                            display: "inline-block",
                            borderRadius: "8px",
                            boxShadow: "0 2px 8px rgba(0,0,0,0.1)"
                        }}>
                            <img
                                src={qrData}
                                alt="VietQR Code"
                                style={{ maxWidth: "250px", display: "block" }}
                            />
                        </div>
                        {/* Polling Alert removed */}


                        <Divider dashed />

                        <Button
                            type="primary"
                            size="large"
                            onClick={() => {
                                // Manually confirm logic
                                setPolling(true);
                                message.loading("Đang xác nhận thanh toán...", 1);
                                paymentAPI.completePayment(payment.paymentId).then(() => {
                                    message.success("Thanh toán thành công!");
                                    onClose();
                                    if (onSuccess) onSuccess();
                                }).catch(err => {
                                    console.error("Complete payment error:", err);
                                    message.error("Không thể xác nhận thanh toán. Vui lòng thử lại.");
                                });
                            }}
                            block
                            style={{ marginTop: '10px', height: '50px', fontSize: '18px' }}
                        >
                            Xác nhận đã thanh toán
                        </Button>
                    </div >
                );
            }

            return (
                <div style={{ textAlign: "center", padding: "20px" }}>
                    <QrcodeOutlined style={{ fontSize: "64px", color: "#1890ff" }} />
                    <h3 style={{ marginTop: "16px" }}>Thanh toán VietQR</h3>
                    <p style={{ color: "#8c8c8c" }}>
                        Quét mã QR bằng ứng dụng ngân hàng để thanh toán
                    </p>
                    <Divider />
                    <Button
                        type="primary"
                        size="large"
                        onClick={handleVietQRPayment}
                        loading={processing}
                        block
                    >
                        Tạo mã QR thanh toán
                    </Button>
                </div>
            );
        }

        // PayPal Payment
        if (method === PAYMENT_METHODS.PAYPAL) {
            return (
                <div style={{ textAlign: "center", padding: "20px" }}>
                    <div style={{ fontSize: "64px" }}>🅿️</div>
                    <h3 style={{ marginTop: "16px" }}>Thanh toán PayPal</h3>
                    <p style={{ color: "#8c8c8c" }}>
                        Bạn sẽ được chuyển hướng đến PayPal để hoàn tất thanh toán
                    </p>
                    <Divider />
                    <Button
                        type="primary"
                        size="large"
                        onClick={handlePayPalPayment}
                        loading={processing}
                        block
                        style={{ backgroundColor: "#0070ba", borderColor: "#0070ba" }}
                    >
                        Thanh toán với PayPal
                    </Button>
                </div>
            );
        }

        return null;
    };

    if (!payment || !order) return null;

    return (
        <Modal
            title={
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <span>{getPaymentMethodText(payment.paymentMethod)}</span>
                </div>
            }
            open={visible}
            onCancel={onClose}
            footer={[
                <Button key="close" onClick={onClose} disabled={processing || polling}>
                    {polling ? "Đang chờ..." : "Đóng"}
                </Button>,
            ]}
            width={400}
            centered
            maskClosable={!processing}
        >
            {/* Header info removed to match simplified design */}

            <Divider style={{ margin: "16px 0" }} />

            {renderPaymentContent()}

            {currentStatus === PAYMENT_STATUS.SUCCESS && (
                <Alert
                    message="Thanh toán thành công"
                    type="success"
                    showIcon
                    icon={<CheckCircleOutlined />}
                    style={{ marginTop: "16px" }}
                />
            )}
        </Modal>
    );
};

export default ProcessPaymentModal;
