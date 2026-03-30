# Promotion Service API Documentation

> **Base URL:** `/api/promotions`
>
> **Yêu cầu gói dịch vụ:** `PRO` hoặc `ENTERPRISE`
>
> **Authentication:** Bearer Token (JWT) — tất cả endpoint đều yêu cầu đăng nhập.

---

## Mục lục

1. [Tổng quan](#tổng-quan)
2. [Data Models](#data-models)
3. [Endpoints](#endpoints)
   - [Lấy danh sách khuyến mãi](#1-lấy-danh-sách-khuyến-mãi)
   - [Tạo khuyến mãi](#2-tạo-khuyến-mãi)
   - [Cập nhật khuyến mãi](#3-cập-nhật-khuyến-mãi)
   - [Xóa khuyến mãi](#4-xóa-khuyến-mãi)
4. [Enum Reference](#enum-reference)
5. [Phân quyền (Permissions)](#phân-quyền-permissions)
6. [Error Codes](#error-codes)
7. [Ghi chú cho FE](#ghi-chú-cho-fe)

---

## Tổng quan

Promotion Service quản lý các chương trình khuyến mãi của cửa hàng. Hỗ trợ 2 loại giảm giá:

| Loại | Mô tả | Ví dụ |
|------|--------|-------|
| `PERCENT` | Giảm theo phần trăm | `discountValue: 10` → giảm 10% |
| `AMOUNT` | Giảm cố định số tiền | `discountValue: 50000` → giảm 50,000đ |

### Phạm vi áp dụng

Khuyến mãi hỗ trợ **2 cấp độ phạm vi**:

| `branchId` | Phạm vi | Mô tả |
|------------|---------|-------|
| `null` / không truyền | **Toàn shop** | Áp dụng cho tất cả chi nhánh |
| `"branch123"` | **Một chi nhánh** | Chỉ áp dụng cho chi nhánh cụ thể |

Khuyến mãi có thể áp dụng cho **toàn bộ sản phẩm** (khi `applicableProductIds` rỗng/null) hoặc **một số sản phẩm cụ thể**.

---

## Data Models

### PromotionRequest (Request Body)

```json
{
  "name": "string (bắt buộc)",
  "discountType": "PERCENT | AMOUNT (bắt buộc)",
  "discountValue": "number > 0 (bắt buộc)",
  "applicableProductIds": ["string"] | null,
  "startDate": "datetime (bắt buộc)",
  "endDate": "datetime (bắt buộc)",
  "active": "boolean (mặc định: true)",
  "branchId": "string | null"
}
```

| Field | Type | Required | Validation | Mô tả |
|-------|------|----------|------------|-------|
| `name` | `string` | **Có** | `@NotBlank` | Tên khuyến mãi |
| `discountType` | `string` | **Có** | `@NotNull`, enum `PERCENT` / `AMOUNT` | Loại giảm giá |
| `discountValue` | `number` | **Có** | `@Positive` (> 0) | Giá trị giảm (% hoặc số tiền) |
| `applicableProductIds` | `string[]` | Không | — | Danh sách ID sản phẩm được áp dụng. `null`/rỗng = tất cả |
| `startDate` | `datetime` | **Có** | `@NotNull` | Ngày bắt đầu (ISO 8601) |
| `endDate` | `datetime` | **Có** | `@NotNull` | Ngày kết thúc (ISO 8601) |
| `active` | `boolean` | Không | — | Trạng thái kích hoạt. Mặc định `true` |
| `branchId` | `string` | Không | — | ID chi nhánh. `null` = áp dụng toàn shop |

### PromotionResponse (Response Body)

```json
{
  "id": "string",
  "name": "string",
  "discountType": "PERCENT | AMOUNT",
  "discountValue": 10.0,
  "applicableProductIds": ["productId1", "productId2"],
  "startDate": "2026-04-01T00:00:00",
  "endDate": "2026-04-30T23:59:59",
  "active": true,
  "branchId": "branch456" | null
}
```

| Field | Type | Mô tả |
|-------|------|-------|
| `id` | `string` | ID duy nhất của khuyến mãi |
| `name` | `string` | Tên khuyến mãi |
| `discountType` | `string` | `PERCENT` hoặc `AMOUNT` |
| `discountValue` | `number` | Giá trị giảm giá |
| `applicableProductIds` | `string[]` | Danh sách ID sản phẩm được áp dụng |
| `startDate` | `datetime` | Ngày bắt đầu |
| `endDate` | `datetime` | Ngày kết thúc |
| `active` | `boolean` | Trạng thái kích hoạt |
| `branchId` | `string \| null` | ID chi nhánh. `null` = áp dụng toàn shop |

### API Response Wrapper

Tất cả response đều được bọc trong `ApiResponseDto`:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Thành công",
  "data": { ... },
  "timestamp": "2026-03-30T10:00:00.000Z"
}
```

---

## Endpoints

### 1. Lấy danh sách khuyến mãi

Lấy danh sách khuyến mãi theo shop với phân trang. Hỗ trợ lọc theo chi nhánh.

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/promotions` |
| **Permission** | `PROMOTION_VIEW` |
| **Plan** | `PRO`, `ENTERPRISE` |

#### Query Parameters

| Parameter | Type | Required | Mô tả |
|-----------|------|----------|-------|
| `shopId` | `string` | **Có** | ID của cửa hàng |
| `branchId` | `string` | Không | ID chi nhánh. Xem logic bên dưới |
| `page` | `number` | Không | Số trang (bắt đầu từ 0). Mặc định: `0` |
| `size` | `number` | Không | Số item mỗi trang. Mặc định: `20` |
| `sort` | `string` | Không | Sắp xếp. VD: `startDate,desc` |

#### Logic lọc theo branchId

| Trường hợp | Kết quả |
|-------------|---------|
| **Không truyền** `branchId` | Trả về **tất cả** khuyến mãi của shop (cả toàn shop lẫn riêng từng branch) |
| **Truyền** `branchId=branch456` | Trả về khuyến mãi **toàn shop** (`branchId = null`) + khuyến mãi **riêng branch456** |

> Khi FE hiển thị cho 1 chi nhánh cụ thể, nên truyền `branchId` để chi nhánh đó thấy cả khuyến mãi toàn shop lẫn khuyến mãi riêng của mình.

#### Request Example

```http
GET /api/promotions?shopId=shop123&branchId=branch456&page=0&size=10&sort=startDate,desc
Authorization: Bearer <token>
```

#### Response Example

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Thành công",
  "data": {
    "content": [
      {
        "id": "promo001",
        "name": "Giảm giá mùa hè (toàn shop)",
        "discountType": "PERCENT",
        "discountValue": 15.0,
        "applicableProductIds": ["prod001", "prod002"],
        "startDate": "2026-06-01T00:00:00",
        "endDate": "2026-06-30T23:59:59",
        "active": true,
        "branchId": null
      },
      {
        "id": "promo002",
        "name": "Flash Sale chi nhánh Q1",
        "discountType": "AMOUNT",
        "discountValue": 50000.0,
        "applicableProductIds": null,
        "startDate": "2026-04-01T00:00:00",
        "endDate": "2026-04-01T23:59:59",
        "active": true,
        "branchId": "branch456"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": { "sorted": true, "unsorted": false, "empty": false }
    },
    "totalElements": 2,
    "totalPages": 1,
    "last": true,
    "first": true,
    "size": 10,
    "number": 0,
    "numberOfElements": 2,
    "empty": false
  },
  "timestamp": "2026-03-30T10:00:00.000Z"
}
```

#### Error Responses

| HTTP Code | Code | Mô tả |
|-----------|------|-------|
| 401 | `UNAUTHORIZED` | Chưa đăng nhập |
| 403 | `FORBIDDEN` | Không có quyền `PROMOTION_VIEW` hoặc gói dịch vụ không đủ |

---

### 2. Tạo khuyến mãi

Tạo một chương trình khuyến mãi mới. Nếu không truyền `branchId`, khuyến mãi sẽ áp dụng cho toàn bộ shop.

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/promotions` |
| **Permission** | `PROMOTION_CREATE` |
| **Plan** | `PRO`, `ENTERPRISE` |

#### Query Parameters

| Parameter | Type | Required | Mô tả |
|-----------|------|----------|-------|
| `shopId` | `string` | **Có** | ID của cửa hàng |

#### Request Body

**Tạo khuyến mãi cho 1 chi nhánh:**

```json
{
  "name": "Flash Sale chi nhánh Q1",
  "discountType": "AMOUNT",
  "discountValue": 50000,
  "applicableProductIds": ["prod001"],
  "startDate": "2026-06-01T00:00:00",
  "endDate": "2026-06-30T23:59:59",
  "active": true,
  "branchId": "branch456"
}
```

**Tạo khuyến mãi toàn shop (không truyền branchId):**

```json
{
  "name": "Giảm giá mùa hè",
  "discountType": "PERCENT",
  "discountValue": 15,
  "applicableProductIds": null,
  "startDate": "2026-06-01T00:00:00",
  "endDate": "2026-06-30T23:59:59",
  "active": true
}
```

#### Request Example

```http
POST /api/promotions?shopId=shop123
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Giảm giá mùa hè",
  "discountType": "PERCENT",
  "discountValue": 15,
  "applicableProductIds": null,
  "startDate": "2026-06-01T00:00:00",
  "endDate": "2026-06-30T23:59:59",
  "active": true
}
```

#### Response Example (200 OK)

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Thành công",
  "data": {
    "id": "promo001",
    "name": "Giảm giá mùa hè",
    "discountType": "PERCENT",
    "discountValue": 15.0,
    "applicableProductIds": null,
    "startDate": "2026-06-01T00:00:00",
    "endDate": "2026-06-30T23:59:59",
    "active": true,
    "branchId": null
  },
  "timestamp": "2026-03-30T10:00:00.000Z"
}
```

#### Error Responses

| HTTP Code | Code | Mô tả |
|-----------|------|-------|
| 400 | `BAD_REQUEST` | Validation failed (xem chi tiết bên dưới) |
| 401 | `UNAUTHORIZED` | Chưa đăng nhập |
| 403 | `FORBIDDEN` | Không có quyền `PROMOTION_CREATE` hoặc gói dịch vụ không đủ |

#### Validation Errors

| Field | Rule | Message |
|-------|------|---------|
| `name` | Không được trống | must not be blank |
| `discountType` | Không được null | must not be null |
| `discountValue` | Phải > 0 | must be positive |
| `startDate` | Không được null | must not be null |
| `endDate` | Không được null | must not be null |

---

### 3. Cập nhật khuyến mãi

Cập nhật thông tin một khuyến mãi đã tồn tại.

| | |
|---|---|
| **Method** | `PUT` |
| **URL** | `/api/promotions/{id}` |
| **Permission** | `PROMOTION_UPDATE` |
| **Plan** | `PRO`, `ENTERPRISE` |

#### Path Parameters

| Parameter | Type | Required | Mô tả |
|-----------|------|----------|-------|
| `id` | `string` | **Có** | ID của khuyến mãi cần cập nhật |

#### Query Parameters

| Parameter | Type | Required | Mô tả |
|-----------|------|----------|-------|
| `shopId` | `string` | **Có** | ID của cửa hàng |

#### Request Body

Giống hoàn toàn với [PromotionRequest](#promotionrequest-request-body).

> **Lưu ý quan trọng:** `branchId` trong body **phải trùng** với `branchId` hiện tại của khuyến mãi. Không được thay đổi phạm vi (branch → shop-wide hoặc ngược lại). Nếu không khớp, API trả lỗi `UNAUTHORIZED`.

| Promotion hiện tại | Request gửi lên | Kết quả |
|---------------------|-----------------|---------|
| `branchId: null` | `branchId: null` hoặc không truyền | OK |
| `branchId: null` | `branchId: "branch456"` | **Lỗi UNAUTHORIZED** |
| `branchId: "branch456"` | `branchId: "branch456"` | OK |
| `branchId: "branch456"` | `branchId: null` | **Lỗi UNAUTHORIZED** |

#### Request Example

```http
PUT /api/promotions/promo001?shopId=shop123
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Giảm giá mùa hè - Updated",
  "discountType": "PERCENT",
  "discountValue": 20,
  "applicableProductIds": ["prod001", "prod002", "prod003"],
  "startDate": "2026-06-01T00:00:00",
  "endDate": "2026-07-15T23:59:59",
  "active": true,
  "branchId": null
}
```

#### Response Example (200 OK)

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Thành công",
  "data": {
    "id": "promo001",
    "name": "Giảm giá mùa hè - Updated",
    "discountType": "PERCENT",
    "discountValue": 20.0,
    "applicableProductIds": ["prod001", "prod002", "prod003"],
    "startDate": "2026-06-01T00:00:00",
    "endDate": "2026-07-15T23:59:59",
    "active": true,
    "branchId": null
  },
  "timestamp": "2026-03-30T10:00:00.000Z"
}
```

#### Error Responses

| HTTP Code | Code | Mô tả |
|-----------|------|-------|
| 400 | `BAD_REQUEST` | Validation failed |
| 401 | `UNAUTHORIZED` | Chưa đăng nhập hoặc `branchId` không khớp |
| 403 | `FORBIDDEN` | Không có quyền `PROMOTION_UPDATE` hoặc gói dịch vụ không đủ |
| 404 | `PROMOTION_NOT_FOUND` | Không tìm thấy khuyến mãi với ID này trong shop |

---

### 4. Xóa khuyến mãi

Xóa mềm (soft delete) một khuyến mãi. Dữ liệu không bị xóa khỏi database mà chỉ đánh dấu `deleted = true`.

| | |
|---|---|
| **Method** | `DELETE` |
| **URL** | `/api/promotions/{id}` |
| **Permission** | `PROMOTION_DELETE` |
| **Plan** | `PRO`, `ENTERPRISE` |

#### Path Parameters

| Parameter | Type | Required | Mô tả |
|-----------|------|----------|-------|
| `id` | `string` | **Có** | ID của khuyến mãi cần xóa |

#### Query Parameters

| Parameter | Type | Required | Mô tả |
|-----------|------|----------|-------|
| `shopId` | `string` | **Có** | ID của cửa hàng |

#### Request Example

```http
DELETE /api/promotions/promo001?shopId=shop123
Authorization: Bearer <token>
```

#### Response Example (200 OK)

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Thành công",
  "data": null,
  "timestamp": "2026-03-30T10:00:00.000Z"
}
```

#### Error Responses

| HTTP Code | Code | Mô tả |
|-----------|------|-------|
| 401 | `UNAUTHORIZED` | Chưa đăng nhập |
| 403 | `FORBIDDEN` | Không có quyền `PROMOTION_DELETE` hoặc gói dịch vụ không đủ |
| 404 | `PROMOTION_NOT_FOUND` | Không tìm thấy khuyến mãi với ID này trong shop |

---

## Enum Reference

### DiscountType

| Value | Mô tả |
|-------|-------|
| `PERCENT` | Giảm giá theo phần trăm (%). VD: `discountValue = 10` → giảm 10% |
| `AMOUNT` | Giảm cố định số tiền (VNĐ). VD: `discountValue = 50000` → giảm 50,000đ |

---

## Phân quyền (Permissions)

Mỗi endpoint yêu cầu permission riêng. User cần được gán permission tương ứng mới có thể gọi API.

| Permission | Mô tả | Endpoint |
|------------|-------|----------|
| `PROMOTION_VIEW` | Xem danh sách khuyến mãi | `GET /api/promotions` |
| `PROMOTION_CREATE` | Tạo khuyến mãi mới | `POST /api/promotions` |
| `PROMOTION_UPDATE` | Cập nhật khuyến mãi | `PUT /api/promotions/{id}` |
| `PROMOTION_DELETE` | Xóa khuyến mãi | `DELETE /api/promotions/{id}` |

> Ngoài permission, user phải thuộc gói **PRO** hoặc **ENTERPRISE** mới truy cập được tính năng khuyến mãi.

---

## Error Codes

| Code | Mô tả |
|------|-------|
| `SUCCESS` | Thao tác thành công |
| `UNAUTHORIZED` | Không có quyền truy cập / branchId không khớp khi update |
| `PROMOTION_NOT_FOUND` | Khuyến mãi không tồn tại hoặc đã bị xóa |
| `BAD_REQUEST` | Dữ liệu đầu vào không hợp lệ |
| `FORBIDDEN` | Không đủ quyền hoặc gói dịch vụ không hợp lệ |

---

## Ghi chú cho FE

### 1. branchId — Phạm vi khuyến mãi

Đây là trường quan trọng nhất cần hiểu:

| Khi tạo (POST) | Ý nghĩa |
|-----------------|---------|
| Không truyền `branchId` hoặc `branchId: null` | Khuyến mãi áp dụng **toàn shop** (tất cả chi nhánh) |
| `branchId: "branch456"` | Khuyến mãi chỉ áp dụng cho **chi nhánh đó** |

| Khi lấy danh sách (GET) | Ý nghĩa |
|--------------------------|---------|
| Không truyền `branchId` | Trả tất cả khuyến mãi của shop |
| `branchId=branch456` | Trả khuyến mãi toàn shop + khuyến mãi riêng branch456 |

**FE nên:**
- Trong form tạo/sửa: hiển thị dropdown "Phạm vi" với 2 option: "Toàn shop" (`null`) và "Chi nhánh cụ thể" (chọn branchId)
- Trong danh sách: hiển thị tag/badge phân biệt "Toàn shop" vs tên chi nhánh
- Khi update: **không cho phép thay đổi** `branchId` (disable field hoặc tự gán lại giá trị cũ từ response)

### 2. Datetime Format

Sử dụng format ISO 8601 **không có timezone**:

```
2026-06-01T00:00:00
```

Khi gửi request, FE nên format datetime theo dạng `LocalDateTime` (không kèm `Z` hay `+07:00`).

### 3. Pagination

Sử dụng Spring Pageable chuẩn:

- `page`: bắt đầu từ **0**
- `size`: số lượng item mỗi trang
- `sort`: `fieldName,asc|desc` (VD: `startDate,desc`)

Response trả về object `Page` với các thuộc tính: `content`, `totalElements`, `totalPages`, `number`, `size`, `first`, `last`, `empty`.

### 4. applicableProductIds

- Nếu muốn áp dụng cho **tất cả sản phẩm**: gửi `null` hoặc `[]`
- Nếu muốn áp dụng cho **một số sản phẩm**: gửi mảng product ID

FE nên hiển thị UI cho phép chọn sản phẩm cụ thể hoặc "Tất cả sản phẩm".

### 5. Trạng thái khuyến mãi

FE nên hiển thị trạng thái dựa trên cả `active` và khoảng thời gian:

| active | Thời gian | Hiển thị |
|--------|-----------|----------|
| `true` | `startDate <= now <= endDate` | **Đang hoạt động** |
| `true` | `now < startDate` | **Sắp diễn ra** |
| `true` | `now > endDate` | **Đã hết hạn** |
| `false` | — | **Tạm dừng** |

### 6. Validation phía FE

Nên validate trước khi gửi request:

- `name`: không được trống
- `discountType`: phải là `PERCENT` hoặc `AMOUNT`
- `discountValue`:
  - Phải > 0
  - Nếu `PERCENT`: nên giới hạn max 100
  - Nếu `AMOUNT`: nên giới hạn hợp lý theo giá sản phẩm
- `startDate` < `endDate`
- `startDate` và `endDate`: không được null

### 7. Xóa khuyến mãi

API sử dụng **soft delete**. Sau khi xóa, khuyến mãi sẽ không xuất hiện trong danh sách nữa. FE chỉ cần gọi DELETE và reload danh sách.

### 8. TypeScript Interfaces (tham khảo)

```typescript
enum DiscountType {
  PERCENT = 'PERCENT',
  AMOUNT = 'AMOUNT',
}

interface PromotionRequest {
  name: string;
  discountType: DiscountType;
  discountValue: number;
  applicableProductIds?: string[] | null;
  startDate: string; // ISO 8601: "2026-06-01T00:00:00"
  endDate: string;   // ISO 8601: "2026-06-30T23:59:59"
  active?: boolean;  // default: true
  branchId?: string | null; // null = toàn shop
}

interface PromotionResponse {
  id: string;
  name: string;
  discountType: DiscountType;
  discountValue: number;
  applicableProductIds: string[] | null;
  startDate: string;
  endDate: string;
  active: boolean;
  branchId: string | null; // null = toàn shop
}

interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
}

interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
```
