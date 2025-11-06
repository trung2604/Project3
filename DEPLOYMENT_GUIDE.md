# 🚀 Hướng dẫn Deploy Project3 lên Render & Vercel

## 📋 Tổng quan kiến trúc

Hệ thống Project3 bao gồm:
- **Frontend**: React/Vite (Port 5173)
- **Backend Services**: Spring Boot microservices
  - API Gateway (Port 8080)
  - User Service (Port 8005)
  - Menu Service (Port 8002)
  - Inventory Service (Port 8003)
  - Order Service (Port 8001)
  - Notification Service
  - Discovery Server (Port 8761)
- **Infrastructure**:
  - PostgreSQL (nhiều databases)
  - Keycloak (Port 8180)
  - Kafka + Zookeeper
  - Axon Server (cho Event Sourcing)
  - Redis (cho API Gateway)

## 🎯 Chiến lược Deployment

### **Option 1: Render (Khuyến nghị cho Production)**
- ✅ Frontend → Render Static Site
- ✅ Backend Services → Render Web Services
- ✅ PostgreSQL → Render PostgreSQL
- ✅ Redis → Render Redis
- ✅ Keycloak → Render Docker Service
- ⚠️ Kafka → Railway hoặc Confluent Cloud
- ⚠️ Axon Server → Railway hoặc Render Docker

### **Option 2: Vercel + Render (Tối ưu cho Frontend)**
- ✅ Frontend → Vercel (tốt nhất cho React)
- ✅ Backend Services → Render Web Services
- ✅ Databases → Render PostgreSQL
- ✅ Infrastructure → Render Docker Services

---

## 📦 BƯỚC 1: Chuẩn bị Repository

### 1.1. Tạo các file cấu hình cần thiết

#### **Dockerfile cho mỗi Service**

Tạo `Dockerfile` trong mỗi service folder:

**`userservice/Dockerfile`**:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8005
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**`menuservice/Dockerfile`**:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8002
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**`inventoryservice/Dockerfile`**:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8003
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**`orderservice/Dockerfile`**:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8001
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**`apigateway/Dockerfile`**:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**`discoveryserver/Dockerfile`**:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**`notificationservice/Dockerfile`**:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### **render.yaml cho Render**

Tạo file `render.yaml` ở root:

