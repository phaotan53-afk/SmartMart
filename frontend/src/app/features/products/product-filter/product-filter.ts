
import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface ProductFilterValue {
  keyword: string;
  category: string;
}

@Component({
  selector: 'app-product-filter',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './product-filter.html',
  styleUrl: './product-filter.css'
})
export class ProductFilter {

  keyword = '';
  selectedCategory = 'Tất cả danh mục';

  categories = [
    'Tất cả danh mục',
    'Đồ uống',
    'Thực phẩm',
    'Bánh kẹo',
    'Gia dụng'
  ];

  @Output() filterChange = new EventEmitter<ProductFilterValue>();

  search(): void {
    this.filterChange.emit({
      keyword: this.keyword,
      category: this.selectedCategory
    });
  }

  reset(): void {
    this.keyword = '';
    this.selectedCategory = 'Tất cả danh mục';
    this.search();
  }
}
