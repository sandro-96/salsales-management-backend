# Phân tích Service Layer

> Cập nhật: 13/03/2026

---

## Tổng quan cấu trúc

```
service/
├── BaseService.java                  ← Abstract base (helper methods)
├── AuthService.java                  ← Xác thực người dùng
├── TokenService.java                 ← JWT / Refresh token
├── UserService.java                  ← Quản lý profile người dùng
├── ShopService.java                  ← Quản lý cửa hàng
├── ShopUserService.java              ← Thành viên & quyền trong shop
├── BranchService.java                ← Quản lý chi nhánh
├── CustomerService.java              ← Quản lý khách hàng
├── TableService.java                 ← Quản lý bàn (F&B)
├── PromotionService.java             ← Quản lý khuyến mãi
├── OrderService.java                 ← Quản lý đơn hàng
├── ReportService.java                ← Báo cáo doanh thu
├── PaymentService.java               ← Subscription / nâng cấp gói
├── MailService.java                  ← Gửi email (HTML template)
├── FileUploadService.java            ← Upload / xóa file S3
├── AuditLogService.java              ← Ghi log hoạt động
├── SequenceService.java (interface)  ← Sinh mã tự động (SKU, barcode)
├── ProductService.java (interface)   ← Quản lý sản phẩm
├── InventoryService.java (interface) ← Quản lý tồn kho
├── ExcelImportService.java (interface) ← Import Excel
├── ExcelExportService.java           ← Export Excel
├── tax/
│   ├── TaxPolicyService.java         ← Quản lý chính sách thuế
│   ├── TaxCalculationService.java    ← Tính toán thuế
│   └── OrderTaxApplier.java          ← Áp thuế vào đơn hàng
└── impl/
    ├── ProductServiceImpl.java
    ├── InventoryServiceImpl.java
    ├── SequenceServiceImpl.java
    └── ExcelImportServiceImpl.java
```

---

## 1. BaseService *(abstract)*

**Mục đích:** Abstract class dùng chung cho các service cần xác thực entity.

| Method | Mô tả |
|---|---|
| `checkShopExists()` | Tìm Shop theo ID, throw nếu không có |
| `checkShopUserExists()` | Tìm ShopUser theo shopId + userId |
| `checkOrderExists()` | Tìm Order theo orderId + shopId |

**Được extend bởi:** `OrderService`, `ShopService`, `ShopUserService`, `ProductServiceImpl`

---

## 2. AuthService

**Mục đích:** Toàn bộ luồng xác thực người dùng.

| Method | Mô tả |
|---|---|
| `register()` | Đăng ký, gửi email xác thực |
| `login()` | Đăng nhập email/password, trả JWT + refresh token |
| `loginWithGoogle()` | OAuth2 qua Google ID Token (verify + auto register) |
| `verifyEmail()` | Xác thực tài khoản qua token |
| `forgotPassword()` | Gửi email reset password |
| `resetPassword()` | Đặt lại mật khẩu qua token |
| `logout()` | Xóa refresh token |

**Dependencies:** `UserRepository`, `JwtUtil`, `PasswordEncoder`, `MailService`, `TokenService`, `AuditLogService`, `FileUploadService`, `SimpMessagingTemplate` (WebSocket)

**Đặc điểm:**
- Hỗ trợ cả login thường + Google OAuth
- Gửi thông báo WebSocket khi đăng nhập mới
- Reset token có thời hạn cấu hình qua `app.reset-token.expiry-minutes`

---

## 3. TokenService

**Mục đích:** Quản lý JWT access token và refresh token.

| Method | Mô tả |
|---|---|
| `createRefreshToken()` | Tạo refresh token (TTL 7 ngày), lưu DB |
| `refreshAccessToken()` | Validate refresh token → cấp access token mới |

**Lưu ý:** Refresh token được lưu trong collection `refresh_tokens`, có TTL 7 ngày.

---

## 4. UserService

