
package com.smartmart.smartmart_backend.nhan

import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/nhan/products")
class ProductControllerNhan(
    private val productService: ProductServiceNhan
) {

    // API tìm kiếm và lọc sản phẩm
    @GetMapping("/search")
    fun timKiemSanPham(
        @RequestParam(required = false) tuKhoa: String?,
        @RequestParam(required = false) maDanhMuc: Long?,
        @RequestParam(required = false) giaMin: Double?,
        @RequestParam(required = false) giaMax: Double?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Page<ProductNhan>> {

        // Kiểm tra số trang và kích thước trang
        if (page < 0 || size < 1 || size > 100) {
            return ResponseEntity.badRequest().build()
        }

        // Gọi Service xử lý nghiệp vụ
        val result = productService.timKiemSanPham(
            tuKhoa,
            maDanhMuc,
            giaMin,
            giaMax,
            page,
            size
        )

        return ResponseEntity.ok(result)
    }

    // API xem chi tiết sản phẩm theo ID
    @GetMapping("/{id}")
    fun layChiTietSanPhamTheoId(
        @PathVariable id: Long
    ): ResponseEntity<ProductNhan> {

        // Tìm sản phẩm theo ID
        val product = productService.layChiTietSanPhamTheoId(id)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(product)
    }
}
