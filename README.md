# 🏪 Project3 - Microservices với Event Sourcing

## 📋 Tổng quan
Project3 là một hệ thống microservices được xây dựng theo kiến trúc **Event Sourcing** và **CQRS** sử dụng Axon Framework và Spring Boot.

## 🚀 Các Services

### 🍽️ **Menu Service (Port 8002)**
- **Chức năng**: Quản lý menu nhà hàng
- **Features**: CRUD món ăn, danh mục, combo, gắn nguyên liệu
- **API**: 21 endpoints với Swagger documentation
- **Swagger UI**: http://localhost:8002/swagger-ui.html

### 🏪 **Inventory Service (Port 8003)**
- **Chức năng**: Quản lý kho hàng
- **Features**: CRUD nguyên liệu, nhập/xuất hàng, cảnh báo tồn kho
- **API**: 21 endpoints với Swagger documentation
- **Swagger UI**: http://localhost:8003/swagger-ui.html

### 🔍 **Discovery Server (Port 8761)**
- **Chức năng**: Service discovery với Eureka
- **Dashboard**: http://localhost:8761

## 🏗️ Kiến trúc

### **Event Sourcing + CQRS**
```
Commands → Aggregates → Events → Event Handlers → Read Models
Queries → Projections → DTOs → Controllers
```

### **Tech Stack**
- **Framework**: Spring Boot 3.5.6
- **Event Sourcing**: Axon Framework 4.9.1
- **Database**: PostgreSQL
- **Service Discovery**: Eureka Client
- **API Documentation**: Swagger/OpenAPI
- **Build Tool**: Maven

## 🚀 Quick Start

### **Prerequisites**
- Java 21+
- PostgreSQL
- Maven 3.6+

### **Chạy Services**
```bash
# 1. Start Discovery Server
cd discoveryserver
mvn spring-boot:run

# 2. Start Menu Service
cd menuservice
mvn spring-boot:run

# 3. Start Inventory Service
cd inventoryservice
mvn spring-boot:run
```

### **Truy cập Services**
- **Menu Service Swagger**: http://localhost:8002/swagger-ui.html
- **Inventory Service Swagger**: http://localhost:8003/swagger-ui.html
- **Eureka Dashboard**: http://localhost:8761

## 📚 Documentation

- **Swagger Integration Guide**: [SWAGGER_INTEGRATION_GUIDE.md](./SWAGGER_INTEGRATION_GUIDE.md)
- **Inventory Service**: [inventoryservice/README.md](./inventoryservice/README.md)
- **Postman Collection**: [inventoryservice/InventoryService_Postman_Collection.json](./inventoryservice/InventoryService_Postman_Collection.json)

## 🎯 Features

### ✅ **Event Sourcing**
- Complete CQRS implementation
- Event store với Axon Framework
- Event handlers với business logic

### ✅ **API Documentation**
- Professional Swagger UI
- Interactive API testing
- Complete request/response examples

### ✅ **Production Ready**
- Database integration
- Service discovery
- Error handling
- Auto ID generation
- Scheduled tasks

## 📊 API Endpoints

### **Menu Service (21 endpoints)**
- Menu Items: CRUD + toggle + ingredients + auto-toggle
- Categories: CRUD + toggle + query by type
- Combos: CRUD + toggle + add/remove items

### **Inventory Service (21 endpoints)**
- Ingredients: CRUD + toggle
- Stock Operations: in, out, adjust, take
- Transactions: history tracking
- Alerts: low stock, expiry notifications

## 🤝 Contributing

1. Fork repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

## 👥 Team

- **Project3 Team** - project3@example.com
- **GitHub**: [@trung2604](https://github.com/trung2604)

## 🙏 Acknowledgments

- Axon Framework team
- Spring Boot team
- OpenAPI/Swagger community
