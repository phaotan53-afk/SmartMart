package com.smartmart.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;               // Mã sản phẩm
    private String tenSanPham;     // Tên sản phẩm
    private Double giaBan;         // Giá bán
    private Long maDanhMuc;        // Mã danh mục
    private String moTa;           // Mô tả sản phẩm
    private Integer soLuongTon;    // Số lượng tồn kho
    private Double danhGia;        // Đánh giá sao (1.0 - 5.0)
}