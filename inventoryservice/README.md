# 🏪 Inventory Service

## 📋 Tổng quan
Inventory Service là một microservice quản lý kho hàng được xây dựng theo kiến trúc **Event Sourcing** và **CQRS** sử dụng Axon Framework.

## 🚀 Các chức năng chính

### ✅ **CRUD Nguyên liệu**
- Tạo, đọc, cập nhật, xóa nguyên liệu
- Quản lý số lượng tồn kho, đơn vị tính
- Thông tin nhà cung cấp và chi phí
- Phân loại nguyên liệu theo category

### ✅ **Phiếu Nhập/Xuất Hàng**
- **Stock In**: Nhập hàng vào kho
- **Stock Out**: Xuất hàng khỏi kho
- Theo dõi lịch sử giao dịch
- Validation số lượng tồn kho

### ✅ **Điều chỉnh Tồn kho**
- **Stock Adjustment**: Điều chỉnh số lượng tồn kho
- **Stock Take**: Kiểm kê thực tế vs hệ thống
- Tính toán variance (chênh lệch)

### ✅ **Hệ thống Cảnh báo**
- **Low Stock Alert**: Cảnh báo khi tồn kho thấp
- **Expiry Alert**: Cảnh báo hết hạn sử dụng
- **Critical Alert**: Cảnh báo khẩn cấp
- Tự động kiểm tra theo lịch trình

## 🏗️ Kiến trúc

### **Event Sourcing + CQRS**
```
Commands → Aggregates → Events → Event Handlers → Read Models
Queries → Projections → DTOs → Controllers
```

### **Các thành phần chính:**
- **Commands**: CreateIngredientCommand, StockInCommand, StockOutCommand, etc.
- **Events**: IngredientCreatedEvent, StockInEvent, LowStockAlertEvent, etc.
- **Aggregates**: IngredientAggregate (quản lý state và business logic)
- **Projections**: IngredientProjection, StockTransactionProjection, StockAlertProjection
- **Entities**: Ingredient, StockTransaction, StockAlert

## 🔧 Cấu hình

### **Database**
- **PostgreSQL**: `InventoryService` database
- **Port**: 8003
- **Eureka**: Service discovery

### **Dependencies**
- Spring Boot 3.5.6
- Axon Framework 4.9.1
- Spring Data JPA
- PostgreSQL Driver
- Lombok
- Guava

## 📡 API Endpoints

### **Ingredient Management**
```
POST   /api/inventory/ingredient                    # Tạo nguyên liệu
GET    /api/inventory/ingredient                    # Lấy danh sách (có filter)
GET    /api/inventory/ingredient/{id}              # Lấy theo ID
PUT    /api/inventory/ingredient/{id}               # Cập nhật
DELETE /api/inventory/ingredient/{id}               # Xóa
PUT    /api/inventory/ingredient/{id}/toggle        # Bật/tắt
```

### **Stock Operations**
```
POST   /api/inventory/ingredient/{id}/stock-in      # Nhập hàng
POST   /api/inventory/ingredient/{id}/stock-out     # Xuất hàng
POST   /api/inventory/ingredient/{id}/adjust        # Điều chỉnh
POST   /api/inventory/ingredient/{id}/stock-take    # Kiểm kê
```

### **Stock Transactions**
```
GET    /api/inventory/transactions                   # Lấy tất cả giao dịch
GET    /api/inventory/transactions/ingredient/{id}  # Theo nguyên liệu
GET    /api/inventory/transactions/type/{type}      # Theo loại giao dịch
```

### **Stock Alerts**
```
GET    /api/inventory/alerts                        # Tất cả cảnh báo
GET    /api/inventory/alerts/active                 # Cảnh báo đang hoạt động
GET    /api/inventory/alerts/low-stock              # Cảnh báo tồn kho thấp
GET    /api/inventory/alerts/expiry                 # Cảnh báo hết hạn
GET    /api/inventory/alerts/critical               # Cảnh báo khẩn cấp
```

### **Advanced Queries**
```
GET    /api/inventory/ingredient/low-stock          # Nguyên liệu tồn kho thấp
GET    /api/inventory/ingredient/category/{cat}     # Theo category
```

## 🧪 Testing với Postman

