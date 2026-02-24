# 📊 Phân Tích Cấu Trúc: Product vs BranchProduct

## 🎯 Tóm Tắt Nhanh

Dự án của bạn sử dụng mô hình **hai lớp sản phẩm (Two-Level Product Model)**:
- **Product**: Định nghĩa sản phẩm **chung cho toàn bộ cửa hàng**
- **BranchProduct**: Thông tin sản phẩm **riêng biệt cho từng chi nhánh**

---

## 📋 Chi Tiết Từng Model

### 1️⃣ **Product Model** (Định Nghĩa Chung)

**Vị trí**: `src/main/java/com/example/sales/model/Product.java`

**Mục đích**: Lưu trữ thông tin sản phẩm chung cho toàn shop

**Các trường chính**:

```java
@Document("products")
@CompoundIndex(def = "{'shopId': 1, 'sku': 1}", unique = true)
@CompoundIndex(def = "{'shopId': 1, 'barcode': 1}", unique = true)
public class Product {
    // Thông tin cơ bản (không thay đổi theo chi nhánh)
    - String id;
    - String shopId;          // ✅ Cửa hàng sở hữu
    - String name;            // ✅ Tên sản phẩm
    - String sku;             // ✅ DÙNG DÙNG MÃ (unique trong shop) ⭐
    - String category;        // ✅ Danh mục
    - String barcode;         // ✅ Mã vạch
    
    // Giá mặc định (có thể override ở chi nhánh)
    - double costPrice;       // ✅ Giá nhập mặc định
    - double defaultPrice;    // ✅ Giá bán mặc định
    
    // Thông tin bổ sung
    - String unit;            // ✅ Đơn vị (kg, lít, cái)
    - String description;
    - List<String> images;
    - String supplierId;      // ✅ Nhà cung cấp
    
    // Biến thể và lịch sử
    - List<ProductVariant> variants;
    - List<PriceHistory> priceHistory;
    
    // Trạng thái
    - boolean active;         // ✅ Trạng thái sản phẩm
}
```

**Đặc điểm**:
- ✅ **Một bản ghi Product** ứng với một sản phẩm duy nhất trong toàn shop
- ✅ SKU là **duy nhất trong phạm vi shop** (unique index)
- ✅ Barcode cũng **duy nhất trong phạm vi shop**
- ✅ Lưu thông tin "mẫu" để tái sử dụng ở nhiều chi nhánh
- ✅ **Không chứa thông tin tồn kho**

**Database**: MongoDB collection `products`

---

### 2️⃣ **BranchProduct Model** (Chi Tiết Chi Nhánh)

**Vị trí**: File được gửi trong attachment `BranchProduct.java`

**Mục đích**: Lưu trữ thông tin sản phẩm **riêng biệt cho từng chi nhánh cụ thể**

**Các trường chính**:

```java
@Document("branch_products")
public class BranchProduct extends BaseEntity {
    // Tham chiếu đến sản phẩm và vị trí
    - String id;              // ✅ ID riêng của BranchProduct
    - String productId;       // ✅ Tham chiếu đến Product
    - String shopId;          // ✅ Cửa hàng
    - String branchId;        // ✅ Chi nhánh cụ thể ⭐
    
    // Thông tin tồn kho (riêng biệt theo chi nhánh)
    - int quantity;           // ✅ SỐ LƯỢNG TỒN CỦA CHI NHÁNH này
    - int minQuantity;        // ✅ Mức cảnh báo nhập hàng
    
    // Giá tại chi nhánh (có thể khác với giá mặc định)
    - double price;           // ✅ Giá bán tại chi nhánh
    - double branchCostPrice; // ✅ Giá nhập tại chi nhánh
    
    // Khuyến mãi
    - Double discountPrice;   // ✅ Giá khuyến mãi
    - Double discountPercentage; // ✅ % giảm giá
    
    // Hạn sử dụng
    - LocalDate expiryDate;   // ✅ Cho thực phẩm, thuốc
    
    // Trạng thái
    - boolean activeInBranch; // ✅ Kích hoạt bán tại chi nhánh này
    
    // Biến thể
    - List<BranchProductVariant> variants;
    
    // Tham chiếu trực tiếp
    @DBRef
    - Product product;
    - Shop shop;
    - Branch branch;
}
```

