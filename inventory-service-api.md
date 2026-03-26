# Inventory Service API Documentation

Tài liệu mô tả các endpoint của Inventory Service và format request/response để team Frontend triển khai các tính năng liên quan đến quản lý tồn kho.

## Overview

Inventory Service quản lý tồn kho sản phẩm theo từng chi nhánh (branch), bao gồm: nhập kho, xuất kho, điều chỉnh tồn kho và xem lịch sử giao dịch. Tất cả endpoint yêu cầu xác thực JWT và quyền tương ứng.

**Base URL:** `/api/shops/{shopId}/inventory`

**Authentication:** Bearer Token (JWT) — user info được lấy tự động từ token.

**Permissions:**
- `INVENTORY_MANAGE` — Nhập, xuất, điều chỉnh tồn kho
- `INVENTORY_VIEW` — Xem lịch sử giao dịch

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

## 1. Nhập kho (Import)

Tăng số lượng tồn kho cho một sản phẩm tại chi nhánh.

- **Method:** `POST`
- **URL:** `/api/shops/{shopId}/inventory/import`
- **Permission:** `INVENTORY_MANAGE`

**Path Parameters:**

| Param    | Type   | Required | Description  |
|----------|--------|----------|--------------|
| `shopId` | string | Yes      | ID cửa hàng  |

**Request Body:**

```json
{
  "branchId": "string",
  "branchProductId": "string",
  "type": "IMPORT",
  "quantity": 10,
  "note": "Nhập hàng từ nhà cung cấp",
  "referenceId": null
}
```

| Field             | Type          | Required | Validation                  | Description                        |
|-------------------|---------------|----------|-----------------------------|------------------------------------|
| `branchId`        | string        | Yes      | `@NotBlank`                 | ID chi nhánh                       |
| `branchProductId` | string        | Yes      | `@NotBlank`                 | ID sản phẩm tại chi nhánh         |
| `type`            | InventoryType | Yes      | `@NotNull`, phải là `IMPORT`| Loại giao dịch                     |
| `quantity`        | int           | Yes      | `>= 0` (service yêu cầu `> 0`) | Số lượng nhập                  |
| `note`            | string        | No       |                             | Ghi chú                           |
| `referenceId`     | string        | No       |                             | Không dùng cho import              |

**Response (200):**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Thành công",
  "data": 20,
  "timestamp": "2024-01-01T12:00:00.000Z"
}
```

> `data` là số lượng tồn kho mới sau khi nhập.

**Error Codes:**
| HTTP | Code               | Condition                              |
|------|--------------------|----------------------------------------|
| 400  | `VALIDATION_ERROR` | `quantity <= 0` hoặc `type != IMPORT`  |
| 403  | —                  | Không có quyền `INVENTORY_MANAGE`      |
| 404  | `PRODUCT_NOT_FOUND`| Sản phẩm chi nhánh không tồn tại      |
| 404  | `SHOP_NOT_FOUND`   | Cửa hàng không tồn tại                |

---

## 2. Xuất kho (Export)

Giảm số lượng tồn kho cho một sản phẩm tại chi nhánh.

- **Method:** `POST`
- **URL:** `/api/shops/{shopId}/inventory/export`
- **Permission:** `INVENTORY_MANAGE`

**Path Parameters:**

| Param    | Type   | Required | Description  |
|----------|--------|----------|--------------|
| `shopId` | string | Yes      | ID cửa hàng  |

**Request Body:**

```json
{
  "branchId": "string",
  "branchProductId": "string",
  "type": "EXPORT",
  "quantity": 5,
  "note": "Xuất hàng hỏng",
  "referenceId": "order-id-123"
}
```

| Field             | Type          | Required | Validation                   | Description                              |
|-------------------|---------------|----------|------------------------------|------------------------------------------|
| `branchId`        | string        | Yes      | `@NotBlank`                  | ID chi nhánh                             |
| `branchProductId` | string        | Yes      | `@NotBlank`                  | ID sản phẩm tại chi nhánh               |
| `type`            | InventoryType | Yes      | `@NotNull`, phải là `EXPORT` | Loại giao dịch                           |
| `quantity`        | int           | Yes      | `>= 0` (service yêu cầu `> 0`) | Số lượng xuất                        |
| `note`            | string        | No       |                              | Ghi chú                                 |
| `referenceId`     | string        | No       |                              | ID tham chiếu (vd: Order ID)            |

**Response (200):**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Thành công",
  "data": 15,
  "timestamp": "2024-01-01T12:00:00.000Z"
}
```

> `data` là số lượng tồn kho mới sau khi xuất.

**Error Codes:**
| HTTP | Code                | Condition                              |
|------|---------------------|----------------------------------------|
| 400  | `VALIDATION_ERROR`  | `quantity <= 0` hoặc `type != EXPORT`  |
| 400  | `INSUFFICIENT_STOCK`| Tồn kho không đủ để xuất              |
| 403  | —                   | Không có quyền `INVENTORY_MANAGE`      |
| 404  | `PRODUCT_NOT_FOUND` | Sản phẩm chi nhánh không tồn tại      |
| 404  | `SHOP_NOT_FOUND`    | Cửa hàng không tồn tại                |

