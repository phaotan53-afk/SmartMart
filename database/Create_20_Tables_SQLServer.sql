
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

/* VERIFY: should return 20 user tables */
SELECT COUNT(*) AS TotalTables
FROM sys.tables
WHERE schema_id = SCHEMA_ID('dbo');
GO
