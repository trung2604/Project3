import React, { useEffect, useState } from 'react';
import { Card, Table, Button, Input, Space, Tag, App, Modal, Form } from 'antd';
import { ReloadOutlined, PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { menuService } from '../services/menuService';

const { Search } = Input;

const MenuCategories = () => {
    const { message } = App.useApp();
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const [editing, setEditing] = useState(null);
    const [form] = Form.useForm();

    const load = async () => {
        setLoading(true);
        try {
            const res = await menuService.getCategories();
            setCategories(res.data || []);
        } catch (e) {
            message.error('Lỗi tải danh mục');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { load(); }, []);

    const columns = [
        { title: 'Tên danh mục', dataIndex: 'name', key: 'name' },
        { title: 'Mô tả', dataIndex: 'description', key: 'description' },
        {
            title: 'Loại', dataIndex: 'type', key: 'type',
            render: (t) => <Tag color="blue">{t}</Tag>
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
                            await menuService.deleteCategory(record.categoryId);
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
                await menuService.updateCategory(editing.categoryId, values);
                message.success('Cập nhật thành công');
            } else {
                await menuService.createCategory(values);
                message.success('Tạo thành công');
            }
            setModalVisible(false);
            load();
        } catch { }
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-gray-800">Danh mục</h1>
                <Space>
                    <Button icon={<ReloadOutlined />} onClick={load}>Làm mới</Button>
                    <Button type="primary" icon={<PlusOutlined />} onClick={onCreate}>Thêm danh mục</Button>
                </Space>
            </div>
            <Card className="restaurant-card">
                <Table
                    columns={columns}
                    dataSource={categories}
                    rowKey="categoryId"
                    loading={loading}
                />
            </Card>

            <Modal
                title={editing ? 'Sửa danh mục' : 'Thêm danh mục'}
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
                    <Form.Item name="type" label="Loại" rules={[{ required: true, message: 'Nhập loại' }]}>
                        <Input />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default MenuCategories;


