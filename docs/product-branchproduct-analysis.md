# Phân tích Product & BranchProduct

> Cập nhật lần cuối: 13/03/2026 — phản ánh trạng thái code hiện tại sau khi áp dụng tất cả các fix.

---

## 1. Mô hình dữ liệu (Data Model)

### Quan hệ tổng quan

```
Shop (1) ──────── (N) Product           ← Định nghĩa sản phẩm chung
                          │
                          │ productId (plain String reference)
                          ▼
Shop (1) ──── (N) BranchProduct (N) ──── (1) Branch
```

Đây là mô hình **"Product Template + Branch Instance"**: một sản phẩm được định nghĩa một lần ở cấp Shop, sau đó nhân bản thành nhiều `BranchProduct` tại mỗi chi nhánh.

---

### 📦 Product — Định nghĩa sản phẩm chung (collection: `products`)

| Field | Kiểu | Mô tả |
|---|---|---|
| `id` | String | PK |
| `shopId` | String | Thuộc về shop nào |
| `name` | String | Tên sản phẩm (required) |
| `nameTranslations` | Map\<String, String\> | Đa ngôn ngữ |
| `category` | String | Danh mục |
| `sku` | String | Mã SKU, duy nhất trong shop (`shopId + sku` unique index) |
| `barcode` | String | Mã vạch EAN-13 (regex `^([A-Z0-9_]*\|[0-9]{12,13})$`), unique trong shop |
| `costPrice` | double | Giá nhập **mặc định** (dùng để seed BranchProduct) |
| `defaultPrice` | double | Giá bán **mặc định** (dùng để seed BranchProduct) |
| `unit` | String | Đơn vị đo lường (kg, lít, cái…) |
| `description` | String | Mô tả |
| `images` | List\<String\> | URL ảnh (giới hạn 10) |
| `supplierId` | String | Nhà cung cấp |
| `variants` | List\<ProductVariant\> | Biến thể cấp shop (size, màu sắc…) |
| `priceHistory` | List\<PriceHistory\> | Lịch sử giá |
| `active` | boolean | Trạng thái cấp shop (default: `true`) |
| `deleted` | boolean | Soft delete (từ BaseEntity) |

**Compound indexes:**
- `{shopId: 1, sku: 1}` — unique
- `{shopId: 1, barcode: 1}` — unique

---

### 🏪 BranchProduct — Thông tin sản phẩm tại chi nhánh (collection: `branch_products`)

| Field | Kiểu | Mô tả |
|---|---|---|
| `id` | String | PK |
| `productId` | String | FK → Product (plain String) |
| `shopId` | String | FK → Shop |
| `branchId` | String | FK → Branch |
| `quantity` | int | Tồn kho tại chi nhánh |
| `minQuantity` | int | Ngưỡng cảnh báo low stock |
| `price` | double | Giá bán **tại chi nhánh** (override Product.defaultPrice) |
| `branchCostPrice` | double | Giá nhập **tại chi nhánh** (override Product.costPrice) |
| `discountPrice` | Double | Giá khuyến mãi (nullable) |
| `discountPercentage` | Double | % giảm giá (nullable) |
| `expiryDate` | LocalDate | Hạn sử dụng (nullable) |
| `variants` | List\<BranchProductVariant\> | Biến thể tại chi nhánh |
| `activeInBranch` | boolean | Trạng thái bán tại chi nhánh (default: `true`) |
| `deleted` | boolean | Soft delete |

**Compound indexes:**
- `{shopId, branchId, deleted}` — tăng tốc query danh sách sản phẩm theo branch
- `{shopId, deleted}` — tăng tốc query danh sách sản phẩm theo shop
- `{productId, branchId}` — **unique**, đảm bảo mỗi sản phẩm chỉ xuất hiện 1 lần trong 1 branch

> ✅ **Đã fix:** Xóa các field `@DBRef Product product`, `@DBRef Shop shop`, `@DBRef Branch branch` — tránh N+1 queries khi load danh sách. Quan hệ giờ chỉ lưu qua plain String ID.

---

### 🔧 Biến thể (Variants)

| Entity | Variant | Nội dung |
|---|---|---|
| Product | `ProductVariant` | `variantId`, `name`, `sku`, `price` (mặc định), `costPrice`, `attributes` |
| BranchProduct | `BranchProductVariant` | `variantId` (liên kết), `quantity`, `price`, `branchCostPrice`, `discountPrice`, `discountPercentage` |

**Pattern:** Product định nghĩa variant template → BranchProduct override giá/tồn kho cho từng chi nhánh, liên kết qua `variantId`.

---

## 2. Luồng tạo sản phẩm

### 2a. Tạo từ Shop (`createProduct`)
```
ProductRequest
    │
    ├── Validate Shop
    ├── Lấy TẤT CẢ branchIds của shop
    ├── Validate barcode uniqueness (trong shop)
    ├── Generate/validate SKU
    ├── Tạo Product → save
    ├── Update Sequence (SKU + Barcode)
    ├── createBranchProducts(shop, product, branchIds)
    │     ├── validateBranches() → kiểm tra từng branchId thuộc shop
    │     ├── Check trùng lặp BranchProduct
    │     └── saveAll → seed price=defaultPrice, quantity=0
    ├── Audit log
    └── Evict cache toàn shop
```

### 2b. Tạo từ Branch (`createBranchProduct`)
```
ProductRequest + branchId
    │
    ├── Validate Shop + Branch cụ thể (findByIdAndShopId)
    ├── Validate barcode + SKU
    ├── Tạo Product → save
    └── createBranchProducts(shop, product, [branchId])
          └── Chỉ seed BranchProduct cho 1 chi nhánh đó
```

**Khác biệt chính:** `createProduct` seed toàn bộ branches, `createBranchProduct` seed chỉ branch hiện tại.

---

## 3. Luồng cập nhật sản phẩm

### 3a. Cập nhật Product (`updateProduct`)
- Cập nhật thông tin **chung**: name, category, images, barcode, variants…
- Upload ảnh mới → merge với ảnh giữ lại trong request → giới hạn 10 ảnh
- Tự động xóa ảnh bị remove khỏi S3
- **Không** chạm đến BranchProduct (price, quantity tại branch độc lập)

### 3b. Cập nhật BranchProduct (`updateBranchProduct`)
- Cập nhật thông tin **riêng chi nhánh**: price, branchCostPrice, quantity, discount, expiryDate, variants
- Nếu `shop.type.isTrackInventory() = false` → bỏ qua cập nhật `quantity`
- Không ảnh hưởng đến Product gốc

---

## 4. Cơ chế Toggle Active (2 cấp)

```
Product.active    BranchProduct.activeInBranch    Kết quả
─────────────────────────────────────────────────────────────
true              true                            Đang bán ✅
true              false                           Tạm ngưng tại branch ⛔
false             false                           Ngưng kinh doanh toàn shop ⛔
false             true  (không hợp lệ)            → throw PRODUCT_SHOP_INACTIVE
```

**Quy tắc:**
- `toggleActiveShop(false)`: tắt `Product.active` → **đồng thời** tắt `activeInBranch` tất cả branches
- `toggleActiveShop(true)`: bật lại `Product.active` → **không** tự bật branch (branch tự quản lý)
- `toggleActiveInBranch`: chỉ toggle tại 1 branch; nếu `Product.active=false` và đang tắt → throw exception

---

## 5. Tìm kiếm (ProductSearchHelper)

Sử dụng **MongoDB Aggregation Pipeline**:

```
branch_products
    │ $match: shopId, branchId (nếu có), deleted=false
    │ $lookup: products → productDetail
    │ $unwind: productDetail
    │ $match: keyword (name/sku/barcode regex), category, activeInBranch, price range
    │ $sort
    │ $skip / $limit
    ▼
List<BranchProduct>
```

**Ưu điểm:** Filter kết hợp cả field của BranchProduct (price, active) và Product (name, sku, barcode, category) trong 1 query duy nhất.

---

## 6. Cache Layer (ProductCache)

**Cache name:** `branch_products_by_shop_branch`