```yaml
services:
  # Discovery Server
  - type: web
    name: discovery-server
    env: docker
    dockerfilePath: ./discoveryserver/Dockerfile
    dockerContext: ./discoveryserver
    buildCommand: cd discoveryserver && mvn clean package -DskipTests
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: production
      - key: SERVER_PORT
        value: 8761
      - key: EUREKA_INSTANCE_HOSTNAME
        value: ${HOSTNAME}

  # User Service
  - type: web
    name: user-service
    env: docker
    dockerfilePath: ./userservice/Dockerfile
    dockerContext: ./userservice
    buildCommand: cd userservice && mvn clean package -DskipTests
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: production
      - key: SERVER_PORT
        value: 8005
      - key: SPRING_DATASOURCE_URL
        fromDatabase:
          name: user-service-db
          property: connectionString
      - key: SPRING_DATASOURCE_USERNAME
        fromDatabase:
          name: user-service-db
          property: user
      - key: SPRING_DATASOURCE_PASSWORD
        fromDatabase:
          name: user-service-db
          property: password
      - key: EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE
        value: https://discovery-server.onrender.com/eureka/
      - key: IDP_URL
        value: https://keycloak-service.onrender.com
      - key: IDP_REDIRECT_URI
        value: https://your-frontend.vercel.app/verify-email
      - key: IDP_FRONTEND_URL
        value: https://keycloak-service.onrender.com
      - key: APP_BASE_URL
        value: https://user-service.onrender.com
      - key: MAIL_HOST
        value: smtp.gmail.com
      - key: MAIL_PORT
        value: 587
      - key: MAIL_USERNAME
        sync: false
      - key: MAIL_PASSWORD
        sync: false
      - key: CLOUDINARY_API_KEY
        sync: false
      - key: CLOUDINARY_API_SECRET
        sync: false
      - key: CLOUDINARY_CLOUD_NAME
        sync: false

  # Menu Service
  - type: web
    name: menu-service
    env: docker
    dockerfilePath: ./menuservice/Dockerfile
    dockerContext: ./menuservice
    buildCommand: cd menuservice && mvn clean package -DskipTests
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: production
      - key: SERVER_PORT
        value: 8002
      - key: SPRING_DATASOURCE_URL
        fromDatabase:
          name: menu-service-db
          property: connectionString
      - key: SPRING_DATASOURCE_USERNAME
        fromDatabase:
          name: menu-service-db
          property: user
      - key: SPRING_DATASOURCE_PASSWORD
        fromDatabase:
          name: menu-service-db
          property: password
      - key: EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE
        value: https://discovery-server.onrender.com/eureka/
      - key: SPRING_KAFKA_BOOTSTRAP_SERVERS
        value: ${KAFKA_BOOTSTRAP_SERVERS}
      - key: CLOUDINARY_CLOUD_NAME
        sync: false
      - key: CLOUDINARY_API_KEY
        sync: false
      - key: CLOUDINARY_API_SECRET
        sync: false

  # Inventory Service
  - type: web
    name: inventory-service
    env: docker
    dockerfilePath: ./inventoryservice/Dockerfile
    dockerContext: ./inventoryservice
    buildCommand: cd inventoryservice && mvn clean package -DskipTests
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: production
      - key: SERVER_PORT
        value: 8003
      - key: SPRING_DATASOURCE_URL
        fromDatabase:
          name: inventory-service-db
          property: connectionString
      - key: SPRING_DATASOURCE_USERNAME
        fromDatabase:
          name: inventory-service-db
          property: user
      - key: SPRING_DATASOURCE_PASSWORD
        fromDatabase:
          name: inventory-service-db
          property: password
      - key: EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE
        value: https://discovery-server.onrender.com/eureka/

  # Order Service
  - type: web
    name: order-service
    env: docker
    dockerfilePath: ./orderservice/Dockerfile
    dockerContext: ./orderservice
    buildCommand: cd orderservice && mvn clean package -DskipTests
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: production
      - key: SERVER_PORT
        value: 8001
      - key: SPRING_DATASOURCE_URL
        fromDatabase:
          name: order-service-db
          property: connectionString
      - key: SPRING_DATASOURCE_USERNAME
        fromDatabase:
          name: order-service-db
          property: user
      - key: SPRING_DATASOURCE_PASSWORD
        fromDatabase:
          name: order-service-db
          property: password
      - key: EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE
        value: https://discovery-server.onrender.com/eureka/
      - key: AXON_SERVERS
        value: ${AXON_SERVER_URL}

  # API Gateway
  - type: web
    name: api-gateway
    env: docker
    dockerfilePath: ./apigateway/Dockerfile
    dockerContext: ./apigateway
    buildCommand: cd apigateway && mvn clean package -DskipTests
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: production
      - key: SERVER_PORT
        value: 8080
      - key: SPRING_DATA_REDIS_HOST
        fromService:
          name: redis
          type: redis
          property: host
      - key: SPRING_DATA_REDIS_PORT
        fromService:
          name: redis
          type: redis
          property: port
      - key: EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE
        value: https://discovery-server.onrender.com/eureka/
      - key: SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI
        value: https://keycloak-service.onrender.com/realms/project3
      - key: SPRING_CLOUD_GATEWAY_GLOBALCORS_CORS_CONFIGURATIONS_ALLOWEDORIGINS
        value: https://your-frontend.vercel.app

  # Notification Service
  - type: web
    name: notification-service
    env: docker
    dockerfilePath: ./notificationservice/Dockerfile
    dockerContext: ./notificationservice
    buildCommand: cd notificationservice && mvn clean package -DskipTests
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: production
      - key: SPRING_KAFKA_BOOTSTRAP_SERVERS
        value: ${KAFKA_BOOTSTRAP_SERVERS}

  # Keycloak
  - type: web
    name: keycloak-service
    env: docker
    dockerfilePath: ./Keycloak/Dockerfile.keycloak
    dockerContext: ./Keycloak
    envVars:
      - key: KC_BOOTSTRAP_ADMIN_USERNAME
        value: admin
      - key: KC_BOOTSTRAP_ADMIN_PASSWORD
        sync: false
      - key: KC_DB
        value: postgres
      - key: KC_DB_USERNAME
        fromDatabase:
          name: keycloak-db
          property: user
      - key: KC_DB_PASSWORD
        fromDatabase:
          name: keycloak-db
          property: password
      - key: KC_DB_URL
        fromDatabase:
          name: keycloak-db
          property: connectionString
      - key: KC_MAIL_HOST
        value: smtp.gmail.com
      - key: KC_MAIL_PORT
        value: 587
      - key: KC_MAIL_FROM
        sync: false
      - key: KC_MAIL_USER
        sync: false
      - key: KC_MAIL_PASSWORD
        sync: false
      - key: KC_MAIL_STARTTLS
        value: true
      - key: KC_MAIL_SSL
        value: false

databases:
  - name: user-service-db
    databaseName: userservice
    user: userservice_user
    plan: free

  - name: menu-service-db
    databaseName: menuservice
    user: menuservice_user
    plan: free

  - name: inventory-service-db
    databaseName: inventoryservice
    user: inventoryservice_user
    plan: free

  - name: order-service-db
    databaseName: orderservice
    user: orderservice_user
    plan: free

  - name: keycloak-db
    databaseName: keycloak
    user: keycloak_user
    plan: free
```

