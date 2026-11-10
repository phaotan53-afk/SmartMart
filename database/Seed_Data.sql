
USE SmartMart;
GO

SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    -- 1. USERS
    IF NOT EXISTS (
        SELECT 1 FROM dbo.Users WHERE Email = 'admin@smartmart.local'
    )
    INSERT INTO dbo.Users
        (FullName, Email, Phone, PasswordHash, Role, IsActive, CreatedAt)
    VALUES
        (N'Quản trị viên', 'admin@smartmart.local', '0900000001',
         N'DEMO_HASH_REPLACE_BEFORE_AUTH', 'ADMIN', 1, SYSDATETIME());

    IF NOT EXISTS (
        SELECT 1 FROM dbo.Users WHERE Email = 'kho@smartmart.local'
    )
    INSERT INTO dbo.Users
        (FullName, Email, Phone, PasswordHash, Role, IsActive, CreatedAt)
    VALUES
        (N'Quản lý kho', 'kho@smartmart.local', '0900000002',
         N'DEMO_HASH_REPLACE_BEFORE_AUTH', 'WAREHOUSE_MANAGER', 1, SYSDATETIME());

    IF NOT EXISTS (
        SELECT 1 FROM dbo.Users WHERE Email = 'khach1@smartmart.local'
    )
    INSERT INTO dbo.Users
        (FullName, Email, Phone, PasswordHash, Role, IsActive, CreatedAt)
    VALUES
        (N'Nguyễn Minh Anh', 'khach1@smartmart.local', '0900000003',
         N'DEMO_HASH_REPLACE_BEFORE_AUTH', 'CUSTOMER', 1, SYSDATETIME());

    IF NOT EXISTS (
        SELECT 1 FROM dbo.Users WHERE Email = 'khach2@smartmart.local'
    )
    INSERT INTO dbo.Users
        (FullName, Email, Phone, PasswordHash, Role, IsActive, CreatedAt)
    VALUES
        (N'Trần Hoàng Nam', 'khach2@smartmart.local', '0900000004',
         N'DEMO_HASH_REPLACE_BEFORE_AUTH', 'CUSTOMER', 1, SYSDATETIME());

    IF NOT EXISTS (
        SELECT 1 FROM dbo.Users WHERE Email = 'shipper@smartmart.local'
    )
    INSERT INTO dbo.Users
        (FullName, Email, Phone, PasswordHash, Role, IsActive, CreatedAt)
    VALUES
        (N'Lê Văn Hùng', 'shipper@smartmart.local', '0900000005',
         N'DEMO_HASH_REPLACE_BEFORE_AUTH', 'SHIPPER', 1, SYSDATETIME());


    -- 2. CATEGORIES
    IF NOT EXISTS (SELECT 1 FROM dbo.Categories WHERE CategoryName = N'Đồ uống')
        INSERT INTO dbo.Categories (CategoryName, Description, IsActive, CreatedAt)
        VALUES (N'Đồ uống', N'Nước uống và sữa', 1, SYSDATETIME());

    IF NOT EXISTS (SELECT 1 FROM dbo.Categories WHERE CategoryName = N'Thực phẩm')
        INSERT INTO dbo.Categories (CategoryName, Description, IsActive, CreatedAt)
        VALUES (N'Thực phẩm', N'Thực phẩm dùng hằng ngày', 1, SYSDATETIME());

    IF NOT EXISTS (SELECT 1 FROM dbo.Categories WHERE CategoryName = N'Bánh kẹo')
        INSERT INTO dbo.Categories (CategoryName, Description, IsActive, CreatedAt)
        VALUES (N'Bánh kẹo', N'Bánh, kẹo và đồ ăn nhẹ', 1, SYSDATETIME());

    IF NOT EXISTS (SELECT 1 FROM dbo.Categories WHERE CategoryName = N'Gia dụng')
        INSERT INTO dbo.Categories (CategoryName, Description, IsActive, CreatedAt)
        VALUES (N'Gia dụng', N'Sản phẩm gia dụng thiết yếu', 1, SYSDATETIME());


    -- 3. PRODUCTS
    IF NOT EXISTS (SELECT 1 FROM dbo.Products WHERE SKU = 'TEST001')
        INSERT INTO dbo.Products
            (CategoryID, SKU, ProductName, Description, Unit,
             Price, CostPrice, IsActive, CreatedAt, UpdatedAt)
        SELECT CategoryID, 'TEST001', N'Nước suối 500ml',
               N'Nước uống đóng chai', N'chai',
               5000, 3000, 1, SYSDATETIME(), SYSDATETIME()
        FROM dbo.Categories WHERE CategoryName = N'Đồ uống';

    IF NOT EXISTS (SELECT 1 FROM dbo.Products WHERE SKU = 'TEST002')
        INSERT INTO dbo.Products
            (CategoryID, SKU, ProductName, Description, Unit,
             Price, CostPrice, IsActive, CreatedAt, UpdatedAt)
        SELECT CategoryID, 'TEST002', N'Sữa tươi 1 lít',
               N'Sữa tươi tiệt trùng', N'hộp',
               32000, 25000, 1, SYSDATETIME(), SYSDATETIME()
        FROM dbo.Categories WHERE CategoryName = N'Đồ uống';

    IF NOT EXISTS (SELECT 1 FROM dbo.Products WHERE SKU = 'TEST003')
        INSERT INTO dbo.Products
            (CategoryID, SKU, ProductName, Description, Unit,
             Price, CostPrice, IsActive, CreatedAt, UpdatedAt)
        SELECT CategoryID, 'TEST003', N'Mì ăn liền',
               N'Mì ăn liền gói', N'gói',
               4500, 3000, 1, SYSDATETIME(), SYSDATETIME()
        FROM dbo.Categories WHERE CategoryName = N'Thực phẩm';

    IF NOT EXISTS (SELECT 1 FROM dbo.Products WHERE SKU = 'TEST004')
        INSERT INTO dbo.Products
            (CategoryID, SKU, ProductName, Description, Unit,
             Price, CostPrice, IsActive, CreatedAt, UpdatedAt)
        SELECT CategoryID, 'TEST004', N'Gạo 5kg',
               N'Gạo đóng túi 5kg', N'túi',
               95000, 80000, 1, SYSDATETIME(), SYSDATETIME()
        FROM dbo.Categories WHERE CategoryName = N'Thực phẩm';

    IF NOT EXISTS (SELECT 1 FROM dbo.Products WHERE SKU = 'TEST005')
        INSERT INTO dbo.Products
            (CategoryID, SKU, ProductName, Description, Unit,
             Price, CostPrice, IsActive, CreatedAt, UpdatedAt)
        SELECT CategoryID, 'TEST005', N'Bánh quy',
               N'Bánh quy đóng hộp', N'hộp',
               25000, 18000, 1, SYSDATETIME(), SYSDATETIME()
        FROM dbo.Categories WHERE CategoryName = N'Bánh kẹo';

    IF NOT EXISTS (SELECT 1 FROM dbo.Products WHERE SKU = 'TEST006')
        INSERT INTO dbo.Products
            (CategoryID, SKU, ProductName, Description, Unit,
             Price, CostPrice, IsActive, CreatedAt, UpdatedAt)
        SELECT CategoryID, 'TEST006', N'Kẹo trái cây',
               N'Kẹo vị trái cây', N'gói',
               15000, 9000, 1, SYSDATETIME(), SYSDATETIME()
        FROM dbo.Categories WHERE CategoryName = N'Bánh kẹo';

    IF NOT EXISTS (SELECT 1 FROM dbo.Products WHERE SKU = 'TEST007')
        INSERT INTO dbo.Products
            (CategoryID, SKU, ProductName, Description, Unit,
             Price, CostPrice, IsActive, CreatedAt, UpdatedAt)
        SELECT CategoryID, 'TEST007', N'Nước rửa chén',
               N'Nước rửa chén dùng trong gia đình', N'chai',
               28000, 20000, 1, SYSDATETIME(), SYSDATETIME()
        FROM dbo.Categories WHERE CategoryName = N'Gia dụng';

    IF NOT EXISTS (SELECT 1 FROM dbo.Products WHERE SKU = 'TEST008')
        INSERT INTO dbo.Products
            (CategoryID, SKU, ProductName, Description, Unit,
             Price, CostPrice, IsActive, CreatedAt, UpdatedAt)
        SELECT CategoryID, 'TEST008', N'Khăn giấy',
               N'Khăn giấy sử dụng hằng ngày', N'gói',
               18000, 12000, 1, SYSDATETIME(), SYSDATETIME()
        FROM dbo.Categories WHERE CategoryName = N'Gia dụng';


    -- 4. PRODUCT IMAGES
    INSERT INTO dbo.ProductImages
        (ProductID, ImageUrl, AltText, DisplayOrder, IsPrimary)
    SELECT p.ProductID,
           N'https://placehold.co/400x400?text=' + p.SKU,
           p.ProductName, 0, 1
    FROM dbo.Products p
    WHERE p.SKU IN
        ('TEST001','TEST002','TEST003','TEST004',
         'TEST005','TEST006','TEST007','TEST008')
      AND NOT EXISTS (
          SELECT 1 FROM dbo.ProductImages i
          WHERE i.ProductID = p.ProductID AND i.IsPrimary = 1
      );


    -- 5. ADDRESSES
    IF NOT EXISTS (
        SELECT 1 FROM dbo.Addresses
        WHERE UserID = (SELECT UserID FROM dbo.Users
                        WHERE Email = 'khach1@smartmart.local')
          AND AddressLine = N'123 Đường Nguyễn Huệ'
    )
    INSERT INTO dbo.Addresses
        (UserID, RecipientName, Phone, AddressLine,
         Ward, District, Province, IsDefault, CreatedAt)
    SELECT UserID, N'Nguyễn Minh Anh', '0900000003',
           N'123 Đường Nguyễn Huệ', N'Phường Trần Phú',
           N'Quy Nhơn', N'Gia Lai', 1, SYSDATETIME()
    FROM dbo.Users WHERE Email = 'khach1@smartmart.local';

    IF NOT EXISTS (
        SELECT 1 FROM dbo.Addresses
        WHERE UserID = (SELECT UserID FROM dbo.Users
                        WHERE Email = 'khach2@smartmart.local')
          AND AddressLine = N'45 Đường Lê Lợi'
    )
    INSERT INTO dbo.Addresses
        (UserID, RecipientName, Phone, AddressLine,
         Ward, District, Province, IsDefault, CreatedAt)
    SELECT UserID, N'Trần Hoàng Nam', '0900000004',
           N'45 Đường Lê Lợi', N'Phường Lê Lợi',
           N'Quy Nhơn', N'Gia Lai', 1, SYSDATETIME()
    FROM dbo.Users WHERE Email = 'khach2@smartmart.local';


    -- 6. CARTS
    INSERT INTO dbo.Carts (UserID, CreatedAt, UpdatedAt)
    SELECT u.UserID, SYSDATETIME(), SYSDATETIME()
    FROM dbo.Users u
    WHERE u.Email IN ('khach1@smartmart.local', 'khach2@smartmart.local')
      AND NOT EXISTS (
          SELECT 1 FROM dbo.Carts c WHERE c.UserID = u.UserID
      );


    -- 7. CART ITEMS
    INSERT INTO dbo.CartItems (CartID, ProductID, Quantity, AddedAt)
    SELECT c.CartID, p.ProductID, v.Quantity, SYSDATETIME()
    FROM (VALUES
        ('khach1@smartmart.local', 'TEST001', 2),
        ('khach1@smartmart.local', 'TEST002', 1),
        ('khach2@smartmart.local', 'TEST003', 3)
    ) v(Email, SKU, Quantity)
    JOIN dbo.Users u ON u.Email = v.Email
    JOIN dbo.Carts c ON c.UserID = u.UserID
    JOIN dbo.Products p ON p.SKU = v.SKU
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.CartItems ci
        WHERE ci.CartID = c.CartID AND ci.ProductID = p.ProductID
    );


    -- 8. VOUCHERS
    IF NOT EXISTS (SELECT 1 FROM dbo.Vouchers WHERE Code = 'WELCOME10')
        INSERT INTO dbo.Vouchers
            (Code, Description, DiscountType, DiscountValue,
             MinOrderValue, StartAt, EndAt, UsageLimit, IsActive)
        VALUES
            ('WELCOME10', N'Giảm 10% cho đơn hàng đủ điều kiện',
             'PERCENT', 10, 50000,
             DATEADD(DAY, -30, SYSDATETIME()),
             DATEADD(DAY, 365, SYSDATETIME()), 100, 1);

    IF NOT EXISTS (SELECT 1 FROM dbo.Vouchers WHERE Code = 'SAVE20000')
        INSERT INTO dbo.Vouchers
            (Code, Description, DiscountType, DiscountValue,
             MinOrderValue, StartAt, EndAt, UsageLimit, IsActive)
        VALUES
            ('SAVE20000', N'Giảm 20.000 đồng',
             'AMOUNT', 20000, 100000,
             DATEADD(DAY, -30, SYSDATETIME()),
             DATEADD(DAY, 365, SYSDATETIME()), 100, 1);

    COMMIT TRANSACTION;
    PRINT N'Đã thêm dữ liệu mẫu cho 8 bảng đầu tiên.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO


