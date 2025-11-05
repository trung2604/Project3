import React, { useState, useEffect } from 'react';
import {
    Card,
    Row,
    Col,
    Input,
    Select,
    Image,
    Tag,
    Space,
    Badge,
    Empty
} from 'antd';
import {
    SearchOutlined
} from '@ant-design/icons';
import { menuService } from '../services/menuService';
import { PAGINATION } from '../constants.js';
import { useAuth } from '../context/AuthContext';
import Loading from '../components/Common/Loading';

const { Option } = Select;
const { Search } = Input;

const MenuView = () => {
    const { role } = useAuth();
    const [menuItems, setMenuItems] = useState([]);
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(false);
    const [filters, setFilters] = useState({
        categoryId: '',
        search: ''
    });

    // Load menu items (only active items)
    const loadMenuItems = async () => {
        setLoading(true);
        try {
            const params = {
                page: 0,
                size: 1000, // Get all items for customer view
                active: true, // Only show active items
                ...(filters.categoryId && filters.categoryId.trim() !== '' && { categoryId: filters.categoryId }),
                ...(filters.search && filters.search.trim() !== '' && { search: filters.search })
            };

            const response = await menuService.getMenuItems(params);
            const data = response.data;
            setMenuItems(data.items || []);
        } catch (error) {
            console.error('Error loading menu items:', error);
        } finally {
            setLoading(false);
        }
    };

    // Load categories
    const loadCategories = async () => {
        try {
            const response = await menuService.getCategories();
            setCategories(response.data || []);
        } catch (error) {
            console.error('Error loading categories:', error);
        }
    };

    useEffect(() => {
        loadMenuItems();
        loadCategories();
    }, [filters]);

    const handleSearch = (value) => {
        setFilters(prev => ({ ...prev, search: value }));
    };

    const handleCategoryChange = (value) => {
        setFilters(prev => ({ ...prev, categoryId: value || '' }));
    };

    if (loading) {
        return <Loading tip="Đang tải thực đơn..." />;
    }

    return (
        <div style={{ padding: '24px', maxWidth: '1400px', margin: '0 auto' }}>
            <div style={{ marginBottom: '24px' }}>
                <h1 style={{ fontSize: '28px', fontWeight: 'bold', marginBottom: '8px', color: '#262626' }}>
                    Thực đơn
                </h1>
                <p style={{ color: '#8c8c8c', margin: 0, fontSize: '16px' }}>
                    Xem các món ăn đang có trong nhà hàng
                </p>
            </div>

            {/* Filters */}
            <Card style={{ marginBottom: '24px' }}>
                <Space direction="vertical" style={{ width: '100%' }} size="middle">
                    <Row gutter={16}>
                        <Col xs={24} sm={12} md={8}>
                            <Search
                                placeholder="Tìm kiếm món ăn..."
                                allowClear
                                onSearch={handleSearch}
                                style={{ width: '100%' }}
                            />
                        </Col>
                        <Col xs={24} sm={12} md={8}>
                            <Select
                                placeholder="Chọn danh mục"
                                allowClear
                                onChange={handleCategoryChange}
                                style={{ width: '100%' }}
                            >
                                {categories.map(category => (
                                    <Option key={category.categoryId} value={category.categoryId}>
                                        {category.name}
                                    </Option>
                                ))}
                            </Select>
                        </Col>
                    </Row>
                </Space>
            </Card>

            {/* Menu Items Grid */}
            {menuItems.length === 0 ? (
                <Card>
                    <Empty description="Không có món ăn nào" />
                </Card>
            ) : (
                <Row gutter={[16, 16]}>
                    {menuItems.map(item => (
                        <Col xs={24} sm={12} md={8} lg={6} key={item.menuItemId}>
                            <Card
                                hoverable
                                cover={
                                    <div style={{ height: '200px', overflow: 'hidden', backgroundColor: '#f5f5f5' }}>
                                        <Image
                                            src={item.imageUrl || '/placeholder-food.jpg'}
                                            alt={item.name}
                                            height={200}
                                            width="100%"
                                            style={{ objectFit: 'cover' }}
                                            fallback="/placeholder-food.jpg"
                                            preview={false}
                                        />
                                    </div>
                                }
                                style={{ height: '100%' }}
                            >
                                <div style={{ padding: '8px 0' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                                        <h3 style={{ margin: 0, fontSize: '16px', fontWeight: '600', color: '#262626' }}>
                                            {item.name}
                                        </h3>
                                        {item.categoryName && (
                                            <Tag color="blue">{item.categoryName}</Tag>
                                        )}
                                    </div>
                                    {item.description && (
                                        <p style={{
                                            fontSize: '13px',
                                            color: '#8c8c8c',
                                            margin: '0 0 8px 0',
                                            display: '-webkit-box',
                                            WebkitLineClamp: 2,
                                            WebkitBoxOrient: 'vertical',
                                            overflow: 'hidden'
                                        }}>
                                            {item.description}
                                        </p>
                                    )}
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '12px' }}>
                                        <div style={{ fontSize: '18px', fontWeight: 'bold', color: '#f59e0b' }}>
                                            {item.price?.toLocaleString()} VNĐ
                                        </div>
                                        {item.active && (
                                            <Badge status="success" text="Đang bán" />
                                        )}
                                    </div>
                                </div>
                            </Card>
                        </Col>
                    ))}
                </Row>
            )}
        </div>
    );
};

export default MenuView;

