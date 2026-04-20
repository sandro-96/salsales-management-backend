// File: src/main/java/com/example/sales/constant/SubscriptionPlan.java
package com.example.sales.constant;

public enum SubscriptionPlan {
    FREE(0L),
    PRO(199_000L),
    ENTERPRISE(599_000L);

    private final long monthlyPriceVnd;

    SubscriptionPlan(long monthlyPriceVnd) {
        this.monthlyPriceVnd = monthlyPriceVnd;
    }

    public long getMonthlyPriceVnd() {
        return monthlyPriceVnd;
    }
}

