
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

import { Product } from '../../../core/models/product.model';
import { ProductService } from '../../../core/services/product.service';
import { ProductFilterValue } from '../product-filter/product-filter';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './product-list.html',
  styleUrl: './product-list.css'
})
export class ProductList implements OnInit {

  products: Product[] = [];
  filteredProducts: Product[] = [];

  keyword = '';
  selectedCategory = 'Tất cả danh mục';

  constructor(private productService: ProductService) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.products = this.productService.getProducts();

    this.applyFilter({
      keyword: this.keyword,
      category: this.selectedCategory
    });
  }

  applyFilter(filter: ProductFilterValue): void {
    this.keyword = filter.keyword.toLowerCase().trim();
    this.selectedCategory = filter.category;

    this.filteredProducts = this.products.filter(product => {
      const matchKeyword =
        product.name.toLowerCase().includes(this.keyword) ||
        product.sku.toLowerCase().includes(this.keyword);

      const matchCategory =
        this.selectedCategory === 'Tất cả danh mục' ||
        product.category === this.selectedCategory;

      return matchKeyword && matchCategory;
    });
  }

  addProduct(product: Product): void {
    this.productService.addProduct(product);
    this.loadProducts();
  }

  get totalProducts(): number {
    return this.products.length;
  }

  get availableProducts(): number {
    return this.products.filter(product => product.stock > 0).length;
  }

  get outOfStockProducts(): number {
    return this.products.filter(product => product.stock === 0).length;
  }

  formatPrice(price: number): string {
    return price.toLocaleString('vi-VN') + ' đ';
  }
}
