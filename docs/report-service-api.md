# Report Service API Documentation

> **Base URL:** `/api/reports`
>
> **Yêu cầu gói dịch vụ:** `PRO` hoặc `ENTERPRISE`
>
> **Authentication:** Bearer Token (JWT) — tất cả endpoint đều yêu cầu đăng nhập.

---

## Mục lục

1. [Tổng quan](#tổng-quan)
2. [Data Models](#data-models)
3. [Endpoints](#endpoints)
   - [Báo cáo tổng hợp](#1-báo-cáo-tổng-hợp)
   - [Báo cáo theo ngày](#2-báo-cáo-theo-ngày)
   - [Sản phẩm bán chạy](#3-sản-phẩm-bán-chạy)
   - [Xuất báo cáo theo ngày (Excel)](#4-xuất-báo-cáo-theo-ngày-ra-excel)
   - [Xuất sản phẩm bán chạy (Excel)](#5-xuất-sản-phẩm-bán-chạy-ra-excel)
4. [Phân quyền (Permissions)](#phân-quyền-permissions)
5. [Error Codes](#error-codes)
6. [Ghi chú cho FE](#ghi-chú-cho-fe)

---

## Tổng quan

Report Service cung cấp các báo cáo doanh thu và phân tích bán hàng cho cửa hàng. Sử dụng MongoDB Aggregation Pipeline để tính toán trực tiếp trên dữ liệu đơn hàng.

### Tính năng chính

| Tính năng | Mô tả |
|-----------|-------|
| **Báo cáo tổng hợp** | Tổng đơn hàng, sản phẩm đã bán, doanh thu, tổng tiền (gồm thuế), giá trị trung bình đơn hàng |
| **Báo cáo theo ngày** | Chi tiết từng ngày: số đơn, sản phẩm, doanh thu, tổng tiền |
| **Sản phẩm bán chạy** | Top N sản phẩm bán chạy nhất theo số lượng và doanh thu |
| **Xuất Excel** | Hỗ trợ xuất báo cáo theo ngày và sản phẩm bán chạy ra file `.xlsx` |

### Phạm vi lọc

Tất cả báo cáo hỗ trợ lọc theo:

| Filter | Mô tả |
|--------|-------|
| `shopId` | **Bắt buộc** — ID cửa hàng |
| `branchId` | Tuỳ chọn — Lọc theo chi nhánh cụ thể. `null` = toàn bộ shop |
| `startDate` / `endDate` | Tuỳ chọn — Khoảng thời gian (phải truyền cả cặp) |
| `status` | Tuỳ chọn — Lọc theo trạng thái đơn hàng (`PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`) |

---

## Data Models

### ReportRequest (Request Body)

```json
{
  "startDate": "2026-04-01",
  "endDate": "2026-04-30",
  "status": "COMPLETED",
  "branchId": "branch456"
}
```

| Field | Type | Required | Validation | Mô tả |
|-------|------|----------|------------|-------|
| `startDate` | `LocalDate` | Không | `@ValidDateRange` (phải truyền cùng endDate) | Ngày bắt đầu (yyyy-MM-dd) |
| `endDate` | `LocalDate` | Không | `@ValidDateRange` (phải truyền cùng startDate) | Ngày kết thúc (yyyy-MM-dd) |
| `status` | `string` | Không | Enum `OrderStatus` | Lọc theo trạng thái đơn hàng |
| `branchId` | `string` | Không | — | ID chi nhánh. `null` = toàn bộ shop |

### ReportResponse

```json
{
  "totalOrders": 150,
  "totalProductsSold": 523,
  "totalRevenue": 25000000,
  "totalAmount": 27500000,
  "averageOrderValue": 166666.67
}
```

| Field | Type | Mô tả |
|-------|------|-------|
| `totalOrders` | `long` | Tổng số đơn hàng |
| `totalProductsSold` | `long` | Tổng số sản phẩm đã bán (cộng quantity tất cả items) |
| `totalRevenue` | `double` | Tổng doanh thu (chưa thuế) |
| `totalAmount` | `double` | Tổng tiền phải trả (gồm thuế) |
| `averageOrderValue` | `double` | Giá trị trung bình đơn hàng = `totalRevenue / totalOrders` |

### DailyReportResponse

```json
{
  "date": "2026-04-15",
  "totalOrders": 12,
  "totalProductsSold": 45,
  "totalRevenue": 3200000,
  "totalAmount": 3520000
}
```

| Field | Type | Mô tả |
|-------|------|-------|
| `date` | `LocalDate` | Ngày trong báo cáo |
| `totalOrders` | `long` | Số đơn hàng trong ngày |
| `totalProductsSold` | `long` | Số sản phẩm bán trong ngày |
| `totalRevenue` | `double` | Doanh thu trong ngày (chưa thuế) |
| `totalAmount` | `double` | Tổng tiền trong ngày (gồm thuế) |

### TopProductResponse

```json
{
  "productId": "prod001",
  "productName": "Cà phê sữa đá",
  "totalQuantitySold": 120,
  "totalRevenue": 3600000
}
```

| Field | Type | Mô tả |
|-------|------|-------|
| `productId` | `string` | ID sản phẩm |
| `productName` | `string` | Tên sản phẩm |
| `totalQuantitySold` | `long` | Tổng số lượng đã bán |
| `totalRevenue` | `double` | Tổng doanh thu từ sản phẩm (quantity × priceAfterDiscount) |

### API Response Wrapper

Tất cả response đều được bọc trong `ApiResponseDto`:

```json
{
  "success": true,
  "code": "2000",
  "message": "Operation successful",
  "data": { ... },
  "timestamp": "2026-04-03T10:00:00.000Z"
}
```

---

## Endpoints

### 1. Báo cáo tổng hợp

Trả về báo cáo tổng hợp doanh thu, số đơn hàng, sản phẩm đã bán, giá trị trung bình đơn hàng trong khoảng thời gian chỉ định.

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/reports/summary` |
| **Permission** | `REPORT_VIEW` |
| **Plan** | `PRO`, `ENTERPRISE` |

#### Query Parameters

| Parameter | Type | Required | Mô tả |
|-----------|------|----------|-------|
| `shopId` | `string` | **Có** | ID của cửa hàng |

#### Request Body

```json
{
  "startDate": "2026-04-01",
  "endDate": "2026-04-30",
  "status": "COMPLETED",
  "branchId": "branch456"
}
```

#### Request Example

```http
POST /api/reports/summary?shopId=shop123
Authorization: Bearer <token>
Content-Type: application/json

{
  "startDate": "2026-04-01",
  "endDate": "2026-04-30",
  "branchId": "branch456"
}
```

#### Response Example (200 OK)

```json
{
  "success": true,
  "code": "2000",
  "message": "Operation successful",
  "data": {
    "totalOrders": 150,
    "totalProductsSold": 523,
    "totalRevenue": 25000000,
    "totalAmount": 27500000,
    "averageOrderValue": 166666.67
  },
  "timestamp": "2026-04-03T10:00:00.000Z"
}
```

#### Response khi không có dữ liệu

```json
{
  "success": true,
  "code": "2000",
  "message": "Operation successful",
  "data": {
    "totalOrders": 0,
    "totalProductsSold": 0,
    "totalRevenue": 0,
    "totalAmount": 0,
    "averageOrderValue": 0
  },
  "timestamp": "2026-04-03T10:00:00.000Z"
}
```

#### Error Responses

| HTTP Code | Code | Mô tả |
|-----------|------|-------|
| 401 | `UNAUTHORIZED` | Chưa đăng nhập |
| 403 | `FORBIDDEN` | Không có quyền `REPORT_VIEW` hoặc gói dịch vụ không đủ |

---

### 2. Báo cáo theo ngày

Trả về danh sách báo cáo chi tiết theo từng ngày, sắp xếp tăng dần theo ngày.

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/reports/daily` |
| **Permission** | `REPORT_VIEW` |
| **Plan** | `PRO`, `ENTERPRISE` |

#### Query Parameters

| Parameter | Type | Required | Mô tả |
|-----------|------|----------|-------|
| `shopId` | `string` | **Có** | ID của cửa hàng |

#### Request Body

Giống hoàn toàn với [ReportRequest](#reportrequest-request-body).

#### Request Example

```http
POST /api/reports/daily?shopId=shop123
Authorization: Bearer <token>
Content-Type: application/json

{
  "startDate": "2026-04-01",
  "endDate": "2026-04-07"
}
```

#### Response Example (200 OK)

```json
{
  "success": true,
  "code": "2000",
  "message": "Operation successful",
  "data": [
    {
      "date": "2026-04-01",
      "totalOrders": 18,
      "totalProductsSold": 65,
      "totalRevenue": 4200000,
      "totalAmount": 4620000
    },
    {
      "date": "2026-04-02",
      "totalOrders": 22,
      "totalProductsSold": 78,
      "totalRevenue": 5100000,
      "totalAmount": 5610000
    },
    {
      "date": "2026-04-03",
      "totalOrders": 15,
      "totalProductsSold": 52,
      "totalRevenue": 3300000,
      "totalAmount": 3630000
    }
  ],
  "timestamp": "2026-04-03T10:00:00.000Z"
}
```

#### Error Responses

| HTTP Code | Code | Mô tả |
|-----------|------|-------|
| 401 | `UNAUTHORIZED` | Chưa đăng nhập |
| 403 | `FORBIDDEN` | Không có quyền `REPORT_VIEW` hoặc gói dịch vụ không đủ |

---

### 3. Sản phẩm bán chạy

Trả về danh sách top N sản phẩm bán chạy nhất, sắp xếp theo số lượng bán giảm dần.

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/reports/top-products` |
| **Permission** | `REPORT_VIEW` |
| **Plan** | `PRO`, `ENTERPRISE` |

#### Query Parameters

| Parameter | Type | Required | Default | Mô tả |
|-----------|------|----------|---------|-------|
| `shopId` | `string` | **Có** | — | ID của cửa hàng |
| `limit` | `int` | Không | `10` | Số lượng sản phẩm trả về |

#### Request Body

Giống hoàn toàn với [ReportRequest](#reportrequest-request-body).

#### Request Example

```http
POST /api/reports/top-products?shopId=shop123&limit=5
Authorization: Bearer <token>
Content-Type: application/json

{
  "startDate": "2026-04-01",
  "endDate": "2026-04-30",
  "branchId": "branch456"
}
```

#### Response Example (200 OK)

```json
{
  "success": true,
  "code": "2000",
  "message": "Operation successful",
  "data": [
    {
      "productId": "prod001",
      "productName": "Cà phê sữa đá",
      "totalQuantitySold": 120,
      "totalRevenue": 3600000
    },
    {
      "productId": "prod005",
      "productName": "Trà đào cam sả",
      "totalQuantitySold": 95,
      "totalRevenue": 3325000
    },
    {
      "productId": "prod003",
      "productName": "Bánh mì thịt",
      "totalQuantitySold": 80,
      "totalRevenue": 2000000
    }
  ],
  "timestamp": "2026-04-03T10:00:00.000Z"
}
```

#### Error Responses

| HTTP Code | Code | Mô tả |
|-----------|------|-------|
| 401 | `UNAUTHORIZED` | Chưa đăng nhập |
| 403 | `FORBIDDEN` | Không có quyền `REPORT_VIEW` hoặc gói dịch vụ không đủ |

---

### 4. Xuất báo cáo theo ngày ra Excel

Xuất dữ liệu báo cáo theo ngày dưới dạng file `.xlsx`.

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/reports/daily/export` |
| **Permission** | `REPORT_VIEW` |
| **Plan** | `PRO`, `ENTERPRISE` |

#### Query Parameters

| Parameter | Type | Required | Mô tả |
|-----------|------|----------|-------|
| `shopId` | `string` | **Có** | ID của cửa hàng |
| `startDate` | `LocalDate` | **Có** | Ngày bắt đầu (yyyy-MM-dd) |
| `endDate` | `LocalDate` | **Có** | Ngày kết thúc (yyyy-MM-dd) |
| `branchId` | `string` | Không | ID chi nhánh (tuỳ chọn) |

#### Request Example

```http
GET /api/reports/daily/export?shopId=shop123&startDate=2026-04-01&endDate=2026-04-30&branchId=branch456
Authorization: Bearer <token>
```

#### Response

- **Content-Type:** `application/octet-stream`
- **Content-Disposition:** `attachment; filename="daily-report.xlsx"`
- File Excel với các cột: **Ngày**, **Tổng đơn**, **Tổng sản phẩm**, **Doanh thu**, **Tổng tiền (gồm thuế)**

#### Error Responses

| HTTP Code | Code | Mô tả |
|-----------|------|-------|
| 401 | `UNAUTHORIZED` | Chưa đăng nhập |
| 403 | `FORBIDDEN` | Không có quyền `REPORT_VIEW` hoặc gói dịch vụ không đủ |

---

### 5. Xuất sản phẩm bán chạy ra Excel

Xuất danh sách sản phẩm bán chạy nhất dưới dạng file `.xlsx`.

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/reports/top-products/export` |
| **Permission** | `REPORT_VIEW` |
| **Plan** | `PRO`, `ENTERPRISE` |

#### Query Parameters

| Parameter | Type | Required | Default | Mô tả |
|-----------|------|----------|---------|-------|
| `shopId` | `string` | **Có** | — | ID của cửa hàng |
| `limit` | `int` | Không | `10` | Số lượng sản phẩm |

#### Request Body

Giống hoàn toàn với [ReportRequest](#reportrequest-request-body).

#### Request Example

```http
POST /api/reports/top-products/export?shopId=shop123&limit=20
Authorization: Bearer <token>
Content-Type: application/json

{
  "startDate": "2026-04-01",
  "endDate": "2026-04-30"
}
```

#### Response

- **Content-Type:** `application/octet-stream`
- **Content-Disposition:** `attachment; filename="top-products.xlsx"`
- File Excel với các cột: **Mã sản phẩm**, **Tên sản phẩm**, **Số lượng bán**, **Doanh thu**

#### Error Responses

| HTTP Code | Code | Mô tả |
|-----------|------|-------|
| 401 | `UNAUTHORIZED` | Chưa đăng nhập |
| 403 | `FORBIDDEN` | Không có quyền `REPORT_VIEW` hoặc gói dịch vụ không đủ |

---

## Phân quyền (Permissions)

| Permission | Mô tả | Endpoints |
|------------|-------|-----------|
| `REPORT_VIEW` | Xem và xuất báo cáo | Tất cả endpoint trong Report Service |

> Ngoài permission, user phải thuộc gói **PRO** hoặc **ENTERPRISE** mới truy cập được tính năng báo cáo.

---

## Error Codes

| Code | Mô tả |
|------|-------|
| `SUCCESS` (2000) | Thao tác thành công |
| `UNAUTHORIZED` (4001) | Không có quyền truy cập |
| `ACCESS_DENIED` (4002) | Không đủ quyền |
| `PLAN_UPGRADE_REQUIRED` (4020) | Gói dịch vụ không đủ (cần PRO/ENTERPRISE) |
| `VALIDATION_ERROR` (4000) | Dữ liệu đầu vào không hợp lệ |

---

## Ghi chú cho FE

### 1. Phân biệt `totalRevenue` vs `totalAmount`

| Field | Ý nghĩa | Công thức |
|-------|---------|-----------|
| `totalRevenue` | Doanh thu chưa thuế | Tổng `totalPrice` của các đơn hàng |
| `totalAmount` | Tổng tiền khách phải trả | Tổng `totalAmount` (= `totalPrice + tax`) |

FE nên hiển thị cả 2 giá trị. `totalRevenue` phù hợp cho phân tích doanh thu nội bộ, `totalAmount` phù hợp cho đối chiếu thu chi thực tế.

### 2. Lọc theo chi nhánh (`branchId`)

| Trường hợp | Kết quả |
|-------------|---------|
| Không truyền `branchId` | Báo cáo tổng hợp **toàn shop** (tất cả chi nhánh) |
| Truyền `branchId=branch456` | Báo cáo chỉ cho **chi nhánh đó** |

FE nên cung cấp dropdown chọn chi nhánh (hoặc "Tất cả chi nhánh") phía trên báo cáo.

### 3. Lọc theo trạng thái đơn hàng

| `status` | Mô tả |
|----------|-------|
| `PENDING` | Đơn mới tạo, chưa xác nhận |
| `CONFIRMED` | Đã xác nhận |
| `SHIPPING` | Đang vận chuyển |
| `COMPLETED` | Hoàn tất |
| `CANCELLED` | Đã huỷ |

> Mặc định không lọc status → bao gồm tất cả đơn hàng. FE nên mặc định chọn `COMPLETED` để báo cáo chính xác doanh thu thực tế.

### 4. Date Format

Request sử dụng `LocalDate` format ISO:

```
2026-04-01   (yyyy-MM-dd)
```

Response `DailyReportResponse.date` cũng trả về ISO format `yyyy-MM-dd`. FE format tuỳ theo locale hiển thị (VD: `01/04/2026`).

### 5. Sản phẩm bán chạy

- `limit` mặc định là `10`, tối đa tuỳ nhu cầu
- Doanh thu (`totalRevenue`) được tính bằng `quantity × priceAfterDiscount` (đã áp dụng khuyến mãi)
- Sắp xếp theo `totalQuantitySold` giảm dần

FE nên hiển thị dưới dạng bảng hoặc biểu đồ thanh (bar chart).

### 6. Tải file Excel

Khi gọi endpoint export, response trả về file binary. FE xử lý:

```javascript
const response = await fetch(url, {
  headers: { Authorization: `Bearer ${token}` }
});
const blob = await response.blob();
const link = document.createElement('a');
link.href = URL.createObjectURL(blob);
link.download = 'daily-report.xlsx'; // hoặc 'top-products.xlsx'
link.click();
URL.revokeObjectURL(link.href);
```

### 7. TypeScript Interfaces (tham khảo)

```typescript
enum OrderStatus {
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  SHIPPING = 'SHIPPING',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
}

interface ReportRequest {
  startDate?: string;   // yyyy-MM-dd
  endDate?: string;     // yyyy-MM-dd
  status?: OrderStatus;
  branchId?: string;    // null = toàn shop
}

interface ReportResponse {
  totalOrders: number;
  totalProductsSold: number;
  totalRevenue: number;
  totalAmount: number;
  averageOrderValue: number;
}

interface DailyReportResponse {
  date: string;         // yyyy-MM-dd
  totalOrders: number;
  totalProductsSold: number;
  totalRevenue: number;
  totalAmount: number;
}

interface TopProductResponse {
  productId: string;
  productName: string;
  totalQuantitySold: number;
  totalRevenue: number;
}

interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
}
```
