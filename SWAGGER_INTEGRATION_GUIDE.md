# 📚 **SWAGGER DOCUMENTATION CHO PROJECT3**

## 🎯 **Tổng quan**
Đã tích hợp **Swagger/OpenAPI** vào cả hai microservices để có API documentation tự động và interactive testing.

## 🚀 **Các Service đã tích hợp Swagger**

### ✅ **1. Inventory Service (Port 8003)**
- **Swagger UI**: `http://localhost:8003/swagger-ui.html`
- **API Docs**: `http://localhost:8003/api-docs`
- **Base URL**: `http://localhost:8003`

### ✅ **2. Menu Service (Port 8002)**
- **Swagger UI**: `http://localhost:8002/swagger-ui.html`
- **API Docs**: `http://localhost:8002/api-docs`
- **Base URL**: `http://localhost:8002`

## 📋 **Cấu hình Swagger**

### **Dependencies đã thêm:**
```xml
<!-- Swagger/OpenAPI Documentation -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>
```

### **Application Properties:**
```properties
# Swagger/OpenAPI Configuration
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
springdoc.swagger-ui.tryItOutEnabled=true
springdoc.swagger-ui.filter=true
springdoc.swagger-ui.displayRequestDuration=true
springdoc.swagger-ui.showExtensions=true
springdoc.swagger-ui.showCommonExtensions=true
```

### **Swagger Configuration:**
```java
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI serviceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Service API")
                        .description("Microservice với Event Sourcing và CQRS")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Project3 Team")
                                .email("project3@example.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:PORT")
                                .description("Development Server")
                ));
    }
}
```

## 🔧 **Swagger Annotations đã sử dụng**

### **Controller Level:**
```java
@Tag(name = "Service Commands", description = "APIs để quản lý service (Commands)")
@Tag(name = "Service Queries", description = "APIs để truy vấn thông tin service")
```

### **Method Level:**
```java
@Operation(summary = "Tạo mới", description = "Tạo một entity mới trong hệ thống")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Thành công"),
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
    @ApiResponse(responseCode = "404", description = "Không tìm thấy"),
    @ApiResponse(responseCode = "500", description = "Lỗi server")
})
```

### **Parameter Level:**
```java
@Parameter(description = "ID của entity") @PathVariable String id
@Parameter(description = "Số trang") @RequestParam(defaultValue = "0") int page
```

## 📊 **API Endpoints với Swagger**

### **🏪 Inventory Service APIs**

#### **Ingredient Commands**
- `POST /api/inventory/ingredient` - Tạo nguyên liệu mới
- `PUT /api/inventory/ingredient/{id}` - Cập nhật nguyên liệu
- `DELETE /api/inventory/ingredient/{id}` - Xóa nguyên liệu
- `PUT /api/inventory/ingredient/{id}/toggle` - Bật/tắt nguyên liệu

#### **Stock Operations**
- `POST /api/inventory/ingredient/{id}/stock-in` - Nhập hàng
- `POST /api/inventory/ingredient/{id}/stock-out` - Xuất hàng
- `POST /api/inventory/ingredient/{id}/adjust` - Điều chỉnh tồn kho
- `POST /api/inventory/ingredient/{id}/stock-take` - Kiểm kê

#### **Ingredient Queries**
- `GET /api/inventory/ingredient` - Lấy danh sách nguyên liệu (có filter)
- `GET /api/inventory/ingredient/{id}` - Lấy nguyên liệu theo ID
- `GET /api/inventory/ingredient/low-stock` - Nguyên liệu tồn kho thấp
- `GET /api/inventory/ingredient/category/{category}` - Theo loại

#### **Stock Transactions**
- `GET /api/inventory/transactions` - Lấy tất cả giao dịch
- `GET /api/inventory/transactions/ingredient/{id}` - Theo nguyên liệu
- `GET /api/inventory/transactions/type/{type}` - Theo loại giao dịch

#### **Stock Alerts**
- `GET /api/inventory/alerts` - Tất cả cảnh báo
- `GET /api/inventory/alerts/active` - Cảnh báo đang hoạt động
- `GET /api/inventory/alerts/low-stock` - Cảnh báo tồn kho thấp
- `GET /api/inventory/alerts/expiry` - Cảnh báo hết hạn
- `GET /api/inventory/alerts/critical` - Cảnh báo khẩn cấp

### **🍽️ Menu Service APIs**

#### **Menu Item Commands**
- `POST /api/restaurant/menu/items` - Tạo món ăn mới
- `PUT /api/restaurant/menu/items/{id}` - Cập nhật món ăn
- `DELETE /api/restaurant/menu/items/{id}` - Xóa món ăn
- `PATCH /api/restaurant/menu/items/{id}/active` - Bật/tắt món ăn
- `PATCH /api/restaurant/menu/items/{id}/ingredients` - Gắn nguyên liệu
- `PATCH /api/restaurant/menu/items/{id}/auto-toggle` - Tự động bật/tắt

#### **Category Commands**
- `POST /api/restaurant/category` - Tạo danh mục mới
- `PUT /api/restaurant/category/{id}` - Cập nhật danh mục
- `DELETE /api/restaurant/category/{id}` - Xóa danh mục
- `PUT /api/restaurant/category/{id}/toggle` - Bật/tắt danh mục

#### **Combo Commands**
- `POST /api/restaurant/combo` - Tạo combo mới
- `PUT /api/restaurant/combo/{id}` - Cập nhật combo
- `DELETE /api/restaurant/combo/{id}` - Xóa combo
- `PUT /api/restaurant/combo/{id}/toggle` - Bật/tắt combo
- `POST /api/restaurant/combo/{id}/add-item` - Thêm món vào combo
- `DELETE /api/restaurant/combo/{id}/remove-item` - Xóa món khỏi combo