USE SmartMart;
GO

SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    /* 9. INVENTORY - TON KHO */
    INSERT INTO dbo.Inventory
        (ProductID, QuantityOnHand, ReorderLevel, UpdatedAt)
    SELECT p.ProductID, v.QuantityOnHand, v.ReorderLevel, SYSDATETIME()
    FROM (VALUES
        ('TEST001', 100, 20),
        ('TEST002',  60, 15),
        ('TEST003', 200, 30),
        ('TEST004',  40, 10),
        ('TEST005',  80, 15),
        ('TEST006', 120, 20),
        ('TEST007',  50, 10),
        ('TEST008',  70, 10)
    ) v(SKU, QuantityOnHand, ReorderLevel)
    JOIN dbo.Products p ON p.SKU = v.SKU
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Inventory i
        WHERE i.ProductID = p.ProductID
    );


    /* 10. ORDERS - DON HANG */
    IF NOT EXISTS (
        SELECT 1 FROM dbo.Orders WHERE OrderCode = 'SM-DEMO-0001'
    )
    INSERT INTO dbo.Orders
        (OrderCode, CustomerID, AddressID, VoucherID,
         OrderStatus, PaymentStatus, Subtotal,
         DiscountAmount, ShippingFee, TotalAmount,
         CustomerNote, CreatedAt)
    SELECT
        'SM-DEMO-0001',
        u.UserID,
        a.AddressID,
        v.VoucherID,
        'DELIVERED',
        'PAID',
        69000,
        6900,
        15000,
        77100,
        N'Đơn hàng mẫu đã giao thành công',
        DATEADD(DAY, -3, SYSDATETIME())
    FROM dbo.Users u
    JOIN dbo.Addresses a
      ON a.UserID = u.UserID
     AND a.AddressLine = N'123 Đường Nguyễn Huệ'
    JOIN dbo.Vouchers v ON v.Code = 'WELCOME10'
    WHERE u.Email = 'khach1@smartmart.local';

    IF NOT EXISTS (
        SELECT 1 FROM dbo.Orders WHERE OrderCode = 'SM-DEMO-0002'
    )
    INSERT INTO dbo.Orders
        (OrderCode, CustomerID, AddressID, VoucherID,
         OrderStatus, PaymentStatus, Subtotal,
         DiscountAmount, ShippingFee, TotalAmount,
         CustomerNote, CreatedAt)
    SELECT
        'SM-DEMO-0002',
        u.UserID,
        a.AddressID,
        NULL,
        'PENDING',
        'UNPAID',
        145000,
        0,
        15000,
        160000,
        N'Đơn hàng mẫu đang chờ xác nhận',
        SYSDATETIME()
    FROM dbo.Users u
    JOIN dbo.Addresses a
      ON a.UserID = u.UserID
     AND a.AddressLine = N'45 Đường Lê Lợi'
    WHERE u.Email = 'khach2@smartmart.local';


    /* 11. ORDER ITEMS - CHI TIET DON HANG */
    INSERT INTO dbo.OrderItems
        (OrderID, ProductID, Quantity, UnitPrice)
    SELECT o.OrderID, p.ProductID, v.Quantity, v.UnitPrice
    FROM (VALUES
        ('SM-DEMO-0001', 'TEST002', 2, 32000.00),
        ('SM-DEMO-0001', 'TEST001', 1,  5000.00),
        ('SM-DEMO-0002', 'TEST004', 1, 95000.00),
        ('SM-DEMO-0002', 'TEST005', 2, 25000.00)
    ) v(OrderCode, SKU, Quantity, UnitPrice)
    JOIN dbo.Orders o ON o.OrderCode = v.OrderCode
    JOIN dbo.Products p ON p.SKU = v.SKU
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.OrderItems oi
        WHERE oi.OrderID = o.OrderID
          AND oi.ProductID = p.ProductID
    );


    /* 12. PAYMENTS - THANH TOAN */
    IF NOT EXISTS (
        SELECT 1 FROM dbo.Payments
        WHERE TransactionRef = N'DEMO-PAY-0001'
    )
    INSERT INTO dbo.Payments
        (OrderID, PaymentMethod, Amount, PaymentStatus,
         TransactionRef, PaidAt, CreatedAt)
    SELECT
        OrderID, 'COD', 77100, 'SUCCESS',
        N'DEMO-PAY-0001',
        DATEADD(DAY, -3, SYSDATETIME()),
        DATEADD(DAY, -3, SYSDATETIME())
    FROM dbo.Orders
    WHERE OrderCode = 'SM-DEMO-0001'
      AND NOT EXISTS (
          SELECT 1 FROM dbo.Payments p
          WHERE p.OrderID = dbo.Orders.OrderID
      );

    IF NOT EXISTS (
        SELECT 1 FROM dbo.Payments
        WHERE TransactionRef = N'DEMO-PAY-0002'
    )
    INSERT INTO dbo.Payments
        (OrderID, PaymentMethod, Amount, PaymentStatus,
         TransactionRef, PaidAt, CreatedAt)
    SELECT
        OrderID, 'EWALLET', 160000, 'PENDING',
        N'DEMO-PAY-0002', NULL, SYSDATETIME()
    FROM dbo.Orders
    WHERE OrderCode = 'SM-DEMO-0002'
      AND NOT EXISTS (
          SELECT 1 FROM dbo.Payments p
          WHERE p.OrderID = dbo.Orders.OrderID
      );


    /* 13. SHIPMENTS - GIAO HANG */
    IF NOT EXISTS (
        SELECT 1 FROM dbo.Shipments s
        JOIN dbo.Orders o ON o.OrderID = s.OrderID
        WHERE o.OrderCode = 'SM-DEMO-0001'
    )
    INSERT INTO dbo.Shipments
        (OrderID, ShipperID, ShipmentStatus,
         AssignedAt, ShippedAt, DeliveredAt, TrackingCode)
    SELECT
        o.OrderID, u.UserID, 'DELIVERED',
        DATEADD(DAY, -3, SYSDATETIME()),
        DATEADD(DAY, -3, SYSDATETIME()),
        DATEADD(DAY, -3, SYSDATETIME()),
        'DEMO-TRACK-0001'
    FROM dbo.Orders o
    CROSS JOIN dbo.Users u
    WHERE o.OrderCode = 'SM-DEMO-0001'
      AND u.Email = 'shipper@smartmart.local';

    IF NOT EXISTS (
        SELECT 1 FROM dbo.Shipments s
        JOIN dbo.Orders o ON o.OrderID = s.OrderID
        WHERE o.OrderCode = 'SM-DEMO-0002'
    )
    INSERT INTO dbo.Shipments
        (OrderID, ShipperID, ShipmentStatus,
         AssignedAt, ShippedAt, DeliveredAt, TrackingCode)
    SELECT
        o.OrderID, NULL, 'UNASSIGNED',
        NULL, NULL, NULL, NULL
    FROM dbo.Orders o
    WHERE o.OrderCode = 'SM-DEMO-0002';


    /* 14. INVENTORY TRANSACTIONS - LICH SU KHO */
    INSERT INTO dbo.InventoryTransactions
        (InventoryID, UserID, TransactionType,
         QuantityChange, Note, CreatedAt)
    SELECT
        i.InventoryID, u.UserID, 'IMPORT',
        v.QuantityChange, v.Note, SYSDATETIME()
    FROM (VALUES
        ('TEST001', 100, N'DEMO-IMPORT-TEST001'),
        ('TEST002',  60, N'DEMO-IMPORT-TEST002'),
        ('TEST003', 200, N'DEMO-IMPORT-TEST003'),
        ('TEST004',  40, N'DEMO-IMPORT-TEST004'),
        ('TEST005',  80, N'DEMO-IMPORT-TEST005'),
        ('TEST006', 120, N'DEMO-IMPORT-TEST006'),
        ('TEST007',  50, N'DEMO-IMPORT-TEST007'),
        ('TEST008',  70, N'DEMO-IMPORT-TEST008')
    ) v(SKU, QuantityChange, Note)
    JOIN dbo.Products p ON p.SKU = v.SKU
    JOIN dbo.Inventory i ON i.ProductID = p.ProductID
    JOIN dbo.Users u ON u.Email = 'kho@smartmart.local'
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.InventoryTransactions t
        WHERE t.InventoryID = i.InventoryID
          AND t.Note = v.Note
    );


    /* 15. REVIEWS - DANH GIA */
    IF NOT EXISTS (
        SELECT 1 FROM dbo.Reviews r
        JOIN dbo.Orders o ON o.OrderID = r.OrderID
        JOIN dbo.Products p ON p.ProductID = r.ProductID
        WHERE o.OrderCode = 'SM-DEMO-0001'
          AND p.SKU = 'TEST002'
          AND r.CustomerID = o.CustomerID
    )
    INSERT INTO dbo.Reviews
        (ProductID, CustomerID, OrderID, Rating, Comment, CreatedAt)
    SELECT
        p.ProductID, o.CustomerID, o.OrderID, 5,
        N'Sản phẩm tốt, giao hàng đúng hẹn.',
        DATEADD(DAY, -2, SYSDATETIME())
    FROM dbo.Orders o
    JOIN dbo.Products p ON p.SKU = 'TEST002'
    WHERE o.OrderCode = 'SM-DEMO-0001'
      AND o.OrderStatus = 'DELIVERED';


    /* 16. FAVORITES - SAN PHAM YEU THICH */
    INSERT INTO dbo.Favorites (CustomerID, ProductID, CreatedAt)
    SELECT u.UserID, p.ProductID, SYSDATETIME()
    FROM (VALUES
        ('khach1@smartmart.local', 'TEST004'),
        ('khach1@smartmart.local', 'TEST005'),
        ('khach2@smartmart.local', 'TEST001'),
        ('khach2@smartmart.local', 'TEST007')
    ) v(Email, SKU)
    JOIN dbo.Users u ON u.Email = v.Email
    JOIN dbo.Products p ON p.SKU = v.SKU
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Favorites f
        WHERE f.CustomerID = u.UserID
          AND f.ProductID = p.ProductID
    );


    /* 17. VOUCHER REDEMPTIONS - MA GIAM GIA DA SU DUNG */
    INSERT INTO dbo.VoucherRedemptions
        (VoucherID, CustomerID, OrderID, RedeemedAt)
    SELECT v.VoucherID, o.CustomerID, o.OrderID,
           DATEADD(DAY, -3, SYSDATETIME())
    FROM dbo.Orders o
    JOIN dbo.Vouchers v ON v.Code = 'WELCOME10'
    WHERE o.OrderCode = 'SM-DEMO-0001'
      AND o.VoucherID = v.VoucherID
      AND NOT EXISTS (
          SELECT 1 FROM dbo.VoucherRedemptions vr
          WHERE vr.OrderID = o.OrderID
      );


    /* 18. ORDER STATUS HISTORY - LICH SU TRANG THAI */