---

## 🌐 BƯỚC 2: Deploy Frontend lên Vercel

### 2.1. Tạo file cấu hình Vercel

**`restaurant-frontend/vercel.json`**:
```json
{
  "buildCommand": "npm run build",
  "outputDirectory": "dist",
  "devCommand": "npm run dev",
  "installCommand": "npm install",
  "framework": "vite",
  "rewrites": [
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ],
  "env": {
    "VITE_API_BASE_URL": "https://api-gateway.onrender.com"
  }
}
```

### 2.2. Tạo file environment variables

**`restaurant-frontend/.env.production`**:
```env
VITE_API_BASE_URL=https://api-gateway.onrender.com
VITE_KEYCLOAK_URL=https://keycloak-service.onrender.com
VITE_KEYCLOAK_REALM=project3
VITE_KEYCLOAK_CLIENT_ID=project3
```

### 2.3. Deploy trên Vercel

1. **Đăng nhập Vercel**: https://vercel.com
2. **Import Project**: 
   - Click "New Project"
   - Connect GitHub repository
   - Select `restaurant-frontend` folder
   - Framework Preset: **Vite**
   - Root Directory: `restaurant-frontend`
   - Build Command: `npm run build`
   - Output Directory: `dist`
3. **Environment Variables**:
   - `VITE_API_BASE_URL`: `https://api-gateway.onrender.com`
   - `VITE_KEYCLOAK_URL`: `https://keycloak-service.onrender.com`
   - `VITE_KEYCLOAK_REALM`: `project3`
   - `VITE_KEYCLOAK_CLIENT_ID`: `project3`
4. **Deploy**: Click "Deploy"

---

## 🏗️ BƯỚC 3: Deploy Backend Services lên Render

### 3.1. Tạo Application Profiles

Tạo file `application-production.yml` cho mỗi service:

**`userservice/src/main/resources/application-production.yml`**:
```yaml
server:
  port: ${PORT:8005}

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE:http://localhost:8761/eureka}
  instance:
    prefer-ip-address: true
    hostname: ${HOSTNAME:localhost}

spring:
  application:
    name: userservice
  
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

idp:
  url: ${IDP_URL:http://localhost:8180}
  client-id: project3
  client-secret: ${IDP_CLIENT_SECRET}
  realm: project3
  redirect-uri: ${IDP_REDIRECT_URI}
  frontend-url: ${IDP_FRONTEND_URL}
  smtp:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    from: ${MAIL_USERNAME}
    from-display-name: Project3 Restaurant
    auth: true
    ssl: false
    starttls: true
    user: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}

cloudinary:
  api-key: ${CLOUDINARY_API_KEY}
  api-secret: ${CLOUDINARY_API_SECRET}
  cloud-name: ${CLOUDINARY_CLOUD_NAME}

app:
  base-url: ${APP_BASE_URL}
```

