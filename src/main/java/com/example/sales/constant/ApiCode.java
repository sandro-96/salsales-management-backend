// File: src/main/java/com/example/sales/constant/ApiCode.java
package com.example.sales.constant;

import lombok.Getter;

@Getter
public enum ApiCode {
    // Success
    SUCCESS("2000", "Operation successful"),
    PRODUCT_CREATED("2001", "Product created successfully"),
    USER_UPDATED("2002", "User updated successfully"),
    PASSWORD_CHANGED("2003", "Password changed successfully"),
    EMAIL_SENT("2004", "Email sent successfully"),

    // Validation & Client Errors
    VALIDATION_ERROR("4000", "Validation error"),
    UNAUTHORIZED("4001", "Unauthorized access"),
    ACCESS_DENIED("4002", "Access denied"),
    NOT_FOUND("4003", "Resource not found"),
    INVALID_TOKEN("4004", "Invalid token"),
    ACCOUNT_LOCKED("4005", "Account is locked"),
    REFRESH_TOKEN_EXPIRED("4006", "Refresh token expired"),
    REFRESH_TOKEN_INVALID("4007", "Invalid refresh token"),
    INCORRECT_PASSWORD("4008", "Incorrect password"),
    EMAIL_EXISTS("4009", "Email already exists"),
    EMAIL_NOT_VERIFIED("4010", "Email not verified"),
    ALREADY_VERIFIED("4011", "Email already verified"),
    TOKEN_EXPIRED("4012", "Token expired"),
    VALIDATION_FILE_ERROR("4013", "Invalid file format"),
    ORDER_ALREADY_PAID("4014", "Order already paid"),
    INVALID_STATUS_TRANSITION("4015", "Invalid status transition"),
    PRODUCT_OUT_OF_STOCK("4016", "Product out of stock"),
    DUPLICATE_DATA("4017", "Duplicate data"),
    CANNOT_DELETE_SELF("4018", "Cannot delete self"),
    SHOP_ALREADY_EXISTS("4019", "Shop already exists"),
    PLAN_UPGRADE_REQUIRED("4020", "Plan upgrade required"),

    // System Errors
    INTERNAL_ERROR("5000", "Internal server error"),
    FILE_UPLOAD_FAILED("5001", "File upload failed"),
    FILE_TYPE_NOT_ALLOWED("5002", "File type not allowed"),

    // Business Logic
    PRODUCT_UPDATED("4102", "Product updated successfully"),
    SHOP_NOT_FOUND("4103", "Shop not found"),
    USER_INFO("4107", "User info retrieved"),
    USER_NOT_FOUND("4108", "User not found"),
    BRANCH_NOT_FOUND("4109", "Branch not found"),
    CUSTOMER_NOT_FOUND("4110", "Customer not found"),
    ORDER_NOT_FOUND("4111", "Order not found"),
    PRODUCT_NOT_FOUND("4112", "Product not found"),
    PROMOTION_NOT_FOUND("4113", "Promotion not found"),
    TABLE_NOT_FOUND("4114", "Table not found"),
    USER_DELETED("4115", "User deleted successfully"),
    ORDER_PAYMENT_CONFIRMED("4116", "Order payment confirmed"),
    ORDER_STATUS_UPDATED("4117", "Order status updated"),
    ORDER_LIST("4118", "Order list retrieved"),
    ORDER_CANCELLED("4119", "Order cancelled"),
    ORDER_CREATED("4120", "Order created successfully"),
    ORDER_UPDATED("4120", "Order updated successfully"),
    CUSTOMER_CREATED("4121", "Customer created successfully"),
    CUSTOMER_UPDATED("4122", "Customer updated successfully"),
    CUSTOMER_DELETED("4123", "Customer deleted successfully"),
    CUSTOMER_LIST("4124", "Customer list retrieved"),
    PRODUCT_DELETED("4125", "Product deleted successfully"),
    PRODUCT_LIST("4126", "Product list retrieved"),
    PRODUCT_FOUND("4127", "Product found"),
    PRODUCT_IMPORTED("4128", "Product imported successfully"),
    PRODUCT_STATUS_UPDATED("4129", "Product status updated successfully"),
    PRODUCT_LOW_STOCK("4130", "Low stock products retrieved"),
    PRODUCT_SEARCH_RESULTS("4131", "Product search results retrieved"),
    SHOP_NAME_EXISTS("4132", "Shop name already exists"),
    SHOP_INACTIVE("4133", "Shop is inactive"),
    INVALID_CAPACITY("4134", "Invalid capacity"),
    TABLE_NAME_EXISTS("4135", "Table name already exists"),
    TABLE_OCCUPIED("4136", "Table is occupied"),
    USER_ALREADY_IN_SHOP("4137", "User already in shop"),
    CANNOT_DELETE_ONLY_BRANCH("4138", "Cannot delete the only branch of the shop"),
    INSUFFICIENT_STOCK ("4139", "Insufficient stock for product"),
    INVALID_CREDENTIALS("4011", "Email or password is incorrect");

    private final String code;
    private final String message;

    ApiCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
