package com.smartmart.smartmart_backend.nhan

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.springframework.data.domain.PageImpl
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ProductServiceNhanTest {

    @Mock
    private lateinit var productRepository: ProductRepositoryNhan

    @InjectMocks
    private lateinit var productService: ProductServiceNhan

    private lateinit var sanPhamMau: ProductNhan

    @BeforeEach
    fun thietLapDuLieuMau() {
        sanPhamMau = ProductNhan(
            maSanPham = 1L,
            tenSanPham = "Điện thoại iPhone 15 Pro",
            giaBan = 25000000.0,
            maDanhMuc = 10L,
            soLuongTon = 50,
            danhGia = 4.8,
            moTa = "Điện thoại thông minh cao cấp"
        )
    }

    @Test
    @DisplayName("Test 01: Tìm kiếm với từ khóa hợp lệ")
    fun test01_TimKiemTuKhoaHopLe() {
        val trangMoc = PageImpl(listOf(sanPhamMau))
        `when`(productRepository.timKiemVaLoc(eq("iPhone"), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(trangMoc)

        val ketQua = productService.timKiemSanPham("iPhone", null, null, null, 0, 10)
        assertNotNull(ketQua)
        assertEquals(1, ketQua.totalElements)
    }

    @Test
    @DisplayName("Test 26: Lấy chi tiết sản phẩm với ID hợp lệ")
    fun test26_LayChiTietIDHopLe() {
        `when`(productRepository.findById(1L)).thenReturn(Optional.of(sanPhamMau))

        val ketQua = productService.layChiTietSanPhamTheoId(1L)
        assertNotNull(ketQua)
        assertEquals("Điện thoại iPhone 15 Pro", ketQua?.tenSanPham)
    }

    @Test
    @DisplayName("Test 28: Ngoại lệ khi ID bằng null")
    fun test28_NgoaiLeIDNull() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            productService.layChiTietSanPhamTheoId(null)
        }
        assertEquals("Mã sản phẩm không hợp lệ", exception.message)
    }
}