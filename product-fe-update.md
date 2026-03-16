# 📋 Cập nhật FE — Product API Changes

> **Ngày cập nhật:** 16/03/2026  
> **Phạm vi:** Product CRUD, BranchProduct Update, Price History

---

## 🔴 Breaking Changes

### 1. `priceHistory` bị xóa khỏi request body

**Trước đây** FE có thể truyền `priceHistory` trong `POST /products` và `PUT /products/{id}`:
```json
// ❌ KHÔNG còn hoạt động
{
  "name": "Cà phê sữa",
  "defaultPrice": 45000,
  "priceHistory": [...]
}
```

**Hiện tại** server tự động quản lý — FE **không được** gửi `priceHistory`. Field này sẽ bị bỏ qua hoàn toàn nếu gửi lên.

---

## 🟡 Thay đổi Request

### `PUT /api/shops/{shopId}/products/{productId}` — Thêm `reason`

Thêm field `reason` (tùy chọn) để ghi chú lý do thay đổi giá:

```json
{
  "name": "Cà phê sữa",
  "defaultPrice": 50000,
  "costPrice": 20000,
  "sku": "CF_001",
  "unit": "ly",
  "reason": "Tăng giá do nguyên liệu tháng 3"
}
```

| Field | Type | Bắt buộc | Mô tả |
|-------|------|----------|-------|
| `reason` | `string` | ❌ | Lý do thay đổi giá. Chỉ được lưu vào history **khi giá thực sự thay đổi**. |

---

### `PUT /api/shops/{shopId}/branches/{branchId}/products/{branchProductId}` — Thêm `reason`

```json
{
  "price": 48000,
  "branchCostPrice": 18000,
  "quantity": 100,
  "minQuantity": 10,
  "reason": "Khuyến mãi tháng 3"
}
```

| Field | Type | Bắt buộc | Mô tả |
|-------|------|----------|-------|
| `reason` | `string` | ❌ | Lý do thay đổi giá tại chi nhánh. |

---

### `GET /api/shops/{shopId}/products` — Thêm `keyword`

### `GET /api/shops/{shopId}/branches/{branchId}/products` — Thêm `keyword`

Hai API listing giờ hỗ trợ tìm kiếm nhanh:

```
GET /api/shops/{shopId}/products?keyword=cà phê&page=0&size=20
GET /api/shops/{shopId}/branches/{branchId}/products?keyword=cf_001&page=0&size=10
```

| Param | Type | Bắt buộc | Mô tả |
|-------|------|----------|-------|
| `keyword` | `string` | ❌ | Tìm theo **tên**, **SKU**, hoặc **barcode** (không phân biệt hoa thường). Để trống = lấy tất cả. |

> ⚠️ Nếu cần lọc thêm theo category, khoảng giá, trạng thái → dùng API `/search`.

---

## 🟢 Thay đổi Response

### Cấu trúc `priceHistory` thay đổi hoàn toàn

**Trước đây:**
```json
"priceHistory": [
  {
    "price": 45000,
    "costPrice": 18000,
    "effectiveDate": "2026-01-15T10:00:00"
  }
]
```

**Hiện tại:**
```json
"priceHistory": [
  {
    "oldPrice": 40000,
    "newPrice": 45000,
    "oldCostPrice": 15000,
    "newCostPrice": 18000,
    "changedAt": "2026-03-16T09:30:00",
    "changedBy": "user_abc123",
    "reason": "Tăng giá do nguyên liệu"
  }
]
```

| Field | Type | Mô tả |
|-------|------|-------|
| `oldPrice` | `number` | Giá bán **trước** khi thay đổi |
| `newPrice` | `number` | Giá bán **sau** khi thay đổi |
| `oldCostPrice` | `number` | Giá nhập **trước** khi thay đổi |
| `newCostPrice` | `number` | Giá nhập **sau** khi thay đổi |
| `changedAt` | `datetime` | Thời điểm thay đổi (server tự set) |
| `changedBy` | `string` | userId của người thực hiện |
| `reason` | `string \| null` | Lý do thay đổi (nếu có) |

---

### `branchPriceHistory` — Field mới trong response

Khi gọi API chi nhánh, response trả thêm `branchPriceHistory` — lịch sử giá **riêng tại chi nhánh đó**:

