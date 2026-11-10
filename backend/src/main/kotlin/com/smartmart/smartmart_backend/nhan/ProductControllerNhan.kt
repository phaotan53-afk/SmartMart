package com.smartmart.smartmart_backend.nhan

import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/nhan/products")
class ProductControllerNhan(
    private val productService: ProductServiceNhan
) {
    @GetMapping("/search")
    fun timKiemSanPham(
        @RequestParam(required = false) tuKhoa: String?,
        @RequestParam(required = false) maDanhMuc: Long?,
        @RequestParam(required = false) giaMin: Double?,
        @RequestParam(required = false) giaMax: Double?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Page<ProductNhan>> {
        val result = productService.timKiemSanPham(tuKhoa, maDanhMuc, giaMin, giaMax, page, size)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{id}")
    fun layChiTietSanPhamTheoId(@PathVariable id: Long): ResponseEntity<ProductNhan> {
        val product = productService.layChiTietSanPhamTheoId(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(product)
    }
}