# Order Service API Documentation

Tài liệu mô tả các endpoint của Order Service và format request/response để team Frontend triển khai các tính năng liên quan đến quản lý đơn hàng.

## Overview

Order Service quản lý toàn bộ vòng đời đơn hàng: tạo đơn, cập nhật, hủy, xác nhận thanh toán và lọc theo trạng thái. Hệ thống tự động xử lý tồn kho, khuyến mãi, thuế và trạng thái bàn khi thao tác đơn hàng.

**Base URL:** `/api/orders`

**Authentication:** Bearer Token (JWT) — user info được lấy tự động từ token.

**Permissions:**
- `ORDER_CREATE` — Tạo đơn hàng
- `ORDER_VIEW` — Xem danh sách đơn hàng
- `ORDER_UPDATE` — Cập nhật đơn hàng, cập nhật trạng thái
- `ORDER_CANCEL` — Hủy đơn hàng
- `ORDER_PAYMENT_CONFIRM` — Xác nhận thanh toán

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

## 1. Lấy danh sách đơn hàng

Lấy danh sách đơn hàng của cửa hàng với phân trang.

- **Method:** `GET`
- **URL:** `/api/orders?shopId={shopId}`
- **Permission:** `ORDER_VIEW`

**Query Parameters:**

| Param    | Type   | Required | Description              |
|----------|--------|----------|--------------------------|
| `shopId` | string | Yes      | ID cửa hàng              |
| `page`   | int    | No       | Số trang (default: 0)    |
| `size`   | int    | No       | Số bản ghi/trang (default: 20) |
| `sort`   | string | No       | Sắp xếp (vd: `createdAt,desc`) |

**Response (200):**

```json
{
  "success": true,
  "code": "ORDER_LIST",
  "message": "...",
  "data": {
    "content": [
      {
        "id": "order-001",
        "shopId": "shop-001",
        "branchId": "branch-001",
        "tableId": "table-001",
        "userId": "user-001",
        "note": "Ghi chú đơn hàng",
        "status": "PENDING",
        "paid": false,
        "paymentMethod": null,
        "paymentId": null,
        "paymentTime": null,
        "totalAmount": 3.0,
        "totalPrice": 150000.0,
        "items": [
          {
            "productId": "prod-001",
            "branchProductId": "bp-001",
            "productName": "Cà phê sữa",
            "quantity": 2,
            "price": 35000.0,
            "priceAfterDiscount": 30000.0,
            "appliedPromotionId": "promo-001"
          }
        ],
        "taxSnapshot": {
          "priceIncludesTax": false,
          "netAmount": 150000.0,
          "taxTotal": 15000.0,
          "grandTotal": 165000.0,
          "taxes": [
            {
              "code": "VAT",
              "label": "Thuế GTGT",
              "rate": 0.1,
              "amount": 15000.0
            }
          ]
        }
      }
    ],
    "totalElements": 50,
    "totalPages": 3,
    "size": 20,
    "number": 0,
    "first": true,
    "last": false
  },
  "timestamp": "2024-01-01T12:00:00.000Z"
}
```

---

## 2. Tạo đơn hàng

Tạo đơn hàng mới với danh sách sản phẩm. Hệ thống tự động áp dụng khuyến mãi, tính thuế, trừ tồn kho và đặt bàn sang trạng thái OCCUPIED (nếu có bàn).

- **Method:** `POST`
- **URL:** `/api/orders?shopId={shopId}&branchId={branchId}`
- **Permission:** `ORDER_CREATE`

**Query Parameters:**

| Param      | Type   | Required | Description    |
|------------|--------|----------|----------------|
| `shopId`   | string | Yes      | ID cửa hàng    |
| `branchId` | string | Yes      | ID chi nhánh   |

**Request Body:**

```json
{
  "shopId": "shop-001",
  "branchId": "branch-001",
  "tableId": "table-001",
  "note": "Ghi chú đơn hàng",
  "items": [
    {
      "productId": "prod-001",
      "quantity": 2
    },
    {
      "productId": "prod-002",
      "quantity": 1
    }
  ]
}
```

| Field      | Type              | Required | Validation                       | Description                            |
|------------|-------------------|----------|----------------------------------|----------------------------------------|
| `shopId`   | string            | Yes      | `@NotBlank`                      | ID cửa hàng                           |
| `branchId` | string            | Yes      | `@NotBlank`                      | ID chi nhánh                           |
| `tableId`  | string            | No       |                                  | ID bàn (null nếu không phải đơn tại bàn)|
| `note`     | string            | No       |                                  | Ghi chú                               |
| `items`    | OrderItemRequest[]| Yes      | `@NotEmpty`, `@Valid`            | Danh sách sản phẩm (ít nhất 1)        |