-- Them du lieu mau cho OrderStatusHistory
INSERT INTO dbo.OrderStatusHistory
(
    OrderID,
    ChangedByUserID,
    OldStatus,
    NewStatus,
    Note,
    ChangedAt
)
SELECT
    o.OrderID,
    u.UserID,
    v.OldStatus,
    v.NewStatus,
    v.Note,
    DATEADD(DAY, v.DayOffset, o.CreatedAt)
FROM
(
    VALUES
        ('ORD001', 'admin@smartmart.test', NULL, 'Pending', N'Tạo đơn hàng', 0),
        ('ORD001', 'admin@smartmart.test', 'Pending', 'Confirmed', N'Xác nhận đơn hàng', 0),
        ('ORD002', 'admin@smartmart.test', NULL, 'Pending', N'Tạo đơn hàng', 0),
        ('ORD003', 'admin@smartmart.test', NULL, 'Pending', N'Tạo đơn hàng', 0)
) AS v
(
    OrderCode,
    ActorEmail,
    OldStatus,
    NewStatus,
    Note,
    DayOffset
)
INNER JOIN dbo.Orders o
    ON o.OrderCode = v.OrderCode
INNER JOIN dbo.Users u
    ON u.Email = v.ActorEmail
WHERE NOT EXISTS
(
    SELECT 1
    FROM dbo.OrderStatusHistory h
    WHERE h.OrderID = o.OrderID
      AND h.NewStatus = v.NewStatus
      AND ISNULL(h.Note, N'') = ISNULL(v.Note, N'')
);

    /* 19. SHIPPER LOCATIONS - VI TRI SHIPPER */
    INSERT INTO dbo.ShipperLocations
        (ShipperID, Latitude, Longitude, RecordedAt)
    SELECT u.UserID, v.Latitude, v.Longitude, SYSDATETIME()
    FROM (VALUES
        (13.782000, 109.219000),
        (13.779000, 109.225000),
        (13.775000, 109.230000)
    ) v(Latitude, Longitude)
    JOIN dbo.Users u ON u.Email = 'shipper@smartmart.local'
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.ShipperLocations sl
        WHERE sl.ShipperID = u.UserID
          AND sl.Latitude = v.Latitude
          AND sl.Longitude = v.Longitude
    );


    /* 20. PRODUCT RECOMMENDATIONS - GOI Y SAN PHAM */
    INSERT INTO dbo.ProductRecommendations
        (CustomerID, ProductID, Score, Reason, GeneratedAt)
    SELECT u.UserID, p.ProductID, v.Score, v.Reason, SYSDATETIME()
    FROM (VALUES
        ('khach1@smartmart.local', 'TEST004', 0.95000, N'Sản phẩm phổ biến'),
        ('khach1@smartmart.local', 'TEST005', 0.87000, N'Cùng nhóm sản phẩm yêu thích'),
        ('khach2@smartmart.local', 'TEST001', 0.92000, N'Phù hợp lịch sử quan tâm'),
        ('khach2@smartmart.local', 'TEST002', 0.81000, N'Sản phẩm được đề xuất')
    ) v(Email, SKU, Score, Reason)
    JOIN dbo.Users u ON u.Email = v.Email
    JOIN dbo.Products p ON p.SKU = v.SKU
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.ProductRecommendations r
        WHERE r.CustomerID = u.UserID
          AND r.ProductID = p.ProductID
    );


    COMMIT TRANSACTION;
    PRINT N'Đã thêm dữ liệu mẫu cho các bảng còn lại.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO


