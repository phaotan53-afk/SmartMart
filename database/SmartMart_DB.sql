/* ============================================================================
   SMARTMART - TAO DATABASE SmartMartDB (CAU TRUC + DU LIEU MAU) - 1 FILE DUY NHAT
   Chay bang SSMS: mo file -> chon Execute (F5). Co the chay lai an toan.
   Tai khoan demo (chi dung local) - mat khau chung: SmartMart@123
     admin@smartmart.local | kho@smartmart.local | khach1@smartmart.local
     khach2@smartmart.local | shipper@smartmart.local
   Cac loi da sua so voi 2 file goc:
     1. Thieu CREATE DATABASE -> USE [SmartMartDB] loi va cac lenh sau co the
        chay nham vao database master. Nay tu tao database neu chua co.
     2. THROW o dau file khong dung duoc cac batch sau GO -> nay dung SET NOEXEC.
     3. Luu UTF-8 co BOM de SSMS doc dung tieng Viet.
     4. Gop 2 file thanh 1, khong con phu thuoc thu tu chay.
   ============================================================================ */
SET NOCOUNT ON;
USE [master];
GO
IF DB_ID(N'SmartMartDB') IS NULL
BEGIN
    CREATE DATABASE [SmartMartDB];
    PRINT N'Da tao database SmartMartDB.';
END
ELSE
    PRINT N'Database SmartMartDB da ton tai - dung lai database nay.';
GO
USE [SmartMartDB];
GO
-- An toan: neu USE that bai thi dung han, khong chay nham vao database khac.
IF DB_NAME() <> N'SmartMartDB'
BEGIN
    RAISERROR(N'Khong chuyen duoc sang SmartMartDB. Dung script.', 16, 1);
    SET NOEXEC ON;
END
GO
-- Neu da co bang dbo (chay lai) thi bo qua phan tao cau truc, van nap du lieu mau.
IF EXISTS (SELECT 1 FROM sys.tables WHERE schema_id = SCHEMA_ID(N'dbo'))
BEGIN
    PRINT N'Da co bang dbo - bo qua phan tao cau truc.';
    SET NOEXEC ON;
END
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

/* =========================================================
   SMARTMART - DATA MODEL
   SQL Server - 20 tables
   ========================================================= */

SET NOCOUNT ON;
GO

/* 1. USERS */
CREATE TABLE dbo.Users (
    UserID BIGINT IDENTITY(1,1) PRIMARY KEY,
    FullName NVARCHAR(120) NOT NULL,
    Email NVARCHAR(254) NOT NULL UNIQUE,
    Phone VARCHAR(20) NULL,
    PasswordHash NVARCHAR(255) NOT NULL,
    Role VARCHAR(30) NOT NULL,
    IsActive BIT NOT NULL DEFAULT 1,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT CK_Users_Role CHECK
        (Role IN ('ADMIN','WAREHOUSE_MANAGER','CUSTOMER','SHIPPER'))
);
GO

/* 2. CATEGORIES */
CREATE TABLE dbo.Categories (
    CategoryID INT IDENTITY(1,1) PRIMARY KEY,
    CategoryName NVARCHAR(100) NOT NULL UNIQUE,
    Description NVARCHAR(500) NULL,
    IsActive BIT NOT NULL DEFAULT 1,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

/* 3. PRODUCTS */
CREATE TABLE dbo.Products (
    ProductID BIGINT IDENTITY(1,1) PRIMARY KEY,
    CategoryID INT NOT NULL,
    SKU VARCHAR(40) NOT NULL UNIQUE,
    ProductName NVARCHAR(180) NOT NULL,
    Description NVARCHAR(MAX) NULL,
    Unit NVARCHAR(30) NOT NULL DEFAULT N'sản phẩm',
    Price DECIMAL(12,2) NOT NULL,
    CostPrice DECIMAL(12,2) NULL,
    IsActive BIT NOT NULL DEFAULT 1,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Products_Categories
        FOREIGN KEY (CategoryID) REFERENCES dbo.Categories(CategoryID),
    CONSTRAINT CK_Products_Price CHECK (Price >= 0),
    CONSTRAINT CK_Products_CostPrice
        CHECK (CostPrice IS NULL OR CostPrice >= 0)
);
GO

/* 4. PRODUCT IMAGES */
CREATE TABLE dbo.ProductImages (
    ProductImageID BIGINT IDENTITY(1,1) PRIMARY KEY,
    ProductID BIGINT NOT NULL,
    ImageUrl NVARCHAR(1000) NOT NULL,
    AltText NVARCHAR(250) NULL,
    DisplayOrder INT NOT NULL DEFAULT 0,
    IsPrimary BIT NOT NULL DEFAULT 0,
    CONSTRAINT FK_ProductImages_Products
        FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID)
        ON DELETE CASCADE,
    CONSTRAINT CK_ProductImages_DisplayOrder CHECK (DisplayOrder >= 0)
);
GO

