package com.smartmart.backend.service;

import com.smartmart.backend.model.Product;
import com.smartmart.backend.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product sanPhamMau;

    @BeforeEach
    void thietLapDuLieuMau() {
        sanPhamMau = Product.builder()
                .id(1L)
                .tenSanPham("Điện thoại iPhone 15 Pro")
                .giaBan(25000000.0)
                .maDanhMuc(10L)
                .moTa("Điện thoại thông minh cao cấp")
                .soLuongTon(50)
                .danhGia(4.8)
                .build();
    }

    @Test
    @DisplayName("Test 01: Tìm kiếm với từ khóa hợp lệ")
    void test01_TimKiemTuKhoaHopLe() {
        Page<Product> trangMoc = new PageImpl<>(List.of(sanPhamMau));
        when(productRepository.timKiemVaLoc(eq("iPhone"), any(), any(), any(), any())).thenReturn(trangMoc);

        Page<Product> ketQua = productService.timKiemSanPham("iPhone", null, null, null, 0, 10);
        assertNotNull(ketQua);
        assertEquals(1, ketQua.getTotalElements());
    }

    @Test
    @DisplayName("Test 02: Tìm kiếm với từ khóa có khoảng trắng thừa")
    void test02_TimKiemTuKhoaCoKhoangTrang() {
        Page<Product> trangMoc = new PageImpl<>(List.of(sanPhamMau));
        when(productRepository.timKiemVaLoc(eq("iPhone"), any(), any(), any(), any())).thenReturn(trangMoc);

        Page<Product> ketQua = productService.timKiemSanPham("  iPhone  ", null, null, null, 0, 10);
        assertNotNull(ketQua);
        verify(productRepository).timKiemVaLoc(eq("iPhone"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Test 03: Tìm kiếm khi từ khóa null")
    void test03_TimKiemTuKhoaNull() {
        Page<Product> trangMoc = new PageImpl<>(List.of(sanPhamMau));
        when(productRepository.timKiemVaLoc(isNull(), any(), any(), any(), any())).thenReturn(trangMoc);

        Page<Product> ketQua = productService.timKiemSanPham(null, null, null, null, 0, 10);
        assertNotNull(ketQua);
    }

    @Test
    @DisplayName("Test 04: Tìm kiếm khi từ khóa rỗng")
    void test04_TimKiemTuKhoaRong() {
        Page<Product> trangMoc = new PageImpl<>(List.of(sanPhamMau));
        when(productRepository.timKiemVaLoc(isNull(), any(), any(), any(), any())).thenReturn(trangMoc);

        Page<Product> ketQua = productService.timKiemSanPham("   ", null, null, null, 0, 10);
        assertNotNull(ketQua);
    }

    @Test
    @DisplayName("Test 05: Lọc theo danh mục hợp lệ")
    void test05_LocTheoDanhMuc() {
        Page<Product> trangMoc = new PageImpl<>(List.of(sanPhamMau));
        when(productRepository.timKiemVaLoc(any(), eq(10L), any(), any(), any())).thenReturn(trangMoc);

        Page<Product> ketQua = productService.timKiemSanPham(null, 10L, null, null, 0, 10);
        assertEquals(1, ketQua.getTotalElements());
    }

    @Test
    @DisplayName("Test 06: Lọc theo khoảng giá hợp lệ")
    void test06_LocTheoKhoangGia() {
        Page<Product> trangMoc = new PageImpl<>(List.of(sanPhamMau));
        when(productRepository.timKiemVaLoc(any(), any(), eq(20000000.0), eq(30000000.0), any())).thenReturn(trangMoc);

        Page<Product> ketQua = productService.timKiemSanPham(null, null, 20000000.0, 30000000.0, 0, 10);
        assertEquals(1, ketQua.getTotalElements());
    }

    @Test
    @DisplayName("Test 07: Ngoại lệ khi giá tối thiểu lớn hơn giá tối đa")
    void test07_NgoaiLeGiaMinLonHonGiaMax() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            productService.timKiemSanPham(null, null, 30000000.0, 20000000.0, 0, 10)
        );
        assertEquals("Giá tối thiểu không được lớn hơn giá tối đa", exception.getMessage());
    }

    @Test
    @DisplayName("Test 08: Lọc khi giá tối thiểu và tối đa bằng nhau")
    void test08_GiaMinBangGiaMax() {
        Page<Product> trangMoc = new PageImpl<>(List.of(sanPhamMau));
        when(productRepository.timKiemVaLoc(any(), any(), eq(25000000.0), eq(25000000.0), any())).thenReturn(trangMoc);

        Page<Product> ketQua = productService.timKiemSanPham(null, null, 25000000.0, 25000000.0, 0, 10);
        assertNotNull(ketQua);
    }

    @Test
    @DisplayName("Test 09: Kết hợp từ khóa, danh mục và khoảng giá")
    void test09_KetHopTatCaDieuKien() {
        Page<Product> trangMoc = new PageImpl<>(List.of(sanPhamMau));
        when(productRepository.timKiemVaLoc(eq("iPhone"), eq(10L), eq(20000000.0), eq(30000000.0), any())).thenReturn(trangMoc);

        Page<Product> ketQua = productService.timKiemSanPham("iPhone", 10L, 20000000.0, 30000000.0, 0, 10);
        assertEquals(1, ketQua.getTotalElements());
    }

    @Test
    @DisplayName("Test 10: Tìm kiếm không có kết quả trả về trang rỗng")
    void test10_TimKiemKhongCoKetQua() {
        Page<Product> trangRong = new PageImpl<>(Collections.emptyList());
        when(productRepository.timKiemVaLoc(eq("Laptop"), any(), any(), any(), any())).thenReturn(trangRong);

        Page<Product> ketQua = productService.timKiemSanPham("Laptop", null, null, null, 0, 10);
        assertTrue(ketQua.isEmpty());
    }

    @Test
    @DisplayName("Test 11: Kiểm tra phân trang - Trang 0, Kích thước 5")
    void test11_PhanTrangHopLe() {
        Page<Product> trangMoc = new PageImpl<>(List.of(sanPhamMau));
        when(productRepository.timKiemVaLoc(any(), any(), any(), any(), eq(PageRequest.of(0, 5)))).thenReturn(trangMoc);

        Page<Product> ketQua = productService.timKiemSanPham(null, null, null, null, 0, 5);
        assertNotNull(ketQua);
    }

    @Test
    @DisplayName("Test 12: Lọc theo danh mục không tồn tại")
    void test12_DanhMucKhongTonTai() {
        Page<Product> trangRong = new PageImpl<>(Collections.emptyList());
        when(productRepository.timKiemVaLoc(any(), eq(999L), any(), any(), any())).thenReturn(trangRong);

        Page<Product> ketQua = productService.timKiemSanPham(null, 999L, null, null, 0, 10);
        assertEquals(0, ketQua.getTotalElements());
    }

    @Test
    @DisplayName("Test 13: Từ khóa viết hoa hoặc viết thường đều tìm được")
    void test13_TuKhoaKhongPhanBietHoaThuong() {
        Page<Product> trangMoc = new PageImpl<>(List.of(sanPhamMau));
        when(productRepository.timKiemVaLoc(eq("IPHONE"), any(), any(), any(), any())).thenReturn(trangMoc);

        Page<Product> ketQua = productService.timKiemSanPham("IPHONE", null, null, null, 0, 10);
        assertEquals(1, ketQua.getTotalElements());
    }

    @Test
    @DisplayName("Test 14: Lọc với giá tối thiểu bằng 0")
    void test14_GiaMinBang0() {
        Page<Product> trangMoc = new PageImpl<>(List.of(sanPhamMau));
        when(productRepository.timKiemVaLoc(any(), any(), eq(0.0), any(), any())).thenReturn(trangMoc);

        Page<Product> ketQua = productService.timKiemSanPham(null, null, 0.0, null, 0, 10);
        assertNotNull(ketQua);
    }

    @Test
    @DisplayName("Test 15: Tìm kiếm với từ khóa ký tự đặc biệt")
    void test15_TuKhoaKyTuDacBiet() {
        when(productRepository.timKiemVaLoc(eq("@Class"), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        Page<Product> ketQua = productService.timKiemSanPham("@Class", null, null, null, 0, 10);

        assertNotNull(ketQua);
        assertEquals(0, ketQua.getTotalElements());
    }

    @Test @DisplayName("Test 16: Tìm kiếm với từ khóa siêu dài") void test16() {
        when(productRepository.timKiemVaLoc(any(), any(), any(), any(), any())).thenReturn(Page.empty());
        assertNotNull(productService.timKiemSanPham("A".repeat(100), null, null, null, 0, 10));
    }
    @Test @DisplayName("Test 17: Lọc giá tối đa bằng 0") void test17() {
        when(productRepository.timKiemVaLoc(any(), any(), any(), eq(0.0), any())).thenReturn(Page.empty());
        assertNotNull(productService.timKiemSanPham(null, null, null, 0.0, 0, 10));
    }
    @Test @DisplayName("Test 18: Lọc mã danh mục bằng 0") void test18() {
        when(productRepository.timKiemVaLoc(any(), eq(0L), any(), any(), any())).thenReturn(Page.empty());
        assertNotNull(productService.timKiemSanPham(null, 0L, null, null, 0, 10));
    }
    @Test @DisplayName("Test 19: Từ khóa 1 ký tự") void test19() {
        when(productRepository.timKiemVaLoc(eq("a"), any(), any(), any(), any())).thenReturn(Page.empty());
        assertNotNull(productService.timKiemSanPham("a", null, null, null, 0, 10));
    }
    @Test @DisplayName("Test 20: Phân trang trang số 2") void test20() {
        when(productRepository.timKiemVaLoc(any(), any(), any(), any(), eq(PageRequest.of(2, 20)))).thenReturn(Page.empty());
        assertNotNull(productService.timKiemSanPham(null, null, null, null, 2, 20));
    }
    @Test @DisplayName("Test 21: Từ khóa chứa số") void test21() {
        when(productRepository.timKiemVaLoc(eq("15"), any(), any(), any(), any())).thenReturn(Page.empty());
        assertNotNull(productService.timKiemSanPham("15", null, null, null, 0, 10));
    }
    @Test @DisplayName("Test 22: Giá tối thiểu null và giá tối đa có giá trị") void test22() {
        when(productRepository.timKiemVaLoc(any(), any(), isNull(), eq(500.0), any())).thenReturn(Page.empty());
        assertNotNull(productService.timKiemSanPham(null, null, null, 500.0, 0, 10));
    }
    @Test @DisplayName("Test 23: Giá tối thiểu có giá trị và giá tối đa null") void test23() {
        when(productRepository.timKiemVaLoc(any(), any(), eq(100.0), isNull(), any())).thenReturn(Page.empty());
        assertNotNull(productService.timKiemSanPham(null, null, 100.0, null, 0, 10));
    }
    @Test @DisplayName("Test 24: Kích thước trang bằng 100") void test24() {
        when(productRepository.timKiemVaLoc(any(), any(), any(), any(), eq(PageRequest.of(0, 100)))).thenReturn(Page.empty());
        assertNotNull(productService.timKiemSanPham(null, null, null, null, 0, 100));
    }
    @Test @DisplayName("Test 25: Từ khóa tiếng Việt có dấu") void test25() {
        when(productRepository.timKiemVaLoc(eq("Điện thoại"), any(), any(), any(), any())).thenReturn(Page.empty());
        assertNotNull(productService.timKiemSanPham("Điện thoại", null, null, null, 0, 10));
    }

    @Test
    @DisplayName("Test 26: Lấy chi tiết sản phẩm với ID hợp lệ")
    void test26_LayChiTietIDHopLe() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sanPhamMau));

        Optional<Product> ketQua = productService.layChiTietSanPhamTheoId(1L);
        assertTrue(ketQua.isPresent());
        assertEquals("Điện thoại iPhone 15 Pro", ketQua.get().getTenSanPham());
    }

    @Test
    @DisplayName("Test 27: Trả về empty khi ID không tồn tại")
    void test27_IDKhongTonTai() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Product> ketQua = productService.layChiTietSanPhamTheoId(999L);
        assertFalse(ketQua.isPresent());
    }

    @Test
    @DisplayName("Test 28: Ngoại lệ khi ID bằng null")
    void test28_NgoaiLeIDNull() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            productService.layChiTietSanPhamTheoId(null)
        );
        assertEquals("Mã sản phẩm không hợp lệ", exception.getMessage());
    }

    @Test
    @DisplayName("Test 29: Ngoại lệ khi ID bằng 0")
    void test29_NgoaiLeIDBang0() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            productService.layChiTietSanPhamTheoId(0L)
        );
        assertEquals("Mã sản phẩm không hợp lệ", exception.getMessage());
    }

    @Test
    @DisplayName("Test 30: Ngoại lệ khi ID âm")
    void test30_NgoaiLeIDAm() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            productService.layChiTietSanPhamTheoId(-5L)
        );
        assertEquals("Mã sản phẩm không hợp lệ", exception.getMessage());
    }

    @Test @DisplayName("Test 31: Kiểm tra ID lớn nhất Long") void test31() {
        when(productRepository.findById(Long.MAX_VALUE)).thenReturn(Optional.empty());
        assertFalse(productService.layChiTietSanPhamTheoId(Long.MAX_VALUE).isPresent());
    }
    @Test @DisplayName("Test 32: Kiểm tra thông tin giá bán sản phẩm") void test32() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sanPhamMau));
        assertEquals(25000000.0, productService.layChiTietSanPhamTheoId(1L).get().getGiaBan());
    }
    @Test @DisplayName("Test 33: Kiểm tra số lượng tồn kho") void test33() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sanPhamMau));
        assertEquals(50, productService.layChiTietSanPhamTheoId(1L).get().getSoLuongTon());
    }
    @Test @DisplayName("Test 34: Kiểm tra điểm đánh giá") void test34() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sanPhamMau));
        assertEquals(4.8, productService.layChiTietSanPhamTheoId(1L).get().getDanhGia());
    }
    @Test @DisplayName("Test 35: Kiểm tra thông tin mô tả") void test35() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sanPhamMau));
        assertNotNull(productService.layChiTietSanPhamTheoId(1L).get().getMoTa());
    }
    @Test @DisplayName("Test 36: Kiểm tra mã danh mục") void test36() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sanPhamMau));
        assertEquals(10L, productService.layChiTietSanPhamTheoId(1L).get().getMaDanhMuc());
    }
    @Test @DisplayName("Test 37: Lấy chi tiết sản phẩm với ID = 1") void test37() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sanPhamMau));
        assertTrue(productService.layChiTietSanPhamTheoId(1L).isPresent());
    }
    @Test @DisplayName("Test 38: Kiểm tra sản phẩm hết hàng (Tồn kho = 0)") void test38() {
        Product hetHang = Product.builder().id(2L).tenSanPham("Áo").soLuongTon(0).build();
        when(productRepository.findById(2L)).thenReturn(Optional.of(hetHang));
        assertEquals(0, productService.layChiTietSanPhamTheoId(2L).get().getSoLuongTon());
    }
    @Test @DisplayName("Test 39: Kiểm tra hàm findById được gọi đúng 1 lần") void test39() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sanPhamMau));
        productService.layChiTietSanPhamTheoId(1L);
        verify(productRepository, times(1)).findById(1L);
    }
    @Test @DisplayName("Test 40: Kiểm tra ID = 100L") void test40() {
        when(productRepository.findById(100L)).thenReturn(Optional.empty());
        assertFalse(productService.layChiTietSanPhamTheoId(100L).isPresent());
    }
    // --- UNIT TEST CHO SWA-82: Lấy danh sách sản phẩm gợi ý ---

    @Test
    void testLayDanhSachGoiY_ThanhCong() {
        // 1. Giả lập dữ liệu sản phẩm hiện tại (ID: 1, Danh mục: 10)
        Product sanPhamHienTai = new Product(1L, "Áo thun nam", 100000.0, 10L, "Mô tả", 50, 4.5);

        // 2. Giả lập danh sách sản phẩm gợi ý cùng danh mục (ID: 2 và 3)
        Product spGoiY1 = new Product(2L, "Áo sơ mi nam", 150000.0, 10L, "Mô tả 1", 30, 4.0);
        Product spGoiY2 = new Product(3L, "Áo khoác nam", 200000.0, 10L, "Mô tả 2", 20, 5.0);

        List<Product> danhSachGoiYFake = List.of(spGoiY1, spGoiY2);

        // 3. Định nghĩa hành vi của Repository (Mockito)
        when(productRepository.findById(1L)).thenReturn(Optional.of(sanPhamHienTai));
        when(productRepository.findTop6ByMaDanhMucAndMaSanPhamNot(10L, 1L)).thenReturn(danhSachGoiYFake);

        // 4. Gọi phương thức cần test
        List<Product> ketQua = productService.layDanhSachGoiY(1L);

        // 5. Kiểm tra kết quả
        assertNotNull(ketQua);
        assertEquals(2, ketQua.size());
        assertEquals("Áo sơ mi nam", ketQua.get(0).getTenSanPham());
        verify(productRepository, times(1)).findTop6ByMaDanhMucAndMaSanPhamNot(10L, 1L);
    }
}