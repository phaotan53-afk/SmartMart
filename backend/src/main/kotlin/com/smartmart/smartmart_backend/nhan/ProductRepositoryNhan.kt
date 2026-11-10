package com.smartmart.smartmart_backend.nhan

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ProductRepositoryNhan : JpaRepository<ProductNhan, Long> {

    @Query(
        """
        SELECT p FROM ProductNhan p 
        WHERE (:tuKhoa IS NULL OR LOWER(p.tenSanPham) LIKE LOWER(CONCAT('%', :tuKhoa, '%')))
          AND (:maDanhMuc IS NULL OR p.maDanhMuc = :maDanhMuc)
          AND (:giaMin IS NULL OR p.giaBan >= :giaMin)
          AND (:giaMax IS NULL OR p.giaBan <= :giaMax)
        """
    )
    fun timKiemVaLoc(
        @Param("tuKhoa") tuKhoa: String?,
        @Param("maDanhMuc") maDanhMuc: Long?,
        @Param("giaMin") giaMin: Double?,
        @Param("giaMax") giaMax: Double?,
        pageable: Pageable
    ): Page<ProductNhan>
}