# 🏪 Project3 – Golden Dragon Restaurant Management System

Hệ thống quản lý nhà hàng **Golden Dragon** được xây dựng theo kiến trúc **microservices**, áp dụng **Event Sourcing** và **CQRS** với Axon Framework và Spring Boot. Mục tiêu là cung cấp một nền tảng quản lý nhà hàng toàn diện, dễ mở rộng, dễ bảo trì và hỗ trợ nhiều dịch vụ nghiệp vụ như menu, kho, đơn hàng, thanh toán, khách hàng thân thiết và thông báo real-time.

---

## 📋 Tổng quan hệ thống

**Các bài toán chính mà hệ thống giải quyết:**

- Quản lý menu (món ăn, danh mục, combo) gắn với nguyên liệu trong kho
- Quản lý kho nguyên liệu với nhập/xuất, điều chỉnh, kiểm kê, cảnh báo tồn kho thấp/hết hạn
- Quản lý đơn hàng với luồng trạng thái đầy đủ, đồng bộ với trạng thái thanh toán
- Hỗ trợ thanh toán đa kênh (PayPal, VietQR, tiền mặt)
- Chương trình khách hàng thân thiết: điểm thưởng, voucher, khuyến mãi
- Thông báo real-time cho nhân viên/quản lý về đơn mới, thanh toán, tồn kho, khuyến mãi
- Xác thực tập trung, phân quyền theo vai trò (admin, staff, user)

---

## 🧩 Các dịch vụ (Microservices)

| Service                  | Port | Mô tả ngắn                                                              |
| ------------------------ | ---- | ----------------------------------------------------------------------- |
| **API Gateway**          | 8081 | Cổng vào duy nhất, routing, JWT validation, rate limiting (Redis)       |
| **Discovery Server**     | 8761 | Eureka – Service discovery cho toàn hệ thống                            |
| **User Service**         | 8005 | Quản lý người dùng, tích hợp Keycloak, Cloudinary, gửi email            |
| **Menu Service**         | 8002 | Quản lý menu, món ăn, danh mục, combo (Event Sourcing + CQRS)           |
| **Inventory Service**    | 8003 | Quản lý kho nguyên liệu, nhập/xuất, cảnh báo (Event Sourcing + CQRS)    |
| **Order Service**        | 8001 | Quản lý đơn hàng, luồng trạng thái, đồng bộ thanh toán (Event Sourcing) |
| **Payment Service**      | –    | Xử lý thanh toán PayPal, VietQR, tiền mặt; refund; publish events       |
| **Loyalty Service**      | –    | Điểm thưởng, voucher, khuyến mãi, tích điểm từ đơn hàng                 |
| **Notification Service** | –    | Thông báo real-time qua WebSocket/STOMP, lưu lịch sử                    |
| **Frontend (React)**     | 5173 | Giao diện quản trị nhà hàng, kết nối qua API Gateway                    |

Các service giao tiếp với nhau qua:

- **REST API** (đồng bộ, qua API Gateway)
- **Apache Kafka** (bất đồng bộ – ví dụ: OrderCompleted, PaymentCompleted, PaymentRefunded, InventoryLow…)

---

## 🏗️ Kiến trúc & Mô hình

### 🔄 Event Sourcing + CQRS

Hệ thống áp dụng Event Sourcing và CQRS cho các domain quan trọng (Menu, Inventory, Order, Payment, Loyalty):

```text
Commands → Aggregates → Events → Event Handlers → Read Models
Queries  → Projections → DTOs → Controllers
```

- **Event Sourcing**: Trạng thái aggregate được xây dựng từ chuỗi events, cho phép audit và replay.
- **CQRS**: Tách biệt rõ **Command side** (ghi, thay đổi trạng thái) và **Query side** (đọc, tối ưu truy vấn).
- **Axon Framework**: Cung cấp infrastructure cho aggregates, command bus, event bus, event store, projections.

### 🌐 Giao tiếp & tích hợp

- **API Gateway (Spring Cloud Gateway)**:
  - Routing đến các service (userservice, menuservice, inventoryservice, orderservice, paymentservice, loyaltyservice, notificationservice)
  - JWT filter tích hợp Keycloak
  - Rate limiting bằng Redis
- **Eureka Discovery**: Các service tự đăng ký/tìm kiếm nhau.
- **Kafka**: Trao đổi events giữa Order, Payment, Loyalty, Inventory, Notification.

---

## 🎯 Các tính năng chính

### 🍽️ Quản lý Menu (Menu Service)

- CRUD món ăn (tên, mô tả, giá, hình ảnh, trạng thái hoạt động)
- Quản lý danh mục (categories) theo loại (món chính, món phụ, đồ uống…)
- Quản lý combo (tập hợp nhiều món, giá ưu đãi)
- Gắn nguyên liệu cho món ăn (liên kết với Inventory Service)
- Tự động bật/tắt món dựa trên trạng thái tồn kho (nhận events từ Inventory Service)
- Bulk operations (bật/tắt, xóa hàng loạt)
- Upload ảnh món ăn qua Cloudinary

### 🏪 Quản lý Kho (Inventory Service)

- CRUD nguyên liệu (tên, đơn vị, tồn kho hiện tại, min/max stock, ngày hết hạn, nhà cung cấp…)
- Nhập hàng (Stock In), xuất hàng (Stock Out) với validation
- Điều chỉnh tồn kho (Stock Adjustment), kiểm kê (Stock Take)
- Lịch sử giao dịch tồn kho (stock transactions)
- Hệ thống cảnh báo:
  - **Low Stock**: Tồn kho thấp hơn ngưỡng
  - **Expiry**: Sắp hết hạn
  - **Critical**: Hết hàng hoặc đã hết hạn
