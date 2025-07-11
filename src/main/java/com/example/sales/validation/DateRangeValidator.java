// File: src/main/java/com/example/sales/validation/DateRangeValidator.java
package com.example.sales.validation;

import com.example.sales.dto.report.ReportRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, ReportRequest> {

    @Override
    public boolean isValid(ReportRequest req, ConstraintValidatorContext context) {
        if (req.getStartDate() == null || req.getEndDate() == null) {
            return false; // bắt buộc cả 2 phải có
        }
        return !req.getStartDate().isAfter(req.getEndDate());
    }
}
