package com.smartmart.backend.controller;

import com.smartmart.backend.model.Product;
import com.smartmart.backend.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // Endpoint SWA-17: API Tìm kiếm & Lọc
    @GetMapping("/search")
    public ResponseEntity<Page<Product>> timKiemSanPham(
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) Long maDanhMuc,
            @RequestParam(required = false) Double giaToiThieu,
            @RequestParam(required = false) Double giaToiDa,
            @RequestParam(defaultValue = "0") int trang,
            @RequestParam(defaultValue = "10") int kichThuoc) {
        
        Page<Product> ketQua = productService.timKiemSanPham(tuKhoa, maDanhMuc, giaToiThieu, giaToiDa, trang, kichThuoc);
        return ResponseEntity.ok(ketQua);
    }

    // Endpoint SWA-36: API Xem chi tiết sản phẩm
    @GetMapping("/{id}")
    public ResponseEntity<Product> layChiTietSanPhamTheoId(@PathVariable Long id) {
        return productService.layChiTietSanPhamTheoId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}