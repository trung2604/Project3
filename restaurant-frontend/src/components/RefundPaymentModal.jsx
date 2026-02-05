import React, { useState } from "react";
import { Modal, Form, InputNumber, Input, message, Alert } from "antd";
import paymentAPI from "../services/paymentService";
import { formatCurrency, getRefundableAmount } from "../utils/paymentHelpers";

const { TextArea } = Input;

const RefundPaymentModal = ({ visible, payment, onClose, onSuccess }) => {
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (values) => {
        setLoading(true);
        try {
            const refundData = {
                refundAmount: values.refundAmount,
                reason: values.reason,
                requestedBy: null, // Will be set by backend from JWT
            };

            const response = await paymentAPI.refundPayment(payment.paymentId, refundData);

            if (response?.success) {
                message.success("Hoàn tiền thành công");
                form.resetFields();
                onClose();
                if (onSuccess) {
                    onSuccess();
                }
            } else {
                message.error(response?.message || "Hoàn tiền thất bại");
            }
        } catch (error) {
            console.error("Error refunding payment:", error);
            message.error(error?.response?.data?.message || "Có lỗi xảy ra khi hoàn tiền");
        } finally {
            setLoading(false);
        }
    };

    const refundableAmount = payment ? getRefundableAmount(payment) : 0;

    return (
        <Modal
            title="Hoàn Tiền Thanh Toán"
            open={visible}
            onCancel={onClose}
            onOk={() => form.submit()}
            confirmLoading={loading}
            okText="Xác Nhận Hoàn Tiền"
            cancelText="Hủy"
            okButtonProps={{ danger: true }}
            width={500}
        >
            {payment && (
                <>
                    <Alert
                        message={`Số tiền có thể hoàn: ${formatCurrency(refundableAmount)}`}
                        type="info"
                        showIcon
                        style={{ marginBottom: 16 }}
                    />

                    <Form
                        form={form}
                        layout="vertical"
                        onFinish={handleSubmit}
                        initialValues={{
                            refundAmount: refundableAmount,
                        }}
                    >
                        <Form.Item
                            label="Số tiền hoàn"
                            name="refundAmount"
                            rules={[
                                { required: true, message: "Vui lòng nhập số tiền hoàn" },
                                {
                                    type: "number",
                                    min: 1,
                                    message: "Số tiền hoàn phải lớn hơn 0",
                                },
                                {
                                    type: "number",
                                    max: refundableAmount,
                                    message: `Số tiền hoàn không được vượt quá ${formatCurrency(refundableAmount)}`,
                                },
                            ]}
                        >
                            <InputNumber
                                style={{ width: "100%" }}
                                formatter={(value) =>
                                    `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ",")
                                }
                                parser={(value) => value.replace(/,/g, "")}
                                addonAfter="VNĐ"
                                placeholder="Nhập số tiền hoàn"
                            />
                        </Form.Item>

                        <Form.Item
                            label="Lý do hoàn tiền"
                            name="reason"
                            rules={[
                                { required: true, message: "Vui lòng nhập lý do hoàn tiền" },
                                {
                                    min: 10,
                                    message: "Lý do hoàn tiền phải có ít nhất 10 ký tự",
                                },
                            ]}
                        >
                            <TextArea
                                rows={4}
                                placeholder="Nhập lý do hoàn tiền (ví dụ: Khách hàng yêu cầu, Sản phẩm lỗi, ...)"
                            />
                        </Form.Item>

                        <Alert
                            message="Lưu ý"
                            description="Hoàn tiền sẽ được xử lý ngay lập tức và không thể hoàn tác. Vui lòng kiểm tra kỹ thông tin trước khi xác nhận."
                            type="warning"
                            showIcon
                        />
                    </Form>
                </>
            )}
        </Modal>
    );
};

export default RefundPaymentModal;
