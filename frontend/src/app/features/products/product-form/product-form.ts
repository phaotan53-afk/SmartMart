
import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { Product } from '../../../core/models/product.model';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './product-form.html',
  styleUrl: './product-form.css'
})
export class ProductForm {

  @Output() saveProduct = new EventEmitter<Product>();
  @Output() cancel = new EventEmitter<void>();

  name = '';
  sku = '';
  category = 'Đồ uống';
  price = 0;
  stock = 0;

  categories = [
    'Đồ uống',
    'Thực phẩm',
    'Bánh kẹo',
    'Gia dụng'
  ];

  submitForm(): void {
    if (
      !this.name.trim() ||
      !this.sku.trim() ||
      this.price <= 0 ||
      this.stock < 0
    ) {
      return;
    }

    const product: Product = {
      id: Date.now(),
      sku: this.sku.trim(),
      name: this.name.trim(),
      category: this.category,
      price: this.price,
      stock: this.stock,
      image: '📦'
    };

    this.saveProduct.emit(product);
  }
}