/* 5. ADDRESSES */
CREATE TABLE dbo.Addresses (
    AddressID BIGINT IDENTITY(1,1) PRIMARY KEY,
    UserID BIGINT NOT NULL,
    RecipientName NVARCHAR(120) NOT NULL,
    Phone VARCHAR(20) NOT NULL,
    AddressLine NVARCHAR(250) NOT NULL,
    Ward NVARCHAR(100) NULL,
    District NVARCHAR(100) NOT NULL,
    Province NVARCHAR(100) NOT NULL,
    IsDefault BIT NOT NULL DEFAULT 0,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Addresses_Users
        FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID),
    CONSTRAINT UQ_Addresses_User_Address UNIQUE (UserID, AddressID)
);
GO

/* 6. CARTS */
CREATE TABLE dbo.Carts (
    CartID BIGINT IDENTITY(1,1) PRIMARY KEY,
    UserID BIGINT NOT NULL UNIQUE,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Carts_Users
        FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID)
);
GO

/* 7. CART ITEMS */
CREATE TABLE dbo.CartItems (
    CartItemID BIGINT IDENTITY(1,1) PRIMARY KEY,
    CartID BIGINT NOT NULL,
    ProductID BIGINT NOT NULL,
    Quantity INT NOT NULL,
    AddedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_CartItems_Carts
        FOREIGN KEY (CartID) REFERENCES dbo.Carts(CartID)
        ON DELETE CASCADE,
    CONSTRAINT FK_CartItems_Products
        FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID),
    CONSTRAINT UQ_CartItems_Cart_Product UNIQUE (CartID, ProductID),
    CONSTRAINT CK_CartItems_Quantity CHECK (Quantity > 0)
);
GO

/* 8. VOUCHERS */
CREATE TABLE dbo.Vouchers (
    VoucherID BIGINT IDENTITY(1,1) PRIMARY KEY,
    Code VARCHAR(40) NOT NULL UNIQUE,
    Description NVARCHAR(300) NULL,
    DiscountType VARCHAR(10) NOT NULL,
    DiscountValue DECIMAL(12,2) NOT NULL,
    MinOrderValue DECIMAL(12,2) NOT NULL DEFAULT 0,
    StartAt DATETIME2 NOT NULL,
    EndAt DATETIME2 NOT NULL,
    UsageLimit INT NULL,
    IsActive BIT NOT NULL DEFAULT 1,
    CONSTRAINT CK_Vouchers_Type
        CHECK (DiscountType IN ('PERCENT','AMOUNT')),
    CONSTRAINT CK_Vouchers_Value
        CHECK (DiscountValue > 0 AND
        (DiscountType <> 'PERCENT' OR DiscountValue <= 100)),
    CONSTRAINT CK_Vouchers_MinOrder CHECK (MinOrderValue >= 0),
    CONSTRAINT CK_Vouchers_Dates CHECK (EndAt > StartAt),
    CONSTRAINT CK_Vouchers_Usage
        CHECK (UsageLimit IS NULL OR UsageLimit > 0)
);
GO

/* 9. ORDERS */
CREATE TABLE dbo.Orders (
    OrderID BIGINT IDENTITY(1,1) PRIMARY KEY,
    OrderCode VARCHAR(30) NOT NULL UNIQUE,
    CustomerID BIGINT NOT NULL,
    AddressID BIGINT NOT NULL,
    VoucherID BIGINT NULL,
    OrderStatus VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    PaymentStatus VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    Subtotal DECIMAL(12,2) NOT NULL,
    DiscountAmount DECIMAL(12,2) NOT NULL DEFAULT 0,
    ShippingFee DECIMAL(12,2) NOT NULL DEFAULT 0,
    TotalAmount DECIMAL(12,2) NOT NULL,
    CustomerNote NVARCHAR(500) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Orders_Customers
        FOREIGN KEY (CustomerID) REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_Orders_Addresses
        FOREIGN KEY (CustomerID, AddressID)
        REFERENCES dbo.Addresses(UserID, AddressID),
    CONSTRAINT FK_Orders_Vouchers
        FOREIGN KEY (VoucherID) REFERENCES dbo.Vouchers(VoucherID),
    CONSTRAINT CK_Orders_Status CHECK
        (OrderStatus IN
        ('PENDING','CONFIRMED','PREPARING','SHIPPING',
         'DELIVERED','CANCELLED','REFUNDED')),
    CONSTRAINT CK_Orders_PaymentStatus CHECK
        (PaymentStatus IN ('UNPAID','PAID','FAILED','REFUNDED')),
    CONSTRAINT CK_Orders_Amounts CHECK
        (Subtotal >= 0 AND DiscountAmount >= 0
         AND ShippingFee >= 0 AND TotalAmount >= 0
         AND DiscountAmount <= Subtotal)
);
GO

