# Table Service API Documentation

Tài liệu mô tả các endpoint của Table Service và format request/response để team Frontend triển khai các tính năng liên quan đến quản lý bàn.

## Overview

Table Service quản lý bàn trong cửa hàng/chi nhánh: tạo bàn, xem danh sách, cập nhật thông tin, cập nhật trạng thái và xóa bàn (soft delete). Trạng thái bàn cũng được tự động quản lý bởi Order Service khi tạo/hoàn thành đơn hàng.

**Base URL:** `/api/tables`

**Authentication:** Bearer Token (JWT) — user info được lấy tự động từ token.

**Permissions:**
- `TABLE_CREATE` — Xem danh sách bàn (theo code hiện tại)
- `TABLE_UPDATE` — Cập nhật trạng thái, cập nhật thông tin bàn
- `TABLE_DELETE` — Xóa bàn

> Lưu ý: Endpoint tạo bàn kiểm tra quyền qua `ShopRole.OWNER` trong service layer thay vì dùng `@RequirePermission`.

---

## Response Wrapper

Tất cả response được bọc trong `ApiResponseDto`:

```json
{
  "success": true,
  "code": "string",
  "message": "string",
  "data": "...",
  "timestamp": "2024-01-01T12:00:00.000Z"
}
```

---

## 1. Tạo bàn mới

Tạo một bàn mới trong cửa hàng. Yêu cầu role `OWNER`.

- **Method:** `POST`
- **URL:** `/api/tables`
- **Permission:** `OWNER` role (kiểm tra trong service layer)

**Request Body:**

```json
{
  "name": "Bàn 01",
  "shopId": "shop-001",
  "branchId": "branch-001",
  "status": "AVAILABLE",
  "capacity": 4,
  "note": "Bàn VIP gần cửa sổ"
}
```

| Field      | Type        | Required | Validation  | Default       | Description                      |
|------------|-------------|----------|-------------|---------------|----------------------------------|
| `name`     | string      | Yes      | `@NotBlank` | —             | Tên bàn (phải duy nhất trong shop+branch) |
| `shopId`   | string      | No       |             | —             | ID cửa hàng                      |
| `branchId` | string      | No       |             | —             | ID chi nhánh                     |
| `status`   | TableStatus | No       |             | `AVAILABLE`   | Trạng thái ban đầu               |
| `capacity` | int \| null | No       | `> 0`       | null          | Sức chứa (số người)             |
| `note`     | string      | No       |             | —             | Ghi chú                          |

**Response (200):**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Thành công",
  "data": {
    "id": "table-001",
    "name": "Bàn 01",
    "status": "AVAILABLE",
    "shopId": "shop-001",
    "shopName": "Quán Cà Phê ABC",
    "branchId": "branch-001",
    "capacity": 4,
    "note": "Bàn VIP gần cửa sổ",
    "currentOrderId": null
  },
  "timestamp": "2024-01-01T12:00:00.000Z"
}
```

**Error Codes:**
| HTTP | Code               | Condition                          |
|------|--------------------|------------------------------------|
| 400  | `TABLE_NAME_EXISTS`| Tên bàn đã tồn tại trong shop+branch |
| 400  | `INVALID_CAPACITY` | `capacity <= 0`                    |
| 403  | —                  | Không phải `OWNER`                 |
| 404  | `SHOP_NOT_FOUND`   | Cửa hàng không tồn tại            |

---

## 2. Lấy danh sách bàn

Lấy danh sách bàn của cửa hàng theo chi nhánh với phân trang.

- **Method:** `GET`
- **URL:** `/api/tables?shopId={shopId}&branchId={branchId}`
- **Permission:** `TABLE_CREATE` (theo annotation hiện tại)

**Query Parameters:**

| Param      | Type   | Required | Description              |
|------------|--------|----------|--------------------------|
| `shopId`   | string | Yes      | ID cửa hàng              |
| `branchId` | string | Yes      | ID chi nhánh              |
| `page`     | int    | No       | Số trang (default: 0)    |
| `size`     | int    | No       | Số bản ghi/trang (default: 20) |
| `sort`     | string | No       | Sắp xếp (vd: `name,asc`) |

**Response (200):**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Thành công",
  "data": {
    "content": [
      {
        "id": "table-001",
        "name": "Bàn 01",
        "status": "AVAILABLE",
        "shopId": "shop-001",
        "shopName": "Quán Cà Phê ABC",
        "branchId": "branch-001",
        "capacity": 4,
        "note": "Bàn VIP gần cửa sổ",
        "currentOrderId": null
      },
      {
        "id": "table-002",
        "name": "Bàn 02",
        "status": "OCCUPIED",
        "shopId": "shop-001",
        "shopName": "Quán Cà Phê ABC",
        "branchId": "branch-001",
        "capacity": 6,
        "note": null,
        "currentOrderId": "order-005"
      }
    ],
    "totalElements": 10,
    "totalPages": 1,
    "size": 20,
    "number": 0,
    "first": true,
    "last": true
  },
  "timestamp": "2024-01-01T12:00:00.000Z"
}
```

**Error Codes:**
| HTTP | Code             | Condition                                  |
|------|------------------|--------------------------------------------|
| 403  | —                | Không có quyền (yêu cầu `OWNER` hoặc `STAFF`) |
| 404  | `SHOP_NOT_FOUND` | Cửa hàng không tồn tại                    |

---

## 3. Cập nhật trạng thái bàn

Cập nhật trạng thái bàn (AVAILABLE, OCCUPIED, CLOSED).

- **Method:** `PUT`
- **URL:** `/api/tables/{id}/status?status={status}`
- **Permission:** `TABLE_UPDATE`