#### **Menu Queries**
- `GET /api/restaurant/menu/items` - Lấy danh sách món ăn (có filter)
- `GET /api/restaurant/menu/items/{id}` - Lấy món ăn theo ID
- `GET /api/restaurant/category` - Lấy danh sách danh mục
- `GET /api/restaurant/category/{id}` - Lấy danh mục theo ID
- `GET /api/restaurant/category/type/{type}` - Lấy danh mục theo loại
- `GET /api/restaurant/combo` - Lấy danh sách combo
- `GET /api/restaurant/combo/{id}` - Lấy combo theo ID
- `GET /api/restaurant/combo/menu-item/{id}` - Lấy combo theo món ăn

## 🎨 **Tính năng Swagger UI**

### **✅ Đã cấu hình:**
- **Operations Sorter**: Sắp xếp theo method (GET, POST, PUT, DELETE)
- **Tags Sorter**: Sắp xếp theo alphabet
- **Try It Out**: Cho phép test API trực tiếp
- **Filter**: Tìm kiếm API
- **Request Duration**: Hiển thị thời gian response
- **Extensions**: Hiển thị các extension
- **Common Extensions**: Hiển thị common extensions

### **🎯 Cách sử dụng:**
1. **Truy cập Swagger UI**: `http://localhost:PORT/swagger-ui.html`
2. **Xem API Documentation**: Tất cả endpoints được nhóm theo tags
3. **Test API**: Click "Try it out" → Nhập data → Execute
4. **Xem Response**: Kết quả trả về với status code và data
5. **Download OpenAPI Spec**: JSON format từ `/api-docs`

## 🔍 **Ví dụ Test API**

### **Test Inventory Service:**
```bash
# 1. Truy cập Swagger UI
http://localhost:8003/swagger-ui.html

# 2. Test tạo nguyên liệu
POST /api/inventory/ingredient
{
  "name": "Tomato",
  "description": "Fresh red tomatoes",
  "unit": "kg",
  "initialStock": 50.0,
  "minStockLevel": 10.0,
  "maxStockLevel": 100.0,
  "expiryDate": "2024-12-31",
  "supplierName": "Fresh Farm Co.",
  "supplierContact": "+84 123 456 789",
  "unitCost": 15000.0,
  "currency": "VND",
  "category": "Vegetables"
}

# 3. Test nhập hàng
POST /api/inventory/ingredient/{ingredientId}/stock-in
{
  "quantity": 25.0,
  "unit": "kg",
  "unitCost": 16000.0,
  "reference": "INV-2024-001",
  "notes": "Fresh delivery from supplier",
  "createdBy": "admin"
}
```

### **Test Menu Service:**
```bash
# 1. Truy cập Swagger UI
http://localhost:8002/swagger-ui.html

# 2. Test tạo món ăn
POST /api/restaurant/menu/items
{
  "name": "Phở Bò",
  "categoryId": "cat-food-001",
  "description": "Phở bò truyền thống",
  "price": 45000.0,
  "ingredients": ["bánh phở", "thịt bò", "hành tây", "rau thơm"]
}

# 3. Test tạo danh mục
POST /api/restaurant/category
{
  "name": "Món Chính",
  "type": "FOOD",
  "description": "Các món ăn chính"
}
```

## 🚀 **Chạy và Test**

### **1. Start Services:**
```bash
# Terminal 1 - Eureka Server
cd discoveryserver
mvn spring-boot:run

# Terminal 2 - Menu Service
cd menuservice
mvn spring-boot:run

# Terminal 3 - Inventory Service
cd inventoryservice
mvn spring-boot:run
```

### **2. Truy cập Swagger UI:**
- **Menu Service**: http://localhost:8002/swagger-ui.html
- **Inventory Service**: http://localhost:8003/swagger-ui.html

### **3. Test Workflow:**
1. **Tạo nguyên liệu** trong Inventory Service
2. **Tạo danh mục** trong Menu Service
3. **Tạo món ăn** và gắn nguyên liệu
4. **Nhập/xuất hàng** để test stock operations
5. **Xem alerts** khi tồn kho thấp

## 📈 **Lợi ích của Swagger Integration**

### **✅ Cho Developers:**
- **API Documentation**: Tự động cập nhật khi code thay đổi
- **Interactive Testing**: Test API trực tiếp trên browser
- **Request/Response Examples**: Xem format data chuẩn
- **Error Handling**: Hiểu rõ các error codes

### **✅ Cho QA/Testing:**
- **API Testing**: Không cần Postman để test cơ bản
- **Data Validation**: Kiểm tra validation rules
- **Integration Testing**: Test flow giữa các services

### **✅ Cho Frontend Developers:**
- **API Contract**: Hiểu rõ API structure
- **Data Models**: Xem DTOs và response format
- **Authentication**: Hiểu cách authenticate (nếu có)

### **✅ Cho DevOps:**
- **API Monitoring**: Theo dõi API health
- **Performance**: Xem request duration
- **Documentation**: Maintain API docs tự động

## 🎉 **Kết luận**

**Đã tích hợp thành công Swagger/OpenAPI vào cả hai services:**

✅ **Inventory Service**: 21 API endpoints với full documentation
✅ **Menu Service**: 21 API endpoints với full documentation  
✅ **Interactive Testing**: Test API trực tiếp trên browser
✅ **Auto Documentation**: Tự động cập nhật khi code thay đổi
✅ **Professional UI**: Swagger UI với đầy đủ tính năng

**Services sẵn sàng cho development, testing và production!** 🚀

### **🔗 Quick Links:**
- **Menu Service Swagger**: http://localhost:8002/swagger-ui.html
- **Inventory Service Swagger**: http://localhost:8003/swagger-ui.html
- **Eureka Dashboard**: http://localhost:8761