/* 10. ORDER ITEMS */
CREATE TABLE dbo.OrderItems (
    OrderItemID BIGINT IDENTITY(1,1) PRIMARY KEY,
    OrderID BIGINT NOT NULL,
    ProductID BIGINT NOT NULL,
    Quantity INT NOT NULL,
    UnitPrice DECIMAL(12,2) NOT NULL,
    LineTotal AS (CONVERT(DECIMAL(14,2), Quantity * UnitPrice)) PERSISTED,
    CONSTRAINT FK_OrderItems_Orders
        FOREIGN KEY (OrderID) REFERENCES dbo.Orders(OrderID)
        ON DELETE CASCADE,
    CONSTRAINT FK_OrderItems_Products
        FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID),
    CONSTRAINT UQ_OrderItems_Order_Product UNIQUE (OrderID, ProductID),
    CONSTRAINT CK_OrderItems_Quantity CHECK (Quantity > 0),
    CONSTRAINT CK_OrderItems_UnitPrice CHECK (UnitPrice >= 0)
);
GO

/* 11. PAYMENTS */
CREATE TABLE dbo.Payments (
    PaymentID BIGINT IDENTITY(1,1) PRIMARY KEY,
    OrderID BIGINT NOT NULL,
    PaymentMethod VARCHAR(20) NOT NULL,
    Amount DECIMAL(12,2) NOT NULL,
    PaymentStatus VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    TransactionRef NVARCHAR(100) NULL UNIQUE,
    PaidAt DATETIME2 NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Payments_Orders
        FOREIGN KEY (OrderID) REFERENCES dbo.Orders(OrderID)
        ON DELETE CASCADE,
    CONSTRAINT CK_Payments_Method CHECK
        (PaymentMethod IN ('COD','EWALLET','CARD','BANK_TRANSFER')),
    CONSTRAINT CK_Payments_Status CHECK
        (PaymentStatus IN ('PENDING','SUCCESS','FAILED','REFUNDED')),
    CONSTRAINT CK_Payments_Amount CHECK (Amount >= 0)
);
GO

/* 12. SHIPMENTS */
CREATE TABLE dbo.Shipments (
    ShipmentID BIGINT IDENTITY(1,1) PRIMARY KEY,
    OrderID BIGINT NOT NULL UNIQUE,
    ShipperID BIGINT NULL,
    ShipmentStatus VARCHAR(20) NOT NULL DEFAULT 'UNASSIGNED',
    AssignedAt DATETIME2 NULL,
    ShippedAt DATETIME2 NULL,
    DeliveredAt DATETIME2 NULL,
    TrackingCode VARCHAR(40) NULL UNIQUE,
    CONSTRAINT FK_Shipments_Orders
        FOREIGN KEY (OrderID) REFERENCES dbo.Orders(OrderID)
        ON DELETE CASCADE,
    CONSTRAINT FK_Shipments_Shippers
        FOREIGN KEY (ShipperID) REFERENCES dbo.Users(UserID),
    CONSTRAINT CK_Shipments_Status CHECK
        (ShipmentStatus IN
        ('UNASSIGNED','ASSIGNED','PICKED_UP','IN_TRANSIT',
         'DELIVERED','FAILED','RETURNED'))
);
GO

/* 13. INVENTORY */
CREATE TABLE dbo.Inventory (
    InventoryID BIGINT IDENTITY(1,1) PRIMARY KEY,
    ProductID BIGINT NOT NULL UNIQUE,
    QuantityOnHand INT NOT NULL DEFAULT 0,
    ReorderLevel INT NOT NULL DEFAULT 0,
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Inventory_Products
        FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID),
    CONSTRAINT CK_Inventory_Quantity CHECK (QuantityOnHand >= 0),
    CONSTRAINT CK_Inventory_Reorder CHECK (ReorderLevel >= 0)
);
GO

/* 14. INVENTORY TRANSACTIONS */
CREATE TABLE dbo.InventoryTransactions (
    InventoryTransactionID BIGINT IDENTITY(1,1) PRIMARY KEY,
    InventoryID BIGINT NOT NULL,
    UserID BIGINT NOT NULL,
    TransactionType VARCHAR(20) NOT NULL,
    QuantityChange INT NOT NULL,
    Note NVARCHAR(300) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_InventoryTransactions_Inventory
        FOREIGN KEY (InventoryID) REFERENCES dbo.Inventory(InventoryID),
    CONSTRAINT FK_InventoryTransactions_Users
        FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID),
    CONSTRAINT CK_InventoryTransactions_Type CHECK
        (TransactionType IN ('IMPORT','ADJUSTMENT','SALE','RETURN','DAMAGE')),
    CONSTRAINT CK_InventoryTransactions_Quantity CHECK (QuantityChange <> 0)
);
GO

