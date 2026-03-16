# 📦 Tổng hợp API Product

Base URL: `/api`

---

## 1. ProductCrudController

### 🟢 CREATE
| Method | Endpoint | Mô tả | Permission |
|--------|----------|--------|------------|
| `POST` | `/shops/{shopId}/products` | Tạo sản phẩm mới (multipart: `product` + `files` tùy chọn). Tự động tạo BranchProduct cho **tất cả chi nhánh** của shop. | `PRODUCT_CREATE` |

### 🔵 READ
| Method | Endpoint | Mô tả | Permission |
|--------|----------|--------|------------|
| `GET` | `/shops/{shopId}/products` | Lấy danh sách sản phẩm toàn shop (phân trang) | `PRODUCT_VIEW` |
| `GET` | `/shops/{shopId}/branches/{branchId}/products` | Lấy danh sách sản phẩm theo chi nhánh (phân trang) | `PRODUCT_VIEW` |
| `GET` | `/shops/{shopId}/products/search` | Tìm kiếm nâng cao: keyword (name/SKU/barcode), category, khoảng giá, trạng thái, branchId | `PRODUCT_VIEW` |
| `GET` | `/shops/{shopId}/branches/{branchId}/products/{id}` | Lấy chi tiết 1 sản phẩm tại chi nhánh (`id` = BranchProduct ID) | `PRODUCT_VIEW` |

### 🟡 UPDATE
| Method | Endpoint | Mô tả | Permission |
|--------|----------|--------|------------|
| `PUT` | `/shops/{shopId}/products/{productId}` | Cập nhật thông tin chung (tên, SKU, barcode, giá mặc định...). Có thể kèm `files` để thay ảnh. | `PRODUCT_UPDATE` |
| `PUT` | `/shops/{shopId}/branches/{branchId}/products/{branchProductId}` | Cập nhật thông tin riêng tại chi nhánh (giá bán, giá nhập, tồn kho, giảm giá...) | `PRODUCT_UPDATE` |

### 🔴 DELETE
| Method | Endpoint | Mô tả | Permission |
|--------|----------|--------|------------|
| `DELETE` | `/shops/{shopId}/products/{productId}` | Xóa mềm Product + tất cả BranchProduct liên quan trên mọi chi nhánh | `PRODUCT_DELETE` |

### 🖼️ IMAGE
| Method | Endpoint | Mô tả | Permission |
|--------|----------|--------|------------|
| `POST` | `/shops/{shopId}/products/{productId}/images` | Upload ảnh sản phẩm lên S3 (tối đa 10 ảnh, ≤5MB/ảnh, JPEG/PNG/WEBP) | `PRODUCT_UPDATE` |
| `DELETE` | `/shops/{shopId}/products/{productId}/images?imageUrl=...` | Xóa một ảnh theo URL, trả về danh sách ảnh còn lại | `PRODUCT_UPDATE` |

### 🔤 SUGGEST
| Method | Endpoint | Mô tả | Permission |
|--------|----------|--------|------------|
| `GET` | `/shops/{shopId}/suggested-sku?industry=&category=` | Gợi ý mã SKU | _(public)_ |
| `GET` | `/shops/{shopId}/suggested-barcode?industry=&category=` | Gợi ý mã barcode | _(public)_ |

---

## 2. ProductStatusController

### 🔄 TOGGLE STATUS
| Method | Endpoint | Mô tả | Permission |
|--------|----------|--------|------------|
| `PATCH` | `/shops/{shopId}/products/{productId}/toggle` | Bật/tắt `Product.active` ở cấp **shop**. Khi TẮT → tất cả BranchProduct bị tắt theo. Khi BẬT → chỉ bật Product, BranchProduct **không** tự bật. | `PRODUCT_UPDATE_STATUS` |
| `PATCH` | `/shops/{shopId}/branches/{branchId}/products/{branchProductId}/toggle` | Bật/tắt `BranchProduct.activeInBranch` tại **1 chi nhánh**. Không thể bật nếu `Product.active = false`. | `PRODUCT_UPDATE_STATUS` |

### 📉 LOW STOCK
| Method | Endpoint | Mô tả | Permission |
|--------|----------|--------|------------|
| `GET` | `/shops/{shopId}/products/low-stock?branchId=&threshold=10` | Danh sách sản phẩm tồn kho thấp (quantity < threshold). Lọc theo chi nhánh hoặc toàn shop. | `PRODUCT_VIEW_LOW_STOCK` |

---

## 3. ProductImportExportController

Base URL: `/api/products/import-export`

| Method | Endpoint | Mô tả | Permission |
|--------|----------|--------|------------|
| `POST` | `/import?shopId=&branchId=` | Nhập sản phẩm từ file Excel vào chi nhánh cụ thể (multipart: `file`) | `PRODUCT_IMPORT` |
| `GET` | `/export?shopId=&branchId=` | Xuất sản phẩm ra file Excel. `branchId` tùy chọn — nếu bỏ trống thì xuất toàn shop. | `PRODUCT_EXPORT` |

---

## 📌 Ghi chú ID
| ID | Ý nghĩa |
|----|---------|
| `shopId` | ID cửa hàng |
| `branchId` | ID chi nhánh |
| `productId` | ID sản phẩm chung (`Product`) |
| `branchProductId` | ID sản phẩm tại chi nhánh (`BranchProduct`) |

---

## 📊 Thống kê
- **Tổng số API:** 17
- **ProductCrudController:** 12 API
- **ProductStatusController:** 3 API
- **ProductImportExportController:** 2 API

---

_Cập nhật lần cuối: 16/03/2026_