---

## 3. Điều chỉnh tồn kho (Adjust)

Đặt số lượng tồn kho của sản phẩm về một giá trị cụ thể.

- **Method:** `POST`
- **URL:** `/api/shops/{shopId}/inventory/adjust`
- **Permission:** `INVENTORY_MANAGE`

**Path Parameters:**

| Param    | Type   | Required | Description  |
|----------|--------|----------|--------------|
| `shopId` | string | Yes      | ID cửa hàng  |

**Request Body:**

```json
{
  "branchId": "string",
  "branchProductId": "string",
  "type": "ADJUSTMENT",
  "quantity": 30,
  "note": "Kiểm kê tồn kho",
  "referenceId": null
}
```

| Field             | Type          | Required | Validation                        | Description                           |
|-------------------|---------------|----------|-----------------------------------|---------------------------------------|
| `branchId`        | string        | Yes      | `@NotBlank`                       | ID chi nhánh                          |
| `branchProductId` | string        | Yes      | `@NotBlank`                       | ID sản phẩm tại chi nhánh            |
| `type`            | InventoryType | Yes      | `@NotNull`, phải là `ADJUSTMENT`  | Loại giao dịch                        |
| `quantity`        | int           | Yes      | `>= 0`                           | Số lượng tồn kho mới (newQuantity)    |
| `note`            | string        | No       |                                   | Ghi chú                              |
| `referenceId`     | string        | No       |                                   | Không dùng cho adjust                 |

**Response (200):**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Thành công",
  "data": 30,
  "timestamp": "2024-01-01T12:00:00.000Z"
}
```

> `data` là số lượng tồn kho mới sau khi điều chỉnh.

**Error Codes:**
| HTTP | Code               | Condition                                  |
|------|--------------------|--------------------------------------------|
| 400  | `VALIDATION_ERROR` | `quantity < 0` hoặc `type != ADJUSTMENT`   |
| 403  | —                  | Không có quyền `INVENTORY_MANAGE`          |
| 404  | `PRODUCT_NOT_FOUND`| Sản phẩm chi nhánh không tồn tại          |
| 404  | `SHOP_NOT_FOUND`   | Cửa hàng không tồn tại                    |

---

## 4. Lịch sử giao dịch tồn kho (History)

Lấy lịch sử giao dịch tồn kho của một sản phẩm tại chi nhánh, có phân trang.

- **Method:** `GET`
- **URL:** `/api/shops/{shopId}/inventory/branches/{branchId}/products/{branchProductId}/history`
- **Permission:** `INVENTORY_VIEW`

**Path Parameters:**

| Param             | Type   | Required | Description              |
|-------------------|--------|----------|--------------------------|
| `shopId`          | string | Yes      | ID cửa hàng              |
| `branchId`        | string | Yes      | ID chi nhánh              |
| `branchProductId` | string | Yes      | ID sản phẩm tại chi nhánh|

**Query Parameters (Pageable):**

| Param  | Type   | Default | Description          |
|--------|--------|---------|----------------------|
| `page` | int    | 0       | Số trang (0-indexed) |
| `size` | int    | 20      | Số bản ghi mỗi trang|
| `sort` | string | —       | Sắp xếp (vd: `createdAt,desc`) |

**Response (200):**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Thành công",
  "data": {
    "content": [
      {
        "id": "txn-001",
        "shopId": "shop-001",
        "branchId": "branch-001",
        "branchProductId": "bp-001",
        "productName": "Cà phê sữa",
        "sku": "CF-001",
        "type": "IMPORT",
        "quantity": 10,
        "currentStock": 50,
        "note": "Nhập hàng từ nhà cung cấp",
        "referenceId": null,
        "createdAt": "2024-01-01T12:00:00",
        "createdBy": "user-001"
      }
    ],
    "totalElements": 100,
    "totalPages": 5,
    "size": 20,
    "number": 0,
    "first": true,
    "last": false
  },
  "timestamp": "2024-01-01T12:00:00.000Z"
}
```

**Error Codes:**
| HTTP | Code               | Condition                            |
|------|--------------------|--------------------------------------|
| 403  | —                  | Không có quyền `INVENTORY_VIEW`      |
| 404  | `PRODUCT_NOT_FOUND`| Sản phẩm chi nhánh không tồn tại    |

---

## Enums

### InventoryType

| Value        | Description           |
|--------------|-----------------------|
| `IMPORT`     | Nhập kho              |
| `EXPORT`     | Xuất kho              |
| `ADJUSTMENT` | Điều chỉnh tồn kho   |

---

## Lưu ý

- Tất cả endpoint yêu cầu JWT token trong header: `Authorization: Bearer <token>`.
- `shopId` luôn là **path variable**, không nằm trong request body.
- `userId` được lấy tự động từ token, **không cần truyền trong request**.
- Nếu shop không yêu cầu quản lý tồn kho (tuỳ loại hình kinh doanh), các thao tác import/export/adjust sẽ bị bỏ qua và trả về `-1`.
- Tất cả giao dịch được ghi log audit.
- Thông tin `productName` và `sku` trong lịch sử là snapshot tại thời điểm giao dịch.
