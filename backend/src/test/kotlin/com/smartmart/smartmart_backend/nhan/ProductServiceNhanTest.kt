
package com.smartmart.smartmart_backend.nhan

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
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
            moTa = "Điện thoại thông minh cao cấp",
            isActive = true
        )
    }

    @Nested
    @DisplayName("SWA-17: Tìm kiếm & Lọc sản phẩm")
    inner class Sw17SearchAndFilterTests {

        @Test
        @DisplayName("1. Tìm kiếm và lọc với tham số hợp lệ (Happy Path)")
        fun test01_TimKiemThanhCong() {
            val pageable = PageRequest.of(0, 10)

            `when`(
                productRepository.timKiemVaLoc(
                    "iPhone",
                    10L,
                    10000000.0,
                    30000000.0,
                    pageable
                )
            ).thenReturn(PageImpl(listOf(sanPhamMau)))

            val result = productService.timKiemSanPham(
                "iPhone", 10L, 10000000.0, 30000000.0, 0, 10
            )

            assertNotNull(result)
            assertEquals(1, result.totalElements)
            assertEquals("Điện thoại iPhone 15 Pro", result.content[0].tenSanPham)
        }

        @Test
        @DisplayName("2. Xử lý khoảng trắng từ khóa (Trimming & Empty string)")
        fun test02_TuKhoaRongHoacKhoangTrang() {
            val pageable = PageRequest.of(0, 10)

            `when`(
                productRepository.timKiemVaLoc(
                    null,
                    null,
                    null,
                    null,
                    pageable
                )
            ).thenReturn(PageImpl(listOf(sanPhamMau)))

            val result = productService.timKiemSanPham(
                "   ", null, null, null, 0, 10
            )

            assertNotNull(result)
            assertEquals(1, result.totalElements)

            verify(productRepository).timKiemVaLoc(
                null,
                null,
                null,
                null,
                pageable
            )
        }

        @Test
        @DisplayName("3. Ngoại lệ khi Giá tối thiểu lớn hơn Giá tối đa (Validation Error)")
        fun test03_GiaMinLonHonGiaMax() {
            val exception = assertThrows(IllegalArgumentException::class.java) {
                productService.timKiemSanPham(
                    null, null, 50000000.0, 10000000.0, 0, 10
                )
            }

            assertEquals(
                "Giá tối thiểu không được lớn hơn giá tối đa",
                exception.message
            )

            verifyNoInteractions(productRepository)
        }

        @Test
        @DisplayName("4. Trường hợp không tìm thấy kết quả nào (Empty List)")
        fun test04_KhongTimThayKetQua() {
            val pageable = PageRequest.of(0, 10)

            `when`(
                productRepository.timKiemVaLoc(
                    "Laptop",
                    null,
                    null,
                    null,
                    pageable
                )
            ).thenReturn(PageImpl(emptyList()))

            val result = productService.timKiemSanPham(
                "Laptop", null, null, null, 0, 10
            )

            assertNotNull(result)
            assertTrue(result.isEmpty)
        }
    }

    @Nested
    @DisplayName("SWA-36: Xem chi tiết sản phẩm")
    inner class Sw36GetDetailTests {

        @Test
        @DisplayName("5. Lấy chi tiết thành công với ID hợp lệ (Happy Path)")
        fun test05_LayChiTietThanhCong() {
            `when`(productRepository.findById(1L))
                .thenReturn(Optional.of(sanPhamMau))

            val result = productService.layChiTietSanPhamTheoId(1L)

            assertNotNull(result)
            assertEquals(1L, result?.maSanPham)
            assertEquals("Điện thoại iPhone 15 Pro", result?.tenSanPham)
        }

        @Test
        @DisplayName("6. Trả về null khi ID không tồn tại trong CSDL")
        fun test06_IDKhongTonTai() {
            `when`(productRepository.findById(999L))
                .thenReturn(Optional.empty())

            val result = productService.layChiTietSanPhamTheoId(999L)

            assertNull(result)
        }

        @Test
        @DisplayName("7. Ngoại lệ khi ID bằng null hoặc âm (Validation Error)")
        fun test07_IDKhongHopLe() {
            val ex1 = assertThrows(IllegalArgumentException::class.java) {
                productService.layChiTietSanPhamTheoId(null)
            }

            val ex2 = assertThrows(IllegalArgumentException::class.java) {
                productService.layChiTietSanPhamTheoId(-1L)
            }

            assertEquals("Mã sản phẩm không hợp lệ", ex1.message)
            assertEquals("Mã sản phẩm không hợp lệ", ex2.message)

            verifyNoInteractions(productRepository)
        }

        @Test
        @DisplayName("8. Kiểm tra trạng thái kích hoạt sản phẩm (IsActive)")
        fun test08_KiemTraIsActive() {
            `when`(productRepository.findById(1L))
                .thenReturn(Optional.of(sanPhamMau))

            val result = productService.layChiTietSanPhamTheoId(1L)

            assertEquals(true, result?.isActive)
        }
    }
}