**Đặc điểm**:
- ✅ **Nhiều BranchProduct** có thể tham chiếu đến **cùng một Product**
- ✅ Mỗi chi nhánh có **bản ghi BranchProduct riêng**
- ✅ Giá và tồn kho **có thể khác nhau** giữa các chi nhánh
- ✅ Có trường `activeInBranch` để **bật/tắt bán hàng** tại từng chi nhánh
- ✅ **Lưu trữ tồn kho cụ thể**

**Database**: MongoDB collection `branch_products`

---

## 🔄 Sơ Đồ Quan Hệ (Relationship)

```
┌─────────────────────────────────────────────────────────────┐
│                          SHOP (1)                           │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ├─ Product (1) ─────┐
                           │                   │
                           ├─ Product (2) ─────┼─ SKU duy nhất
                           │                   │   trong shop
                           ├─ Product (n) ─────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
    BRANCH (1)         BRANCH (2)         BRANCH (3)
        │                  │                  │
        ├─ BranchProduct 1.1  ├─ BranchProduct 2.1  ├─ BranchProduct 3.1
        │  ├─ productId: 1    │  ├─ productId: 1    │  ├─ productId: 1
        │  ├─ quantity: 100   │  ├─ quantity: 50    │  ├─ quantity: 75
        │  ├─ price: 100k     │  ├─ price: 105k     │  ├─ price: 95k
        │                     │                     │
        ├─ BranchProduct 1.2  ├─ BranchProduct 2.2  └─ BranchProduct 3.2
        │  ├─ productId: 2    │  ├─ productId: 2       ├─ productId: 2
        │  ├─ quantity: 200   │  ├─ quantity: 150      └─ quantity: 80
        │  └─ price: 50k      └─ price: 52k
        │
        └─ BranchProduct 1.n ...

Ví dụ:
- Product 1: "Cà Chua" (SKU: FOOD_TOMATO)
  ├─ Chi nhánh 1: 100 cái, 100k/cái
  ├─ Chi nhánh 2: 50 cái, 105k/cái
  └─ Chi nhánh 3: 75 cái, 95k/cái

- Product 2: "Dưa Chuột" (SKU: FOOD_CUCUMBER)
  ├─ Chi nhánh 1: 200 cái, 50k/cái
  ├─ Chi nhánh 2: 150 cái, 52k/cái
  └─ Chi nhánh 3: 80 cái, 48k/cái
```

---

## 📝 Các API Sản Phẩm Hiện Tại

### **API 1: Lấy Danh Sách Sản Phẩm** ⭐ (CẠP NHẬT)

```
GET /api/shops/{shopId}/products
```

**Parameters**:
```
- shopId (path)      : ID cửa hàng (bắt buộc)
- branchId (query)   : ID chi nhánh (tùy chọn, để trống = tất cả chi nhánh)
- page (query)       : Số trang (mặc định: 0)
- size (query)       : Số sản phẩm/trang (mặc định: 20)
- sort (query)       : Sắp xếp (mặc định: createdAt,DESC)
```

**Example Request**:
```
GET /api/shops/shop1/products?page=0&size=20&sort=createdAt,DESC
GET /api/shops/shop1/products?branchId=branch1&page=0&size=20
```

**Response**:
```json
{
  "code": "PRODUCT_LIST",
  "message": "Success",
  "data": {
    "content": [
      {
        "id": "branchProduct1",
        "productId": "product1",
        "name": "Cà Chua",
        "sku": "FOOD_TOMATO",
        "category": "Rau Quả",
        "costPrice": 80000,
        "defaultPrice": 100000,
        "price": 100000,
        "quantity": 100,
        "branchId": "branch1",
        "activeInBranch": true,
        "createdAt": "2026-02-24T10:30:00",
        "updatedAt": "2026-02-24T10:30:00"
      },
      {
        "id": "branchProduct2",
        "productId": "product2",
        "name": "Dưa Chuột",
        "sku": "FOOD_CUCUMBER",
        "category": "Rau Quả",
        "costPrice": 50000,
        "defaultPrice": 65000,
        "price": 65000,
        "quantity": 150,
        "branchId": "branch1",
        "activeInBranch": true,
        "createdAt": "2026-02-24T11:00:00",
        "updatedAt": "2026-02-24T11:00:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
      },
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "last": true,
    "totalPages": 1,
    "totalElements": 2,
    "first": true,
    "size": 20,
    "number": 0,
    "empty": false
  }
}
```

**Ghi chú**:
- ✅ **Không cần branchId**: Lấy sản phẩm từ **tất cả chi nhánh** của shop
- ✅ **Có branchId**: Lấy sản phẩm từ **chi nhánh cụ thể**
- ✅ **Mặc định sắp xếp**: Theo `createdAt` giảm dần (sản phẩm mới nhất trước)
- ✅ **Dùng cache**: Kết quả được cache để tăng hiệu năng

---

### **API 2: Tạo từ Shop** (Quản lý từ cửa hàng)

```
POST /api/shops/{shopId}/products
```

**Request**:
```json
{
  "name": "Cà Chua",
  "sku": "FOOD_TOMATO",
  "category": "Rau Quả",
  "costPrice": 80000,
  "defaultPrice": 100000,
  "unit": "cái",
  "barcode": "1234567890123",
  "branchIds": ["branch1", "branch2"],  // ← Tùy chọn: chỉ định chi nhánh
  "quantity": 100,                      // ← Số lượng ban đầu
  "price": 100000,                      // ← Giá bán tại chi nhánh
  "branchCostPrice": 80000
}
```

**Hành động backend**:
1. ✅ **Tạo 1 Product** - lưu định nghĩa chung
2. ✅ **Tạo N BranchProduct** - một cho mỗi chi nhánh được chỉ định
3. ✅ Nếu không có `branchIds`, chỉ tạo Product (chưa có tồn kho)
4. ✅ Nếu có `branchIds`, tạo BranchProduct cho các chi nhánh đó

**Code xử lý**:
```java
@PostMapping("/shops/{shopId}/products")
@RequirePermission(Permission.PRODUCT_CREATE)
public ResponseEntity<ApiResponseDto<ProductResponse>> create(
    @PathVariable String shopId,
    @RequestParam(required = false) List<String> branchIds,  // ← Tùy chọn
    @Valid @RequestBody ProductRequest request) {
    
    ProductResponse response = productService.createProduct(shopId, branchIds, request);
    return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_CREATED, response));
}
```

---

### **API 4: Tạo từ Chi Nhánh** (Quản lý từ chi nhánh)

```
POST /api/shops/{shopId}/branches/{branchId}/products
```

**Request**:
```json
{
  "name": "Cà Chua",
  "sku": "FOOD_TOMATO",
  "category": "Rau Quả",
  "costPrice": 80000,
  "defaultPrice": 100000,
  "unit": "cái",
  "barcode": "1234567890123",
  "quantity": 100,
  "price": 100000,
  "branchCostPrice": 80000
}
```

**Hành động backend**:
1. ✅ **Tạo 1 Product** - lưu định nghĩa chung
2. ✅ **Tạo 1 BranchProduct** - cho chi nhánh được chỉ định
3. ✅ Tự động sử dụng `branchId` hiện tại

**Code xử lý**:
```java
@PostMapping("/shops/{shopId}/branches/{branchId}/products")
@RequirePermission(Permission.PRODUCT_CREATE)
public ResponseEntity<ApiResponseDto<ProductResponse>> createFromBranch(
    @PathVariable String shopId,
    @PathVariable String branchId,
    @Valid @RequestBody ProductRequest request) {
    
    ProductResponse response = productService.createBranchProduct(shopId, branchId, request);
    return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_CREATED, response));
}
```

---

### **API 5: Lấy Chi Tiết Sản Phẩm**

```
GET /api/shops/{shopId}/branches/{branchId}/products/{id}
```

**Parameters**:
```
- shopId (path)   : ID cửa hàng (bắt buộc)
- branchId (path) : ID chi nhánh (bắt buộc)
- id (path)       : ID BranchProduct (bắt buộc)
```

**Response**:
```json
{
  "code": "PRODUCT_FOUND",
  "message": "Success",
  "data": {
    "id": "branchProduct1",
    "productId": "product1",
    "name": "Cà Chua",
    "sku": "FOOD_TOMATO",
    "category": "Rau Quả",
    "costPrice": 80000,
    "defaultPrice": 100000,
    "price": 100000,
    "quantity": 100,
    "minQuantity": 10,
    "branchId": "branch1",
    "activeInBranch": true,
    "discountPrice": null,
    "discountPercentage": null,
    "expiryDate": null,
    "branchVariants": [],
    "createdAt": "2026-02-24T10:30:00",
    "updatedAt": "2026-02-24T10:30:00"
  }
}
```

---

### **API 6: Cập Nhật Sản Phẩm**

```
PUT /api/shops/{shopId}/products/{id}
```

**Parameters**:
```
- shopId (path)    : ID cửa hàng (bắt buộc)
- id (path)        : ID BranchProduct (bắt buộc)
- branchIds (query): Danh sách chi nhánh cần cập nhật (tùy chọn)
```

**Request Body**: Tương tự ProductRequest

**Example**:
```
PUT /api/shops/shop1/products/branchProduct1?branchIds=branch1,branch2
```

**Hành động**:
- ✅ Cập nhật thông tin Product (nếu tương ứng)
- ✅ Cập nhật BranchProduct cho các chi nhánh chỉ định
- ✅ Log audit trail

---

### **API 7: Xóa Sản Phẩm**

```
DELETE /api/shops/{shopId}/branches/{branchId}/products/{id}
```

**Parameters**:
```
- shopId (path)   : ID cửa hàng (bắt buộc)
- branchId (path) : ID chi nhánh (bắt buộc)
- id (path)       : ID BranchProduct (bắt buộc)
```

**Response**:
```json
{
  "code": "PRODUCT_DELETED",
  "message": "Success",
  "data": null
}
```

**Ghi chú**:
- ✅ Xóa **mềm** (soft delete) - đánh dấu `deleted = true`
- ✅ Chỉ xóa BranchProduct, không xóa Product
- ✅ Product vẫn sống nếu còn chi nhánh khác có BranchProduct

---

### **API 8: Bật/Tắt Bán Hàng Tại Chi Nhánh**

```
PATCH /api/shops/{shopId}/branches/{branchId}/products/{branchProductId}/toggle-active
```

**Parameters**:
```
- shopId (path)         : ID cửa hàng (bắt buộc)
- branchId (path)       : ID chi nhánh (bắt buộc)
- branchProductId (path): ID BranchProduct (bắt buộc)
```

**Response**:
```json
{
  "code": "PRODUCT_UPDATED",
  "message": "Success",
  "data": {
    "id": "branchProduct1",
    "productId": "product1",
    "name": "Cà Chua",
    "sku": "FOOD_TOMATO",
    "quantity": 100,
    "activeInBranch": false,  // ← Đã toggle
    "createdAt": "2026-02-24T10:30:00",
    "updatedAt": "2026-02-24T10:35:00"
  }
}
```

**Hành động**:
- ✅ Toggle trạng thái `activeInBranch` của BranchProduct
- ✅ Nếu `false`: Sản phẩm bị ẩn/ngừng bán tại chi nhánh này
- ✅ Nếu `true`: Sản phẩm được kích hoạt bán trở lại
- ✅ Không ảnh hưởng đến chi nhánh khác

---

### **API 9: Lấy Mã SKU Gợi Ý** ⭐ (TỰ ĐỘNG SINH)

```
GET /api/shops/{shopId}/suggested-sku
```

**Parameters**:
```
- shopId (path)  : ID cửa hàng (bắt buộc)
- industry (query): Ngành hàng (bắt buộc) - VD: "FOOD", "ELECTRONICS"
- category (query): Danh mục sản phẩm (tùy chọn) - VD: "VEGETABLE", "FRUIT"
```

**Example Request**:
```
GET /api/shops/shop1/suggested-sku?industry=FOOD&category=VEGETABLE
GET /api/shops/shop1/suggested-sku?industry=FOOD
```

**Response**:
```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": "FOOD_VEGETABLE_000001"
}
```

**Cách Hoạt Động**:

```
Input: 
  - industry = "FOOD"
  - category = "VEGETABLE"
         │
         ▼
1️⃣ Tạo Prefix (Tiền Tố)
   └─ Nếu có category: "{INDUSTRY}_{CATEGORY}" 
      └─ VD: "FOOD_VEGETABLE"
   └─ Nếu không category: "{INDUSTRY}"
      └─ VD: "FOOD"
         │
         ▼
2️⃣ Lấy Sequence Number (Dùng Sequence Service)
   └─ Sequence service lưu số hiệu tự tăng cho từng prefix
   └─ Lần 1: 000001
   └─ Lần 2: 000002
   └─ Lần 3: 000003 (...)
         │
         ▼
3️⃣ Kết Hợp = Prefix + Sequence
   └─ VD: "FOOD_VEGETABLE_000001"
   └─ VD: "FOOD_VEGETABLE_000002"
   └─ VD: "FOOD_VEGETABLE_000003"
```

**Ghi Chú**:
- ✅ **Không dùng trực tiếp**: Chỉ lấy gợi ý, có thể tùy chỉnh hoặc bỏ qua
- ✅ **Tự động tăng**: Sequence service tự động quản lý số hiệu
- ✅ **Unique Per Shop**: Mỗi shop có sequence riêng
- ✅ **Format**: `{INDUSTRY}_{CATEGORY}_{SEQUENCE}` hoặc `{INDUSTRY}_{SEQUENCE}`

---

### **API 10: Lấy Mã Vạch (Barcode) Gợi Ý** ⭐ (EAN-13)

```
GET /api/shops/{shopId}/suggested-barcode
```

**Parameters**:
```
- shopId (path)  : ID cửa hàng (bắt buộc)
- industry (query): Ngành hàng (bắt buộc)
- category (query): Danh mục sản phẩm (tùy chọn)
```

**Example Request**:
```
GET /api/shops/shop1/suggested-barcode?industry=FOOD&category=VEGETABLE
GET /api/shops/shop1/suggested-barcode?industry=ELECTRONICS
```

**Response**:
```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": "8930000000017"
}
```

**Cách Hoạt Động** (Chi Tiết):

