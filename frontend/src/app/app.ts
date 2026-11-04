
import { Component, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ProductList } from './features/products/product-list/product-list';
import { ProductFilter } from './features/products/product-filter/product-filter';
import { ProductForm } from './features/products/product-form/product-form';

import { Product } from './core/models/product.model';
import { ProductFilterValue } from './features/products/product-filter/product-filter';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    ProductList,
    ProductFilter,
    ProductForm
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  showForm = false;

  @ViewChild(ProductList)
  productList!: ProductList;

  onFilterChange(filter: ProductFilterValue): void {
    this.productList.applyFilter(filter);
  }

  onAddProduct(product: Product): void {
    this.productList.addProduct(product);
    this.showForm = false;
  }
}
