import React, { useState, useEffect } from "react";
import { Modal, Descriptions, Tag, Button, Space, Divider, Timeline, message } from "antd";
import {
    CheckCircleOutlined,
    ClockCircleOutlined,
    CloseCircleOutlined,
    DollarOutlined,
} from "@ant-design/icons";
import dayjs from "dayjs";
import {
    getPaymentStatusColor,
    getPaymentStatusText,
    getPaymentMethodText,
    getPaymentMethodIcon,
    formatCurrency,
    canRefundPayment,
    getRefundableAmount,
} from "../utils/paymentHelpers";
import { PAYMENT_STATUS } from "../constants";

const PaymentDetailsModal = ({ visible, paymentId, onClose, onRefund }) => {
    const [payment, setPayment] = useState(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (visible && paymentId) {
            loadPaymentDetails();
        }
    }, [visible, paymentId]);

    const loadPaymentDetails = async () => {
        setLoading(true);
        try {
            const paymentAPI = (await import("../services/paymentService")).default;
            const response = await paymentAPI.getPaymentById(paymentId);
            console.log("Payment Details Response:", response);

            // Interceptor unwraps response, so response is the Payment object
            const paymentDataRaw = response;

            if (paymentDataRaw) {
                // Map API fields to frontend expected fields
                const paymentData = {
                    ...paymentDataRaw,
                    paymentStatus: paymentDataRaw.status || paymentDataRaw.paymentStatus,
                    gatewayPaymentId: paymentDataRaw.gatewayTransactionId || paymentDataRaw.gatewayOrderId
                };
                setPayment(paymentData);
            }
        } catch (error) {
            console.error("Error loading payment details:", error);
            message.error("Không thể tải thông tin thanh toán");
        } finally {
            setLoading(false);
        }
    };

    const handleRefund = () => {
        onClose();
        if (onRefund) {
            onRefund(payment);
        }
    };

    const renderTimeline = () => {
        if (!payment) return null;

        const events = [];

        // Created
        if (payment.createdAt) {
            events.push({
                color: "blue",
                icon: <ClockCircleOutlined />,
                children: (
                    <>
                        <p><strong>Tạo thanh toán</strong></p>
                        <p>{dayjs(payment.createdAt).format("DD/MM/YYYY HH:mm:ss")}</p>
                    </>
                ),
            });
        }

        // Processed
        if (payment.processedAt) {
            const isSuccess = payment.paymentStatus === PAYMENT_STATUS.SUCCESS;
            events.push({
                color: isSuccess ? "green" : "red",
                icon: isSuccess ? <CheckCircleOutlined /> : <CloseCircleOutlined />,
                children: (
                    <>
                        <p><strong>{isSuccess ? "Thanh toán thành công" : "Thanh toán thất bại"}</strong></p>
                        <p>{dayjs(payment.processedAt).format("DD/MM/YYYY HH:mm:ss")}</p>
                        {payment.failureReason && <p style={{ color: "red" }}>Lý do: {payment.failureReason}</p>}
                    </>
                ),
            });
        }

        // Refunded
        if (payment.refundedAt) {
            events.push({
                color: "orange",
                icon: <DollarOutlined />,
                children: (
                    <>
                        <p><strong>Hoàn tiền</strong></p>
                        <p>{dayjs(payment.refundedAt).format("DD/MM/YYYY HH:mm:ss")}</p>
                        <p>Số tiền: {formatCurrency(payment.refundedAmount)}</p>
                        {payment.refundReason && <p>Lý do: {payment.refundReason}</p>}
                    </>
                ),
            });
        }

        return <Timeline items={events} />;
    };

    return (
        <Modal
            title="Chi Tiết Thanh Toán"
            open={visible}
            onCancel={onClose}
            width={700}
            footer={[
                canRefundPayment(payment) && onRefund && (
                    <Button key="refund" type="primary" danger onClick={handleRefund}>
                        Hoàn Tiền
                    </Button>
                ),
                <Button key="close" onClick={onClose}>
                    Đóng
                </Button>,
            ]}
            loading={loading}
        >
            {payment && (
                <>
                    <Descriptions bordered column={2} size="small">
                        <Descriptions.Item label="Mã đơn hàng">
                            <strong style={{ color: "#595959" }}>{payment.orderId}</strong>
                        </Descriptions.Item>

                        <Descriptions.Item label="Mã giao dịch">
                            <span style={{ fontFamily: "monospace" }}>
                                {payment.transactionReference ? payment.transactionReference.substring(0, 15) + "..." : "N/A"}
                            </span>
                        </Descriptions.Item>

                        <Descriptions.Item label="Số tiền">
                            <strong style={{ fontSize: "16px", color: "#1890ff" }}>
                                {formatCurrency(payment.amount)}
                            </strong>
                        </Descriptions.Item>

                        <Descriptions.Item label="Trạng thái">
                            <Tag color={getPaymentStatusColor(payment.paymentStatus)}>
                                {getPaymentStatusText(payment.paymentStatus)}
                            </Tag>
                        </Descriptions.Item>

                        <Descriptions.Item label="Phương thức">
                            {getPaymentMethodIcon(payment.paymentMethod)}{" "}
                            {getPaymentMethodText(payment.paymentMethod)}
                        </Descriptions.Item>

                        <Descriptions.Item label="Thời gian tạo">
                            {payment.createdAt ? dayjs(payment.createdAt).format("DD/MM/YYYY HH:mm") : "N/A"}
                        </Descriptions.Item>

                        {(payment.paymentStatus === PAYMENT_STATUS.REFUNDED ||
                            payment.paymentStatus === PAYMENT_STATUS.PARTIALLY_REFUNDED) && (
                                <>
                                    <Descriptions.Item label="Số tiền đã hoàn">
                                        <Tag color="orange">
                                            {formatCurrency(payment.refundedAmount)}
                                        </Tag>
                                    </Descriptions.Item>
                                </>
                            )}
                    </Descriptions>

                    <Divider>Lịch Sử Giao Dịch</Divider>
                    {renderTimeline()}
                </>
            )}
        </Modal>
    );
};

export default PaymentDetailsModal;
