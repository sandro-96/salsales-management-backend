// File: src/main/java/com/example/sales/export/GenericExcelExporter.java
package com.example.sales.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Function;

public class GenericExcelExporter<T> {

    /**
     * Xuất dữ liệu vào file Excel (không có dòng ghi chú).
     * Cấu trúc: Row 0 = header, Row 1+ = data.
     */
    public void export(String sheetName,
                       List<String> headers,
                       List<T> data,
                       Function<T, List<String>> rowMapper,
                       OutputStream outputStream) throws IOException {
        export(sheetName, headers, null, data, rowMapper, outputStream);
    }

    /**
     * Xuất dữ liệu vào file Excel với dòng ghi chú tùy chọn.
     * Cấu trúc: Row 0 = header, Row 1 = noteRow (nếu có), Row 2+ = data.
     *
     * @param noteRow Nội dung dòng ghi chú (null = bỏ qua, data bắt đầu từ Row 1).
     */
    public void export(String sheetName,
                       List<String> headers,
                       String noteRow,
                       List<T> data,
                       Function<T, List<String>> rowMapper,
                       OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            // Row 0: header
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
            }

            int dataStartRow;
            if (noteRow != null) {
                // Row 1: ghi chú
                Row noteRowObj = sheet.createRow(1);
                noteRowObj.createCell(0).setCellValue(noteRow);
                dataStartRow = 2;
            } else {
                dataStartRow = 1;
            }

            // Row dataStartRow+: data
            int rowNum = dataStartRow;
            for (T item : data) {
                Row row = sheet.createRow(rowNum++);
                List<String> rowData = rowMapper.apply(item);
                for (int i = 0; i < rowData.size(); i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(rowData.get(i));
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
        }
    }
}