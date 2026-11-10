package com.smartmart.smartmart_backend.nhan

import jakarta.persistence.*

@Entity
@Table(name = "products")
data class ProductNhan(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_san_pham")
    var maSanPham: Long? = null,

    @Column(name = "ten_san_pham", nullable = false)
    var tenSanPham: String = "",

    @Column(name = "gia_ban", nullable = false)
    var giaBan: Double = 0.0,

    @Column(name = "ma_danh_muc")
    var maDanhMuc: Long? = null,

    @Column(name = "mo_ta")
    var moTa: String? = null,

    @Column(name = "so_luong_ton")
    var soLuongTon: Int = 0,

    @Column(name = "danh_gia")
    var danhGia: Double? = 0.0
)