**Mục đích:** Quản lý thông tin profile của người dùng hệ thống.

| Method | Mô tả |
|---|---|
| `getCurrentUser()` | Lấy thông tin user hiện tại, map sang `UserResponse` |
| `updateProfile()` | Cập nhật profile (validate phone theo countryCode) |
| `changePassword()` | Đổi mật khẩu (verify mật khẩu cũ) |
| `uploadAvatar()` | Upload ảnh đại diện lên S3 |

---

## 5. ShopService

**Mục đích:** Quản lý vòng đời của cửa hàng (shop).

| Method | Mô tả |
|---|---|
| `createShop()` | Tạo shop + default branch + gán OWNER |
| `updateShop()` | Cập nhật thông tin (tên, địa chỉ, logo...) |
| `getShopById()` | Lấy thông tin shop theo ID |
| `getMyShops()` | Lấy danh sách shop của user hiện tại |
| `toggleActive()` | Kích hoạt / tắt shop |
| `deleteShop()` | Soft delete |

**Đặc điểm:**
- Tự sinh `slug` duy nhất từ tên shop
- Khi tạo shop → tự tạo default branch + thêm creator vào `ShopUser` với role `OWNER`
- Hỗ trợ `BusinessModel` và `ShopType` (F&B, Retail, Pharmacy…)

---

## 6. ShopUserService

**Mục đích:** Quản lý thành viên trong shop (roles, permissions, invitation).

| Method | Mô tả |
|---|---|
| `addUser()` | Thêm thành viên vào shop, gán role + default permissions |
| `removeUser()` | Xóa thành viên (soft delete) |
| `updateRole()` | Thay đổi role + cập nhật permissions |
| `updatePermissions()` | Cập nhật permissions thủ công |
| `getMembers()` | Lấy danh sách thành viên (có phân trang) |
| `getUserShops()` | Lấy danh sách shop mà user đang là thành viên |
| `requireAnyRole()` | Guard check: user phải có ít nhất 1 trong các role yêu cầu |

**Cache:** Sử dụng `ShopUserCache` để cache role của user trong shop.

**Role hierarchy:** `OWNER > MANAGER > STAFF > VIEWER`

---

## 7. BranchService

**Mục đích:** CRUD cho chi nhánh trong shop.

| Method | Mô tả |
|---|---|
| `getAll()` | Lấy tất cả chi nhánh của shop |
| `getById()` | Lấy chi tiết chi nhánh |
| `getBySlug()` | Lấy chi tiết chi nhánh theo slug |
| `create()` | Tạo chi nhánh mới (auto-generate slug) |
| `update()` | Cập nhật thông tin |
| `delete()` | Soft delete (chỉ OWNER) |

**Đặc điểm:**
- Slug được sinh từ tên chi nhánh, unique trong phạm vi shop
- Lưu `openingTime` / `closingTime`, `capacity`, `managerName`
- Mỗi shop có 1 default branch (flag `isDefault`)

---

## 8. CustomerService

**Mục đích:** Quản lý danh sách khách hàng theo shop và chi nhánh.

| Method | Mô tả |
|---|---|
| `getCustomers()` | Lấy danh sách KH theo shopId + branchId |
| `createCustomer()` | Tạo KH mới |
| `updateCustomer()` | Cập nhật thông tin KH (validate branchId) |
| `deleteCustomer()` | Soft delete |

**Lưu ý:** KH gắn với branch — không thể chuyển KH sang branch khác sau khi tạo.

---

## 9. TableService

**Mục đích:** Quản lý bàn ăn / khu vực phục vụ (chủ yếu cho F&B).

| Method | Mô tả |
|---|---|
| `create()` | Tạo bàn mới (validate tên trùng trong branch) |
| `update()` | Cập nhật thông tin bàn |
| `getAll()` | Lấy danh sách bàn (phân trang) |
| `getById()` | Lấy chi tiết bàn |
| `updateStatus()` | Cập nhật trạng thái (AVAILABLE / OCCUPIED / RESERVED) |
| `delete()` | Soft delete (@Transactional) |