### 3.2. Deploy trên Render

#### **A. Discovery Server**

1. **New Web Service** trên Render Dashboard
2. **Connect Repository**: Chọn GitHub repo
3. **Settings**:
   - **Name**: `discovery-server`
   - **Environment**: `Docker`
   - **Region**: Singapore (gần nhất)
   - **Branch**: `main`
   - **Root Directory**: `discoveryserver`
   - **Dockerfile Path**: `discoveryserver/Dockerfile`
   - **Docker Context**: `discoveryserver`
   - **Build Command**: `cd discoveryserver && mvn clean package -DskipTests`
   - **Start Command**: (để trống, dùng Dockerfile)
4. **Environment Variables**:
   ```
   SPRING_PROFILES_ACTIVE=production
   SERVER_PORT=8761
   ```
5. **Deploy**

#### **B. User Service**

1. **New Web Service**
2. **Settings**:
   - **Name**: `user-service`
   - **Environment**: `Docker`
   - **Root Directory**: `userservice`
   - **Dockerfile Path**: `userservice/Dockerfile`
   - **Docker Context**: `userservice`
   - **Build Command**: `cd userservice && mvn clean package -DskipTests`
3. **Link PostgreSQL Database**:
   - **Name**: `user-service-db`
   - **Database Name**: `userservice`
   - **User**: `userservice_user`
4. **Environment Variables**:
   ```
   SPRING_PROFILES_ACTIVE=production
   SERVER_PORT=8005
   EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=https://discovery-server.onrender.com/eureka/
   IDP_URL=https://keycloak-service.onrender.com
   IDP_REDIRECT_URI=https://your-frontend.vercel.app/verify-email
   IDP_FRONTEND_URL=https://keycloak-service.onrender.com
   APP_BASE_URL=https://user-service.onrender.com
   MAIL_HOST=smtp.gmail.com
   MAIL_PORT=587
   MAIL_USERNAME=your-email@gmail.com
   MAIL_PASSWORD=your-app-password
   CLOUDINARY_API_KEY=your-key
   CLOUDINARY_API_SECRET=your-secret
   CLOUDINARY_CLOUD_NAME=your-cloud-name
   ```
5. **Deploy**

#### **C. Menu Service**

Tương tự User Service, nhưng:
- **Name**: `menu-service`
- **Port**: `8002`
- **Database**: `menu-service-db`
- **Additional Env Vars**:
  ```
  SPRING_KAFKA_BOOTSTRAP_SERVERS=your-kafka-url:9092
  ```

#### **D. Inventory Service**

Tương tự, nhưng:
- **Name**: `inventory-service`
- **Port**: `8003`
- **Database**: `inventory-service-db`

#### **E. Order Service**

Tương tự, nhưng:
- **Name**: `order-service`
- **Port**: `8001`
- **Database**: `order-service-db`
- **Additional Env Vars**:
  ```
  AXON_SERVERS=your-axon-server-url:8124
  ```

#### **F. API Gateway**

1. **New Web Service**
2. **Settings**:
   - **Name**: `api-gateway`
   - **Environment**: `Docker`
   - **Root Directory**: `apigateway`
   - **Dockerfile Path**: `apigateway/Dockerfile`
   - **Docker Context**: `apigateway`
   - **Build Command**: `cd apigateway && mvn clean package -DskipTests`
3. **Link Redis**:
   - **Name**: `redis`
   - **Plan**: Free
4. **Environment Variables**:
   ```
   SPRING_PROFILES_ACTIVE=production
   SERVER_PORT=8080
   EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=https://discovery-server.onrender.com/eureka/
   SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://keycloak-service.onrender.com/realms/project3
   SPRING_CLOUD_GATEWAY_GLOBALCORS_CORS_CONFIGURATIONS_ALLOWEDORIGINS=https://your-frontend.vercel.app
   ```