**OrderItemRequest:**

| Field       | Type   | Required | Validation      | Description                  |
|-------------|--------|----------|-----------------|------------------------------|
| `productId` | string | Yes      | `@NotBlank`     | ID sản phẩm (master product)|
| `quantity`  | int    | Yes      | `>= 1`          | Số lượng                    |

**Response (200):** `OrderResponse` (xem cấu trúc ở mục 1)

**Hành vi tự động:**
- Áp dụng khuyến mãi (promotion) nếu có
- Tính thuế theo cấu hình shop
- Trừ tồn kho (nếu shop có quản lý tồn kho)
- Chuyển bàn sang `OCCUPIED` (nếu có `tableId`)

**Error Codes:**
| HTTP | Code               | Condition                                    |
|------|--------------------|----------------------------------------------|
| 400  | `VALIDATION_ERROR` | Dữ liệu không hợp lệ hoặc branchId rỗng    |
| 403  | —                  | Không có quyền `ORDER_CREATE`                |
| 404  | `SHOP_NOT_FOUND`   | Cửa hàng không tồn tại                      |
| 404  | `PRODUCT_NOT_FOUND`| Sản phẩm hoặc sản phẩm chi nhánh không tồn tại |

---

## 3. Cập nhật đơn hàng

Cập nhật thông tin đơn hàng: đổi bàn, ghi chú, hoặc thay đổi danh sách sản phẩm. Chỉ áp dụng cho đơn chưa thanh toán.

- **Method:** `PUT`
- **URL:** `/api/orders/{id}?shopId={shopId}`
- **Permission:** `ORDER_UPDATE`

**Path Parameters:**

| Param | Type   | Required | Description     |
|-------|--------|----------|-----------------|
| `id`  | string | Yes      | ID đơn hàng     |

**Query Parameters:**

| Param    | Type   | Required | Description    |
|----------|--------|----------|----------------|
| `shopId` | string | Yes      | ID cửa hàng    |

**Request Body:**

```json
{
  "tableId": "table-002",
  "note": "Ghi chú cập nhật",
  "items": [
    {
      "productId": "prod-001",
      "quantity": 3,
      "price": 35000.0
    }
  ]
}
```

| Field     | Type                    | Required | Description                                     |
|-----------|-------------------------|----------|-------------------------------------------------|
| `tableId` | string                  | No       | ID bàn mới (nếu muốn đổi bàn)                  |
| `note`    | string                  | No       | Ghi chú cập nhật                                |
| `items`   | OrderItemUpdateRequest[]| No       | Danh sách sản phẩm mới (thay thế toàn bộ items)|

**OrderItemUpdateRequest:**

| Field       | Type   | Required | Validation | Description                                     |
|-------------|--------|----------|------------|-------------------------------------------------|
| `productId` | string | No       |            | ID sản phẩm (master product)                    |
| `quantity`  | int    | Yes      | `>= 1`     | Số lượng                                        |
| `price`     | double | Yes      | `>= 0`     | Giá tại thời điểm cập nhật                      |

**Response (200):** `OrderResponse`

**Hành vi tự động:**
- Nếu đổi bàn: giải phóng bàn cũ, chiếm bàn mới
- Nếu thay đổi items: hoàn kho cũ, trừ kho mới (nếu shop có quản lý tồn kho)
- Tính lại khuyến mãi và thuế

**Error Codes:**
| HTTP | Code                | Condition                          |
|------|---------------------|------------------------------------|
| 400  | `ORDER_ALREADY_PAID`| Đơn hàng đã thanh toán            |
| 403  | —                   | Không có quyền `ORDER_UPDATE`      |
| 404  | `SHOP_NOT_FOUND`    | Cửa hàng không tồn tại            |
| 404  | `PRODUCT_NOT_FOUND` | Sản phẩm không tồn tại            |

---

## 4. Hủy đơn hàng

Hủy đơn hàng nếu chưa thanh toán. Hệ thống tự động hoàn kho.

- **Method:** `PUT`
- **URL:** `/api/orders/{id}/cancel?shopId={shopId}`
- **Permission:** `ORDER_CANCEL`

**Path Parameters:**

