// File: src/main/java/com/example/sales/service/ExcelExportService.java
package com.example.sales.service;

import com.example.sales.export.GenericExcelExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    public <T> ResponseEntity<byte[]> exportExcel(String fileName,
                                                  String sheetName,
                                                  List<String> headers,
                                                  List<T> data,
                                                  Function<T, List<String>> rowMapper) {
        try {
            GenericExcelExporter<T> exporter = new GenericExcelExporter<>();
            InputStream excelStream = exporter.export(sheetName, headers, data, rowMapper);
            byte[] content = excelStream.readAllBytes();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());

            return new ResponseEntity<>(content, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            throw new RuntimeException("Không thể export file Excel", e);
        }
    }
}