#### **G. Keycloak**

1. **New Web Service**
2. **Settings**:
   - **Name**: `keycloak-service`
   - **Environment**: `Docker`
   - **Root Directory**: `Keycloak`
   - **Dockerfile Path**: `Keycloak/Dockerfile.keycloak`
   - **Docker Context**: `Keycloak`
3. **Link PostgreSQL Database**:
   - **Name**: `keycloak-db`
   - **Database Name**: `keycloak`
4. **Environment Variables**:
   ```
   KC_BOOTSTRAP_ADMIN_USERNAME=admin
   KC_BOOTSTRAP_ADMIN_PASSWORD=your-secure-password
   KC_DB=postgres
   KC_HOSTNAME_STRICT=false
   KC_HOSTNAME_STRICT_HTTPS=false
   KC_HTTP_ENABLED=true
   KC_MAIL_HOST=smtp.gmail.com
   KC_MAIL_PORT=587
   KC_MAIL_FROM=your-email@gmail.com
   KC_MAIL_USER=your-email@gmail.com
   KC_MAIL_PASSWORD=your-app-password
   KC_MAIL_STARTTLS=true
   KC_MAIL_SSL=false
   ```

---

## 🔧 BƯỚC 4: Deploy Kafka & Axon Server

### Option A: Railway (Khuyến nghị)

#### **Kafka trên Railway**

1. **Tạo mới Project** trên Railway: https://railway.app
2. **Deploy từ Template**: Chọn "Kafka" template
3. **Hoặc Deploy Docker Compose**:
   - Tạo file `railway-kafka.yml`:
   ```yaml
   services:
     zookeeper:
       image: confluentinc/cp-zookeeper:7.8.0
       environment:
         ZOOKEEPER_CLIENT_PORT: 2181
         ZOOKEEPER_TICK_TIME: 2000
     
     broker:
       image: confluentinc/cp-server:7.8.0
       depends_on:
         - zookeeper
       environment:
         KAFKA_BROKER_ID: 1
         KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
         KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://broker:29092,PLAINTEXT_HOST://${RAILWAY_PUBLIC_DOMAIN}:${PORT}
         KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
   ```
4. **Lấy Public URL**: Railway sẽ cung cấp URL public cho Kafka
5. **Update Environment Variables** trong các service:
   ```
   SPRING_KAFKA_BOOTSTRAP_SERVERS=your-railway-kafka-url:9092
   ```

#### **Axon Server trên Railway**

1. **Deploy Docker Image**: `axoniq/axonserver:latest`
2. **Environment Variables**:
   ```
   AXONIQ_AXONSERVER_NAME=axonserver
   AXONIQ_AXONSERVER_HOSTNAME=axonserver
   ```
3. **Ports**: Expose port `8124` (gRPC) và `8024` (HTTP)
4. **Update Environment Variables** trong các service:
   ```
   AXON_SERVERS=your-railway-axon-url:8124
   ```

### Option B: Confluent Cloud (Managed Kafka)

1. **Đăng ký**: https://www.confluent.io/confluent-cloud/
2. **Tạo Cluster**: Chọn plan Free
3. **Lấy Bootstrap Servers**: Format `pkc-xxxxx.region.provider.confluent.cloud:9092`
4. **API Keys**: Tạo API Key và Secret
5. **Update Environment Variables**:
   ```
   SPRING_KAFKA_BOOTSTRAP_SERVERS=pkc-xxxxx.region.provider.confluent.cloud:9092
   SPRING_KAFKA_SECURITY_PROTOCOL=SASL_SSL
   SPRING_KAFKA_SASL_MECHANISM=PLAIN
   SPRING_KAFKA_SASL_JAAS_CONFIG=org.apache.kafka.common.security.plain.PlainLoginModule required username="your-api-key" password="your-api-secret";
   ```

---

## 🔄 BƯỚC 5: Cập nhật Frontend URLs

Sau khi deploy, cập nhật các URLs trong frontend:

