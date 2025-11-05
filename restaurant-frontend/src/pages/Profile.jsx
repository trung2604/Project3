import React, { useState, useEffect } from 'react';
import {
    Card,
    Form,
    Input,
    Button,
    Upload,
    Avatar,
    Space,
    Row,
    Col,
    App,
    DatePicker
} from 'antd';
import {
    UserOutlined,
    UploadOutlined,
    SaveOutlined,
    LockOutlined,
    MailOutlined,
    PhoneOutlined
} from '@ant-design/icons';
import { userService } from '../services/userService';
import { cloudinaryService } from '../services/cloudinaryService';
import { useAuth } from '../context/AuthContext';
import Loading from '../components/Common/Loading';
import dayjs from 'dayjs';

const { TextArea } = Input;

export default function Profile() {
    const { user, updateUser } = useAuth();
    const { message: antMessage } = App.useApp();
    const [form] = Form.useForm();
    const [passwordForm] = Form.useForm();

    const [loading, setLoading] = useState(false);
    const [passwordLoading, setPasswordLoading] = useState(false);
    const [uploading, setUploading] = useState(false);
    const [avatarUrl, setAvatarUrl] = useState('');
    const [userData, setUserData] = useState(null);

    useEffect(() => {
        if (user) {
            setUserData(user);
            setAvatarUrl(user.avatarUrl || '');
            form.setFieldsValue({
                firstName: user.firstName || '',
                lastName: user.lastName || '',
                email: user.email || '',
                phone: user.phone || '',
                address: user.address || '',
                dateOfBirth: user.dateOfBirth ? dayjs(user.dateOfBirth) : null
            });
        }
    }, [user, form]);

    const handleUpdateProfile = async (values) => {
        try {
            setLoading(true);
            const updatedData = {
                ...values,
                dateOfBirth: values.dateOfBirth ? values.dateOfBirth.format('YYYY-MM-DD') : null
            };
            const updated = await userService.updateMe(updatedData);
            setUserData(updated);
            updateUser(updated);
            antMessage.success('Cập nhật thông tin thành công');
        } catch (e) {
            antMessage.error(e?.response?.data?.message || 'Lỗi cập nhật thông tin');
        } finally {
            setLoading(false);
        }
    };

    const handleChangePassword = async (values) => {
        try {
            setPasswordLoading(true);
            await userService.changeMyPassword({
                currentPassword: values.currentPassword,
                newPassword: values.newPassword
            });
            passwordForm.resetFields();
            antMessage.success('Đổi mật khẩu thành công');
        } catch (e) {
            antMessage.error(e?.response?.data?.message || 'Lỗi đổi mật khẩu');
        } finally {
            setPasswordLoading(false);
        }
    };

    const handleAvatarUpload = async (file) => {
        try {
            setUploading(true);
            const upload = await cloudinaryService.uploadUserAvatar(file);
            const updated = await userService.updateAvatar(user.userId, {
                avatarUrl: upload.url,
                avatarPublicId: upload.publicId
            });
            setAvatarUrl(updated.avatarUrl);
            setUserData(updated);
            updateUser(updated);
            antMessage.success('Cập nhật avatar thành công');
            return false; // Prevent default upload
        } catch (e) {
            antMessage.error(e?.response?.data?.message || 'Lỗi cập nhật avatar');
            return false;
        } finally {
            setUploading(false);
        }
    };

    const uploadProps = {
        accept: 'image/*',
        beforeUpload: handleAvatarUpload,
        showUploadList: false,
        maxCount: 1
    };

    if (!userData) {
        return <Loading tip="Đang tải thông tin..." />;
    }

    return (
        <div style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
            <Row gutter={[24, 24]}>
                {/* Profile Information Card */}
                <Col xs={24} lg={16}>
                    <Card
                        title={
                            <Space>
                                <UserOutlined />
                                <span>Thông tin cá nhân</span>
                            </Space>
                        }
                        bordered={false}
                    >
                        <Form
                            form={form}
                            layout="vertical"
                            onFinish={handleUpdateProfile}
                            initialValues={{
                                firstName: userData.firstName || '',
                                lastName: userData.lastName || '',
                                email: userData.email || '',
                                phone: userData.phone || '',
                                address: userData.address || '',
                                dateOfBirth: userData.dateOfBirth ? dayjs(userData.dateOfBirth) : null
                            }}
                        >
                            <Row gutter={16}>
                                <Col xs={24} sm={12}>
                                    <Form.Item
                                        label="Họ"
                                        name="firstName"
                                        rules={[{ required: false }]}
                                    >
                                        <Input prefix={<UserOutlined />} placeholder="Nhập họ" />
                                    </Form.Item>
                                </Col>
                                <Col xs={24} sm={12}>
                                    <Form.Item
                                        label="Tên"
                                        name="lastName"
                                        rules={[{ required: false }]}
                                    >
                                        <Input prefix={<UserOutlined />} placeholder="Nhập tên" />
                                    </Form.Item>
                                </Col>
                            </Row>

                            <Form.Item
                                label="Email"
                                name="email"
                                rules={[
                                    { type: 'email', message: 'Email không hợp lệ' },
                                    { required: true, message: 'Vui lòng nhập email' }
                                ]}
                            >
                                <Input
                                    prefix={<MailOutlined />}
                                    placeholder="Nhập email"
                                    disabled
                                />
                            </Form.Item>

                            <Form.Item
                                label="Số điện thoại"
                                name="phone"
                                rules={[
                                    { pattern: /^[0-9]{10,11}$/, message: 'Số điện thoại không hợp lệ' }
                                ]}
                            >
                                <Input prefix={<PhoneOutlined />} placeholder="Nhập số điện thoại" />
                            </Form.Item>

                            <Form.Item
                                label="Địa chỉ"
                                name="address"
                            >
                                <TextArea
                                    rows={3}
                                    placeholder="Nhập địa chỉ"
                                />
                            </Form.Item>

                            <Form.Item
                                label="Ngày sinh"
                                name="dateOfBirth"
                            >
                                <DatePicker
                                    style={{ width: '100%' }}
                                    format="DD/MM/YYYY"
                                    placeholder="Chọn ngày sinh"
                                />
                            </Form.Item>

                            <Form.Item>
                                <Button
                                    type="primary"
                                    htmlType="submit"
                                    icon={<SaveOutlined />}
                                    loading={loading}
                                    size="large"
                                    block
                                >
                                    Lưu thông tin
                                </Button>
                            </Form.Item>
                        </Form>
                    </Card>
                </Col>

                {/* Avatar & Password Card */}
                <Col xs={24} lg={8}>
                    <Space direction="vertical" size="large" style={{ width: '100%' }}>
                        {/* Avatar Card */}
                        <Card
                            title={
                                <Space>
                                    <UserOutlined />
                                    <span>Ảnh đại diện</span>
                                </Space>
                            }
                            bordered={false}
                        >
                            <div style={{ textAlign: 'center' }}>
                                <Avatar
                                    size={120}
                                    src={avatarUrl}
                                    icon={<UserOutlined />}
                                    style={{ marginBottom: 16 }}
                                />
                                <div>
                                    <Upload {...uploadProps}>
                                        <Button
                                            icon={<UploadOutlined />}
                                            loading={uploading}
                                            disabled={uploading}
                                        >
                                            {uploading ? 'Đang tải...' : 'Tải ảnh lên'}
                                        </Button>
                                    </Upload>
                                </div>
                            </div>
                        </Card>

                        {/* Change Password Card */}
                        <Card
                            title={
                                <Space>
                                    <LockOutlined />
                                    <span>Đổi mật khẩu</span>
                                </Space>
                            }
                            bordered={false}
                        >
                            <Form
                                form={passwordForm}
                                layout="vertical"
                                onFinish={handleChangePassword}
                            >
                                <Form.Item
                                    label="Mật khẩu hiện tại"
                                    name="currentPassword"
                                    rules={[
                                        { required: true, message: 'Vui lòng nhập mật khẩu hiện tại' }
                                    ]}
                                >
                                    <Input.Password
                                        prefix={<LockOutlined />}
                                        placeholder="Nhập mật khẩu hiện tại"
                                    />
                                </Form.Item>

                                <Form.Item
                                    label="Mật khẩu mới"
                                    name="newPassword"
                                    rules={[
                                        { required: true, message: 'Vui lòng nhập mật khẩu mới' },
                                        { min: 6, message: 'Mật khẩu phải có ít nhất 6 ký tự' }
                                    ]}
                                >
                                    <Input.Password
                                        prefix={<LockOutlined />}
                                        placeholder="Nhập mật khẩu mới"
                                    />
                                </Form.Item>

                                <Form.Item
                                    label="Xác nhận mật khẩu mới"
                                    name="confirmPassword"
                                    dependencies={['newPassword']}
                                    rules={[
                                        { required: true, message: 'Vui lòng xác nhận mật khẩu' },
                                        ({ getFieldValue }) => ({
                                            validator(_, value) {
                                                if (!value || getFieldValue('newPassword') === value) {
                                                    return Promise.resolve();
                                                }
                                                return Promise.reject(new Error('Mật khẩu xác nhận không khớp'));
                                            }
                                        })
                                    ]}
                                >
                                    <Input.Password
                                        prefix={<LockOutlined />}
                                        placeholder="Xác nhận mật khẩu mới"
                                    />
                                </Form.Item>

                                <Form.Item>
                                    <Button
                                        type="primary"
                                        htmlType="submit"
                                        icon={<LockOutlined />}
                                        loading={passwordLoading}
                                        block
                                    >
                                        Đổi mật khẩu
                                    </Button>
                                </Form.Item>
                            </Form>
                        </Card>
                    </Space>
                </Col>
            </Row>
        </div>
    );
}