```
Input: 
  - industry = "FOOD"
  - category = "VEGETABLE"
         │
         ▼
1️⃣ Tạo Prefix & Lấy Sequence
   ├─ Prefix = "FOOD_VEGETABLE" (nếu có category)
   ├─ Lấy next sequence → VD: 1
   └─ Sequence Number = "1"
         │
         ▼
2️⃣ Xây Dựng Base Code (12 chữ số)
   └─ Prefix: "893" (GS1 Vietnam)
   └─ Sequence: "000000001" (9 chữ số, left-padded)
   └─ Base Code = "893" + "000000001" = "893000000001"
         │
         ▼
3️⃣ Tính Check Digit (Chữ Số Kiểm Tra - EAN-13)
   ├─ Thuật Toán Checksum EAN-13:
   │  ├─ Cộng các chữ số ở vị trí lẻ (1,3,5...) × 1
   │  ├─ Cộng các chữ số ở vị trí chẵn (2,4,6...) × 3
   │  ├─ Tổng = OddSum × 1 + EvenSum × 3
   │  ├─ Check Digit = (10 - (Total % 10)) % 10
   │  └─ VD: Nếu Total = 39 → Check = (10 - 9) % 10 = 1
         │
         ▼
4️⃣ Kết Quả Cuối Cùng = Base Code + Check Digit
   └─ VD: "893000000001" + "7" = "8930000000017" (EAN-13, 13 chữ số)
```

**Ví Dụ Chi Tiết Tính Checksum**:

```
Base Code: 893000000001
Position: 1  2  3  4  5  6  7  8  9  10 11 12
Digit:    8  9  3  0  0  0  0  0  0  0  0  1

Odd Position Sum (1,3,5,7,9,11):
= 8 + 3 + 0 + 0 + 0 + 1 = 12

Even Position Sum (2,4,6,8,10,12):
= 9 + 0 + 0 + 0 + 0 + 1 = 10

Total = (12 × 1) + (10 × 3) = 12 + 30 = 42

Check Digit = (10 - (42 % 10)) % 10 
           = (10 - 2) % 10 
           = 8

Final Barcode (EAN-13) = "8930000000018"
```

**Ghi Chú**:
- ✅ **Prefix "893"**: Mã quốc gia GS1 của Việt Nam
- ✅ **EAN-13 Format**: 13 chữ số, tiêu chuẩn quốc tế
- ✅ **Check Digit**: Đảm bảo tính toàn vẹn dữ liệu barcode
- ✅ **Validate Pattern**: Barcode phải match: `^([A-Z0-9_]*|[0-9]{12,13})$`
- ✅ **Unique Per Shop**: Sequence tăng cho mỗi shop riêng
- ✅ **Có thể Thay Đổi**: Client có thể không dùng gợi ý, nhập barcode custom

---

### **So Sánh SKU vs Barcode**

| Tiêu Chí | SKU | Barcode |
|----------|-----|---------|
| **Mục Đích** | Quản lý hệ thống nội bộ | Bán lẻ/tại quầy |
| **Format** | `INDUSTRY_CATEGORY_SEQUENCE` | `893{SEQUENCE}{CHECKDIGIT}` (EAN-13) |
| **Ví Dụ** | `FOOD_VEGETABLE_000001` | `8930000000017` |
| **Độ Dài** | Tuỳ biến | 13 chữ số (EAN-13) |
| **Check Digit** | ❌ Không có | ✅ Có (EAN-13 standard) |
| **Unique** | ✅ Trong shop | ✅ Trong shop |
| **Bắt Buộc** | ✅ Có | ❌ Không (tùy chọn) |

---

## 🎬 Flow Tạo Product (Chi Tiết)

```
ProductRequest với tất cả các trường
             │
             ▼
┌─────────────────────────────────────────┐
│  ProductServiceImpl.createProduct()      │
└─────────────────────────────────────────┘
             │
             ├─ 1️⃣ Validate shop có tồn tại
             │
             ├─ 2️⃣ Validate branchIds (nếu có)
             │
             ├─ 3️⃣ Validate barcode KHÔNG trùng
             │
             ├─ 4️⃣ Tạo/Generate SKU
             │    └─ Nếu không có SKU → dùng sequence service
             │
             ├─ 5️⃣ Validate SKU KHÔNG trùng
             │
             ├─ 6️⃣ ⭐ TẠO PRODUCT
             │    └─ Lưu: name, sku, category, costPrice, defaultPrice
             │       unit, description, images, barcode, variants, etc
             │       (KHÔNG lưu: quantity, price, branchCostPrice, etc)
             │
             ├─ 7️⃣ ⭐ TẠO BRANCHPRODUCT (nếu có branchIds)
             │    ├─ Cho mỗi branchId:
             │    │  └─ Tạo BranchProduct:
             │    │     ├─ productId = vừa tạo
             │    │     ├─ branchId = từ danh sách
             │    │     ├─ quantity, price, branchCostPrice từ request
             │    │     └─ activeInBranch = true
             │    │
             │    └─ Lưu BranchProduct để MongoDB
             │
             ├─ 8️⃣ Log audit trail
             │
             ├─ 9️⃣ Update cache
             │
             └─ 🔟 Trả về ProductResponse
                   (kết hợp từ Product + BranchProduct đầu tiên)
```

