import React, { useEffect, useState } from 'react';
import { Card, Table, Button, Input, Space, Tag, App, Modal, Form, InputNumber, Switch } from 'antd';
import { ReloadOutlined, PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { menuService } from '../services/menuService';

const MenuCombos = () => {
    const { message } = App.useApp();
    const [combos, setCombos] = useState([]);
    const [loading, setLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const [editing, setEditing] = useState(null);
    const [form] = Form.useForm();

    const load = async () => {
        setLoading(true);
        try {
            const res = await menuService.getCombos();
            setCombos(res.data || []);
        } catch (e) {
            message.error('Lỗi tải combo');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { load(); }, []);

    const columns = [
        { title: 'Tên combo', dataIndex: 'name', key: 'name' },
        { title: 'Mô tả', dataIndex: 'description', key: 'description' },
        { title: 'Giá', dataIndex: 'price', key: 'price', render: (v) => `${(v || 0).toLocaleString()} VNĐ` },
        {
            title: 'Trạng thái', dataIndex: 'active', key: 'active',
            render: (a) => <Tag color={a ? 'green' : 'red'}>{a ? 'Đang bán' : 'Tạm dừng'}</Tag>
        },
        {
            title: 'Thao tác', key: 'actions',
            render: (_, record) => (
                <Space>
                    <Button size="small" icon={<EditOutlined />} onClick={() => {
                        setEditing(record);
                        form.setFieldsValue(record);
                        setModalVisible(true);
                    }}>Sửa</Button>
                    <Button size="small" icon={<DeleteOutlined />} danger onClick={async () => {
                        try {
                            await menuService.deleteCombo(record.comboId);
                            message.success('Đã xóa');
                            load();
                        } catch {
                            message.error('Lỗi xóa');
                        }
                    }}>Xóa</Button>
                </Space>
            )
        }
    ];

    const onCreate = () => {
        setEditing(null);
        form.resetFields();
        setModalVisible(true);
    };

    const onSubmit = async () => {
        try {
            const values = await form.validateFields();
            if (editing) {
                await menuService.updateCombo(editing.comboId, values);
                message.success('Cập nhật thành công');
            } else {
                await menuService.createCombo(values);
                message.success('Tạo thành công');
            }
            setModalVisible(false);
            load();
        } catch { }
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-gray-800">Combo</h1>
                <Space>
                    <Button icon={<ReloadOutlined />} onClick={load}>Làm mới</Button>
                    <Button type="primary" icon={<PlusOutlined />} onClick={onCreate}>Thêm combo</Button>
                </Space>
            </div>
            <Card className="restaurant-card">
                <Table
                    columns={columns}
                    dataSource={combos}
                    rowKey="comboId"
                    loading={loading}
                />
            </Card>

            <Modal
                title={editing ? 'Sửa combo' : 'Thêm combo'}
                open={modalVisible}
                onOk={onSubmit}
                onCancel={() => setModalVisible(false)}
                okText="Lưu"
                cancelText="Hủy"
            >
                <Form layout="vertical" form={form}>
                    <Form.Item name="name" label="Tên" rules={[{ required: true, message: 'Nhập tên' }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="description" label="Mô tả">
                        <Input />
                    </Form.Item>
                    <Form.Item name="price" label="Giá" rules={[{ required: true, message: 'Nhập giá' }]}>
                        <InputNumber min={0} style={{ width: '100%' }} />
                    </Form.Item>
                    <Form.Item name="active" label="Trạng thái" valuePropName="checked" initialValue={true}>
                        <Switch checkedChildren="Bán" unCheckedChildren="Tạm dừng" />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default MenuCombos;


