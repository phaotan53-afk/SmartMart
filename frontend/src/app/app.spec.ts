import { TestBed } from '@angular/core/testing';
import { App } from './app';

describe('App Unit Test', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App]
    }).compileComponents();
  });

  it('Test 01: Khởi tạo ứng dụng thành công', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;

    expect(app).toBeTruthy();
  });
});