**Đặc điểm:**
- `@Transactional` trên các write operation
- Validate `capacity > 0`
- Chỉ `OWNER` mới được tạo/xóa bàn

---

## 10. PromotionService

**Mục đích:** Quản lý chương trình khuyến mãi.

| Method | Mô tả |
|---|---|
| `getAll()` | Lấy danh sách khuyến mãi (phân trang) |
| `create()` | Tạo khuyến mãi |
| `update()` | Cập nhật (validate branchId không đổi) |
| `delete()` | Soft delete |
| `getActivePromotions()` | Lấy các KM đang active (theo thời gian hiệu lực) |

**Model:** Hỗ trợ `discountType` (PERCENT / FIXED), `applicableProductIds`, `startDate` / `endDate`.

---

## 11. OrderService *(extends BaseService)*

**Mục đích:** Core service xử lý toàn bộ vòng đời đơn hàng.

| Method | Mô tả |
|---|---|
| `createOrder()` | Tạo đơn hàng mới, tính giá, áp khuyến mãi, trừ tồn kho |
| `updateOrder()` | Cập nhật đơn (trạng thái, items) |
| `completeOrder()` | Hoàn thành đơn → cập nhật trạng thái bàn |
| `cancelOrder()` | Hủy đơn → hoàn tồn kho |
| `getOrders()` | Lấy danh sách đơn (phân trang, filter) |
| `getOrderById()` | Lấy chi tiết đơn |
| `payOrder()` | Xử lý thanh toán |

**Luồng `createOrder()`:**
```
OrderRequest
    ├── Validate shop + branchId
    ├── Với mỗi item:
    │   ├── Lấy Product master (kiểm tra thuộc shop)
    │   ├── Lấy BranchProduct (giá tại chi nhánh)
    │   ├── Áp discountPrice / discountPercentage
    │   └── Trừ tồn kho qua InventoryService (nếu trackInventory=true)
    ├── Áp Promotion (nếu có)
    ├── Áp Tax qua OrderTaxApplier
    └── Cập nhật trạng thái Table → OCCUPIED
```

**Dependencies:** `OrderTaxApplier`, `InventoryService`, `OrderCache`, `AuditLogService`

---

## 12. ReportService

**Mục đích:** Tổng hợp báo cáo doanh thu bằng MongoDB Aggregation.

| Method | Mô tả |
|---|---|
| `getReport()` | Tổng quan: tổng đơn, tổng SP bán, tổng doanh thu |
| `getDailyReport()` | Báo cáo theo ngày trong khoảng thời gian |
| `exportReport()` | Export báo cáo ra file Excel |

**Kỹ thuật:** Dùng `MongoTemplate` + `Aggregation Pipeline` trực tiếp trên collection `orders`.

---

## 13. PaymentService

**Mục đích:** Xử lý nâng cấp / hạ cấp gói subscription của shop.

| Method | Mô tả |
|---|---|
| `upgradeShopPlan()` | Nâng cấp gói, lưu `SubscriptionHistory`, gửi email thông báo |
| `downgradeShopPlan()` | Hạ cấp gói, gửi email thông báo |

**Gói:** `FREE → BASIC → PRO → ENTERPRISE`

**Lưu ý:** Hiện xử lý manual (chưa tích hợp payment gateway). `transactionId` có thể set từ Stripe/VNPAY webhook.

---

## 14. MailService

**Mục đích:** Gửi email plain text và HTML dựa trên Thymeleaf template.

| Method | Mô tả |
|---|---|
| `send()` | Gửi HTML string trực tiếp |
| `sendHtmlTemplate()` | Render Thymeleaf template + gửi |

**Templates có sẵn:**
- `emails/plan-upgraded.html`
- `emails/plan-downgraded.html`
- `emails/plan-expiry-reminder.html`

---

## 15. FileUploadService

**Mục đích:** Upload / xóa file lên AWS S3.