/* 15. REVIEWS */
CREATE TABLE dbo.Reviews (
    ReviewID BIGINT IDENTITY(1,1) PRIMARY KEY,
    ProductID BIGINT NOT NULL,
    CustomerID BIGINT NOT NULL,
    OrderID BIGINT NULL,
    Rating TINYINT NOT NULL,
    Comment NVARCHAR(1000) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Reviews_Products
        FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID),
    CONSTRAINT FK_Reviews_Customers
        FOREIGN KEY (CustomerID) REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_Reviews_Orders
        FOREIGN KEY (OrderID) REFERENCES dbo.Orders(OrderID),
    CONSTRAINT CK_Reviews_Rating CHECK (Rating BETWEEN 1 AND 5)
);
GO

/* 16. FAVORITES */
CREATE TABLE dbo.Favorites (
    FavoriteID BIGINT IDENTITY(1,1) PRIMARY KEY,
    CustomerID BIGINT NOT NULL,
    ProductID BIGINT NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Favorites_Customers
        FOREIGN KEY (CustomerID) REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_Favorites_Products
        FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID),
    CONSTRAINT UQ_Favorites_Customer_Product UNIQUE (CustomerID, ProductID)
);
GO

/* 17. VOUCHER REDEMPTIONS */
CREATE TABLE dbo.VoucherRedemptions (
    VoucherRedemptionID BIGINT IDENTITY(1,1) PRIMARY KEY,
    VoucherID BIGINT NOT NULL,
    CustomerID BIGINT NOT NULL,
    OrderID BIGINT NOT NULL UNIQUE,
    RedeemedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_VoucherRedemptions_Vouchers
        FOREIGN KEY (VoucherID) REFERENCES dbo.Vouchers(VoucherID),
    CONSTRAINT FK_VoucherRedemptions_Customers
        FOREIGN KEY (CustomerID) REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_VoucherRedemptions_Orders
        FOREIGN KEY (OrderID) REFERENCES dbo.Orders(OrderID)
);
GO

/* 18. ORDER STATUS HISTORY */
CREATE TABLE dbo.OrderStatusHistory (
    OrderStatusHistoryID BIGINT IDENTITY(1,1) PRIMARY KEY,
    OrderID BIGINT NOT NULL,
    ChangedByUserID BIGINT NULL,
    OldStatus VARCHAR(20) NULL,
    NewStatus VARCHAR(20) NOT NULL,
    Note NVARCHAR(500) NULL,
    ChangedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_OrderStatusHistory_Orders
        FOREIGN KEY (OrderID) REFERENCES dbo.Orders(OrderID)
        ON DELETE CASCADE,
    CONSTRAINT FK_OrderStatusHistory_Users
        FOREIGN KEY (ChangedByUserID) REFERENCES dbo.Users(UserID),
    CONSTRAINT CK_OrderHistory_NewStatus CHECK
        (NewStatus IN
        ('PENDING','CONFIRMED','PREPARING','SHIPPING',
         'DELIVERED','CANCELLED','REFUNDED'))
);
GO

/* 19. SHIPPER LOCATIONS */
CREATE TABLE dbo.ShipperLocations (
    ShipperLocationID BIGINT IDENTITY(1,1) PRIMARY KEY,
    ShipperID BIGINT NOT NULL,
    Latitude DECIMAL(9,6) NOT NULL,
    Longitude DECIMAL(9,6) NOT NULL,
    RecordedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_ShipperLocations_Users
        FOREIGN KEY (ShipperID) REFERENCES dbo.Users(UserID),
    CONSTRAINT CK_ShipperLocations_Latitude
        CHECK (Latitude BETWEEN -90 AND 90),
    CONSTRAINT CK_ShipperLocations_Longitude
        CHECK (Longitude BETWEEN -180 AND 180)
);
GO

/* 20. PRODUCT RECOMMENDATIONS */
CREATE TABLE dbo.ProductRecommendations (
    ProductRecommendationID BIGINT IDENTITY(1,1) PRIMARY KEY,
    CustomerID BIGINT NOT NULL,
    ProductID BIGINT NOT NULL,
    Score DECIMAL(6,5) NOT NULL DEFAULT 0,
    Reason NVARCHAR(200) NULL,
    GeneratedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_ProductRecommendations_Customers
        FOREIGN KEY (CustomerID) REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_ProductRecommendations_Products
        FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID),
    CONSTRAINT UQ_ProductRecommendations_Customer_Product
        UNIQUE (CustomerID, ProductID),
    CONSTRAINT CK_ProductRecommendations_Score
        CHECK (Score BETWEEN 0 AND 1)
);
GO

/* INDEXES */
CREATE INDEX IX_Products_CategoryID
    ON dbo.Products(CategoryID);

