# Product Excel Import / Export — Tài liệu cho FE

> Cập nhật: 16/03/2026  
> Base URL: `/api/products/import-export`

---

## Mục lục

1. [Cấu trúc cột Excel](#cấu-trúc-cột-excel)
2. [API Export — Tải file Excel mẫu / xuất dữ liệu](#api-export)
3. [API Import — Nhập sản phẩm từ file Excel](#api-import)
4. [Quy tắc dữ liệu](#quy-tắc-dữ-liệu)
5. [Xử lý lỗi](#xử-lý-lỗi)
6. [Ví dụ flow FE](#ví-dụ-flow-fe)

---

## Cấu trúc cột Excel

Cột Excel **phải đúng thứ tự** này — cả khi import lẫn khi xuất file mẫu.

| # | Tên cột | Bắt buộc | Nguồn dữ liệu | Mô tả |
|---|---------|----------|---------------|-------|
| 0 | SKU | ❌ | `Product.sku` | Mã sản phẩm, duy nhất trong shop. Chỉ chứa chữ IN HOA, số, dấu `_`. **Nếu để trống → server tự động sinh** theo quy tắc `{INDUSTRY}_{CATEGORY}_XXX` |
| 1 | **Tên sản phẩm** | ✅ | `Product.name` | Tên hiển thị |
| 2 | **Danh mục** | ✅ | `Product.category` | Tên danh mục. Bắt buộc vì là cơ sở sinh SKU (`{INDUSTRY}_{CATEGORY}_XXX`) và barcode |
| 3 | **Đơn vị** | ✅ | `Product.unit` | VD: `cái`, `kg`, `lít`, `hộp` |
| 4 | Barcode | ❌ | `Product.barcode` | Mã barcode (12-13 số EAN/UPC hoặc mã nội bộ IN HOA) |
| 5 | Giá nhập mặc định | ❌ | `Product.costPrice` | Giá nhập ở cấp shop (dùng làm giá mặc định cho chi nhánh). Số thực ≥ 0 |
| 6 | Giá bán mặc định | ❌ | `Product.defaultPrice` | Giá bán mặc định (dùng khởi tạo chi nhánh). Số thực > 0 |
| 7 | Giá bán chi nhánh | ❌ | `BranchProduct.price` | Giá bán tại chi nhánh được import vào. Để trống = lấy giá bán mặc định |
| 8 | Giá nhập chi nhánh | ❌ | `BranchProduct.branchCostPrice` | Giá nhập tại chi nhánh. Số thực ≥ 0 |
| 9 | Số lượng | ❌ | `BranchProduct.quantity` | Tồn kho tại chi nhánh. Số nguyên ≥ 0 |
| 10 | Số lượng tối thiểu | ❌ | `BranchProduct.minQuantity` | Ngưỡng cảnh báo tồn kho thấp. Số nguyên ≥ 0 |
| 11 | Giá khuyến mãi | ❌ | `BranchProduct.discountPrice` | Giá sau khuyến mãi. Để trống = không có khuyến mãi |
| 12 | % Giảm giá | ❌ | `BranchProduct.discountPercentage` | Phần trăm giảm (0-100). Để trống = không có |
| 13 | Hạn sử dụng | ❌ | `BranchProduct.expiryDate` | Định dạng `yyyy-MM-dd`. VD: `2026-12-31` |
| 14 | Mô tả | ❌ | `Product.description` | Mô tả sản phẩm |
| 15 | Trạng thái SP | ❌ | `Product.active` | `TRUE` / `FALSE`. Mặc định: `TRUE` |
| 16 | Trạng thái chi nhánh | ❌ | `BranchProduct.activeInBranch` | `TRUE` / `FALSE`. Mặc định: `TRUE` |

### Ví dụ file Excel

| SKU | Tên sản phẩm | Danh mục | Đơn vị | Barcode | Giá nhập mặc định | Giá bán mặc định | Giá bán chi nhánh | Giá nhập chi nhánh | Số lượng | Số lượng tối thiểu | Giá khuyến mãi | % Giảm giá | Hạn sử dụng | Mô tả | Trạng thái SP | Trạng thái chi nhánh |
|-----|------------|----------|--------|---------|-------------------|-----------------|-------------------|-------------------|----------|-------------------|---------------|-----------|------------|-------|--------------|---------------------|
| CF_001 | Cà phê đen | Đồ uống | ly | | 8000 | 25000 | 25000 | 8000 | 100 | 10 | | | | Cà phê nguyên chất | TRUE | TRUE |
| CF_002 | Cà phê sữa | Đồ uống | ly | | 10000 | 30000 | 28000 | 10000 | 80 | 10 | 25000 | 10 | | | TRUE | TRUE |
| BANH_001 | Bánh mì | Bánh | cái | 8938500123456 | 5000 | 15000 | 15000 | 5000 | 50 | 5 | | | 2026-12-31 | Bánh mì thơm giòn | TRUE | TRUE |

---

## API Export

### `GET /api/products/import-export/export`

Xuất toàn bộ sản phẩm ra file `.xlsx`. Có thể xuất theo shop hoặc theo chi nhánh.

**Query Params:**

| Param | Bắt buộc | Mô tả |
|-------|----------|-------|
| `shopId` | ✅ | ID cửa hàng |
| `branchId` | ❌ | ID chi nhánh. Nếu bỏ trống, xuất toàn bộ sản phẩm cấp shop (các cột chi nhánh sẽ trống) |

**Headers:**
```
Authorization: Bearer <token>
```

**Response:** File `.xlsx` được tải xuống trực tiếp (stream binary).

**Tên file:**
- Có `branchId`: `products_branch_{branchId}.xlsx`
- Không có `branchId`: `products_shop_{shopId}.xlsx`

**Ví dụ FE (Axios):**
```javascript
const downloadExcel = async (shopId, branchId = null) => {
  const params = { shopId };
  if (branchId) params.branchId = branchId;

  const response = await axios.get('/api/products/import-export/export', {
    params,
    responseType: 'blob',
    headers: { Authorization: `Bearer ${token}` },
  });

  const url = URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.download = branchId
    ? `products_branch_${branchId}.xlsx`
    : `products_shop_${shopId}.xlsx`;
  link.click();
  URL.revokeObjectURL(url);
};
```

**Lưu ý:**
- Khi **không có `branchId`**, các cột `Giá bán chi nhánh`, `Giá nhập chi nhánh`, `Số lượng`, `Số lượng tối thiểu`, `Giá khuyến mãi`, `% Giảm giá`, `Hạn sử dụng`, `Trạng thái chi nhánh` sẽ **để trống**.
- Đây là file mẫu để FE cung cấp cho user download trước khi nhập.

---

## API Import

### `POST /api/products/import-export/import`

Nhập sản phẩm từ file Excel. Server sẽ **upsert** từng dòng:
- Nếu SKU đã tồn tại trong shop → **cập nhật** Product và BranchProduct.
- Nếu SKU chưa tồn tại → **tạo mới** Product và BranchProduct tại chi nhánh.

**Query Params:**

| Param | Bắt buộc | Mô tả |
|-------|----------|-------|
| `shopId` | ✅ | ID cửa hàng |
| `branchId` | ✅ | ID chi nhánh nhập hàng vào |

**Request:** `multipart/form-data`

| Field | Loại | Mô tả |
|-------|------|-------|
| `file` | File (.xlsx / .xls) | File Excel đúng cấu trúc cột ở trên |

**Response thành công:**
```json
{
  "code": "PRODUCT_IMPORTED",
  "message": "Nhập sản phẩm thành công",
  "data": 25
}
```
> `data` = số dòng được import/cập nhật thành công.

**Ví dụ FE (Axios):**
```javascript
const importProducts = async (shopId, branchId, file) => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await axios.post(
    '/api/products/import-export/import',
    formData,
    {
      params: { shopId, branchId },
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response.data; // { code, message, data: số dòng thành công }
};
```

**Ví dụ React component:**
```jsx
function ProductImport({ shopId, branchId }) {
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);

  const handleImport = async () => {
    if (!file) return;
    const res = await importProducts(shopId, branchId, file);
    setResult(`Đã nhập ${res.data} sản phẩm thành công`);
  };

  return (
    <div>
      <input type="file" accept=".xlsx,.xls" onChange={e => setFile(e.target.files[0])} />
      <button onClick={handleImport}>Nhập sản phẩm</button>
      {result && <p>{result}</p>}
    </div>
  );
}
```

---

## Quy tắc dữ liệu

### Danh mục (category)
- **Bắt buộc**. Là cơ sở để:
  - Sinh prefix SKU: `{INDUSTRY}_{CATEGORY}` → VD: `FNB_ĐỒUỐNG`, `RETAIL_THỜITRANG`
  - Liên kết barcode EAN-13 với nhóm hàng
- Giá trị tự do (không enum), server sẽ `.toUpperCase()` khi sinh prefix.

### SKU
- **Tùy chọn**. Nếu để trống, server tự sinh theo pattern `{INDUSTRY}_{CATEGORY}_001`.
- Nếu cung cấp: duy nhất trong shop, chỉ chứa chữ **IN HOA**, số, và dấu `_`. VD: `CF_001`, `BANH_001`.
- **Deduplication:** Nếu SKU đã tồn tại → **cập nhật** Product đó. Nếu SKU trống hoặc chưa tồn tại → **tạo mới**.
- Không chứa chữ thường, khoảng trắng, dấu gạch ngang `-`.

### Barcode
- Tùy chọn.
- Duy nhất trong shop.
- EAN-13/UPC-12 (12-13 chữ số), hoặc mã nội bộ (chữ IN HOA, số, dấu `_`).

### Giá
- `Giá nhập mặc định` (col 5): ≥ 0.
- `Giá bán mặc định` (col 6): > 0.
- `Giá bán chi nhánh` (col 7): > 0. Nếu để trống, lấy giá từ col 6.
- `Giá nhập chi nhánh` (col 8): ≥ 0.
- `Giá khuyến mãi` (col 11): Nếu có, phải < Giá bán chi nhánh.
- `% Giảm giá` (col 12): 0-100, hai trường discount có thể dùng độc lập.

### Số lượng
- `Số lượng` (col 9): Số nguyên ≥ 0.
- `Số lượng tối thiểu` (col 10): Số nguyên ≥ 0, ngưỡng cảnh báo nhập hàng.

### Hạn sử dụng
- Định dạng **`yyyy-MM-dd`** (ISO 8601). VD: `2026-12-31`.
- Nếu để trống: không có hạn.

### Trạng thái
- `Trạng thái SP` (col 15): `TRUE` hoặc `FALSE`. Mặc định `TRUE`.
- `Trạng thái chi nhánh` (col 16): `TRUE` hoặc `FALSE`. Mặc định `TRUE`.

---

## Xử lý lỗi

| HTTP Status | Mô tả |
|-------------|-------|
| 200 | Import thành công, trả về số dòng đã xử lý |
| 400 | File rỗng, file sai định dạng, hoặc dữ liệu không hợp lệ |
| 403 | Không có quyền import/export sản phẩm |

**Lỗi theo dòng:**
- Các dòng **lỗi riêng lẻ** sẽ bị bỏ qua, server ghi log và tiếp tục xử lý các dòng còn lại.
- Số dòng thành công được trả về trong `data`.
- FE nên hiển thị thông báo: *"Đã nhập X sản phẩm. Một số dòng bị bỏ qua do lỗi dữ liệu."*

**Các dòng bị bỏ qua khi:**
- **Tên sản phẩm** hoặc **Danh mục** để trống.
- Dữ liệu số không hợp lệ (chữ thay vì số).
- Ngày không đúng định dạng `yyyy-MM-dd`.

---

## Ví dụ flow FE

### Flow 1: Xuất template + Nhập lại

```
1. User bấm "Tải mẫu Excel"
   → GET /api/products/import-export/export?shopId=xxx&branchId=yyy
   → File products_branch_yyy.xlsx được download

2. User điền dữ liệu vào file Excel

3. User upload file qua dialog
   → POST /api/products/import-export/import?shopId=xxx&branchId=yyy
   → multipart/form-data, field "file"

4. Hiển thị kết quả: "Đã nhập 30 sản phẩm thành công"
5. Gọi lại API danh sách sản phẩm để refresh UI
```

### Flow 2: Xuất báo cáo

```
1. User chọn chi nhánh từ dropdown
2. Bấm "Xuất Excel"
   → GET /api/products/import-export/export?shopId=xxx&branchId=yyy
   → File download với đầy đủ dữ liệu giá + tồn kho chi nhánh

3. Xuất toàn shop (không chọn chi nhánh):
   → GET /api/products/import-export/export?shopId=xxx
   → Chỉ có thông tin Product, cột chi nhánh trống
```

---

## Quyền yêu cầu

| Thao tác | Permission |
|----------|-----------|
| Export | `PRODUCT_EXPORT` |
| Import | `PRODUCT_IMPORT` |

> Kiểm tra quyền trước khi hiển thị nút Import/Export trên UI.

---

## Variants (Biến thể sản phẩm)

> ⚠️ **Lưu ý:** File Excel hiện tại **không hỗ trợ import/export biến thể** (variants).  
> Biến thể (`ProductVariant` và `BranchProductVariant`) phải được quản lý thông qua API CRUD sản phẩm thông thường.

Nếu sản phẩm có biến thể (VD: cà phê Size S / Size M):
- Sử dụng API `PUT /api/shops/{shopId}/products/{productId}` để cập nhật `variants` trong `ProductRequest`.
- Sử dụng API `PUT /api/shops/{shopId}/branches/{branchId}/products/{branchProductId}` để cập nhật `branchVariants` trong `BranchProductRequest`.

