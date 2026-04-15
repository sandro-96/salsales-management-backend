# salsales-management-backend

## Chạy local (Windows)

### Yêu cầu
- **Java 17**
- **MongoDB** (mặc định dev/test trỏ về `mongodb://localhost:27017`)
- (Tuỳ chọn) **Redis** nếu bạn bật cache Redis

### Cấu hình môi trường (dev)
Backend dùng profile `dev` mặc định (xem `src/main/resources/application.properties`).

Các biến môi trường thường cần (dev có default an toàn để chạy local):
- **MongoDB**: `MONGODB_URI`, `MONGODB_DATABASE`
- **JWT**: `APP_JWT_SECRET` (khuyến nghị đặt chuỗi >= 32 ký tự)
- **CORS**: `FRONTEND_URL` (mặc định `http://localhost:5173`)
- **Verify URL**: `FRONTEND_VERIFY_URL` (mặc định `http://localhost:5173/verify`)
- **Webhook**: `WEBHOOK_SECRET`
- **AWS S3** (nếu dùng upload): `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `AWS_S3_BUCKET`, `AWS_REGION`
- **Mail** (nếu bật gửi mail): `MAIL_USERNAME`, `MAIL_PASSWORD` (+ tuỳ chọn `MAIL_HOST`, `MAIL_PORT`)

### Chạy

```bash
.\mvnw.cmd spring-boot:run
```

Mặc định chạy ở `http://localhost:8080`.

### Swagger
Thông thường bạn có thể mở Swagger UI tại:
- `http://localhost:8080/swagger-ui/index.html`

## Test

```bash
.\mvnw.cmd test
```