# ✨ cosmetic-server

**Spring Boot + MySQL + Docker + Railway Deploy**  
Cosmetic E-Commerce App Backend

Ushbu backend server **mahsulotlar**, **foydalanuvchilar**, **buyurtmalar** va **cart (savat)** funksiyalarini boshqarish uchun yaratilgan.

---

## 🚀 Tech Stack

- Java 17
- Spring Boot 3.3.5
- Spring Web / Spring MVC
- Spring Data JPA
- Spring Security (Basic Auth / JWT optional)
- MySQL (Railway Cloud DB)
- Gradle
- Docker (Multi-stage build)
- Railway Deployment

---

## 📌 BASE URL

```
https://cosmetic-server-production.up.railway.app
```

---

# 👤 AUTH API

## ✅ REGISTER – Yangi user yaratish

**POST** `/api/auth/register`

### Request Body
```json
{
  "email": "test@example.com",
  "password": "123456",
  "fullName": "John Doe"
}
```

### Response
```
Registered!
```

---

## 🔐 LOGIN – Token olish (JWT yoqilgan bo‘lsa)

**POST** `/api/auth/login`

### Request Body
```json
{
  "email": "test@example.com",
  "password": "123456"
}
```

### Response (JWT yoqilgan bo‘lsa)
```json
{
  "token": "eyJhbGc..."
}
```

---

# 🛍 PRODUCT API

## ➕ CREATE PRODUCT
**POST** `/api/products`

```json
{
  "name": "Sneakers",
  "description": "Comfortable running shoes",
  "price": 59.99,
  "imageUrl": "https://example.com/sneakers.png",
  "category": "Shoes"
}
```

---

## 📦 GET ALL PRODUCTS
**GET** `/api/products`

---

## 🔍 GET PRODUCT BY ID
**GET** `/api/products/{id}`

Example:
```
/api/products/1
```

---

## ✏️ UPDATE PRODUCT
**PUT** `/api/products/{id}`

Example:
```
/api/products/1
```

### Request Body
```json
{
  "name": "Updated Sneakers",
  "description": "Better and more comfortable shoes",
  "price": 69.99,
  "imageUrl": "https://example.com/sneakers-new.png",
  "category": "Shoes"
}
```

---

## ❌ DELETE PRODUCT
**DELETE** `/api/products/{id}`

---

# 🛒 CART API

## ➕ ADD TO CART
**POST** `/api/cart/add`

```json
{
  "userId": 1,
  "productId": 3,
  "quantity": 2
}
```

---

## 📥 GET USER CART
**GET** `/api/cart/{userId}`

Example:
```
/api/cart/1
```

---

## ✏️ UPDATE CART ITEM
**PUT** `/api/cart/{cartItemId}?quantity=4`

Example:
```
/api/cart/5?quantity=4
```

---

## ❌ DELETE CART ITEM
**DELETE** `/api/cart/{cartItemId}`

Example:
```
/api/cart/5
```

---

# 📦 ORDER API

## ➕ CREATE ORDER
**POST** `/api/orders/create`

```json
{
  "userId": 1,
  "address": "Seoul, Gangnam",
  "totalAmount": 120000
}
```

---

## 🔍 GET ORDER BY ID
**GET** `/api/orders/{id}`

---

## ✏️ UPDATE ORDER STATUS
**PUT** `/api/orders/{orderId}/status`

### Request Body
```
"PAID"
```

---

## ❌ DELETE ORDER
**DELETE** `/api/orders/{orderId}`

---

# 🐳 Docker Deployment

### Dockerfile
```dockerfile
FROM gradle:9.2.1-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

---

# 🗄 Database Configuration (MySQL – Railway)

`src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://gondola.proxy.rlwy.net:48131/railway?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: YOUR_PASSWORD
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect

server:
  port: ${PORT:8080}
```

---

# 📞 Contact

**Backend Developer:** Ismoil  
🔗 GitHub: https://github.com/ismoil201
