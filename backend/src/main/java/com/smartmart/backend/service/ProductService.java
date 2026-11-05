package com.smartmart.backend.service;

import com.smartmart.backend.model.Product;
import com.smartmart.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.List;
@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    // SWA-17: Tìm kiếm và Lọc sản phẩm
    public Page<Product> timKiemSanPham(String tuKhoa, Long maDanhMuc, Double giaToiThieu, Double giaToiDa, int trang, int kichThuoc) {
        if (giaToiThieu != null && giaToiDa != null && giaToiThieu > giaToiDa) {
            throw new IllegalArgumentException("Giá tối thiểu không được lớn hơn giá tối đa");
        }
        String tuKhoaChuanHoa = (tuKhoa != null && !tuKhoa.trim().isEmpty()) ? tuKhoa.trim() : null;
        return productRepository.timKiemVaLoc(tuKhoaChuanHoa, maDanhMuc, giaToiThieu, giaToiDa, PageRequest.of(trang, kichThuoc));
    }

    // SWA-36: Lấy chi tiết sản phẩm theo ID
    public Optional<Product> layChiTietSanPhamTheoId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Mã sản phẩm không hợp lệ");
        }
        return productRepository.findById(id);
    }
    // SWA-82: Lấy danh sách sản phẩm gợi ý cùng danh mục
    public List<Product> layDanhSachGoiY(Long maSanPham) {
        if (maSanPham == null || maSanPham <= 0) {
            throw new IllegalArgumentException("Mã sản phẩm không hợp lệ");
        }

        // 1. Tìm sản phẩm hiện tại để lấy mã danh mục
        Product sanPhamHienTai = productRepository.findById(maSanPham)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + maSanPham));

        // 2. Lấy danh sách gợi ý cùng mã danh mục (loại trừ sản phẩm hiện tại)
        return productRepository.findTop6ByMaDanhMucAndMaSanPhamNot(
                sanPhamHienTai.getMaDanhMuc(), 
                maSanPham
        );
}
}