### **Import Collection**
1. Mở Postman
2. Import file `InventoryService_Postman_Collection.json`
3. Set environment variables:
   - `baseUrl`: `http://localhost:8003`
   - `ingredientId`: ID của nguyên liệu để test

### **Test Flow**
1. **Tạo nguyên liệu** → Lấy `ingredientId`
2. **Nhập hàng** → Kiểm tra stock tăng
3. **Xuất hàng** → Kiểm tra stock giảm
4. **Kiểm kê** → So sánh thực tế vs hệ thống
5. **Xem alerts** → Kiểm tra cảnh báo tự động

## 🔄 Event Flow Example

### **Stock In Flow**
```
StockInCommand → IngredientAggregate → StockInEvent → InventoryEventHandler
                                                      ↓
                                              Update Ingredient Stock
                                                      ↓
                                              Create StockTransaction
```

### **Low Stock Alert Flow**
```
StockOutCommand → IngredientAggregate → StockOutEvent → InventoryEventHandler
                                                      ↓
                                              Check Stock Level
                                                      ↓
                                              LowStockAlertEvent (if low)
                                                      ↓
                                              Create StockAlert
```

## ⚙️ Scheduled Tasks

### **Low Stock Check**
- **Frequency**: Mỗi giờ
- **Logic**: Kiểm tra `currentStock <= minStockLevel`
- **Alert**: Tạo LOW_STOCK alert nếu chưa có

### **Expiry Check**
- **Frequency**: Hàng ngày lúc 9:00 AM
- **Logic**: Kiểm tra `expiryDate <= today + 7 days`
- **Alert**: Tạo EXPIRY alert với severity phù hợp

## 🎯 Business Rules

### **Stock Validation**
- Stock In: `quantity > 0` và `newStock <= maxStockLevel`
- Stock Out: `quantity > 0` và `quantity <= currentStock`
- Adjustment: `newStock >= 0`

### **Alert Severity**
- **CRITICAL**: Stock = 0 hoặc đã hết hạn
- **HIGH**: Stock <= minStockLevel hoặc hết hạn trong ngày
- **MEDIUM**: Hết hạn trong 7 ngày
- **LOW**: Các cảnh báo khác

## 🔗 Integration với Menu Service

### **Event Publishing**
Inventory Service có thể publish events để Menu Service lắng nghe:
- `InventoryLowEvent`: Khi tồn kho thấp
- `InventoryRestockedEvent`: Khi nhập hàng
- `InventoryOutOfStockEvent`: Khi hết hàng

### **Menu Service Integration**
Menu Service có thể:
- Tự động tắt món khi nguyên liệu hết
- Tự động bật lại món khi có nguyên liệu
- Hiển thị trạng thái nguyên liệu trong menu

## 🚀 Chạy Service

### **Prerequisites**
- Java 21+
- PostgreSQL
- Eureka Server (port 8761)

### **Start Service**
```bash
cd inventoryservice
mvn spring-boot:run
```

### **Health Check**
```bash
curl http://localhost:8003/actuator/health
```

## 📊 Database Schema

### **ingredients**
- `ingredient_id` (PK)
- `name`, `description`, `unit`
- `current_stock`, `min_stock_level`, `max_stock_level`
- `expiry_date`, `active`
- `supplier_name`, `supplier_contact`
- `unit_cost`, `currency`, `category`

### **stock_transactions**
- `transaction_id` (PK)
- `ingredient_id` (FK)
- `transaction_type`, `quantity`, `unit`
- `transaction_date`, `reference`, `reason`
- `stock_before`, `stock_after`

### **stock_alerts**
- `alert_id` (PK)
- `ingredient_id` (FK)
- `alert_type`, `severity`, `message`
- `alert_date`, `is_active`

## 🎉 Kết luận

Inventory Service đã hoàn thành đầy đủ các chức năng:
- ✅ CRUD nguyên liệu với số lượng tồn và đơn vị tính
- ✅ Phiếu nhập/xuất hàng với validation
- ✅ Điều chỉnh tồn kho và kiểm kê
- ✅ Hệ thống cảnh báo tự động
- ✅ Event Sourcing + CQRS architecture
- ✅ API endpoints đầy đủ
- ✅ Postman collection để test

Service sẵn sàng để production và có thể tích hợp với Menu Service! 🚀