| Method | Mô tả |
|---|---|
| `upload()` | Validate → upload S3 → trả về public URL |
| `delete()` | Xóa file khỏi S3 theo URL |

**Validation:**
- Kích thước tối đa: **5MB**
- Định dạng cho phép: `JPEG`, `JPG`, `PNG`, `WEBP`, `XLSX`
- Tên file được sanitize + UUID prefix để tránh trùng

---

## 16. AuditLogService

**Mục đích:** Ghi log mọi hoạt động quan trọng vào collection `audit_logs`.

| Method | Mô tả |
|---|---|
| `log()` | Ghi 1 dòng log: `userId`, `shopId`, `targetId`, `targetType`, `action`, `description` |

**Được gọi bởi:** Gần như tất cả các service write operation.

---

## 17. SequenceService *(interface + impl)*

**Mục đích:** Sinh mã tự động tăng dần (SKU, barcode).

| Method | Mô tả |
|---|---|
| `getNextCode()` | Lấy mã tiếp theo theo prefix + type |
| `updateNextSequence()` | Cập nhật sequence sau khi dùng |

**Sử dụng:** `ProductServiceImpl` gọi để sinh SKU và barcode EAN-13.

---

## 18. ProductService *(interface + impl)*

*(Xem tài liệu `product-branchproduct-analysis.md` để chi tiết đầy đủ)*

| Method | Mô tả |
|---|---|
| `createProduct()` | Tạo Product + seed BranchProduct cho tất cả branch |
| `createBranchProduct()` | Tạo Product + seed BranchProduct cho 1 branch |
| `updateProduct()` | Cập nhật thông tin chung (catalog) |
| `updateBranchProduct()` | Cập nhật dữ liệu riêng từng chi nhánh |
| `deleteProductFromShop()` | Soft delete Product + tất cả BranchProduct |
| `getProduct()` | Lấy chi tiết tại 1 chi nhánh |
| `toggleActiveShop()` | Toggle active cấp shop |
| `toggleActiveInBranch()` | Toggle active cấp chi nhánh |
| `searchProducts()` | Tìm kiếm (aggregation pipeline) |
| `getLowStockProducts()` | Lọc sản phẩm sắp hết hàng |
| `getSuggestedSku()` / `getSuggestedBarcode()` | Gợi ý mã |
| `uploadProductImages()` / `deleteProductImage()` | Quản lý ảnh S3 |

---

## 19. InventoryService *(interface + impl)*

**Mục đích:** Quản lý biến động tồn kho và lịch sử giao dịch.

| Method | Mô tả |
|---|---|
| `importProductQuantity()` | Nhập hàng → tạo `InventoryTransaction(IMPORT)` |
| `exportProductQuantity()` | Xuất hàng → tạo `InventoryTransaction(EXPORT)`, validate không âm |
| `adjustProductQuantity()` | Điều chỉnh thủ công → tạo `InventoryTransaction(ADJUSTMENT)` |
| `getTransactionHistory()` | Lịch sử giao dịch tồn kho (phân trang) |
| `isInventoryManagementRequired()` | Kiểm tra shop có bật quản lý tồn kho không |

**Được gọi bởi:** `OrderService` (trừ tồn kho khi tạo đơn), `InventoryController`

---

## 20. ExcelImportService *(interface + impl)*

**Mục đích:** Import danh sách sản phẩm từ file Excel.

| Method | Mô tả |
|---|---|
| `importProducts()` | Đọc từng dòng Excel → tạo/cập nhật Product + BranchProduct |

**Logic:** Nếu SKU đã tồn tại → update, chưa có → create mới.

---

## 21. ExcelExportService

**Mục đích:** Export dữ liệu ra file Excel.

| Method | Mô tả |
|---|---|
| `exportProducts()` | Export sản phẩm theo shop hoặc branch (lấy qua `ProductCache`) |
| `exportExcel()` | Generic export với custom headers + rowMapper |