CREATE INDEX IX_Orders_Customer_CreatedAt
    ON dbo.Orders(CustomerID, CreatedAt);

CREATE INDEX IX_OrderItems_ProductID
    ON dbo.OrderItems(ProductID);

CREATE INDEX IX_OrderStatusHistory_Order_ChangedAt
    ON dbo.OrderStatusHistory(OrderID, ChangedAt);

CREATE INDEX IX_InventoryTransactions_Inventory_CreatedAt
    ON dbo.InventoryTransactions(InventoryID, CreatedAt);

CREATE INDEX IX_ShipperLocations_Shipper_RecordedAt
    ON dbo.ShipperLocations(ShipperID, RecordedAt);
GO

/* Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
   Additive migration on team's 20-table schema; never DROP/TRUNCATE source data. */
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.SchemaMigrations',N'U') IS NULL
    CREATE TABLE dbo.SchemaMigrations(Version VARCHAR(30) PRIMARY KEY, AppliedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME());
IF COL_LENGTH('dbo.Products','Brand') IS NULL ALTER TABLE dbo.Products ADD Brand NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.Products','IsDemo') IS NULL ALTER TABLE dbo.Products ADD IsDemo BIT NOT NULL CONSTRAINT DF_Products_IsDemo DEFAULT 0;
IF COL_LENGTH('dbo.Orders','IdempotencyKey') IS NULL ALTER TABLE dbo.Orders ADD IdempotencyKey VARCHAR(100) NULL;
IF COL_LENGTH('dbo.Orders','CheckoutFingerprint') IS NULL ALTER TABLE dbo.Orders ADD CheckoutFingerprint CHAR(64) NULL;
IF OBJECT_ID(N'dbo.SessionTokens',N'U') IS NULL
CREATE TABLE dbo.SessionTokens(
    TokenHash CHAR(64) PRIMARY KEY,UserID BIGINT NOT NULL,ExpiresAt DATETIME2 NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_SessionTokens_Users FOREIGN KEY(UserID) REFERENCES dbo.Users(UserID));
IF OBJECT_ID(N'dbo.ProductSpecifications',N'U') IS NULL
CREATE TABLE dbo.ProductSpecifications(
    ProductID BIGINT NOT NULL,SpecificationName NVARCHAR(100) NOT NULL,SpecificationValue NVARCHAR(500) NOT NULL,
    CONSTRAINT PK_ProductSpecifications PRIMARY KEY(ProductID,SpecificationName),
    CONSTRAINT FK_ProductSpecifications_Products FOREIGN KEY(ProductID) REFERENCES dbo.Products(ProductID));
IF OBJECT_ID(N'dbo.RefundRequests',N'U') IS NULL
CREATE TABLE dbo.RefundRequests(
    RefundID BIGINT IDENTITY PRIMARY KEY,OrderID BIGINT NOT NULL UNIQUE,RequestedBy BIGINT NOT NULL,
    Amount DECIMAL(12,2) NOT NULL,Reason NVARCHAR(500) NOT NULL,RefundStatus VARCHAR(30) NOT NULL DEFAULT 'PENDING_MANUAL',
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),CompletedAt DATETIME2 NULL,
    CONSTRAINT FK_RefundRequests_Orders FOREIGN KEY(OrderID) REFERENCES dbo.Orders(OrderID),
    CONSTRAINT FK_RefundRequests_Users FOREIGN KEY(RequestedBy) REFERENCES dbo.Users(UserID),
    CONSTRAINT CK_RefundRequests_Amount CHECK(Amount>=0),
    CONSTRAINT CK_RefundRequests_Status CHECK(RefundStatus IN('PENDING_MANUAL','SUCCEEDED')));
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name='IX_SessionTokens_Expiry' AND object_id=OBJECT_ID('dbo.SessionTokens'))
    CREATE INDEX IX_SessionTokens_Expiry ON dbo.SessionTokens(ExpiresAt);
IF NOT EXISTS(SELECT 1 FROM dbo.SchemaMigrations WHERE Version='001') INSERT dbo.SchemaMigrations(Version) VALUES('001');
COMMIT;
GO
-- Separate batch: newly added columns are visible to SQL Server compilation.
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name='UX_Orders_Customer_Idempotency' AND object_id=OBJECT_ID('dbo.Orders'))
    CREATE UNIQUE INDEX UX_Orders_Customer_Idempotency ON dbo.Orders(CustomerID,IdempotencyKey) WHERE IdempotencyKey IS NOT NULL;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name='UX_Reviews_VerifiedPurchase' AND object_id=OBJECT_ID('dbo.Reviews'))
    CREATE UNIQUE INDEX UX_Reviews_VerifiedPurchase ON dbo.Reviews(ProductID,CustomerID,OrderID) WHERE OrderID IS NOT NULL;
GO

SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.Orders','AddressSnapshot') IS NULL ALTER TABLE dbo.Orders ADD AddressSnapshot NVARCHAR(1000) NULL;
IF COL_LENGTH('dbo.OrderItems','ProductNameSnapshot') IS NULL ALTER TABLE dbo.OrderItems ADD ProductNameSnapshot NVARCHAR(180) NULL;
IF COL_LENGTH('dbo.OrderItems','UnitSnapshot') IS NULL ALTER TABLE dbo.OrderItems ADD UnitSnapshot NVARCHAR(30) NULL;
IF NOT EXISTS(SELECT 1 FROM dbo.SchemaMigrations WHERE Version='002') INSERT dbo.SchemaMigrations(Version) VALUES('002');
COMMIT;
GO

SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.MutationKeys',N'U') IS NULL
CREATE TABLE dbo.MutationKeys(
    UserID BIGINT NOT NULL,OperationType VARCHAR(40) NOT NULL,RequestKey VARCHAR(100) NOT NULL,RequestHash CHAR(64) NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_MutationKeys PRIMARY KEY(UserID,OperationType,RequestKey),
    CONSTRAINT FK_MutationKeys_Users FOREIGN KEY(UserID) REFERENCES dbo.Users(UserID));
IF NOT EXISTS(SELECT 1 FROM dbo.SchemaMigrations WHERE Version='003') INSERT dbo.SchemaMigrations(Version) VALUES('003');
COMMIT;
GO

SET XACT_ABORT ON;
BEGIN TRANSACTION;
-- Existing duplicates must be resolved deliberately; never delete or rewrite legacy users.
IF EXISTS(SELECT Phone FROM dbo.Users WHERE Phone IS NOT NULL AND Phone<>'' GROUP BY Phone HAVING COUNT(*)>1)
    THROW 50001, 'Duplicate existing phone numbers block the optional unique-phone migration. No user was changed.', 1;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name='UX_Users_OptionalPhone' AND object_id=OBJECT_ID('dbo.Users'))
    CREATE UNIQUE INDEX UX_Users_OptionalPhone ON dbo.Users(Phone) WHERE Phone IS NOT NULL AND Phone<>'';
IF NOT EXISTS(SELECT 1 FROM dbo.SchemaMigrations WHERE Version='004') INSERT dbo.SchemaMigrations(Version) VALUES('004');
COMMIT;
GO

/* KIEM TRA SAU KHI TAO: phai tra ve 25 bang. */
SELECT DB_NAME() AS DatabaseName, COUNT(*) AS TotalTables
FROM sys.tables
WHERE schema_id = SCHEMA_ID(N'dbo');
GO

SET NOEXEC OFF;   -- bat lai thuc thi (neu truoc do bi bo qua do da co cau truc)
GO
IF DB_NAME() <> N'SmartMartDB'
BEGIN
    RAISERROR(N'Khong o database SmartMartDB. Dung script.', 16, 1);
    SET NOEXEC ON;
END
GO
IF OBJECT_ID(N'dbo.Users', N'U') IS NULL
BEGIN
    RAISERROR(N'Chua co cau truc database - khong the nap du lieu mau.', 16, 1);
    SET NOEXEC ON;
