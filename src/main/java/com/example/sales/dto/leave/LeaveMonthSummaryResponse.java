package com.example.sales.dto.leave;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class LeaveMonthSummaryResponse {
    private String shopId;
    /** yyyy-MM */
    private String month;
    private long totalRequests;
    private long pendingRequests;
    private long approvedRequests;
    private long rejectedRequests;
    private long cancelledRequests;
    /** type -> count */
    private Map<String, Long> byType;
}

