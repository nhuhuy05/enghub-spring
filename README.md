# EngHub Spring Boot

Backend cho hệ thống EngHub, xây bằng Spring Boot với JWT authentication, role/permission authorization, và PostgreSQL.

## Tech Stack

- Java 21
- Spring Boot 3.4
- Spring Web
- Spring Data JPA
- Spring Security + OAuth2 Resource Server
- JWT (Nimbus JOSE)
- PostgreSQL
- MapStruct + Lombok

## Cấu trúc dự án

```text
src/main/java/com/nhuhuy05/enghub
├─ configuration   # Security, CORS, init data
├─ controller      # REST endpoints
├─ dto             # Request/response models
├─ entity          # JPA entities (User, Role, Permission)
├─ enums           # Enum constants
├─ exception       # AppException + global handler
├─ mapper          # MapStruct mappers
├─ repository      # Spring Data repositories
└─ service         # Business logic
```

## Chạy local

### 1) Yêu cầu

- JDK 21
- PostgreSQL

### 2) Cấu hình biến môi trường (khuyên dùng)

```powershell
$env:PORT="8080"
$env:DB_HOST="localhost"
$env:DB_NAME="enghub"
$env:DB_USER="postgres"
$env:DB_PASS="123456"
```

> App dùng `server.servlet.context-path=/enghub`, nên base URL local mặc định là: `http://localhost:8080/enghub`

### 3) Chạy ứng dụng

```powershell
.\mvnw.cmd spring-boot:run
```

## Cấu hình chính

Trong `src/main/resources/application.properties`:

- `server.port=${PORT:8080}`
- `server.servlet.context-path=/enghub`
- `spring.datasource.*` dùng PostgreSQL
- `spring.jpa.hibernate.ddl-auto=update`
- `jwt.signerKey=...` (HS512 secret key)
- `allowed.origins=http://localhost:5173`

## Auth & Authorization

### Public endpoints (không cần token, chỉ cho `POST`)

- `/users`
- `/auth/token`
- `/auth/introspect`
- `/auth/logout` *(đang có trong security config)*

### Protected endpoints

Mọi endpoint còn lại yêu cầu Bearer JWT.

Token được tạo ở `/auth/token` với claims:

- `sub`: email
- `iss`: `nhuhuy05.com`
- `exp`: +1 giờ
- `scope`: danh sách `ROLE_*` + permission names

Phân quyền theo method-level security:

- `getUsers()` dùng `@PreAuthorize("hasRole('ADMIN')")`
- `getUser(id)` dùng `@PostAuthorize("returnObject.email == authentication.name")`

## API chính

### Auth

- `POST /auth/token` - đăng nhập, trả JWT
- `POST /auth/introspect` - kiểm tra token còn hợp lệ

### User

- `POST /users` - tạo user
- `GET /users` - lấy danh sách user (ADMIN)
- `GET /users/{userId}` - lấy user theo id (chỉ chính chủ theo email)
- `GET /users/myInfo` - lấy thông tin user hiện tại
- `PUT /users/{userId}` - cập nhật user
- `DELETE /users/{userId}` - xóa user

### Role

- `POST /roles` - tạo role
- `GET /roles` - danh sách role
- `DELETE /roles/{role}` - xóa role

### Permission

- `POST /permissions` - tạo permission
- `GET /permissions` - danh sách permission
- `DELETE /permissions/{permission}` - xóa permission

## Error response format

Tất cả response bọc trong `ApiResponse<T>`:

```json
{
  "code": 1000,
  "message": "optional",
  "result": {}
}
```

Một số mã lỗi:

- `1002`: USER_EXISTED
- `1005`: USER_NOT_EXISTED
- `1006`: UNAUTHENTICATED
- `1007`: UNAUTHORIZED

## Init data

Khi khởi động app, nếu chưa có user `admin@gmail.com`, hệ thống tạo user admin với password mặc định `admin`.

## Ghi chú bảo mật

- Nên chuyển `jwt.signerKey` sang biến môi trường/secrets manager.
- Nên đổi thông tin DB mặc định trước khi deploy.
- Nên đổi password admin mặc định ngay sau lần chạy đầu.