END
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
         N'pbkdf2-sha256:210000:9Hd91cTfVLmqmPuSMuqQpQ==:QWTVhYeYO+5Yi8uCXoZvJnlbuOy1iMnv9PpH/Mxv2Ng=', 'ADMIN', 1, SYSDATETIME());

    IF NOT EXISTS (
        SELECT 1 FROM dbo.Users WHERE Email = 'kho@smartmart.local'
    )
    INSERT INTO dbo.Users
        (FullName, Email, Phone, PasswordHash, Role, IsActive, CreatedAt)
    VALUES
        (N'Quản lý kho', 'kho@smartmart.local', '0900000002',
         N'pbkdf2-sha256:210000:9Hd91cTfVLmqmPuSMuqQpQ==:QWTVhYeYO+5Yi8uCXoZvJnlbuOy1iMnv9PpH/Mxv2Ng=', 'WAREHOUSE_MANAGER', 1, SYSDATETIME());

    IF NOT EXISTS (
        SELECT 1 FROM dbo.Users WHERE Email = 'khach1@smartmart.local'
    )
    INSERT INTO dbo.Users
        (FullName, Email, Phone, PasswordHash, Role, IsActive, CreatedAt)
    VALUES
        (N'Nguyễn Minh Anh', 'khach1@smartmart.local', '0900000003',
         N'pbkdf2-sha256:210000:9Hd91cTfVLmqmPuSMuqQpQ==:QWTVhYeYO+5Yi8uCXoZvJnlbuOy1iMnv9PpH/Mxv2Ng=', 'CUSTOMER', 1, SYSDATETIME());

    IF NOT EXISTS (
        SELECT 1 FROM dbo.Users WHERE Email = 'khach2@smartmart.local'
    )
    INSERT INTO dbo.Users
        (FullName, Email, Phone, PasswordHash, Role, IsActive, CreatedAt)
    VALUES
        (N'Trần Hoàng Nam', 'khach2@smartmart.local', '0900000004',
         N'pbkdf2-sha256:210000:9Hd91cTfVLmqmPuSMuqQpQ==:QWTVhYeYO+5Yi8uCXoZvJnlbuOy1iMnv9PpH/Mxv2Ng=', 'CUSTOMER', 1, SYSDATETIME());

    IF NOT EXISTS (
        SELECT 1 FROM dbo.Users WHERE Email = 'shipper@smartmart.local'
    )
    INSERT INTO dbo.Users
        (FullName, Email, Phone, PasswordHash, Role, IsActive, CreatedAt)
    VALUES
        (N'Lê Văn Hùng', 'shipper@smartmart.local', '0900000005',
         N'pbkdf2-sha256:210000:9Hd91cTfVLmqmPuSMuqQpQ==:QWTVhYeYO+5Yi8uCXoZvJnlbuOy1iMnv9PpH/Mxv2Ng=', 'SHIPPER', 1, SYSDATETIME());


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
    INSERT INTO dbo.OrderStatusHistory
        (OrderID, ChangedByUserID, OldStatus, NewStatus, Note, ChangedAt)
    SELECT
        o.OrderID,
        u.UserID,
        v.OldStatus,
        v.NewStatus,
        v.Note,
        DATEADD(MINUTE, v.MinuteOffset, o.CreatedAt)
    FROM (VALUES
        ('SM-DEMO-0001', CAST(NULL AS VARCHAR(20)), 'PENDING',   N'Tạo đơn hàng mẫu', 0),
        ('SM-DEMO-0001', 'PENDING',                  'DELIVERED', N'Đơn hàng mẫu đã giao', 60),
        ('SM-DEMO-0002', CAST(NULL AS VARCHAR(20)), 'PENDING',   N'Tạo đơn hàng mẫu', 0)
    ) v(OrderCode, OldStatus, NewStatus, Note, MinuteOffset)
    JOIN dbo.Orders o ON o.OrderCode = v.OrderCode
    JOIN dbo.Users u ON u.Email = 'admin@smartmart.local'
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.OrderStatusHistory h
        WHERE h.OrderID = o.OrderID
          AND h.NewStatus = v.NewStatus
          AND ISNULL(h.Note, N'') = v.Note
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



