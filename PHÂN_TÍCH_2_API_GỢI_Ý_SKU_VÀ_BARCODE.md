# 📊 Phân Tích 2 API Gợi Ý: SKU & Barcode

## 🎯 Tóm Tắt Nhanh

Hai API này giúp **tự động sinh mã SKU và mã vạch** để tiện tạo sản phẩm mới.

---

## 📋 API 1: Gợi Ý SKU (Lấy Mã Sản Phẩm Tự Động)

### Endpoint
```
GET /api/shops/{shopId}/suggested-sku?industry=FOOD&category=VEGETABLE
```

### Parameters
- **shopId** (path): ID cửa hàng
- **industry** (query): Ngành hàng - VD: `FOOD`, `ELECTRONICS`
- **category** (query): Danh mục - VD: `VEGETABLE`, `FRUIT` (tùy chọn)

### Response
```json
{
  "code": "SUCCESS",
  "data": "FOOD_VEGETABLE_000001"
}
```

### Cách Hoạt Động

```
industry = "FOOD"
category = "VEGETABLE"
    ↓
┌─────────────────────────────────────┐
│ 1️⃣ Tạo Prefix (Tiền Tố)            │
│   FOOD_VEGETABLE                    │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 2️⃣ Lấy Sequence (Số Hiệu Tự Tăng)  │
│   Lần 1: 000001                     │
│   Lần 2: 000002                     │
│   Lần 3: 000003                     │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 3️⃣ Kết Hợp = Prefix + Sequence     │
│   FOOD_VEGETABLE_000001             │
│   FOOD_VEGETABLE_000002             │
│   FOOD_VEGETABLE_000003             │
└─────────────────────────────────────┘
```

### Format
```
Nếu có category:  {INDUSTRY}_{CATEGORY}_{SEQUENCE}
VD: FOOD_VEGETABLE_000001

Nếu không category: {INDUSTRY}_{SEQUENCE}
VD: FOOD_000001
```

### Implementation Code
```java
@Override
public String getSuggestedSku(String shopId, String industry, String category) {
    // Tạo prefix: INDUSTRY_CATEGORY hoặc INDUSTRY
    String prefix = StringUtils.hasText(category)
            ? String.format("%s_%s", industry.toUpperCase(), category.toUpperCase())
            : industry.toUpperCase();
    
    // Lấy sequence number tiếp theo từ SequenceService
    return sequenceService.getNextCode(shopId, prefix, 
            AppConstants.SequenceTypes.SEQUENCE_TYPE_SKU);
}
```

### Ghi Chú
- ✅ **Tự động tăng**: Sequence service quản lý số hiệu
- ✅ **Không bắt buộc dùng**: Client có thể tùy chỉnh hoặc bỏ qua
- ✅ **Unique per shop**: Mỗi shop có sequence riêng
- ✅ **Chỉ gợi ý**: Không lưu vào database cho đến khi tạo sản phẩm

---

## 📋 API 2: Gợi Ý Barcode (Lấy Mã Vạch EAN-13 Tự Động)

### Endpoint
```
GET /api/shops/{shopId}/suggested-barcode?industry=FOOD&category=VEGETABLE
```

### Parameters
- **shopId** (path): ID cửa hàng
- **industry** (query): Ngành hàng
- **category** (query): Danh mục (tùy chọn)

### Response
```json
{
  "code": "SUCCESS",
  "data": "8930000000017"
}
```

### Cách Hoạt Động (Chi Tiết)

```
industry = "FOOD"
category = "VEGETABLE"
    ↓
┌─────────────────────────────────────────────────────┐
│ 1️⃣ Tạo Prefix & Lấy Sequence Number                │
│    Prefix = "FOOD_VEGETABLE"                        │
│    Sequence = 1                                     │
└─────────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────────┐
│ 2️⃣ Xây Dựng Base Code (12 chữ số)                   │
│    "893" (GS1 Vietnam) + "000000001" (padded)       │
│    = "893000000001"                                 │
└─────────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────────┐
│ 3️⃣ Tính Check Digit (Chữ Số Kiểm Tra)              │
│    EAN-13 Checksum Algorithm:                       │
│    • Cộng digits ở vị trí lẻ (1,3,5,...) × 1       │
│    • Cộng digits ở vị trí chẵn (2,4,6,...) × 3     │
│    • Total = OddSum + (EvenSum × 3)                 │
│    • Check = (10 - (Total % 10)) % 10              │
│    = 7                                              │
└─────────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────────┐
│ 4️⃣ Kết Quả Final: Base + CheckDigit                │
│    "893000000001" + "7"                             │
│    = "8930000000017" (EAN-13)                       │
└─────────────────────────────────────────────────────┘
```

