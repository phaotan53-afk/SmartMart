import { ProductList } from '../app/features/products/product-list/product-list';
import { ProductService } from '../app/core/services/product.service';
import { Product } from '../app/core/models/product.model';

describe('ProductList Unit Test - SWA-35', () => {

  let productList: ProductList;
  let productService: ProductService;

  let danhSachSanPham: Product[];

  beforeEach(() => {
    danhSachSanPham = [
      {
        id: 1,
        sku: 'SP001',
        name: 'Sữa tươi Vinamilk',
        category: 'Đồ uống',
        price: 32000,
        stock: 50,
        image: ''
      },
      {
        id: 2,
        sku: 'SP002',
        name: 'Mì Hảo Hảo',
        category: 'Thực phẩm',
        price: 4500,
        stock: 0,
        image: ''
      },
      {
        id: 3,
        sku: 'SP003',
        name: 'Coca Cola',
        category: 'Đồ uống',
        price: 10000,
        stock: 20,
        image: ''
      }
    ];

    productService = {
      getProducts: () => danhSachSanPham,
      addProduct: (product: Product) => {
        danhSachSanPham.push(product);
      }
    } as ProductService;

    productList = new ProductList(productService);
  });

  it('Test 01: Khởi tạo ProductList thành công', () => {
    expect(productList).toBeTruthy();
  });

  it('Test 02: Khởi tạo danh sách sản phẩm rỗng', () => {
    expect(productList.products.length).toBe(0);
  });

  it('Test 03: Tải danh sách sản phẩm thành công', () => {
    productList.loadProducts();

    expect(productList.products.length).toBe(3);
  });

  it('Test 04: Danh sách lọc ban đầu có đầy đủ sản phẩm', () => {
    productList.loadProducts();

    expect(productList.filteredProducts.length).toBe(3);
  });

  it('Test 05: Tìm kiếm sản phẩm theo tên', () => {
    productList.loadProducts();

    productList.applyFilter({
      keyword: 'Sữa tươi',
      category: 'Tất cả danh mục'
    });

    expect(productList.filteredProducts.length).toBe(1);
    expect(productList.filteredProducts[0].name)
      .toBe('Sữa tươi Vinamilk');
  });

  it('Test 06: Tìm kiếm sản phẩm theo SKU', () => {
    productList.loadProducts();

    productList.applyFilter({
      keyword: 'SP002',
      category: 'Tất cả danh mục'
    });

    expect(productList.filteredProducts.length).toBe(1);
    expect(productList.filteredProducts[0].sku).toBe('SP002');
  });

  it('Test 07: Tìm kiếm không phân biệt chữ hoa chữ thường', () => {
    productList.loadProducts();

    productList.applyFilter({
      keyword: 'COCA',
      category: 'Tất cả danh mục'
    });

    expect(productList.filteredProducts.length).toBe(1);
  });

  it('Test 08: Tìm kiếm với khoảng trắng thừa', () => {
    productList.loadProducts();

    productList.applyFilter({
      keyword: '  Coca Cola  ',
      category: 'Tất cả danh mục'
    });

    expect(productList.filteredProducts.length).toBe(1);
  });

  it('Test 09: Tìm kiếm với từ khóa rỗng', () => {
    productList.loadProducts();

    productList.applyFilter({
      keyword: '',
      category: 'Tất cả danh mục'
    });

    expect(productList.filteredProducts.length).toBe(3);
  });

  it('Test 10: Tìm kiếm không có kết quả', () => {
    productList.loadProducts();

    productList.applyFilter({
      keyword: 'Laptop',
      category: 'Tất cả danh mục'
    });

    expect(productList.filteredProducts.length).toBe(0);
  });

  it('Test 11: Lọc sản phẩm theo danh mục Đồ uống', () => {
    productList.loadProducts();

    productList.applyFilter({
      keyword: '',
      category: 'Đồ uống'
    });

    expect(productList.filteredProducts.length).toBe(2);
  });

  it('Test 12: Lọc sản phẩm theo danh mục Thực phẩm', () => {
    productList.loadProducts();

    productList.applyFilter({
      keyword: '',
      category: 'Thực phẩm'
    });

    expect(productList.filteredProducts.length).toBe(1);
  });

  it('Test 13: Lọc danh mục không có sản phẩm', () => {
    productList.loadProducts();

    productList.applyFilter({
      keyword: '',
      category: 'Điện tử'
    });

    expect(productList.filteredProducts.length).toBe(0);
  });

  it('Test 14: Kết hợp tìm kiếm và lọc danh mục', () => {
    productList.loadProducts();

    productList.applyFilter({
      keyword: 'Coca',
      category: 'Đồ uống'
    });

    expect(productList.filteredProducts.length).toBe(1);
  });

  it('Test 15: Tìm kiếm không khớp danh mục', () => {
    productList.loadProducts();

    productList.applyFilter({
      keyword: 'Coca',
      category: 'Thực phẩm'
    });

    expect(productList.filteredProducts.length).toBe(0);
  });

  it('Test 16: Kiểm tra tổng số sản phẩm', () => {
    productList.loadProducts();

    expect(productList.totalProducts).toBe(3);
  });

  it('Test 17: Kiểm tra số sản phẩm còn hàng', () => {
    productList.loadProducts();

    expect(productList.availableProducts).toBe(2);
  });

  it('Test 18: Kiểm tra số sản phẩm hết hàng', () => {
    productList.loadProducts();

    expect(productList.outOfStockProducts).toBe(1);
  });

  it('Test 19: Kiểm tra định dạng giá sản phẩm', () => {
    const giaFormatted = productList.formatPrice(32000);

    expect(giaFormatted).toBe(
      (32000).toLocaleString('vi-VN') + ' đ'
    );
  });

  it('Test 20: Thêm sản phẩm thành công', () => {
    const sanPhamMoi: Product = {
      id: 4,
      sku: 'SP004',
      name: 'Pepsi',
      category: 'Đồ uống',
      price: 12000,
      stock: 15,
      image: ''
    };

    productList.addProduct(sanPhamMoi);

    expect(danhSachSanPham.length).toBe(4);
    expect(productList.products.length).toBe(4);
  });

  it('Test 21: Kiểm tra sản phẩm hết hàng', () => {
    productList.loadProducts();

    const sanPham = productList.products.find(
      product => product.sku === 'SP002'
    );

    expect(sanPham).toBeTruthy();
    expect(sanPham!.stock).toBe(0);
  });

  it('Test 22: Kiểm tra sản phẩm còn hàng', () => {
    productList.loadProducts();

    const sanPham = productList.products.find(
      product => product.sku === 'SP001'
    );

    expect(sanPham).toBeTruthy();
    expect(sanPham!.stock).toBeGreaterThan(0);
  });

  it('Test 23: Tải lại danh sách sản phẩm', () => {
    productList.loadProducts();
    productList.loadProducts();

    expect(productList.products.length).toBe(3);
  });

  it('Test 24: Kiểm tra từ khóa được cập nhật', () => {
    productList.loadProducts();

    productList.applyFilter({
      keyword: '  COCA  ',
      category: 'Tất cả danh mục'
    });

    expect(productList.keyword).toBe('coca');
  });

  it('Test 25: Kiểm tra danh mục được cập nhật', () => {
    productList.loadProducts();

    productList.applyFilter({
      keyword: '',
      category: 'Thực phẩm'
    });

    expect(productList.selectedCategory).toBe('Thực phẩm');
  });

});
