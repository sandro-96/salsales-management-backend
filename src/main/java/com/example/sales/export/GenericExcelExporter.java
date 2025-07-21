// File: src/main/java/com/example/sales/export/GenericExcelExporter.java
package com.example.sales.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream; // Import OutputStream
import java.util.List;
import java.util.function.Function;

public class GenericExcelExporter<T> {

    /**
     * Xuất dữ liệu vào một file Excel.
     *
     * @param sheetName Tên của sheet.
     * @param headers Danh sách các tiêu đề cột.
     * @param data Danh sách dữ liệu cần xuất.
     * @param rowMapper Hàm ánh xạ một đối tượng T thành một danh sách các String cho một hàng.
     * @param outputStream OutputStream để ghi dữ liệu Excel.
     * @throws IOException Nếu có lỗi trong quá trình ghi file.
     */
    public void export(String sheetName,
                       List<String> headers,
                       List<T> data,
                       Function<T, List<String>> rowMapper,
                       OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            // Tạo hàng tiêu đề
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
            }

            // Ghi dữ liệu
            int rowNum = 1;
            for (T item : data) {
                Row row = sheet.createRow(rowNum++);
                List<String> rowData = rowMapper.apply(item);
                for (int i = 0; i < rowData.size(); i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(rowData.get(i));
                }
            }

            // Tự động điều chỉnh độ rộng cột (tùy chọn)
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream); // Ghi workbook vào OutputStream
        }
    }
}