- Scheduled tasks tự động kiểm tra tồn kho và ngày hết hạn
- Publish events (InventoryLowEvent, InventoryOutOfStockEvent, InventoryRestockedEvent) cho Menu Service/Notification Service.

### 🧾 Quản lý Đơn hàng (Order Service)

- Tạo đơn hàng (chọn món, số lượng, ghi chú, loại đơn: DINE_IN, TAKEAWAY, DELIVERY)
- Luồng trạng thái: `PENDING → COOKING → READY → DELIVERING → COMPLETED` (hoặc `CANCELLED`)
- Chia bill (split bill)
- Đồng bộ trạng thái thanh toán qua events từ Payment Service
- Lưu lịch sử đơn hàng bằng Event Sourcing.

### 💳 Thanh toán (Payment Service)

- Hỗ trợ nhiều phương thức:
  - **PayPal** (PayPal Checkout SDK)
  - **VietQR** (tạo mã QR thanh toán ngân hàng nội địa)
  - **Tiền mặt**
- Trạng thái thanh toán: PENDING, SUCCESS, FAILED, REFUNDED, PARTIALLY_REFUNDED
- Xử lý refund, publish PaymentRefundedEvent cho Loyalty/Order/Notification.

### ⭐ Khách hàng thân thiết (Loyalty Service)

- Tài khoản điểm (loyalty account) cho từng user
- Tự động tích điểm khi đơn hàng hoàn thành (consume OrderCompleted/PaymentCompleted)
- Đổi điểm lấy voucher, theo dõi lịch sử điểm (EARNED / REDEEMED)
- Quản lý voucher (mã giảm giá) và khuyến mãi (COMBO, HAPPY_HOUR, DISCOUNT, FREE_ITEM, POINTS_MULTIPLIER).

### 🔔 Thông báo (Notification Service)

- Gửi thông báo real-time qua WebSocket/STOMP
- Lắng nghe events từ nhiều service (Order, Payment, Menu, Inventory, Loyalty)
- Lưu lịch sử thông báo trong database, API để lấy danh sách và đánh dấu đã đọc.

### 👥 Người dùng & phân quyền (User Service + Keycloak)

- Đăng ký / đăng nhập qua Keycloak (OAuth2/OIDC)
- Phân quyền theo vai trò: ROLE_USER, ROLE_ADMIN, ROLE_STAFF
- Quản lý hồ sơ người dùng, avatar (Cloudinary), xác minh email
- Đồng bộ user từ Keycloak về database nội bộ.

### 💻 Frontend (restaurant-frontend)

- Giao diện quản trị xây dựng bằng React + Vite, Ant Design, TailwindCSS
- Các trang chính:
  - Dashboard, Menu Management, Menu Categories, Menu Combos, Menu View
  - Inventory Management, Inventory Alerts, Inventory Transactions
  - Order Management, Loyalty Management, Voucher Management, Promotion Management
  - Notifications, Staff, Profile, Payment Success
- Real-time notifications, modal thanh toán, polling trạng thái thanh toán.

---

## 🛠️ Tech Stack

### Backend

- **Ngôn ngữ**: Java 21
- **Framework**: Spring Boot 3.5.x, Spring Cloud 2025.0.0
- **Event Sourcing & CQRS**: Axon Framework 4.9.1 + Axon Server
- **Giao tiếp**: REST (Spring Web), Apache Kafka (Spring Kafka)
- **Auth**: Keycloak (OAuth2/OIDC, JWT)
- **Persistence**: Spring Data JPA, PostgreSQL (database-per-service)
- **Khác**: Redis (rate limiting), Lombok, SpringDoc OpenAPI (Swagger), PayPal SDK, Spring Mail, WebFlux (HTTP client cho VietQR)

### Frontend

- **React 18** + **Vite**
- **Ant Design 5** + **TailwindCSS**
- **React Router DOM** cho routing
- **Axios** cho HTTP client
- **STOMP + SockJS** cho WebSocket notifications
- **dayjs**, **qrcode.react**, **cloudinary-core** cho tiện ích UI.

### Database & Infrastructure

- **PostgreSQL** – cho dữ liệu transactional (menuservice, inventoryservice, orderservice, userservice, paymentservice, loyaltyservice, notificationservice, keycloak)
- **Axon Server** – event store cho Event Sourcing
- **Redis** – cache/rate limiting cho API Gateway
- **Eureka** – service discovery
- **Spring Cloud Gateway** – API Gateway
- **Docker / Render / Vercel** – hướng triển khai production (xem `DEPLOYMENT_GUIDE.md` và `render.yaml`).

---

## 📚 Tài liệu liên quan trong repo

- `SWAGGER_INTEGRATION_GUIDE.md` – Hướng dẫn tích hợp Swagger/OpenAPI cho các service
- `inventoryservice/README.md` – Mô tả chi tiết Inventory Service
- `DEPLOYMENT_GUIDE.md` – Hướng dẫn deploy Project3 lên Render & Vercel
- `restaurant-frontend/README.md` – README riêng cho frontend

---

## 👤 Tác giả & Repository

- **Tác giả chính**: Đỗ Đình Trung
- **GitHub**: [@trung2604](https://github.com/trung2604)
- **Repository**: `https://github.com/trung2604/Project3`