/* Demo only. Stable SKU, no overwrite of existing catalog, no fabricated rating/sales/origin. */
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF NOT EXISTS(SELECT 1 FROM dbo.Categories WHERE CategoryName=N'Trái cây tươi') INSERT dbo.Categories(CategoryName,Description) VALUES(N'Trái cây tươi',N'Danh mục trình diễn SmartMart Master');
IF NOT EXISTS(SELECT 1 FROM dbo.Categories WHERE CategoryName=N'Rau củ tươi') INSERT dbo.Categories(CategoryName,Description) VALUES(N'Rau củ tươi',N'Danh mục trình diễn SmartMart Master');
IF NOT EXISTS(SELECT 1 FROM dbo.Categories WHERE CategoryName=N'Sữa và đồ uống') INSERT dbo.Categories(CategoryName,Description) VALUES(N'Sữa và đồ uống',N'Danh mục trình diễn SmartMart Master');
INSERT dbo.Products(CategoryID,SKU,ProductName,Description,Unit,Price,IsActive,IsDemo,Brand)
SELECT c.CategoryID,v.SKU,v.ProductName,v.Description,v.Unit,v.Price,1,1,v.Brand
FROM(VALUES
 ('MASTER-DEMO-APPLE',N'Trái cây tươi',N'Táo đỏ tươi',N'Táo đỏ dùng cho kịch bản mua sắm thử nghiệm. Giá và tồn kho là dữ liệu demo.',N'kg',45000.00,N'Vườn Xanh Demo','apple'),
 ('MASTER-DEMO-ORANGE',N'Trái cây tươi',N'Cam vàng',N'Cam vàng trong danh mục demo.',N'kg',38000.00,N'Vườn Xanh Demo','orange'),
 ('MASTER-DEMO-BANANA',N'Trái cây tươi',N'Chuối chín',N'Chuối trong danh mục demo.',N'nải',25000.00,N'Vườn Xanh Demo','banana'),
 ('MASTER-DEMO-TOMATO',N'Rau củ tươi',N'Cà chua đỏ',N'Cà chua trong danh mục demo.',N'kg',28000.00,N'Nông Sản Demo','tomato'),
 ('MASTER-DEMO-BROCCOLI',N'Rau củ tươi',N'Bông cải xanh',N'Bông cải trong danh mục demo.',N'cây',32000.00,N'Nông Sản Demo','broccoli'),
 ('MASTER-DEMO-LETTUCE',N'Rau củ tươi',N'Xà lách xanh',N'Xà lách trong danh mục demo.',N'bó',15000.00,N'Nông Sản Demo','lettuce'),
 ('MASTER-DEMO-CARROT',N'Rau củ tươi',N'Cà rốt',N'Cà rốt trong danh mục demo.',N'kg',24000.00,N'Nông Sản Demo','carrot'),
 ('MASTER-DEMO-MILK',N'Sữa và đồ uống',N'Sữa tươi',N'Sữa trong danh mục demo.',N'hộp',34000.00,N'SmartMart Demo','milk')
)v(SKU,CategoryName,ProductName,Description,Unit,Price,Brand,Asset)
JOIN dbo.Categories c ON c.CategoryName=v.CategoryName
WHERE NOT EXISTS(SELECT 1 FROM dbo.Products p WHERE p.SKU=v.SKU);
INSERT dbo.ProductImages(ProductID,ImageUrl,AltText,DisplayOrder,IsPrimary)
SELECT p.ProductID,N'/assets/products/'+v.Asset+N'.jpg',p.ProductName,0,1
FROM(VALUES('MASTER-DEMO-APPLE','apple'),('MASTER-DEMO-ORANGE','orange'),('MASTER-DEMO-BANANA','banana'),('MASTER-DEMO-TOMATO','tomato'),('MASTER-DEMO-BROCCOLI','broccoli'),('MASTER-DEMO-LETTUCE','lettuce'),('MASTER-DEMO-CARROT','carrot'),('MASTER-DEMO-MILK','milk'))v(SKU,Asset)
JOIN dbo.Products p ON p.SKU=v.SKU AND p.IsDemo=1
WHERE NOT EXISTS(SELECT 1 FROM dbo.ProductImages i WHERE i.ProductID=p.ProductID);
-- Secondary licensed demo views make the selectable native gallery verifiable.
-- Only this task's labeled apple demo is eligible; primary images and real products stay intact.
INSERT dbo.ProductImages(ProductID,ImageUrl,AltText,DisplayOrder,IsPrimary)
SELECT p.ProductID,N'/assets/products/'+v.Asset+N'.jpg',N'Ảnh minh họa táo đỏ cho dữ liệu demo',v.DisplayOrder,0
FROM(VALUES('apple_detail_1',1),('apple_detail_2',2))v(Asset,DisplayOrder)
JOIN dbo.Products p ON p.SKU='MASTER-DEMO-APPLE' AND p.IsDemo=1
WHERE NOT EXISTS(SELECT 1 FROM dbo.ProductImages i WHERE i.ProductID=p.ProductID AND i.ImageUrl=N'/assets/products/'+v.Asset+N'.jpg');
INSERT dbo.Inventory(ProductID,QuantityOnHand,ReorderLevel)
SELECT p.ProductID,40,5 FROM dbo.Products p WHERE p.SKU LIKE 'MASTER-DEMO-%' AND p.IsDemo=1 AND NOT EXISTS(SELECT 1 FROM dbo.Inventory i WHERE i.ProductID=p.ProductID);
INSERT dbo.ProductSpecifications(ProductID,SpecificationName,SpecificationValue)
SELECT p.ProductID,N'Loại dữ liệu',N'Sản phẩm trình diễn; không phải dữ kiện thương mại đã xác nhận'
FROM dbo.Products p WHERE p.SKU LIKE 'MASTER-DEMO-%' AND p.IsDemo=1 AND NOT EXISTS(SELECT 1 FROM dbo.ProductSpecifications s WHERE s.ProductID=p.ProductID AND s.SpecificationName=N'Loại dữ liệu');
IF NOT EXISTS(SELECT 1 FROM dbo.Vouchers WHERE Code='MASTER10')
INSERT dbo.Vouchers(Code,Description,DiscountType,DiscountValue,MinOrderValue,StartAt,EndAt,UsageLimit)
VALUES('MASTER10',N'Voucher demo giảm 10 phần trăm','PERCENT',10,50000,DATEADD(DAY,-1,SYSDATETIME()),DATEADD(YEAR,1,SYSDATETIME()),100);
COMMIT;
GO

/* KIEM TRA DU LIEU SAU KHI NAP. */
SELECT
    (SELECT COUNT(*) FROM dbo.Users) AS TotalUsers,
    (SELECT COUNT(*) FROM dbo.Categories) AS TotalCategories,
    (SELECT COUNT(*) FROM dbo.Products) AS TotalProducts,
    (SELECT COUNT(*) FROM dbo.Inventory) AS TotalInventoryRows,
    (SELECT COUNT(*) FROM dbo.Orders) AS TotalOrders;
GO

SET NOEXEC OFF;
GO
PRINT N'HOAN TAT: SmartMartDB da san sang.';
GO