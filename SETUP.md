# 🔧 Project Setup Guide

## 📋 Prerequisites
- Java 21+
- PostgreSQL
- Maven 3.6+

## 🚀 Quick Setup

### 1. Database Setup
Create PostgreSQL databases:
```sql
CREATE DATABASE "MenuService";
CREATE DATABASE "InventoryService";
CREATE DATABASE "OrderService";
```

### 2. Configuration Files
Copy example configuration files and update with your settings:

```bash
# Copy example files
cp apigateway/src/main/resources/application.properties.example apigateway/src/main/resources/application.properties
cp menuservice/src/main/resources/application.properties.example menuservice/src/main/resources/application.properties
cp inventoryservice/src/main/resources/application.properties.example inventoryservice/src/main/resources/application.properties
cp discoveryserver/src/main/resources/application.properties.example discoveryserver/src/main/resources/application.properties
cp orderservice/src/main/resources/application.properties.example orderservice/src/main/resources/application.properties
cp commonservice/src/main/resources/application.properties.example commonservice/src/main/resources/application.properties
```

**Important**: Update database passwords in each `application.properties` file!

### 3. Start Services (in order)

```bash
# 1. Start Discovery Server
cd discoveryserver
mvn spring-boot:run

# 2. Start API Gateway
cd ../apigateway
mvn spring-boot:run

# 3. Start Menu Service
cd ../menuservice
mvn spring-boot:run

# 4. Start Inventory Service
cd ../inventoryservice
mvn spring-boot:run

# 5. Start Order Service
cd ../orderservice
mvn spring-boot:run
```

### 4. Access Services
- **Eureka Dashboard**: http://localhost:8761
- **API Gateway**: http://localhost:8080
- **Menu Service Swagger**: http://localhost:8002/swagger-ui.html
- **Inventory Service Swagger**: http://localhost:8003/swagger-ui.html
- **Order Service Swagger**: http://localhost:8004/swagger-ui.html

## 🔒 Security Notes
- Configuration files with sensitive data are excluded from git
- Each developer should create their own `application.properties` files
- Never commit database credentials or API keys to git

## 🐛 Troubleshooting
- Ensure PostgreSQL is running
- Check that all databases exist
- Verify Eureka registration at http://localhost:8761
- Check service logs for connection errors