| Param | Type   | Required | Description     |
|-------|--------|----------|-----------------|
| `id`  | string | Yes      | ID đơn hàng     |

**Query Parameters:**

| Param    | Type   | Required | Description    |
|----------|--------|----------|----------------|
| `shopId` | string | Yes      | ID cửa hàng    |

**Response (200):**

```json
{
  "success": true,
  "code": "ORDER_CANCELLED",
  "message": "...",
  "data": null,
  "timestamp": "2024-01-01T12:00:00.000Z"
}
```

**Hành vi tự động:**
- Hoàn kho cho tất cả sản phẩm trong đơn (nếu shop có quản lý tồn kho)

**Error Codes:**
| HTTP | Code                | Condition                    |
|------|---------------------|------------------------------|
| 400  | `ORDER_ALREADY_PAID`| Đơn hàng đã thanh toán      |
| 403  | —                   | Không có quyền `ORDER_CANCEL`|
| 404  | —                   | Đơn hàng không tồn tại      |

---

## 5. Xác nhận thanh toán

Xác nhận thanh toán cho đơn hàng. Đơn hàng sẽ chuyển sang trạng thái `COMPLETED` và bàn được giải phóng.

- **Method:** `POST`
- **URL:** `/api/orders/{orderId}/confirm-payment?shopId={shopId}&paymentId={paymentId}&paymentMethod={paymentMethod}`
- **Permission:** `ORDER_PAYMENT_CONFIRM`

**Path Parameters:**

| Param     | Type   | Required | Description     |
|-----------|--------|----------|-----------------|
| `orderId` | string | Yes      | ID đơn hàng     |

**Query Parameters:**

| Param           | Type   | Required | Description                            |
|-----------------|--------|----------|----------------------------------------|
| `shopId`        | string | Yes      | ID cửa hàng                           |
| `paymentId`     | string | Yes      | ID giao dịch thanh toán               |
| `paymentMethod` | string | Yes      | Phương thức thanh toán (vd: `Cash`, `Card`, `Transfer`) |

**Response (200):** `OrderResponse` với `paid = true`, `status = "COMPLETED"`

**Hành vi tự động:**
- Chuyển trạng thái sang `COMPLETED`
- Đánh dấu `paid = true`
- Ghi nhận `paymentTime`
- Giải phóng bàn (chuyển về `AVAILABLE`)

**Error Codes:**
| HTTP | Code                | Condition                              |
|------|---------------------|----------------------------------------|
| 400  | `ORDER_ALREADY_PAID`| Đơn hàng đã thanh toán                |
| 403  | —                   | Không có quyền `ORDER_PAYMENT_CONFIRM` |
| 404  | —                   | Đơn hàng không tồn tại                |

---

## 6. Cập nhật trạng thái đơn hàng

Cập nhật trạng thái đơn hàng thủ công.

- **Method:** `PUT`
- **URL:** `/api/orders/{id}/status?shopId={shopId}&status={status}`
- **Permission:** `ORDER_UPDATE`

**Path Parameters:**

| Param | Type   | Required | Description     |
|-------|--------|----------|-----------------|
| `id`  | string | Yes      | ID đơn hàng     |

**Query Parameters:**

| Param    | Type        | Required | Description         |
|----------|-------------|----------|---------------------|
| `shopId` | string      | Yes      | ID cửa hàng         |
| `status` | OrderStatus | Yes      | Trạng thái mới      |

**Response (200):** `OrderResponse`

**Hành vi đặc biệt:**
- Nếu chuyển sang `COMPLETED` mà chưa thanh toán: tự động đánh dấu `paid = true`, `paymentMethod = "Cash"`, giải phóng bàn
- Không thể cập nhật đơn đã `CANCELLED` hoặc `COMPLETED`
- Không thể hủy đơn đã thanh toán

**Error Codes:**
| HTTP | Code                | Condition                                        |
|------|---------------------|--------------------------------------------------|
| 400  | `VALIDATION_ERROR`  | Đơn hàng đã `CANCELLED` hoặc `COMPLETED`        |
| 400  | `ORDER_ALREADY_PAID`| Cố hủy đơn đã thanh toán                        |
| 403  | —                   | Không có quyền `ORDER_UPDATE`                    |
| 404  | —                   | Đơn hàng không tồn tại                          |

---

## 7. Lọc đơn hàng theo trạng thái

Lấy danh sách đơn hàng theo trạng thái và chi nhánh với phân trang.

