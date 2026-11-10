package com.smartmart.smartmart_backend.nhan

import jakarta.persistence.*

@Entity
@Table(name = "Products")
data class ProductNhan(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductID")
    var maSanPham: Long? = null,

    @Column(name = "ProductName", nullable = false)
    var tenSanPham: String = "",

    @Column(name = "Price", nullable = false)
    var giaBan: Double = 0.0,

    @Column(name = "CategoryID")
    var maDanhMuc: Long? = null,

    @Column(name = "Description")
    var moTa: String? = null,

    @Column(name = "IsActive")
    var isActive: Boolean? = true
)