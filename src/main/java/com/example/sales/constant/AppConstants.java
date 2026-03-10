package com.example.sales.constant;

public class AppConstants {
    public static final String DEFAULT_BRANCH_NAME = "DEFAULT_BRANCH";

    public static class SequenceTypes {
        public static final String SEQUENCE_TYPE_SKU = "SKU";
        public static final String SEQUENCE_TYPE_ORDER = "ORDER";
        public static final String SEQUENCE_TYPE_INVENTORY = "INVENTORY";
        public static final String SEQUENCE_TYPE_TICKET = "TICKET";
        public static final String SEQUENCE_TYPE_BARCODE = "BARCODE";
    }

    public static class SequencePrefixes {
        /** Prefix dùng chung cho tất cả barcode EAN-13 — bất kể industry/category,
         *  đảm bảo số thứ tự barcode unique toàn shop. */
        public static final String BARCODE_GLOBAL = "BARCODE";
    }
}