---

## 📊 ProductRequest DTO Mapping

```
ProductRequest
│
├─ FIELDS → Product (định nghĩa chung)
│  ├─ name ........................ Product.name
│  ├─ nameTranslations ............ Product.nameTranslations
│  ├─ category .................... Product.category
│  ├─ sku ......................... Product.sku
│  ├─ costPrice ................... Product.costPrice (giá nhập mặc định)
│  ├─ defaultPrice ................ Product.defaultPrice (giá bán mặc định)
│  ├─ unit ........................ Product.unit
│  ├─ description ................. Product.description
│  ├─ images ...................... Product.images
│  ├─ barcode ..................... Product.barcode
│  ├─ supplierId .................. Product.supplierId
│  ├─ variants .................... Product.variants
│  └─ priceHistory ................ Product.priceHistory
│
└─ FIELDS → BranchProduct (chi tiết chi nhánh)
   ├─ quantity ..................... BranchProduct.quantity
   ├─ minQuantity .................. BranchProduct.minQuantity
   ├─ price ........................ BranchProduct.price (giá bán tại chi nhánh)
   ├─ branchCostPrice .............. BranchProduct.branchCostPrice
   ├─ discountPrice ................ BranchProduct.discountPrice
   ├─ discountPercentage ........... BranchProduct.discountPercentage
   ├─ expiryDate ................... BranchProduct.expiryDate
   └─ branchVariants ............... BranchProduct.variants
```

---

## 💡 ProductResponse DTO

```java
ProductResponse
├─ ID từ BranchProduct
│  ├─ id .......................... BranchProduct.id ⭐
│  ├─ productId ................... Product.id ⭐
│  └─ branchId .................... BranchProduct.branchId ⭐
│
├─ Thông tin từ Product (chung)
│  ├─ name, category, sku
│  ├─ costPrice, defaultPrice
│  ├─ unit, description, images
│  └─ barcode, supplierId, variants, priceHistory
│
└─ Thông tin từ BranchProduct (chi nhánh)
   ├─ quantity (tồn kho)
   ├─ minQuantity
   ├─ price (giá bán tại chi nhánh)
   ├─ branchCostPrice
   ├─ discountPrice, discountPercentage
   ├─ expiryDate
   ├─ branchVariants
   └─ activeInBranch
```

---

## 📚 Danh Sách Tất Cả API Sản Phẩm

