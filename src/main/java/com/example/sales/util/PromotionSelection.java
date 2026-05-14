package com.example.sales.util;

import com.example.sales.constant.DiscountType;
import com.example.sales.model.Promotion;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Quy tắc chọn <strong>một</strong> khuyến mãi áp dụng khi nhiều chương trình cùng thỏa điều kiện (thời gian, chi nhánh, sản phẩm).
 * <p>
 * Thứ tự: {@code priority} cao hơn thắng → nếu bằng nhau, giá đơn vị sau giảm <strong>thấp hơn</strong> (lợi hơn cho khách) thắng
 * → cuối cùng {@code id} tăng dần để kết quả ổn định.
 */
public final class PromotionSelection {

    private PromotionSelection() {
    }

    public static int effectivePriority(Promotion p) {
        return p != null ? p.getPriority() : 0;
    }

    public static double discountedUnitPrice(double unitBeforePromo, Promotion p) {
        if (p == null || unitBeforePromo <= 0) {
            return unitBeforePromo;
        }
        if (p.getDiscountType() == DiscountType.PERCENT) {
            return unitBeforePromo * (1 - p.getDiscountValue() / 100.0);
        }
        return Math.max(0, unitBeforePromo - p.getDiscountValue());
    }

    /**
     * @param applicable danh sách đã lọc active / branch / thời gian / sản phẩm
     * @param unitBeforePromo giá đơn vị (sau biến thể + topping nếu có) trước khi áp KM
     */
    public static Promotion selectWinningPromotion(List<Promotion> applicable, double unitBeforePromo) {
        if (applicable == null || applicable.isEmpty()) {
            return null;
        }
        return applicable.stream()
                .sorted(Comparator
                        .comparingInt(PromotionSelection::effectivePriority).reversed()
                        .thenComparingDouble(p -> discountedUnitPrice(unitBeforePromo, p))
                        .thenComparing(p -> Objects.toString(p.getId(), "")))
                .findFirst()
                .orElse(null);
    }
}