```json
{
  "id": "bp_xyz",
  "productId": "prod_abc",
  "name": "Cà phê sữa",
  "defaultPrice": 45000,
  "price": 48000,
  "priceHistory": [
    {
      "oldPrice": 40000,
      "newPrice": 45000,
      "changedAt": "2026-03-10T08:00:00",
      "changedBy": "user_001",
      "reason": null
    }
  ],
  "branchPriceHistory": [
    {
      "oldPrice": 45000,
      "newPrice": 48000,
      "oldCostPrice": 18000,
      "newCostPrice": 18000,
      "changedAt": "2026-03-16T09:30:00",
      "changedBy": "user_001",
      "reason": "Khuyến mãi tháng 3"
    }
  ]
}
```

| Field | Nguồn | Ý nghĩa |
|-------|-------|---------|
| `priceHistory` | `Product` | Lịch sử thay đổi **giá mặc định toàn shop** (`defaultPrice`, `costPrice`) |
| `branchPriceHistory` | `BranchProduct` | Lịch sử thay đổi **giá tại chi nhánh này** (`price`, `branchCostPrice`) |

> 📌 `branchPriceHistory` sẽ là `null` hoặc `[]` khi gọi API `GET /shops/{shopId}/products` (không có context chi nhánh).

---

## 📐 Cấu trúc Response đầy đủ (ProductResponse)

```json
{
  "id": "branch_product_id",
  "productId": "product_id",
  "shopId": "shop_id",
  "branchId": "branch_id",

  // --- Thông tin chung (Product) ---
  "name": "Cà phê sữa",
  "nameTranslations": { "en": "Milk Coffee" },
  "category": "Đồ uống",
  "sku": "FB_DOUONG_001",
  "barcode": "8930000000015",
  "unit": "ly",
  "description": "Cà phê sữa đặc truyền thống",
  "images": ["https://..."],
  "costPrice": 18000,
  "defaultPrice": 45000,
  "active": true,
  "supplierId": "sup_001",
  "variants": [],
  "priceHistory": [],          // Lịch sử giá mặc định toàn shop

  // --- Thông tin tại chi nhánh (BranchProduct) ---
  "price": 48000,
  "branchCostPrice": 18000,
  "quantity": 100,
  "minQuantity": 10,
  "discountPrice": null,
  "discountPercentage": null,
  "expiryDate": null,
  "activeInBranch": true,
  "branchVariants": [],
  "branchPriceHistory": [],    // Lịch sử giá tại chi nhánh này (MỚI)

  "createdAt": "2026-01-01T00:00:00",
  "updatedAt": "2026-03-16T09:30:00"
}
```

---

## ⚙️ Logic tự động (FE cần biết)

| Hành động | Kết quả |
|-----------|---------|
| Gọi `PUT /products/{id}` với `defaultPrice` hoặc `costPrice` **thay đổi** | Server tự động append 1 entry vào `priceHistory` |
| Gọi `PUT /products/{id}` với giá **không thay đổi** | `priceHistory` **không** có entry mới (dù gửi `reason`) |
| Gọi `PUT /branches/{b}/products/{bp}` với `price` hoặc `branchCostPrice` thay đổi | Server tự động append 1 entry vào `branchPriceHistory` |
| `priceHistory` / `branchPriceHistory` tối đa **50 entries** | Entry cũ nhất bị xóa tự động khi vượt giới hạn |
| Tạo sản phẩm mới (`POST /products`) | `priceHistory` bắt đầu là `[]` — không có entry khởi tạo |

---

## 🧩 Gợi ý hiển thị UI

### Bảng lịch sử giá (Price History Table)

| Thời điểm | Giá bán cũ | Giá bán mới | Giá nhập cũ | Giá nhập mới | Người thay đổi | Lý do |
|-----------|-----------|------------|------------|-------------|---------------|-------|
| 16/03/2026 09:30 | 40,000đ | 45,000đ | 15,000đ | 18,000đ | admin | Tăng giá Q1 |

### Input gợi ý khi chỉnh giá

Thêm field `reason` vào form cập nhật giá — placeholder: `"Lý do thay đổi giá (không bắt buộc)"`.

```
┌─────────────────────────────────────────┐
│ Giá bán mặc định *    [ 50,000        ] │
│ Giá nhập mặc định     [ 20,000        ] │
│ Lý do thay đổi giá    [               ] │
│                           (tùy chọn)    │
└─────────────────────────────────────────┘
```

