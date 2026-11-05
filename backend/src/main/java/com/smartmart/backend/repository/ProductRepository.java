package com.smartmart.backend.repository;

import com.smartmart.backend.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE " +
           "(:tuKhoa IS NULL OR LOWER(p.tenSanPham) LIKE LOWER(CONCAT('%', :tuKhoa, '%'))) AND " +
           "(:maDanhMuc IS NULL OR p.maDanhMuc = :maDanhMuc) AND " +
           "(:giaToiThieu IS NULL OR p.giaBan >= :giaToiThieu) AND " +
           "(:giaToiDa IS NULL OR p.giaBan <= :giaToiDa)")
    Page<Product> timKiemVaLoc(
            @Param("tuKhoa") String tuKhoa,
            @Param("maDanhMuc") Long maDanhMuc,
            @Param("giaToiThieu") Double giaToiThieu,
            @Param("giaToiDa") Double giaToiDa,
            Pageable pageable
    );

    // Gợi ý sản phẩm cùng danh mục (khác sản phẩm đang xem)
    List<Product> findTop6ByMaDanhMucAndMaSanPhamNot(Long maDanhMuc, Long maSanPham);
}