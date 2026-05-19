// File: src/main/java/com/example/sales/model/Shop.java
package com.example.sales.model;

import com.example.sales.constant.BusinessModel;
import com.example.sales.constant.Country;
import com.example.sales.constant.ShopType;
import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "shops")
public class Shop extends BaseEntity {

    @Id
    private String id;

    private String name;
    private String ownerId;
    private ShopType type;
    private BusinessModel businessModel;
    private String address;
    /** SĐT chính (đồng bộ với phần tử đầu của {@link #phones}). */
    private String phone;
    /** Danh sách SĐT liên hệ cửa hàng. */
    private List<String> phones;
    private String logoUrl;
    private Country countryCode;
    private String slug;

    /**
     * Mã số thuế (MST) đăng ký của pháp nhân / cửa hàng.
     * Chi nhánh có thể ghi đè riêng nếu có MST tại điểm kinh doanh.
     */
    private String taxRegistrationNumber;

    /** Liên kết ngoài (tuỳ chọn) — hiển thị cài đặt cửa hàng / marketing */
    private String zaloPageUrl;
    private String facebookUrl;
    private String tiktokUrl;
    private String shopeeUrl;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private String currency = "VND";

    @Builder.Default
    private String timezone = "Asia/Ho_Chi_Minh";

    @Builder.Default
    private String orderPrefix = "ORD";

    @Builder.Default
    private SubscriptionPlan plan = SubscriptionPlan.FREE;

    private LocalDateTime planExpiry;

    /**
     * Bật tính năng topping (POS + gán topping cho sản phẩm). Mặc định tắt.
     */
    @Builder.Default
    private boolean toppingsEnabled = false;

    /** Danh mục topping dùng chung toàn shop (khi {@code toppingsEnabled}). */
    private List<ShopTopping> shopToppings;

    /**
     * Bật chế độ bán hàng online (storefront công khai theo slug). Mặc định tắt.
     * Khi bật, mọi guest đều có thể truy cập URL công khai để xem sản phẩm và đặt đơn COD.
     */
    @Builder.Default
    private boolean onlineSalesEnabled = false;

    /**
     * Bật chế độ khách tự order tại bàn qua QR code (dine-in self-service). Mặc định tắt.
     * Khi bật, khách quét QR ở bàn → mở URL {@code /t/{slug}/{qrToken}} để xem menu & đặt món;
     * mỗi bàn duy trì 1 tab/đơn duy nhất (gộp các lần quét). Staff thu tiền tại quầy qua POS.
     */
    @Builder.Default
    private boolean tableOrderingEnabled = false;
}