| Key pattern | Trường hợp sử dụng |
|---|---|
| `{shopId}:all:p{page}:s{size}:{sort}` | Danh sách cấp shop (không lọc branch) |
| `{shopId}:{branchId}:p{page}:s{size}:{sort}` | Danh sách theo branch |

- `evictByShop(shopId)`: xóa **toàn bộ** entries của shop (scan prefix `{shopId}:`)
- Được gọi sau mọi write operation: create, update, delete, toggle

---

## 7. Các fix đã áp dụng

### ✅ Fix 1 — Bug EAN-13 check digit (`ProductServiceImpl`)

Công thức tính check digit theo chuẩn EAN-13:
- Vị trí lẻ (index 0,2,4,6,8,10) × **1**
- Vị trí chẵn (index 1,3,5,7,9,11) × **3**

```java
// Trước (SAI):
int total = oddSum * 3 + evenSum;   // odd×3 + even×1

// Sau (ĐÚNG):
int total = oddSum + evenSum * 3;   // odd×1 + even×3
```

### ✅ Fix 2 — `@Async` vô hiệu (`ProductServiceImpl`)

```java
// Trước: @Async nhưng .join() ngay lập tức → block thread, không có lợi gì
@Async("taskExecutor")
public CompletableFuture<List<BranchProduct>> saveAllBranchProducts(...) {
    return CompletableFuture.completedFuture(repo.saveAll(branchProducts));
}
return saveAllBranchProducts(branchProducts).join();

// Sau: synchronous thẳng, bỏ @Async + CompletableFuture
private List<BranchProduct> saveAllBranchProducts(...) {
    return branchProductRepository.saveAll(branchProducts);
}
return saveAllBranchProducts(branchProducts);
```

### ✅ Fix 3 — `@DBRef` overhead (`BranchProduct`)

```java
// Trước: 3 @DBRef → extra query mỗi lần load BranchProduct
@DBRef private Product product;
@DBRef private Shop shop;
@DBRef private Branch branch;

// Sau: xóa hoàn toàn, giữ quan hệ qua plain String ID (productId, shopId, branchId)
// Validation branch được đảm bảo bằng validateBranches() trong createBranchProducts()
```

### ✅ Fix 4 — Thiếu compound index (`BranchProduct`)

```java
@CompoundIndexes({
    @CompoundIndex(def = "{'shopId': 1, 'branchId': 1, 'deleted': 1}", name = "idx_shop_branch_deleted"),
    @CompoundIndex(def = "{'shopId': 1, 'deleted': 1}",               name = "idx_shop_deleted"),
    @CompoundIndex(def = "{'productId': 1, 'branchId': 1}", unique = true, name = "idx_product_branch_unique")
})
```

---

## 8. Tổng quan kiến trúc hiện tại

```
ProductService (interface)
    └── ProductServiceImpl
          ├── createProduct()          → createBranchProducts() cho TẤT CẢ branches
          ├── createBranchProduct()    → createBranchProducts() cho 1 branch
          ├── updateProduct()          → chỉ update Product (catalog), evict cache
          ├── updateBranchProduct()    → chỉ update BranchProduct, evict cache
          ├── deleteProductFromShop()  → soft-delete Product + tất cả BranchProduct
          ├── toggleActiveShop()       → toggle Product.active + sync tất cả branch khi tắt
          ├── toggleActiveInBranch()   → toggle BranchProduct.activeInBranch
          ├── searchProducts()         → ProductSearchHelper (aggregation pipeline)
          ├── getLowStockProducts()    → filter quantity < threshold
          ├── getSuggestedSku()        → SequenceService
          ├── getSuggestedBarcode()    → SequenceService + calculateEan13CheckDigit()
          ├── uploadProductImages()    → FileUploadService + S3
          └── deleteProductImage()     → FileUploadService + S3

ProductCache
    ├── getAllByShop()     @Cacheable
    ├── getAllByBranch()   @Cacheable
    └── evictByShop()     @CacheEvict (prefix scan)

ProductMapper
    └── toResponse(BranchProduct, Product) → ProductResponse (merged view)

ProductSearchHelper
    └── search() / count()  → MongoTemplate aggregation pipeline
```

