# salsales-management-backend

## Chạy local (Windows)

### Yêu cầu
- **Java 17**
- **MongoDB** (mặc định dev/test trỏ về `mongodb://localhost:27017`)
- (Tuỳ chọn) **Redis** nếu bạn bật cache Redis

### Cấu hình môi trường (dev)
Backend dùng profile `dev` mặc định (xem `src/main/resources/application.properties`).

File `.env` ở thư mục gốc backend phải lưu **UTF-8** (trong VS Code/Cursor: góc phải status bar → **UTF-8** → Save). Nếu tên chủ TK / brand hiện ký tự `` hoặc `Sá»•`, file đang lưu ANSI/Windows-1252 — mở lại `.env`, chọn **Save with Encoding → UTF-8**, hoặc tạm bỏ dấu (`CHU THANH TRI`).

Các biến môi trường thường cần (dev có default an toàn để chạy local):
- **MongoDB**: `MONGODB_URI`, `MONGODB_DATABASE`
- **JWT**: `APP_JWT_SECRET` (khuyến nghị đặt chuỗi >= 32 ký tự)
- **CORS**: `FRONTEND_URL` (mặc định `http://localhost:5173`)
- **Verify URL**: `FRONTEND_VERIFY_URL` (mặc định `http://localhost:5173/verify`)
- **Webhook**: `WEBHOOK_SECRET`
- **AWS S3** (nếu dùng upload): `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `AWS_S3_BUCKET`, `AWS_REGION`
- **Mail** (nếu bật gửi mail): `MAIL_USERNAME`, `MAIL_PASSWORD` (+ tuỳ chọn `MAIL_HOST`, `MAIL_PORT`)
- **Thông báo đăng ký mới** (gửi tới mọi `ROLE_ADMIN` + email phụ): `ADMIN_USER_REGISTRATION_NOTIFY_ENABLED` (mặc định `true`), `ADMIN_REGISTRATION_NOTIFY_EMAILS` (tuỳ chọn, phân tách bằng dấu phẩy)

### Chạy

```bash
.\mvnw.cmd spring-boot:run
```

Mặc định chạy ở `http://localhost:8080`.

**UTF-8 (Windows local):** Profile `dev` bật `spring.mandatory-file-encoding=UTF-8`. `mvnw spring-boot:run` dùng `.mvn/jvm.config` + plugin JVM UTF-8. IDE: VM options `-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8`.

**Railway/Docker:** Profile `staging`/`prod` không bật mandatory encoding (Alpine cần `LANG=C.UTF-8` trong `Dockerfile` — đã cấu hình). Tiếng Việt vẫn qua `Utf8TextUtil` + mail UTF-8.

### Swagger
Thông thường bạn có thể mở Swagger UI tại:
- `http://localhost:8080/swagger-ui/index.html`

## Test

```bash
.\mvnw.cmd test
```

## Staging (~$0)

Triển khai Vercel + Atlas M0 + Railway: xem [docs/deploy/STAGING.md](../salsales-management-web/docs/deploy/STAGING.md) (trong repo web).

- Profile: `staging` → `application-staging.properties`
- Biến mẫu: `env.staging.example`
- Railway: `railway.toml`
- CORS nhiều origin: `FRONTEND_CORS_ORIGINS` (vd. `https://app.vercel.app,https://*.vercel.app`)