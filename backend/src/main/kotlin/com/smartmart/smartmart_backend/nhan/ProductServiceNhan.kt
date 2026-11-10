package com.smartmart.smartmart_backend.nhan

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class ProductServiceNhan(
    private val productRepository: ProductRepositoryNhan
) {
    fun timKiemSanPham(
        tuKhoa: String?,
        maDanhMuc: Long?,
        giaMin: Double?,
        giaMax: Double?,
        page: Int,
        size: Int
    ): Page<ProductNhan> {
        if (giaMin != null && giaMax != null && giaMin > giaMax) {
            throw IllegalArgumentException("Giá tối thiểu không được lớn hơn giá tối đa")
        }
        val tuKhoaChuanHoa = tuKhoa?.trim()?.ifEmpty { null }
        val pageable = PageRequest.of(page, size)
        return productRepository.timKiemVaLoc(tuKhoaChuanHoa, maDanhMuc, giaMin, giaMax, pageable)
    }

    fun layChiTietSanPhamTheoId(maSanPham: Long?): ProductNhan? {
        if (maSanPham == null || maSanPham <= 0) {
            throw IllegalArgumentException("Mã sản phẩm không hợp lệ")
        }
        return productRepository.findById(maSanPham).orElse(null)
    }
}