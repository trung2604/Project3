# 🍽️ Golden Dragon Restaurant Management System

Hệ thống quản lý nhà hàng Golden Dragon được xây dựng với React + Vite, sử dụng Ant Design và TailwindCSS.

## 📋 Thông tin nhà hàng

- **Tên nhà hàng**: Golden Dragon Restaurant
- **Chủ sở hữu**: Đỗ Đình Trung
- **Email**: dodinhtrungthptyv@gmail.com
- **Số điện thoại**: 0383034491
- **Địa chỉ**: 123 Nguyễn Huệ, Quận 1, TP.HCM

## 🚀 Tính năng chính

### 📦 Quản lý kho hàng (Inventory Management)
- ✅ Quản lý nguyên liệu (CRUD)
- ✅ Nhập/xuất hàng
- ✅ Điều chỉnh tồn kho
- ✅ Kiểm kê thực tế
- ✅ Cảnh báo tồn kho thấp
- ✅ Cảnh báo hết hạn
- ✅ Lịch sử giao dịch
- ✅ Báo cáo tồn kho

### 🍽️ Quản lý menu (Menu Management)
- ✅ Quản lý món ăn (CRUD)
- ✅ Quản lý danh mục
- ✅ Quản lý combo
- ✅ Cập nhật giá
- ✅ Quản lý nguyên liệu cho món ăn
- ✅ Trạng thái hoạt động

### 📊 Dashboard
- ✅ Tổng quan hệ thống
- ✅ Thống kê tồn kho
- ✅ Thống kê menu
- ✅ Cảnh báo quan trọng
- ✅ Thao tác nhanh

## 🛠️ Công nghệ sử dụng

- **Frontend**: React 18 + Vite
- **UI Framework**: Ant Design 5.x
- **Styling**: TailwindCSS 3.x
- **Routing**: React Router DOM 6.x
- **HTTP Client**: Axios
- **Icons**: Ant Design Icons
- **Backend API**: Spring Boot Microservices (Port 8081 - API Gateway)

## 📦 Cài đặt và chạy

### Yêu cầu hệ thống
- Node.js >= 16.0.0
- npm >= 8.0.0

### Cài đặt dependencies
```bash
cd restaurant-frontend
npm install
```

### Chạy ứng dụng
```bash
npm run dev
```

Hoặc sử dụng script có sẵn:
```bash
# Windows
start-dev.bat

# Linux/Mac
./start-dev.sh
```

Ứng dụng sẽ chạy tại: `http://localhost:5173`

### 🔧 Khắc phục sự cố

#### Lỗi TailwindCSS PostCSS
```bash
npm install -D @tailwindcss/postcss
```

#### Lỗi React Plugin
```bash
npm install -D @vitejs/plugin-react
```

#### Lỗi import style.css
- Đảm bảo chỉ có file `src/index.css`
- Xóa file `src/style.css` nếu có
- Cập nhật `index.html` để sử dụng `main.jsx`

### Build cho production
```bash
npm run build
```

## 🔧 Cấu hình

### API Configuration
File: `src/constants/index.js`
```javascript
export const API_BASE_URL = 'http://localhost:8081';
```

### Restaurant Information
File: `src/constants/index.js`
```javascript
export const RESTAURANT_INFO = {
  name: 'Golden Dragon Restaurant',
  owner: 'Đỗ Đình Trung',
  email: 'dodinhtrungthptyv@gmail.com',
  phone: '0383034491',
  // ...
};
```

## 📁 Cấu trúc thư mục

```
src/
├── components/          # Components tái sử dụng
│   └── Layout/         # Layout components
├── pages/              # Các trang chính
│   ├── Dashboard.jsx
│   ├── InventoryManagement.jsx
│   ├── InventoryAlerts.jsx
│   ├── InventoryTransactions.jsx
│   └── MenuManagement.jsx
├── services/           # API services
│   ├── api.js
│   ├── inventoryService.js
│   └── menuService.js
├── constants/          # Constants và config
├── utils/              # Utility functions
├── App.jsx            # Main App component
├── main.jsx           # Entry point
└── index.css          # Global styles
```

## 🎨 Theme và Styling

### Ant Design Theme
- **Primary Color**: #f59e0b (Golden)
- **Success Color**: #52c41a (Green)
- **Warning Color**: #faad14 (Yellow)
- **Error Color**: #ff4d4f (Red)

### TailwindCSS Classes
- `.restaurant-card`: Card styling với shadow và hover effects
- `.restaurant-button`: Button với gradient background
- `.restaurant-gradient`: Gradient background cho header

## 🔌 API Integration

### Inventory Service Endpoints
- `GET /api/inventory/ingredient` - Lấy danh sách nguyên liệu
- `POST /api/inventory/ingredient` - Tạo nguyên liệu mới
- `PUT /api/inventory/ingredient/{id}` - Cập nhật nguyên liệu
- `DELETE /api/inventory/ingredient/{id}` - Xóa nguyên liệu
- `POST /api/inventory/ingredient/{id}/stock-in` - Nhập hàng
- `POST /api/inventory/ingredient/{id}/stock-out` - Xuất hàng
- `GET /api/inventory/transactions` - Lấy lịch sử giao dịch
- `GET /api/inventory/alerts` - Lấy danh sách cảnh báo

### Menu Service Endpoints
- `GET /api/restaurant/menu/items` - Lấy danh sách món ăn
- `POST /api/restaurant/menu/items` - Tạo món ăn mới
- `PUT /api/restaurant/menu/items/{id}` - Cập nhật món ăn
- `DELETE /api/restaurant/menu/items/{id}` - Xóa món ăn
- `GET /api/restaurant/category` - Lấy danh mục
- `POST /api/restaurant/category` - Tạo danh mục mới

## 📱 Responsive Design

Ứng dụng được thiết kế responsive với:
- **Mobile**: < 768px
- **Tablet**: 768px - 1024px
- **Desktop**: > 1024px

Sử dụng Ant Design Grid system và TailwindCSS responsive utilities.

## 🚀 Deployment

### Vercel
```bash
npm run build
# Upload dist/ folder to Vercel
```

### Netlify
```bash
npm run build
# Upload dist/ folder to Netlify
```

### Docker
```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build
EXPOSE 3000
CMD ["npm", "run", "preview"]
```

## 🤝 Contributing

1. Fork repository
2. Tạo feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Tạo Pull Request

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

## 📞 Support

Nếu có vấn đề hoặc câu hỏi, vui lòng liên hệ:
- **Email**: dodinhtrungthptyv@gmail.com
- **Phone**: 0383034491

---

**Golden Dragon Restaurant Management System** - Quản lý nhà hàng chuyên nghiệp 🍽️