**Path Parameters:**

| Param | Type   | Required | Description |
|-------|--------|----------|-------------|
| `id`  | string | Yes      | ID bàn      |

**Query Parameters:**

| Param    | Type        | Required | Description    |
|----------|-------------|----------|----------------|
| `status` | TableStatus | Yes      | Trạng thái mới |

**Response (200):** `TableResponse` (xem cấu trúc ở mục 1)

**Error Codes:**
| HTTP | Code              | Condition                            |
|------|-------------------|--------------------------------------|
| 403  | —                 | Không có quyền `TABLE_UPDATE`        |
| 404  | `TABLE_NOT_FOUND` | Bàn không tồn tại                   |

---

## 4. Cập nhật thông tin bàn

Cập nhật thông tin bàn: tên, sức chứa, ghi chú, trạng thái. Không thể cập nhật bàn đang có khách (`OCCUPIED`).

- **Method:** `PUT`
- **URL:** `/api/tables/{id}`
- **Permission:** `TABLE_UPDATE`

**Path Parameters:**

| Param | Type   | Required | Description |
|-------|--------|----------|-------------|
| `id`  | string | Yes      | ID bàn      |

**Request Body:**

```json
{
  "name": "Bàn 01 - VIP",
  "shopId": "shop-001",
  "branchId": "branch-001",
  "status": "AVAILABLE",
  "capacity": 6,
  "note": "Bàn VIP đã nâng cấp"
}
```

| Field      | Type        | Required | Validation  | Description                             |
|------------|-------------|----------|-------------|-----------------------------------------|
| `name`     | string      | Yes      | `@NotBlank` | Tên bàn mới (duy nhất trong shop+branch)|
| `shopId`   | string      | No       |             | ID cửa hàng                            |
| `branchId` | string      | No       |             | ID chi nhánh                            |
| `status`   | TableStatus | No       |             | Trạng thái (default: `AVAILABLE`)       |
| `capacity` | int \| null | No       | `> 0`       | Sức chứa                               |
| `note`     | string      | No       |             | Ghi chú                                |

**Response (200):** `TableResponse`

**Error Codes:**
| HTTP | Code               | Condition                                  |
|------|--------------------|--------------------------------------------|
| 400  | `TABLE_OCCUPIED`   | Bàn đang có khách, không thể cập nhật      |
| 400  | `TABLE_NAME_EXISTS`| Tên bàn đã tồn tại trong shop+branch       |
| 400  | `INVALID_CAPACITY` | `capacity <= 0`                            |
| 403  | —                  | Không có quyền `TABLE_UPDATE` hoặc không phải `OWNER` |
| 404  | `TABLE_NOT_FOUND`  | Bàn không tồn tại                          |
| 404  | `SHOP_NOT_FOUND`   | Cửa hàng không tồn tại                    |

---

## 5. Xóa bàn

Xóa mềm (soft delete) một bàn. Không thể xóa bàn đang có khách (`OCCUPIED`).

- **Method:** `DELETE`
- **URL:** `/api/tables/{id}`
- **Permission:** `TABLE_DELETE`

**Path Parameters:**

| Param | Type   | Required | Description |
|-------|--------|----------|-------------|
| `id`  | string | Yes      | ID bàn      |

**Response (200):**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Thành công",
  "data": null,
  "timestamp": "2024-01-01T12:00:00.000Z"
}
```

**Error Codes:**
| HTTP | Code             | Condition                          |
|------|------------------|------------------------------------|
| 400  | `TABLE_OCCUPIED` | Bàn đang có khách, không thể xóa  |
| 403  | —                | Không có quyền `TABLE_DELETE` hoặc không phải `OWNER` |
| 404  | `TABLE_NOT_FOUND`| Bàn không tồn tại                 |

---

## Enums

### TableStatus

| Value       | Description        |
|-------------|--------------------|
| `AVAILABLE` | Trống, sẵn sàng    |
| `OCCUPIED`  | Đang có khách       |
| `CLOSED`    | Không hoạt động     |

---

## Data Models

### TableResponse

| Field            | Type           | Description                                  |
|------------------|----------------|----------------------------------------------|
| `id`             | string         | ID bàn                                       |
| `name`           | string         | Tên bàn                                      |
| `status`         | TableStatus    | Trạng thái hiện tại                          |
| `shopId`         | string         | ID cửa hàng                                  |
| `shopName`       | string \| null | Tên cửa hàng                                 |
| `branchId`       | string \| null | ID chi nhánh                                 |
| `capacity`       | int \| null    | Sức chứa (số người)                          |
| `note`           | string \| null | Ghi chú                                      |
| `currentOrderId` | string \| null | ID đơn hàng đang phục vụ (null nếu bàn trống)|

---

## Lưu ý

- Tất cả endpoint yêu cầu JWT token trong header: `Authorization: Bearer <token>`.
- `userId` được lấy tự động từ token, **không cần truyền trong request**.
- Xóa bàn là **soft delete** (đánh dấu `deleted = true`), dữ liệu vẫn tồn tại trong database.
- Trạng thái bàn được tự động quản lý bởi **Order Service**:
  - Khi tạo đơn hàng có `tableId` → bàn chuyển sang `OCCUPIED` và gán `currentOrderId`.
  - Khi thanh toán hoặc hoàn thành đơn → bàn chuyển về `AVAILABLE` và xóa `currentOrderId`.
- Tên bàn phải **duy nhất** trong cùng một shop + branch.
- Không thể cập nhật hoặc xóa bàn đang ở trạng thái `OCCUPIED`.