---

## 22. Tax Services (`service/tax/`)

### TaxPolicyService

| Method | Mô tả |
|---|---|
| `resolveEffectivePolicy()` | Tìm TaxPolicy đang hiệu lực tại thời điểm cho shop/branch |
| `getAllPolicies()` | Lấy tất cả policy của shop |
| `createPolicy()` | Tạo policy mới |
| `updatePolicy()` | Cập nhật |

**Priority:** Branch policy > Shop default policy

### TaxCalculationService

| Method | Mô tả |
|---|---|
| `calculate()` | Tính thuế từ `baseAmount` theo `TaxPolicy` |

**Hỗ trợ:**
- `priceIncludesTax = true`: tách NET từ GROSS
- Multiple `TaxRule` (PERCENT / FLAT)
- `applyOnPreviousTaxes`: thuế cộng dồn (compound tax)

### OrderTaxApplier

| Method | Mô tả |
|---|---|
| `applyTax()` | Resolve policy → tính thuế → gắn `OrderTaxSnapshot` vào Order |

---

## Sơ đồ phụ thuộc giữa các Service

```
AuthService ──────────────────────────────────────────┐
    ├── TokenService                                   │
    ├── MailService                                    │
    ├── FileUploadService                              │
    └── AuditLogService ◄────────────────────────── (tất cả service write)

ShopService ──────────────────────────────────────────┐
    ├── ShopUserService                                │
    │     └── ShopUserCache                            │
    ├── BranchService                                  │
    └── AuditLogService                                │
                                                       │
ProductService ────────────────────────────────────────┤
    ├── SequenceService (SKU, barcode)                 │
    ├── FileUploadService (images S3)                  │
    ├── ProductCache                                   │
    └── AuditLogService                                │
                                                       │
OrderService ──────────────────────────────────────────┤
    ├── InventoryService (trừ tồn kho)                 │
    ├── OrderTaxApplier                                │
    │     ├── TaxPolicyService                         │
    │     └── TaxCalculationService                    │
    ├── OrderCache                                     │
    └── AuditLogService                                │
                                                       │
ReportService ─────────────────────────────────────────┤
    └── ExcelExportService                             │
                                                       │
PaymentService ────────────────────────────────────────┘
    └── MailService
```

---

## Nhận xét tổng thể

### ✅ Điểm tốt
| Điểm | Mô tả |
|---|---|
| Separation of Concerns | Mỗi service đúng 1 responsibility |
| Interface + Impl | Các service phức tạp (Product, Inventory, Sequence, Import) dùng interface → dễ test/mock |
| Audit trail đầy đủ | `AuditLogService` được gọi nhất quán ở mọi write operation |
| Tax tách module riêng | Sub-package `tax/` với 3 class riêng biệt (Policy, Calculation, Applier) |
| Cache nhất quán | `ProductCache`, `ShopCache`, `ShopUserCache`, `OrderCache` |

### ⚠️ Điểm cần cải thiện
| Vấn đề | Mô tả |
|---|---|
| `OrderService` quá lớn | 373 dòng, ôm nhiều logic → nên tách `OrderItemService`, `OrderPaymentService` |
| `AuthService` không phải interface | Khó mock trong unit test; nên tạo `AuthService` interface |
| `PaymentService` chưa tích hợp gateway | `transactionId = null`, hardcode `"MANUAL"` → cần hoàn thiện khi tích hợp Stripe/VNPAY |
| `ExcelExportService` phụ thuộc `ProductCache` | Nên qua `ProductService` interface thay vì trực tiếp vào cache layer |
| Thiếu `@Transactional` | `ProductServiceImpl.createProduct()` tạo nhiều document nhưng không có transaction → nếu fail giữa chừng sẽ dữ liệu không nhất quán |
| Scheduler chưa tích hợp service | `PlanReminderScheduler`, `PlanExpiryScheduler` cần kiểm tra có gọi đúng `PaymentService` / `MailService` không |

