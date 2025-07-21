// File: src/main/java/com/example/sales/service/ExcelExportService.java
package com.example.sales.service;

import com.example.sales.dto.product.ProductResponse; // Import ProductResponse
import com.example.sales.export.GenericExcelExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream; // Thêm import này
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ExcelExportService {
    // Chúng ta sẽ không cần ProductRepository trực tiếp ở đây nữa,
    // mà sẽ sử dụng ProductService để lấy dữ liệu đã được map sang ProductResponse
    private final ProductService productService; // Tiêm ProductService

    // Phương thức exportExcel tổng quát có thể giữ nguyên nếu bạn vẫn dùng nó ở nơi khác
    public <T> ResponseEntity<byte[]> exportExcel(String fileName,
                                                  String sheetName,
                                                  List<String> headers,
                                                  List<T> data,
                                                  Function<T, List<String>> rowMapper) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) { // Sử dụng ByteArrayOutputStream
            GenericExcelExporter<T> exporter = new GenericExcelExporter<>();
            exporter.export(sheetName, headers, data, rowMapper, bos); // Ghi vào OutputStream

            byte[] content = bos.toByteArray(); // Lấy byte array từ OutputStream

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());

            return new ResponseEntity<>(content, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            throw new RuntimeException("Không thể export file Excel", e);
        }
    }

    // ✅ Phương thức exportProducts được cập nhật để sử dụng ProductService và ProductResponse
    public ResponseEntity<byte[]> exportProducts(String shopId, String branchId) throws IOException {
        // Lấy dữ liệu ProductResponse từ ProductService
        // Sử dụng phân trang để tránh load quá nhiều dữ liệu cùng lúc
        Pageable pageable = PageRequest.of(0, 1000); // Lấy 1000 sản phẩm mỗi lần
        Page<ProductResponse> productPage;
        List<ProductResponse> allProducts = new java.util.ArrayList<>();

        do {
            productPage = productService.getAllByShop(shopId, branchId, pageable);
            allProducts.addAll(productPage.getContent());
            if (productPage.hasNext()) {
                pageable = productPage.nextPageable();
            } else {
                break;
            }
        } while (true);


        // Định nghĩa headers cho file Excel
        List<String> headers = List.of(
                "SKU", "Tên sản phẩm", "Danh mục", "Chi nhánh ID",
                "Số lượng", "Giá", "Đơn vị", "URL Hình ảnh", "Mô tả", "Trạng thái hoạt động"
        );

        // Định nghĩa cách map ProductResponse thành các dòng dữ liệu
        Function<ProductResponse, List<String>> rowMapper = p -> List.of(
                p.getSku(),
                p.getName(),
                p.getCategory(),
                p.getBranchId(),
                String.valueOf(p.getQuantity()),
                String.valueOf(p.getPrice()),
                p.getUnit(),
                p.getImageUrl() != null ? p.getImageUrl() : "",
                p.getDescription() != null ? p.getDescription() : "",
                p.isActiveInBranch() ? "Có" : "Không"
        );

        // Sử dụng GenericExcelExporter để tạo file Excel
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            GenericExcelExporter<ProductResponse> exporter = new GenericExcelExporter<>();
            exporter.export("Products", headers, allProducts, rowMapper, bos);
            byte[] content = bos.toByteArray();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("products.xlsx").build().toString())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(content);
        } catch (Exception e) {
            throw new RuntimeException("Không thể export file Excel sản phẩm", e);
        }
    }
}