- **Method:** `GET`
- **URL:** `/api/orders/filter?shopId={shopId}&status={status}`
- **Permission:** `ORDER_VIEW`

**Query Parameters:**

| Param      | Type        | Required | Description                    |
|------------|-------------|----------|--------------------------------|
| `shopId`   | string      | Yes      | ID cửa hàng                    |
| `status`   | OrderStatus | Yes      | Trạng thái lọc                 |
| `branchId` | string      | No       | ID chi nhánh (tùy chọn)        |
| `page`     | int         | No       | Số trang (default: 0)          |
| `size`     | int         | No       | Số bản ghi/trang (default: 20) |
| `sort`     | string      | No       | Sắp xếp                       |

**Response (200):** `Page<OrderResponse>` (giống mục 1)

---

## Enums

### OrderStatus

| Value       | Description       |
|-------------|-------------------|
| `PENDING`   | Mới tạo           |
| `CONFIRMED` | Đã xác nhận       |
| `SHIPPING`  | Đang vận chuyển   |
| `COMPLETED` | Hoàn tất          |
| `CANCELLED` | Đã hủy            |

---

## Data Models

### OrderResponse

| Field            | Type              | Description                                      |
|------------------|-------------------|--------------------------------------------------|
| `id`             | string            | ID đơn hàng                                      |
| `shopId`         | string            | ID cửa hàng                                      |
| `branchId`       | string            | ID chi nhánh                                     |
| `tableId`        | string \| null    | ID bàn (null nếu không phải đơn tại bàn)         |
| `userId`         | string            | ID người tạo đơn                                 |
| `note`           | string \| null    | Ghi chú                                          |
| `status`         | OrderStatus       | Trạng thái đơn hàng                              |
| `paid`           | boolean           | Đã thanh toán                                    |
| `paymentMethod`  | string \| null    | Phương thức thanh toán                            |
| `paymentId`      | string \| null    | ID giao dịch thanh toán                           |
| `paymentTime`    | datetime \| null  | Thời gian thanh toán                              |
| `totalAmount`    | double            | Tổng số lượng sản phẩm                            |
| `totalPrice`     | double            | Tổng giá trị đơn hàng sau chiết khấu              |
| `items`          | OrderItemResponse[]| Danh sách sản phẩm                               |
| `taxSnapshot`    | OrderTaxSnapshot  | Thông tin thuế tại thời điểm tạo đơn              |

### OrderItemResponse

| Field                | Type           | Description                              |
|----------------------|----------------|------------------------------------------|
| `productId`          | string         | ID sản phẩm (master)                     |
| `branchProductId`    | string         | ID sản phẩm tại chi nhánh               |
| `productName`        | string         | Tên sản phẩm                             |
| `quantity`           | int            | Số lượng                                 |
| `price`              | double         | Giá gốc tại thời điểm đặt hàng          |
| `priceAfterDiscount` | double         | Giá sau khuyến mãi                       |
| `appliedPromotionId` | string \| null | ID khuyến mãi đã áp dụng                |

### OrderTaxSnapshot

| Field              | Type           | Description                 |
|--------------------|----------------|-----------------------------|
| `priceIncludesTax` | boolean        | Giá đã bao gồm thuế chưa   |
| `netAmount`        | double         | Tổng tiền chưa thuế (NET)   |
| `taxTotal`         | double         | Tổng tiền thuế              |
| `grandTotal`       | double         | Tổng tiền phải thanh toán   |
| `taxes`            | OrderTaxLine[] | Chi tiết từng loại thuế     |

### OrderTaxLine

| Field    | Type   | Description          |
|----------|--------|----------------------|
| `code`   | string | Mã thuế (vd: `VAT`)  |
| `label`  | string | Tên thuế              |
| `rate`   | double | Tỷ lệ thuế (vd: 0.1) |
| `amount` | double | Số tiền thuế          |

---

## Lưu ý

- Tất cả endpoint yêu cầu JWT token trong header: `Authorization: Bearer <token>`.
- `userId` được lấy tự động từ token, **không cần truyền trong request**.
- `shopId` và `branchId` truyền qua **query parameters**, không nằm trong path.
- Khi tạo hoặc cập nhật đơn, hệ thống tự động xử lý: khuyến mãi, thuế, tồn kho, trạng thái bàn.
- Khi hủy đơn, tồn kho được hoàn lại tự động.
- Giá trong `OrderItemResponse` là snapshot tại thời điểm đặt hàng, không thay đổi theo giá sản phẩm hiện tại.
