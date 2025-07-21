// File: src/main/java/com/example/sales/security/PlanInterceptor.java
package com.example.sales.security;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Shop;
import com.example.sales.service.ShopService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class PlanInterceptor implements HandlerInterceptor {

    private final ShopService shopService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) return true;

        RequirePlan annotation = method.getMethodAnnotation(RequirePlan.class);
        if (annotation == null) return true;

        String shopId = request.getParameter("shopId");
        if (shopId == null || shopId.isBlank()) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR); // hoặc custom lỗi
        }

        Shop shop = shopService.getShopById(shopId); // bạn cần viết hàm này
        SubscriptionPlan currentPlan = shop.getPlan();

        boolean allowed = Arrays.stream(annotation.value())
                .anyMatch(p -> currentPlan.ordinal() >= p.ordinal());

        if (!allowed) {
            throw new BusinessException(ApiCode.PLAN_UPGRADE_REQUIRED); // hoặc tạo code riêng như `PLAN_UPGRADE_REQUIRED`
        }

        return true;
    }
}
