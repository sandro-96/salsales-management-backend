# Customer Service API Documentation

> **Base URL:** `/api/customers`
>
> **Authentication:** Bearer Token (JWT) — tất cả endpoint đều yêu cầu đăng nhập.

---

## Mục lục

1. [Tổng quan](#tổng-quan)
2. [Data Models](#data-models)
3. [Endpoints](#endpoints)
   - [Tìm kiếm khách hàng](#1-tìm-kiếm-khách-hàng)
   - [Lấy chi tiết khách hàng](#2-lấy-chi-tiết-khách-hàng)
   - [Tạo khách hàng](#3-tạo-khách-hàng)
   - [Cập nhật khách hàng](#4-cập-nhật-khách-hàng)
   - [Xóa khách hàng](#5-xóa-khách-hàng)
   - [Xuất danh sách khách hàng (Excel)](#6-xuất-danh-sách-khách-hàng-ra-excel)
4. [Phân quyền (Permissions)](#phân-quyền-permissions)
5. [Error Codes](#error-codes)
6. [Ghi chú cho FE](#ghi-chú-cho-fe)

---

## Tổng quan

Customer Service quản lý thông tin khách hàng của cửa hàng. Hỗ trợ tìm kiếm với phân trang, lọc theo từ khoá / khoảng ngày, sắp xếp, và xuất Excel.

### Tính năng chính

| Tính năng | Mô tả |
|-----------|-------|
| **Tìm kiếm & Phân trang** | Tìm theo tên, SĐT, email với phân trang và sắp xếp |
| **CRUD** | Tạo, xem chi tiết, cập nhật, xóa mềm khách hàng |
| **Lọc theo chi nhánh** | Hiển thị khách hàng thuộc chi nhánh cụ thể hoặc toàn shop |
| **Lọc theo ngày** | Lọc khách hàng theo khoảng ngày tạo |
| **Xuất Excel** | Xuất danh sách khách hàng ra file `.xlsx` với cùng bộ lọc |
| **Audit Log** | Ghi log mọi thao tác tạo, cập nhật, xóa |

### Phạm vi khách hàng

| `branchId` | Phạm vi | Mô tả |
|------------|---------|-------|
| `null` / không truyền | **Toàn shop** | Khách hàng chung, không gắn với chi nhánh cụ thể |
| `"branch456"` | **Một chi nhánh** | Khách hàng thuộc chi nhánh cụ thể |

---

## Data Models

### CustomerRequest (Request Body)

```json
{
  "name": "Nguyễn Văn A",
  "phone": "0901234567",
  "email": "nguyenvana@email.com",
  "address": "123 Nguyễn Huệ, Q.1, TP.HCM",
  "note": "Khách VIP",
  "branchId": "branch456"
}
```

| Field | Type | Required | Validation | Mô tả |
|-------|------|----------|------------|-------|
| `name` | `string` | **Có** | `@NotBlank` | Tên khách hàng |
| `phone` | `string` | Không | — | Số điện thoại |
| `email` | `string` | Không | `@Email` | Địa chỉ email |
| `address` | `string` | Không | — | Địa chỉ |
| `note` | `string` | Không | — | Ghi chú |
| `branchId` | `string` | Không | — | ID chi nhánh. `null` = không phân biệt chi nhánh |

### CustomerResponse

```json
{
  "id": "cust001",
  "name": "Nguyễn Văn A",
  "phone": "0901234567",
  "email": "nguyenvana@email.com",
  "address": "123 Nguyễn Huệ, Q.1, TP.HCM",
  "note": "Khách VIP",
  "branchId": "branch456",
  "createdAt": "2026-04-01T10:30:00"
}
```

| Field | Type | Mô tả |
|-------|------|-------|
| `id` | `string` | ID duy nhất của khách hàng |
| `name` | `string` | Tên khách hàng |
| `phone` | `string` | Số điện thoại |
| `email` | `string` | Địa chỉ email |
| `address` | `string` | Địa chỉ |
| `note` | `string` | Ghi chú |
| `branchId` | `string \| null` | ID chi nhánh. `null` = không phân biệt chi nhánh |
| `createdAt` | `datetime` | Thời điểm tạo (ISO 8601) |

### CustomerSearchRequest (Query Parameters)

| Parameter | Type | Required | Default | Mô tả |
|-----------|------|----------|---------|-------|
| `keyword` | `string` | Không | `""` | Từ khoá tìm kiếm (tên, SĐT, email) |
| `page` | `int` | Không | `0` | Số trang (bắt đầu từ 0) |
| `size` | `int` | Không | `20` | Số item mỗi trang |
| `fromDate` | `LocalDate` | Không | — | Lọc từ ngày (yyyy-MM-dd) |
| `toDate` | `LocalDate` | Không | — | Lọc đến ngày (yyyy-MM-dd) |
| `sortBy` | `string` | Không | `createdAt` | Trường sắp xếp |
| `sortDir` | `string` | Không | `desc` | Hướng sắp xếp: `asc` hoặc `desc` |

### API Response Wrapper

Tất cả response đều được bọc trong `ApiResponseDto`:

```json
{
  "success": true,
  "code": "4124",
  "message": "Customer list retrieved",
  "data": { ... },
  "timestamp": "2026-04-03T10:00:00.000Z"
}
```

---

## Endpoints

### 1. Tìm kiếm khách hàng

Tìm kiếm khách hàng theo cửa hàng với phân trang, lọc theo từ khoá, khoảng ngày tạo, và sắp xếp.

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/customers` |
| **Permission** | `CUSTOMER_VIEW` |

#### Query Parameters

| Parameter | Type | Required | Mô tả |
|-----------|------|----------|-------|
| `shopId` | `string` | **Có** | ID của cửa hàng |
| `branchId` | `string` | Không | ID chi nhánh. Không truyền = toàn shop |
| `keyword` | `string` | Không | Từ khoá tìm kiếm (tên, SĐT, email) — case-insensitive |
| `page` | `int` | Không | Số trang (default: 0) |
| `size` | `int` | Không | Số item/trang (default: 20) |
| `fromDate` | `LocalDate` | Không | Lọc từ ngày tạo (yyyy-MM-dd) |
| `toDate` | `LocalDate` | Không | Lọc đến ngày tạo (yyyy-MM-dd) |
| `sortBy` | `string` | Không | Trường sắp xếp (default: `createdAt`) |
| `sortDir` | `string` | Không | `asc` hoặc `desc` (default: `desc`) |

#### Request Example

```http
GET /api/customers?shopId=shop123&branchId=branch456&keyword=Nguyễn&page=0&size=10&sortBy=name&sortDir=asc
Authorization: Bearer <token>
```

#### Response Example (200 OK)

```json
{
  "success": true,
  "code": "4124",
  "message": "Customer list retrieved",
  "data": {
    "content": [
      {
        "id": "cust001",
        "name": "Nguyễn Văn A",
        "phone": "0901234567",
        "email": "nguyenvana@email.com",
        "address": "123 Nguyễn Huệ, Q.1",
        "note": "Khách VIP",
        "branchId": "branch456",
        "createdAt": "2026-03-15T10:30:00"
      },
      {
        "id": "cust002",
        "name": "Nguyễn Thị B",
        "phone": "0907654321",
        "email": null,
        "address": "456 Lê Lợi, Q.3",
        "note": null,
        "branchId": "branch456",
        "createdAt": "2026-04-01T14:20:00"
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
  "timestamp": "2026-04-03T10:00:00.000Z"
}
```

#### Logic tìm kiếm

| Filter | Cách hoạt động |
|--------|---------------|
| `keyword` | Tìm theo regex (case-insensitive) trên 3 trường: `name`, `email`, `phone` |
| `branchId` | Nếu truyền, chỉ lấy khách hàng thuộc chi nhánh đó. Không truyền = toàn shop |
| `fromDate` / `toDate` | Lọc theo `createdAt` của khách hàng |
| Soft delete | Tự động loại bỏ khách hàng đã xóa (`deleted = false`) |

#### Error Responses

| HTTP Code | Code | Mô tả |
|-----------|------|-------|
| 401 | `UNAUTHORIZED` | Chưa đăng nhập |
| 403 | `FORBIDDEN` | Không có quyền `CUSTOMER_VIEW` |

---

### 2. Lấy chi tiết khách hàng

Lấy thông tin chi tiết một khách hàng theo ID.

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/customers/{id}` |
| **Permission** | `CUSTOMER_VIEW` |

#### Path Parameters

| Parameter | Type | Required | Mô tả |
|-----------|------|----------|-------|
| `id` | `string` | **Có** | ID của khách hàng |

#### Query Parameters

| Parameter | Type | Required | Mô tả |
|-----------|------|----------|-------|
| `shopId` | `string` | **Có** | ID của cửa hàng |

#### Request Example

```http
GET /api/customers/cust001?shopId=shop123
Authorization: Bearer <token>
```

#### Response Example (200 OK)

```json
{
  "success": true,
  "code": "2000",
  "message": "Operation successful",
  "data": {
    "id": "cust001",
    "name": "Nguyễn Văn A",
    "phone": "0901234567",
    "email": "nguyenvana@email.com",
    "address": "123 Nguyễn Huệ, Q.1, TP.HCM",
    "note": "Khách VIP",
    "branchId": "branch456",
    "createdAt": "2026-03-15T10:30:00"
  },
  "timestamp": "2026-04-03T10:00:00.000Z"
}
```

#### Error Responses

| HTTP Code | Code | Mô tả |
|-----------|------|-------|
| 401 | `UNAUTHORIZED` | Chưa đăng nhập |
| 403 | `FORBIDDEN` | Không có quyền `CUSTOMER_VIEW` |
| 404 | `CUSTOMER_NOT_FOUND` | Không tìm thấy khách hàng với ID này trong shop |

---

### 3. Tạo khách hàng

Tạo một khách hàng mới trong cửa hàng.

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/customers` |
| **Permission** | `CUSTOMER_UPDATE` |

#### Query Parameters

| Parameter | Type | Required | Mô tả |
|-----------|------|----------|-------|
| `shopId` | `string` | **Có** | ID của cửa hàng |

#### Request Body

```json
{
  "name": "Nguyễn Văn A",
  "phone": "0901234567",
  "email": "nguyenvana@email.com",
  "address": "123 Nguyễn Huệ, Q.1, TP.HCM",
  "note": "Khách VIP",
  "branchId": "branch456"
}
```

#### Request Example

```http
POST /api/customers?shopId=shop123
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Nguyễn Văn A",
  "phone": "0901234567",
  "email": "nguyenvana@email.com",
  "address": "123 Nguyễn Huệ, Q.1",
  "note": "Khách VIP",
  "branchId": "branch456"
}
```

#### Response Example (200 OK)

```json
{
  "success": true,
  "code": "4121",
  "message": "Customer created successfully",
  "data": {
    "id": "cust001",
    "name": "Nguyễn Văn A",
    "phone": "0901234567",
    "email": "nguyenvana@email.com",
    "address": "123 Nguyễn Huệ, Q.1",
    "note": "Khách VIP",
    "branchId": "branch456",
    "createdAt": "2026-04-03T10:30:00"
  },
  "timestamp": "2026-04-03T10:30:00.000Z"
}
```

#### Validation Errors

| Field | Rule | Message |
|-------|------|---------|
| `name` | Không được trống | Tên khách hàng không được để trống |
| `email` | Phải đúng format (nếu có) | Email không hợp lệ |

#### Error Responses

| HTTP Code | Code | Mô tả |
|-----------|------|-------|
| 400 | `VALIDATION_ERROR` | Dữ liệu đầu vào không hợp lệ |
| 401 | `UNAUTHORIZED` | Chưa đăng nhập |
| 403 | `FORBIDDEN` | Không có quyền `CUSTOMER_UPDATE` |

---

### 4. Cập nhật khách hàng

Cập nhật thông tin khách hàng theo ID.

| | |
|---|---|
| **Method** | `PUT` |
| **URL** | `/api/customers/{id}` |
| **Permission** | `CUSTOMER_UPDATE` |

#### Path Parameters

| Parameter | Type | Required | Mô tả |
|-----------|------|----------|-------|
| `id` | `string` | **Có** | ID của khách hàng cần cập nhật |

#### Query Parameters

| Parameter | Type | Required | Mô tả |
|-----------|------|----------|-------|
| `shopId` | `string` | **Có** | ID của cửa hàng |

#### Request Body

Giống hoàn toàn với [CustomerRequest](#customerrequest-request-body).

> **Lưu ý quan trọng:** Nếu khách hàng hiện tại có `branchId`, thì `branchId` trong request body **phải trùng** với giá trị hiện tại. Không được thay đổi chi nhánh của khách hàng. Nếu không khớp, API trả lỗi `UNAUTHORIZED`.

| Customer hiện tại | Request gửi lên | Kết quả |
|-------------------|-----------------|---------|
| `branchId: null` | `branchId`: bất kỳ | OK (không kiểm tra vì hiện tại null) |
| `branchId: "branch456"` | `branchId: "branch456"` | OK |
| `branchId: "branch456"` | `branchId: "branch789"` | **Lỗi UNAUTHORIZED** |

#### Request Example

```http
PUT /api/customers/cust001?shopId=shop123
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Nguyễn Văn A - Updated",
  "phone": "0901234567",
  "email": "nguyenvana@email.com",
  "address": "789 Trần Hưng Đạo, Q.5",
  "note": "Khách VIP - Diamond",
  "branchId": "branch456"
}
```

#### Response Example (200 OK)

```json
{
  "success": true,
  "code": "4122",
  "message": "Customer updated successfully",
  "data": {
    "id": "cust001",
    "name": "Nguyễn Văn A - Updated",
    "phone": "0901234567",
    "email": "nguyenvana@email.com",
    "address": "789 Trần Hưng Đạo, Q.5",
    "note": "Khách VIP - Diamond",
    "branchId": "branch456",
    "createdAt": "2026-03-15T10:30:00"
  },
  "timestamp": "2026-04-03T11:00:00.000Z"
}
```

#### Error Responses

| HTTP Code | Code | Mô tả |
|-----------|------|-------|
| 400 | `VALIDATION_ERROR` | Dữ liệu đầu vào không hợp lệ |
| 401 | `UNAUTHORIZED` | Chưa đăng nhập hoặc branchId không khớp |
| 403 | `FORBIDDEN` | Không có quyền `CUSTOMER_UPDATE` |
| 404 | `CUSTOMER_NOT_FOUND` | Không tìm thấy khách hàng với ID này trong shop |

---

### 5. Xóa khách hàng

Xóa mềm (soft delete) một khách hàng. Dữ liệu không bị xóa khỏi database mà chỉ đánh dấu `deleted = true`.

| | |
|---|---|
| **Method** | `DELETE` |
| **URL** | `/api/customers/{id}` |
| **Permission** | `CUSTOMER_DELETE` |

#### Path Parameters

| Parameter | Type | Required | Mô tả |
|-----------|------|----------|-------|
| `id` | `string` | **Có** | ID của khách hàng cần xóa |

#### Query Parameters

| Parameter | Type | Required | Mô tả |
|-----------|------|----------|-------|
| `shopId` | `string` | **Có** | ID của cửa hàng |
| `branchId` | `string` | **Có** | ID chi nhánh (dùng để xác minh quyền) |

#### Request Example

```http
DELETE /api/customers/cust001?shopId=shop123&branchId=branch456
Authorization: Bearer <token>
```

#### Response Example (200 OK)

```json
{
  "success": true,
  "code": "4123",
  "message": "Customer deleted successfully",
  "data": null,
  "timestamp": "2026-04-03T12:00:00.000Z"
}
```

#### Error Responses

| HTTP Code | Code | Mô tả |
|-----------|------|-------|
| 401 | `UNAUTHORIZED` | Chưa đăng nhập hoặc branchId không khớp |
| 403 | `FORBIDDEN` | Không có quyền `CUSTOMER_DELETE` |
| 404 | `CUSTOMER_NOT_FOUND` | Không tìm thấy khách hàng với ID này trong shop |

---

### 6. Xuất danh sách khách hàng ra Excel

Xuất danh sách khách hàng ra file `.xlsx` với cùng bộ lọc như endpoint tìm kiếm.

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/customers/export` |
| **Permission** | `CUSTOMER_VIEW` |

#### Query Parameters

Giống hoàn toàn với endpoint [Tìm kiếm khách hàng](#1-tìm-kiếm-khách-hàng) (ngoại trừ `page` và `size` không áp dụng vì xuất toàn bộ).

| Parameter | Type | Required | Mô tả |
|-----------|------|----------|-------|
| `shopId` | `string` | **Có** | ID của cửa hàng |
| `branchId` | `string` | Không | ID chi nhánh |
| `keyword` | `string` | Không | Từ khoá tìm kiếm |
| `fromDate` | `LocalDate` | Không | Lọc từ ngày (yyyy-MM-dd) |
| `toDate` | `LocalDate` | Không | Lọc đến ngày (yyyy-MM-dd) |

#### Request Example

```http
GET /api/customers/export?shopId=shop123&branchId=branch456&keyword=Nguyễn
Authorization: Bearer <token>
```

#### Response

- **Content-Type:** `application/octet-stream`
- **Content-Disposition:** `attachment; filename="customers.xlsx"`
- File Excel với các cột: **Tên**, **Số điện thoại**, **Email**, **Địa chỉ**, **Ghi chú**, **Ngày tạo**
- Dữ liệu sắp xếp theo tên (A → Z)

#### Error Responses

| HTTP Code | Code | Mô tả |
|-----------|------|-------|
| 401 | `UNAUTHORIZED` | Chưa đăng nhập |
| 403 | `FORBIDDEN` | Không có quyền `CUSTOMER_VIEW` |

---

## Phân quyền (Permissions)

| Permission | Mô tả | Endpoints |
|------------|-------|-----------|
| `CUSTOMER_VIEW` | Xem danh sách, chi tiết, và xuất khách hàng | `GET /api/customers`, `GET /api/customers/{id}`, `GET /api/customers/export` |
| `CUSTOMER_UPDATE` | Tạo và cập nhật khách hàng | `POST /api/customers`, `PUT /api/customers/{id}` |
| `CUSTOMER_DELETE` | Xóa khách hàng | `DELETE /api/customers/{id}` |

---

## Error Codes

| Code | Mô tả |
|------|-------|
| `SUCCESS` (2000) | Thao tác thành công |
| `CUSTOMER_CREATED` (4121) | Tạo khách hàng thành công |
| `CUSTOMER_UPDATED` (4122) | Cập nhật khách hàng thành công |
| `CUSTOMER_DELETED` (4123) | Xóa khách hàng thành công |
| `CUSTOMER_LIST` (4124) | Lấy danh sách khách hàng thành công |
| `CUSTOMER_NOT_FOUND` (4110) | Không tìm thấy khách hàng |
| `UNAUTHORIZED` (4001) | Không có quyền truy cập hoặc branchId không khớp |
| `VALIDATION_ERROR` (4000) | Dữ liệu đầu vào không hợp lệ |

---

## Ghi chú cho FE

### 1. Tìm kiếm — Phân trang và Bộ lọc

Sử dụng query parameters trên URL (không phải request body):

```
GET /api/customers?shopId=shop123&keyword=Nguyễn&page=0&size=20&sortBy=name&sortDir=asc
```

Response trả về object `Page` chuẩn Spring với: `content`, `totalElements`, `totalPages`, `number`, `size`, `first`, `last`, `empty`.

FE nên:
- Hiển thị input tìm kiếm (keyword) với debounce 300–500ms
- Hiển thị dropdown lọc chi nhánh
- Hiển thị date picker cho `fromDate` / `toDate`
- Hỗ trợ sắp xếp theo cột (click header)

### 2. branchId — Phạm vi khách hàng

| Khi tạo (POST) | Ý nghĩa |
|-----------------|---------|
| Không truyền `branchId` | Khách hàng thuộc **toàn shop** |
| `branchId: "branch456"` | Khách hàng thuộc **chi nhánh cụ thể** |

| Khi tìm kiếm (GET) | Ý nghĩa |
|---------------------|---------|
| Không truyền `branchId` | Trả tất cả khách hàng trong shop |
| `branchId=branch456` | Chỉ trả khách hàng thuộc branch456 |

**FE nên:**
- Trong form tạo: auto-fill `branchId` theo chi nhánh đang chọn của user
- Trong danh sách: hiển thị dropdown "Chi nhánh" để lọc
- Khi update: **không cho phép thay đổi** `branchId` (auto gán lại giá trị cũ)

### 3. Date Format

Query parameters sử dụng ISO format:

```
fromDate=2026-04-01&toDate=2026-04-30
```

Response `createdAt` trả về ISO 8601 `LocalDateTime`:

```
2026-04-01T10:30:00
```

### 4. Tải file Excel

```javascript
const params = new URLSearchParams({
  shopId: 'shop123',
  branchId: 'branch456',
  keyword: 'Nguyễn'
});
const response = await fetch(`/api/customers/export?${params}`, {
  headers: { Authorization: `Bearer ${token}` }
});
const blob = await response.blob();
const link = document.createElement('a');
link.href = URL.createObjectURL(blob);
link.download = 'customers.xlsx';
link.click();
URL.revokeObjectURL(link.href);
```

### 5. Xóa khách hàng

API sử dụng **soft delete**. Sau khi xóa, khách hàng sẽ không xuất hiện trong danh sách nữa. FE chỉ cần gọi DELETE và reload danh sách.

`branchId` là **bắt buộc** khi xóa — dùng để xác minh rằng user đang thao tác đúng chi nhánh. Nếu khách hàng thuộc chi nhánh khác, API trả lỗi.

### 6. Validation phía FE

Nên validate trước khi gửi request:

- `name`: không được trống
- `email`: đúng format email (nếu có nhập)
- `phone`: nên validate format SĐT Việt Nam (nếu cần)

### 7. Audit Log

Mọi thao tác tạo, cập nhật, xóa đều được ghi vào audit log với thông tin:
- User thực hiện (từ JWT token)
- Shop ID
- Customer ID
- Loại thao tác: `CREATED`, `UPDATED`, `DELETED`
- Mô tả chi tiết

### 8. TypeScript Interfaces (tham khảo)

```typescript
interface CustomerRequest {
  name: string;
  phone?: string;
  email?: string;
  address?: string;
  note?: string;
  branchId?: string | null;
}

interface CustomerResponse {
  id: string;
  name: string;
  phone: string | null;
  email: string | null;
  address: string | null;
  note: string | null;
  branchId: string | null;
  createdAt: string; // ISO 8601: "2026-04-01T10:30:00"
}

interface CustomerSearchParams {
  shopId: string;
  branchId?: string;
  keyword?: string;
  page?: number;     // default: 0
  size?: number;     // default: 20
  fromDate?: string; // yyyy-MM-dd
  toDate?: string;   // yyyy-MM-dd
  sortBy?: string;   // default: "createdAt"
  sortDir?: 'asc' | 'desc'; // default: "desc"
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

interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
}
```