### Ví Dụ Tính Checksum Chi Tiết

```
Base Code: 893000000001
Position:  1  2  3  4  5  6  7  8  9  10 11 12
Digit:     8  9  3  0  0  0  0  0  0  0  0  1

Odd Position (1,3,5,7,9,11): 8 + 3 + 0 + 0 + 0 + 1 = 12
Even Position (2,4,6,8,10,12): 9 + 0 + 0 + 0 + 0 + 1 = 10

Total = (12 × 1) + (10 × 3) = 12 + 30 = 42

Check Digit = (10 - (42 % 10)) % 10
            = (10 - 2) % 10
            = 8

Final Barcode = "8930000000018" ✅ (EAN-13, 13 chữ số)
```

### Implementation Code
```java
@Override
public String getSuggestedBarcode(String shopId, String industry, String category) {
    // Tạo prefix
    String prefix = StringUtils.hasText(category)
            ? String.format("%s_%s", industry.toUpperCase(), category.toUpperCase())
            : industry.toUpperCase();
    
    // Lấy sequence number
    String sequence = sequenceService.getNextCode(shopId, prefix, 
            AppConstants.SequenceTypes.SEQUENCE_TYPE_BARCODE);
    
    // Extract sequence number từ format "INDUSTRY_CATEGORY_001"
    String sequenceNumber = sequence.split("_")[2];
    
    // Xây dựng base code: "893" + 9 chữ số (left-padded)
    String baseCode = "893" + String.format("%09d", Integer.parseInt(sequenceNumber));
    
    // Tính check digit EAN-13
    String checkDigit = calculateEan13CheckDigit(baseCode);
    
    // Trả về barcode hoàn chỉnh
    return baseCode + checkDigit;
}

private String calculateEan13CheckDigit(String baseCode) {
    // Chuyển string thành array int
    int[] digits = baseCode.chars().map(c -> c - '0').toArray();
    
    // Tính tổng odd positions (1,3,5,...) và even positions (2,4,6,...)
    int oddSum = 0;
    int evenSum = 0;
    for (int i = 0; i < 12; i++) {
        if (i % 2 == 0) {  // Index 0,2,4,... = Position 1,3,5,...
            oddSum += digits[i];
        } else {           // Index 1,3,5,... = Position 2,4,6,...
            evenSum += digits[i];
        }
    }
    
    // EAN-13 checksum = (10 - ((oddSum + evenSum*3) % 10)) % 10
    int total = oddSum + (evenSum * 3);
    int checkDigit = (10 - (total % 10)) % 10;
    
    return String.valueOf(checkDigit);
}
```

### Format
```
"893" + Sequence(9 chữ số) + CheckDigit(1 chữ số)
= 13 chữ số tổng cộng (EAN-13 standard)

VD: 
- "8930000000017"
- "8930000000018"
- "8930000000019"
```

### Ghi Chú
- ✅ **GS1 Vietnam**: Prefix "893" là mã quốc gia Việt Nam
- ✅ **EAN-13**: Tiêu chuẩn quốc tế 13 chữ số
- ✅ **Check Digit**: Đảm bảo tính toàn vẹn dữ liệu
- ✅ **Unique per shop**: Sequence tăng cho mỗi shop
- ✅ **Có thể thay đổi**: Client không bắt buộc dùng, có thể nhập barcode custom
- ✅ **Validate**: Phải match pattern `^([A-Z0-9_]*|[0-9]{12,13})$`

---

## 🔄 So Sánh SKU vs Barcode

