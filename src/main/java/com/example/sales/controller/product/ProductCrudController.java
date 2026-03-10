package com.example.sales.controller.product;

import com.example.sales.cache.ProductCache;
import com.example.sales.constant.ApiCode;
import com.example.sales.constant.Permission;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.product.BranchProductRequest;
import com.example.sales.dto.product.ProductRequest;
import com.example.sales.dto.product.ProductResponse;
import com.example.sales.dto.product.ProductSearchRequest;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequirePermission;
import com.example.sales.service.FileUploadService;
import com.example.sales.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller xử lý CRUD cho sản phẩm.
 *
 * Tách biệt rõ ràng 2 luồng:
 *   - Thông tin chung (Product)  : POST/PUT/DELETE /shops/{shopId}/products[/{productId}]
 *   - Thông tin chi nhánh        : PUT/DELETE /shops/{shopId}/branches/{branchId}/products/{branchProductId}
 *
 * toggle-active và low-stock được xử lý tại ProductStatusController.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductCrudController {

    private final ProductService productService;
    private final ProductCache productCache;
    private final FileUploadService fileUploadService;

    // ─────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Tạo sản phẩm mới",
            description = "Tạo Product (thông tin chung) và tự động tạo BranchProduct cho tất cả chi nhánh " +
                    "hoặc chỉ các chi nhánh được chỉ định qua query param branchIds. " +
                    "Giá và tồn kho khởi tạo lấy từ defaultPrice/costPrice, " +
                    "cập nhật riêng từng chi nhánh bằng PUT /branches/{branchId}/products/{branchProductId}. " +
                    "Có thể đính kèm ảnh ngay khi tạo qua part 'files' (tùy chọn, tối đa 10 ảnh).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tạo sản phẩm thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền tạo")
    })
    @PostMapping(value = "/shops/{shopId}/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(Permission.PRODUCT_CREATE)
    public ResponseEntity<ApiResponseDto<ProductResponse>> create(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "Danh sách ID chi nhánh (tùy chọn, mặc định: tất cả chi nhánh)")
            @RequestParam(required = false) List<String> branchIds,
            @RequestPart("product") @Valid ProductRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        // Upload ảnh trước nếu có, gắn URL vào request
        if (files != null && !files.isEmpty()) {
            List<String> imageUrls = files.stream()
                    .map(f -> fileUploadService.upload(f, "products/" + shopId))
                    .collect(java.util.stream.Collectors.toList());
            request.setImages(imageUrls);
        }
        ProductResponse response = productService.createProduct(shopId, branchIds, request);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_CREATED, response));
    }

    // ─────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Lấy danh sách sản phẩm với phân trang",
            description = "Lấy tất cả sản phẩm của shop, tùy chọn lọc theo chi nhánh.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Trả về danh sách sản phẩm")})
    @GetMapping("/shops/{shopId}/products")
    @RequirePermission(Permission.PRODUCT_VIEW)
    public ResponseEntity<ApiResponseDto<Page<ProductResponse>>> getAll(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID chi nhánh (tùy chọn)") @RequestParam(required = false) String branchId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductResponse> response = productCache.getAllByShop(shopId, branchId, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_LIST, response));
    }

    @Operation(summary = "Tìm kiếm sản phẩm nâng cao",
            description = "Tìm kiếm theo keyword (name/SKU/barcode), category, khoảng giá, trạng thái active. " +
                    "Hỗ trợ lọc theo chi nhánh cụ thể hoặc toàn shop.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Trả về kết quả tìm kiếm")})
    @GetMapping("/shops/{shopId}/products/search")
    @RequirePermission(Permission.PRODUCT_VIEW)
    public ResponseEntity<ApiResponseDto<Page<ProductResponse>>> search(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @ModelAttribute ProductSearchRequest request,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductResponse> response = productService.searchProducts(shopId, request.getBranchId(), request, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_SEARCH_RESULTS, response));
    }

    @Operation(summary = "Lấy chi tiết sản phẩm tại một chi nhánh cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về chi tiết sản phẩm"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm")
    })
    @GetMapping("/shops/{shopId}/branches/{branchId}/products/{id}")
    @RequirePermission(Permission.PRODUCT_VIEW)
    public ResponseEntity<ApiResponseDto<ProductResponse>> getById(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID chi nhánh") @PathVariable String branchId,
            @Parameter(description = "ID sản phẩm (BranchProduct ID)") @PathVariable String id) {
        ProductResponse response = productService.getProduct(shopId, branchId, id);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_FOUND, response));
    }

    // ─────────────────────────────────────────────────────────────────
    // UPDATE — Product (thông tin chung)
    // ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Cập nhật thông tin chung của sản phẩm",
            description = "Chỉ cập nhật các trường thuộc Product (tên, SKU, barcode, giá mặc định, mô tả...). " +
                    "Không ảnh hưởng đến giá/tồn kho từng chi nhánh. " +
                    "Dùng PUT /branches/{branchId}/products/{branchProductId} để cập nhật từng chi nhánh. " +
                    "Có thể thay toàn bộ ảnh bằng cách đính kèm part 'files' (tùy chọn). " +
                    "Nếu không gửi 'files', danh sách ảnh trong JSON sẽ được giữ nguyên.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền cập nhật"),
            @ApiResponse(responseCode = "404", description = "Sản phẩm không tồn tại")
    })
    @PutMapping(value = "/shops/{shopId}/products/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(Permission.PRODUCT_UPDATE)
    public ResponseEntity<ApiResponseDto<ProductResponse>> update(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID sản phẩm (Product ID)") @PathVariable String productId,
            @RequestPart("product") @Valid ProductRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        // Nếu có file mới → upload lên S3, ảnh cũ sẽ được thay thế bởi danh sách mới
        if (files != null && !files.isEmpty()) {
            List<String> imageUrls = files.stream()
                    .map(f -> fileUploadService.upload(f, "products/" + shopId + "/" + productId))
                    .collect(java.util.stream.Collectors.toList());
            request.setImages(imageUrls);
        }
        ProductResponse response = productService.updateProduct(user.getId(), shopId, productId, request);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_UPDATED, response));
    }

    // ─────────────────────────────────────────────────────────────────
    // UPDATE — BranchProduct (thông tin riêng tại từng chi nhánh)
    // ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Cập nhật thông tin sản phẩm tại một chi nhánh",
            description = "Cập nhật các trường thuộc BranchProduct: giá bán, giá nhập, tồn kho, " +
                    "giảm giá, hạn sử dụng... Chỉ ảnh hưởng đến chi nhánh được chỉ định.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền cập nhật"),
            @ApiResponse(responseCode = "404", description = "Sản phẩm hoặc chi nhánh không tồn tại")
    })
    @PutMapping("/shops/{shopId}/branches/{branchId}/products/{branchProductId}")
    @RequirePermission(Permission.PRODUCT_UPDATE)
    public ResponseEntity<ApiResponseDto<ProductResponse>> updateBranchProduct(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID chi nhánh") @PathVariable String branchId,
            @Parameter(description = "ID BranchProduct") @PathVariable String branchProductId,
            @Valid @RequestBody BranchProductRequest request) {
        ProductResponse response = productService.updateBranchProduct(user.getId(), shopId, branchId, branchProductId, request);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_UPDATED, response));
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Xóa sản phẩm khỏi toàn bộ shop",
            description = "Xóa mềm Product và tất cả BranchProduct liên quan ở mọi chi nhánh. " +
                    "Dùng khi ngừng kinh doanh sản phẩm hoàn toàn. " +
                    "Để tạm dừng bán tại một chi nhánh, dùng toggle-active thay vì xóa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm"),
            @ApiResponse(responseCode = "403", description = "Không có quyền xóa")
    })
    @DeleteMapping("/shops/{shopId}/products/{productId}")
    @RequirePermission(Permission.PRODUCT_DELETE)
    public ResponseEntity<ApiResponseDto<Void>> deleteFromShop(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID sản phẩm (Product ID)") @PathVariable String productId) {
        productService.deleteProductFromShop(user.getId(), shopId, productId);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_DELETED, null));
    }

    // ─────────────────────────────────────────────────────────────────
    // SUGGEST (SKU / Barcode)
    // ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Lấy mã SKU gợi ý cho sản phẩm")
    @GetMapping("/shops/{shopId}/suggested-sku")
    public ResponseEntity<ApiResponseDto<String>> getSuggestedSku(
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "Ngành hàng") @RequestParam String industry,
            @Parameter(description = "Danh mục") @RequestParam String category) {
        String suggestedCode = productService.getSuggestedSku(shopId, industry, category);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.SUCCESS, suggestedCode));
    }

    @Operation(summary = "Lấy mã vạch gợi ý cho sản phẩm")
    @GetMapping("/shops/{shopId}/suggested-barcode")
    public ResponseEntity<ApiResponseDto<String>> getSuggestedBarcode(
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "Ngành hàng") @RequestParam String industry,
            @Parameter(description = "Danh mục") @RequestParam String category) {
        String suggestedCode = productService.getSuggestedBarcode(shopId, industry, category);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.SUCCESS, suggestedCode));
    }

    // ─────────────────────────────────────────────────────────────────
    // IMAGE UPLOAD / DELETE
    // ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Upload ảnh cho sản phẩm",
            description = "Upload một hoặc nhiều ảnh lên S3 và gắn URL vào sản phẩm. " +
                    "Tối đa 10 ảnh/sản phẩm. Định dạng hỗ trợ: JPEG, JPG, PNG, WEBP. Kích thước tối đa: 5MB/ảnh.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upload ảnh thành công"),
            @ApiResponse(responseCode = "400", description = "File không hợp lệ hoặc vượt giới hạn ảnh"),
            @ApiResponse(responseCode = "403", description = "Không có quyền cập nhật"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm")
    })
    @PostMapping(value = "/shops/{shopId}/products/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(Permission.PRODUCT_UPDATE)
    public ResponseEntity<ApiResponseDto<List<String>>> uploadProductImages(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID sản phẩm (Product ID)") @PathVariable String productId,
            @Parameter(description = "Danh sách file ảnh (tối đa 10 ảnh, mỗi ảnh ≤ 5MB)")
            @RequestParam("files") List<MultipartFile> files) {
        List<String> imageUrls = productService.uploadProductImages(user.getId(), shopId, productId, files);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_IMAGE_UPLOADED, imageUrls));
    }

    @Operation(summary = "Xóa một ảnh khỏi sản phẩm",
            description = "Xóa ảnh khỏi S3 và danh sách ảnh của sản phẩm theo URL. " +
                    "Trả về danh sách URL ảnh còn lại sau khi xóa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xóa ảnh thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền cập nhật"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm hoặc ảnh")
    })
    @DeleteMapping("/shops/{shopId}/products/{productId}/images")
    @RequirePermission(Permission.PRODUCT_UPDATE)
    public ResponseEntity<ApiResponseDto<List<String>>> deleteProductImage(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID sản phẩm (Product ID)") @PathVariable String productId,
            @Parameter(description = "URL đầy đủ của ảnh cần xóa") @RequestParam String imageUrl) {
        List<String> remainingImages = productService.deleteProductImage(user.getId(), shopId, productId, imageUrl);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_IMAGE_DELETED, remainingImages));
    }
}
