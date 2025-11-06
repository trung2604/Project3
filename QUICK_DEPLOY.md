# 🚀 Quick Start Deployment Guide

## 📋 Tổng quan

Hướng dẫn này sẽ giúp bạn deploy Project3 lên **Render** (backend) và **Vercel** (frontend).

## ⚡ Quick Steps

### 1️⃣ **Chuẩn bị Environment Variables**

Trước khi deploy, chuẩn bị các giá trị sau:

#### **Email (Gmail App Password)**
- `MAIL_USERNAME`: Email của bạn (ví dụ: `your-email@gmail.com`)
- `MAIL_PASSWORD`: Gmail App Password (tạo tại https://myaccount.google.com/apppasswords)

#### **Cloudinary**
- `CLOUDINARY_CLOUD_NAME`: Tên cloud của bạn
- `CLOUDINARY_API_KEY`: API Key
- `CLOUDINARY_API_SECRET`: API Secret

#### **Kafka & Axon Server**
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: URL của Kafka (ví dụ: `your-kafka.railway.app:9092`)
- `AXON_SERVERS`: URL của Axon Server (ví dụ: `your-axon.railway.app:8124`)

#### **Keycloak**
- `KC_BOOTSTRAP_ADMIN_PASSWORD`: Mật khẩu admin cho Keycloak

---

### 2️⃣ **Deploy Frontend lên Vercel**

1. **Đăng nhập**: https://vercel.com
2. **New Project** → Connect GitHub repository
3. **Settings**:
   - **Root Directory**: `restaurant-frontend`
   - **Framework Preset**: Vite
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
4. **Environment Variables**:
   ```
   VITE_API_BASE_URL=https://api-gateway.onrender.com
   VITE_KEYCLOAK_URL=https://keycloak-service.onrender.com
   VITE_KEYCLOAK_REALM=project3
   VITE_KEYCLOAK_CLIENT_ID=project3
   ```
5. **Deploy** → Lưu lại URL frontend (ví dụ: `https://project3-frontend.vercel.app`)

---

### 3️⃣ **Deploy Backend lên Render**

#### **Option A: Sử dụng render.yaml (Khuyến nghị)**

1. **Đăng nhập**: https://render.com
2. **New Blueprint** → Connect GitHub repository
3. **Select `render.yaml`** → Render sẽ tự động detect
4. **Update Environment Variables** trong Render Dashboard:
   - Thay `your-frontend.vercel.app` bằng URL frontend thực tế
   - Điền các giá trị `sync: false` (MAIL_PASSWORD, CLOUDINARY_*, etc.)
5. **Deploy** → Render sẽ deploy tất cả services tự động

#### **Option B: Deploy từng service thủ công**

**Thứ tự deploy:**

1. **Discovery Server** (phải deploy đầu tiên)
2. **Databases** (PostgreSQL cho mỗi service)
3. **Redis** (cho API Gateway)
4. **User Service**
5. **Menu Service**
6. **Inventory Service**
7. **Order Service**
8. **Notification Service**
9. **API Gateway**
10. **Keycloak**

**Cách deploy mỗi service:**

1. **New Web Service** → Connect GitHub repo
2. **Settings**:
   - **Name**: `service-name`
   - **Environment**: `Docker`
   - **Root Directory**: `service-folder`
   - **Dockerfile Path**: `service-folder/Dockerfile`
   - **Docker Context**: `service-folder`
   - **Build Command**: `cd service-folder && mvn clean package -DskipTests`
3. **Environment Variables**: Copy từ `DEPLOYMENT_GUIDE.md`
4. **Deploy**

---

### 4️⃣ **Deploy Kafka & Axon Server**

#### **Railway (Khuyến nghị)**

**Kafka:**
1. **New Project** trên Railway
2. **Deploy from Template** → Chọn "Kafka" hoặc deploy Docker Compose
3. **Lấy Public URL** → Update `SPRING_KAFKA_BOOTSTRAP_SERVERS` trong Render

**Axon Server:**
1. **New Service** → **Deploy from Docker Hub**
2. **Image**: `axoniq/axonserver:latest`
3. **Ports**: Expose `8124` và `8024`
4. **Lấy Public URL** → Update `AXON_SERVERS` trong Render

---

### 5️⃣ **Cập nhật URLs**

Sau khi deploy xong, cập nhật:

1. **Frontend URL** trong Render:
   - `SPRING_CLOUD_GATEWAY_GLOBALCORS_CORS_CONFIGURATIONS_ALLOWEDORIGINS`
   - `IDP_REDIRECT_URI`

2. **Backend URLs** trong Vercel:
   - `VITE_API_BASE_URL`
   - `VITE_KEYCLOAK_URL`

---

### 6️⃣ **Kiểm tra**

✅ **Checklist:**

- [ ] Discovery Server: https://discovery-server.onrender.com
- [ ] Eureka Dashboard hiển thị các services đã register
- [ ] Keycloak: https://keycloak-service.onrender.com
- [ ] API Gateway: https://api-gateway.onrender.com
- [ ] Frontend: https://your-frontend.vercel.app
- [ ] Test login/logout
- [ ] Test API calls từ frontend

---

## 🔧 Troubleshooting

### Service không start
- Kiểm tra logs trong Render Dashboard
- Kiểm tra environment variables đã đúng chưa
- Kiểm tra database connection

### CORS errors
- Đảm bảo frontend URL đúng trong `SPRING_CLOUD_GATEWAY_GLOBALCORS_CORS_CONFIGURATIONS_ALLOWEDORIGINS`
- Không có trailing slash trong URL

### Database connection failed
- Kiểm tra database đã được tạo trong Render
- Kiểm tra credentials trong environment variables

### Keycloak không accessible
- Kiểm tra `KC_HOSTNAME_STRICT=false`
- Kiểm tra `KC_HTTP_ENABLED=true`
- Xem logs của Keycloak service

---

## 📚 Chi tiết

Xem file `DEPLOYMENT_GUIDE.md` để biết chi tiết đầy đủ.

---

## 💡 Tips

1. **Deploy theo thứ tự**: Discovery Server → Databases → Services → Gateway → Frontend
2. **Test từng service** sau khi deploy
3. **Lưu lại tất cả URLs** để dễ quản lý
4. **Monitor logs** trong Render Dashboard
5. **Backup databases** định kỳ

---

**Good luck! 🚀**