| Tiêu Chí | SKU | Barcode |
|----------|-----|---------|
| **Mục Đích** | Quản lý nội bộ | Bán lẻ/tại quầy |
| **Format** | `INDUSTRY_CATEGORY_SEQUENCE` | `893{SEQUENCE}{CHECK}` |
| **Ví Dụ** | `FOOD_VEGETABLE_000001` | `8930000000017` |
| **Độ Dài** | Tuỳ biến | 13 chữ số (EAN-13) |
| **Check Digit** | ❌ Không | ✅ Có (EAN-13) |
| **Unique** | ✅ Trong shop | ✅ Trong shop |
| **Bắt Buộc** | ✅ Có | ❌ Tùy chọn |
| **Quét QR/Mã Vạch** | ❌ Không | ✅ Có |

---

## 💡 Use Cases (Trường Hợp Sử Dụng)

### Case 1: Tạo Sản Phẩm Với Gợi Ý Tự Động
```
1. Client gọi: GET /api/shops/shop1/suggested-sku?industry=FOOD&category=FRUIT
   → Nhận: "FOOD_FRUIT_000001"

2. Client gọi: GET /api/shops/shop1/suggested-barcode?industry=FOOD&category=FRUIT
   → Nhận: "8930000000017"

3. Client tạo product dùng cả hai gợi ý:
   POST /api/shops/shop1/products
   {
     "name": "Cam Tươi",
     "sku": "FOOD_FRUIT_000001",           ← Dùng gợi ý SKU
     "barcode": "8930000000017",            ← Dùng gợi ý Barcode
     "category": "FRUIT",
     "costPrice": 30000,
     "defaultPrice": 45000
   }
```

### Case 2: Tạo Sản Phẩm Với SKU Custom
```
Client không thích gợi ý, tự nhập:
POST /api/shops/shop1/products
{
  "name": "Cam Tươi",
  "sku": "CAM_001",                       ← Custom SKU
  "barcode": "8930000000017",              ← Dùng gợi ý Barcode
  "category": "FRUIT"
}
```

### Case 3: Tạo Sản Phẩm Không Có Barcode
```
Cho sản phẩm không cần barcode:
POST /api/shops/shop1/products
{
  "name": "Dịch Vụ Giao Hàng",
  "sku": "SERVICE_DELIVERY_001",           ← Custom SKU
  "barcode": null,                          ← Không cần barcode
  "category": "SERVICE"
}
```

---

## 🎯 Khi Nào Dùng API Nào?

| Tình Huống | API Nào |
|-----------|---------|
| Muốn tạo sản phẩm nhanh với mã tự động | Gợi ý SKU + Barcode |
| Tạo sản phẩm bán lẻ cần mã vạch | Gợi ý Barcode |
| Tạo sản phẩm với mã custom riêng | Nhập manual, không dùng API |
| Không muốn dùng barcode | Chỉ dùng gợi ý SKU |

---

## 📌 Quan Trọng

1. ✅ **Gợi ý ≠ Bắt buộc**: Có thể nhập SKU/Barcode custom
2. ✅ **Không tự động lưu**: Chỉ gợi ý, phải gọi API tạo product để lưu
3. ✅ **Unique per shop**: Sequence riêng cho mỗi shop
4. ✅ **EAN-13 standard**: Barcode tuân theo tiêu chuẩn quốc tế
5. ✅ **Có thể thay đổi sau**: Khi cập nhật product, có thể đổi barcode/sku (nếu chưa dùng)

---

## 🔗 Liên Kết Với Các API Khác

- **Sử dụng khi tạo Product**: API 2 và API 3 (Tạo từ Shop / Tạo từ Chi Nhánh)
- **Không bắt buộc**: Client có thể nhập SKU/Barcode custom hoàn toàn

---

## 📚 Tham Khảo

- **EAN-13 Checksum**: https://www.gs1.org/ (GS1 Global Standards)
- **GS1 Vietnam**: Prefix "893" là mã quốc gia của Việt Nam
- **SequenceService**: Quản lý tự động tăng số hiệu theo prefix và shop