| # | API | Method | Endpoint | Mục đích |
|---|-----|--------|----------|---------|
| 1 | **Lấy Danh Sách** | GET | `/api/shops/{shopId}/products` | Lấy danh sách sản phẩm (có/không filter chi nhánh) |
| 2 | **Tạo từ Shop** | POST | `/api/shops/{shopId}/products` | Tạo product + branchproduct cho nhiều chi nhánh |
| 3 | **Tạo từ Chi Nhánh** | POST | `/api/shops/{shopId}/branches/{branchId}/products` | Tạo product + branchproduct cho 1 chi nhánh |
| 4 | **Lấy Chi Tiết** | GET | `/api/shops/{shopId}/branches/{branchId}/products/{id}` | Lấy thông tin chi tiết sản phẩm |
| 5 | **Cập Nhật** | PUT | `/api/shops/{shopId}/products/{id}` | Cập nhật thông tin sản phẩm |
| 6 | **Xóa** | DELETE | `/api/shops/{shopId}/branches/{branchId}/products/{id}` | Xóa mềm sản phẩm |
| 7 | **Toggle Active** | PATCH | `/api/shops/{shopId}/branches/{branchId}/products/{branchProductId}/toggle-active` | Bật/tắt bán hàng tại chi nhánh |
| 8 | **Gợi Ý SKU** | GET | `/api/shops/{shopId}/suggested-sku` | Lấy mã SKU gợi ý (tự động sinh) |
| 9 | **Gợi Ý Barcode** | GET | `/api/shops/{shopId}/suggested-barcode` | Lấy mã vạch gợi ý (EAN-13) |

---

## 🚀 Các Case Sử Dụng

### **Case 1: Thêm sản phẩm mới tại 1 chi nhánh cụ thể**
```
Dùng API: POST /api/shops/{shopId}/branches/{branchId}/products
→ Tạo 1 Product + 1 BranchProduct
→ Sản phẩm chỉ có tồn kho tại chi nhánh này
```

### **Case 2: Thêm sản phẩm mới, có tồn kho tại nhiều chi nhánh**
```
Dùng API: POST /api/shops/{shopId}/products?branchIds=branch1,branch2,branch3
→ Tạo 1 Product + 3 BranchProduct
→ Mỗi chi nhánh có bản ghi riêng với số lượng riêng
```

### **Case 3: Thêm sản phẩm mới nhưng chưa phân bổ cho chi nhánh**
```
Dùng API: POST /api/shops/{shopId}/products (không có branchIds)
→ Tạo 1 Product (dự phòng)
→ Không tạo BranchProduct
→ Có thể thêm vào chi nhánh sau
```

### **Case 4: Cập nhật giá/số lượng tại 1 chi nhánh**
```
Dùng API: PUT /api/shops/{shopId}/branches/{branchId}/products/{branchProductId}
→ Cập nhật BranchProduct (price, quantity, discount, etc.)
→ Không ảnh hưởng đến chi nhánh khác
```

---

## ⚠️ Những Điều Cần Lưu Ý

| Điểm | Product | BranchProduct |
|------|---------|---------------|
| **Số lượng** | 1 cho mỗi sản phẩm | 1 cho mỗi (sản phẩm + chi nhánh) |
| **Tồn kho** | ❌ KHÔNG có | ✅ CÓ `quantity` |
| **Giá** | ✅ Giá mặc định | ✅ Giá riêng từng chi nhánh |
| **Active** | ✅ `active` | ✅ `activeInBranch` |
| **SKU** | ✅ Duy nhất trong shop | ❌ Không duy nhất (tham chiếu) |
| **Database** | `products` | `branch_products` |
| **Xóa** | Xóa product → tất cả BranchProduct mất | Xóa BranchProduct → Product vẫn sống |

---

## 🎓 Kết Luận

**Bạn nên:**

1. ✅ **Tạo Product** khi: Cần định nghĩa sản phẩm chung cho shop (lần đầu)
2. ✅ **Tạo BranchProduct** khi: Sản phẩm được phân bổ tới một chi nhánh cụ thể
3. ✅ **Sử dụng API `/shops/{shopId}/products`** khi: Thêm sản phẩm từ cấp shop, có thể chỉ định nhiều chi nhánh
4. ✅ **Sử dụng API `/shops/{shopId}/branches/{branchId}/products`** khi: Thêm sản phẩm từ cấp chi nhánh

**Đơn giản hóa**: 
- **Product** = "Cái hộp sản phẩm chứa thông tin chung"
- **BranchProduct** = "Hàng của cái sản phẩm đó được đặt ở chi nhánh này"