**`restaurant-frontend/src/utils/keycloak.js`**:
```javascript
export const KEYCLOAK_URL = import.meta.env.VITE_KEYCLOAK_URL || 'https://keycloak-service.onrender.com';
export const KEYCLOAK_REALM = import.meta.env.VITE_KEYCLOAK_REALM || 'project3';
export const KEYCLOAK_CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'project3';
```

**`restaurant-frontend/src/services/api.js`**:
```javascript
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'https://api-gateway.onrender.com';
```

---

## ✅ BƯỚC 6: Kiểm tra và Test

### Checklist:

- [ ] Discovery Server đang chạy và các service đã register
- [ ] Keycloak accessible và có thể login
- [ ] Frontend có thể connect đến API Gateway
- [ ] Database connections thành công
- [ ] Kafka topics được tạo và messages được consume
- [ ] Axon Server connected và events được lưu
- [ ] CORS được cấu hình đúng
- [ ] Email verification hoạt động

### Test URLs:

- **Frontend**: `https://your-frontend.vercel.app`
- **API Gateway**: `https://api-gateway.onrender.com`
- **Discovery Server**: `https://discovery-server.onrender.com`
- **Keycloak**: `https://keycloak-service.onrender.com`
- **User Service Swagger**: `https://user-service.onrender.com/swagger-ui.html`
- **Menu Service Swagger**: `https://menu-service.onrender.com/swagger-ui.html`

---

## 🐛 Troubleshooting

### 1. Service không register vào Eureka
- Kiểm tra `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` đúng URL
- Kiểm tra network connectivity giữa services
- Xem logs của Discovery Server

### 2. Database connection failed
- Kiểm tra database credentials trong Environment Variables
- Kiểm tra database đã được tạo và accessible
- Xem logs của service để biết lỗi cụ thể

### 3. CORS errors
- Đảm bảo `SPRING_CLOUD_GATEWAY_GLOBALCORS_CORS_CONFIGURATIONS_ALLOWEDORIGINS` có frontend URL
- Kiểm tra frontend URL đúng format (không có trailing slash)

### 4. Keycloak không accessible
- Kiểm tra `KC_HOSTNAME_STRICT=false` và `KC_HTTP_ENABLED=true`
- Kiểm tra database connection
- Xem logs của Keycloak

### 5. Kafka connection timeout
- Kiểm tra Kafka URL và port đúng
- Nếu dùng Railway, đảm bảo public domain được expose
- Kiểm tra firewall/security groups

---

## 💰 Cost Estimation

### Render (Free Tier):
- ✅ 750 hours/month free cho Web Services
- ✅ PostgreSQL databases free (limited storage)
- ✅ Redis free (limited memory)
- ⚠️ Nếu vượt quá: ~$7-25/month per service

### Vercel (Free Tier):
- ✅ Unlimited deployments
- ✅ 100GB bandwidth/month
- ✅ Hobby plan đủ cho production nhỏ

### Railway:
- ✅ $5 credit free/month
- ⚠️ Kafka + Axon Server: ~$10-20/month

### Tổng ước tính: **$0-50/month** cho production nhỏ

---

## 📚 Tài liệu tham khảo

- **Render Docs**: https://render.com/docs
- **Vercel Docs**: https://vercel.com/docs
- **Railway Docs**: https://docs.railway.app
- **Keycloak Deployment**: https://www.keycloak.org/server/containers
- **Confluent Cloud**: https://docs.confluent.io/cloud/current/

---

## 🎯 Next Steps

1. ✅ Tạo các Dockerfile cho mỗi service
2. ✅ Tạo `render.yaml` và `vercel.json`
3. ✅ Setup databases trên Render
4. ✅ Deploy Discovery Server trước
5. ✅ Deploy các services theo thứ tự dependency
6. ✅ Deploy Frontend trên Vercel
7. ✅ Test toàn bộ hệ thống
8. ✅ Setup monitoring và logging (optional)

---

**Lưu ý**: Đây là hướng dẫn tổng quan. Bạn có thể cần điều chỉnh dựa trên cấu hình cụ thể của từng service.

