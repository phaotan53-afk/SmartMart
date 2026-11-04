
import { Injectable } from '@angular/core';
import { Product } from '../models/product.model';

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  private products: Product[] = [
    {
      id: 1,
      sku: 'SP001',
      name: 'Sữa tươi Vinamilk',
      category: 'Đồ uống',
      price: 32000,
      stock: 50,
      image: '🥛'
    },
    {
      id: 2,
      sku: 'SP002',
      name: 'Mì Hảo Hảo',
      category: 'Thực phẩm',
      price: 4500,
      stock: 100,
      image: '🍜'
    },
    {
      id: 3,
      sku: 'SP003',
      name: 'Bánh Oreo',
      category: 'Bánh kẹo',
      price: 12000,
      stock: 30,
      image: '🍪'
    },
    {
      id: 4,
      sku: 'SP004',
      name: 'Nước suối Aquafina',
      category: 'Đồ uống',
      price: 7000,
      stock: 80,
      image: '💧'
    },
    {
      id: 5,
      sku: 'SP005',
      name: 'Dầu ăn Simply',
      category: 'Thực phẩm',
      price: 45000,
      stock: 25,
      image: '🫒'
    },
    {
      id: 6,
      sku: 'SP006',
      name: 'Nước rửa chén',
      category: 'Gia dụng',
      price: 28000,
      stock: 0,
      image: '🧴'
    }
  ];

  getProducts(): Product[] {
    return [...this.products];
  }

  addProduct(product: Product): void {
    this.products.push(product);
  }
}
