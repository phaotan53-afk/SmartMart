package com.smartmart.smartmart_backend

import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.dao.CannotAcquireLockException
import org.springframework.transaction.support.TransactionSynchronizationManager

/** Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
 * Tests thực thi service proxy và transaction trên SQL Server thật, không H2/mock JDBC.
 * Chỉ cho phép DB phát triển riêng của Master. Fixture có nhãn TEST và được ngừng
 * kinh doanh/vô hiệu hóa sau test; lịch sử ghi không bị xóa để giữ FK và đối soát.
 */
@SpringBootTest
class SqlServerIntegrationTest {
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var auth: AuthService
    @Autowired lateinit var catalog: CatalogService
    @Autowired lateinit var commerce: CommerceService
    @Autowired lateinit var admin: AdminService
    @Autowired lateinit var shipping: ShippingService
    @Autowired lateinit var checkoutTransactions: CheckoutTransactions
    private val users=mutableListOf<Long>()
    private val products=mutableListOf<Long>()
    private val categories=mutableListOf<Int>()
    private var administrator: SessionResponse?=null
    private val password="Test-only password not used by real users"
    private data class Fixture(val id: Long,val categoryId: Int,val sku: String)

    @BeforeEach fun requireDedicatedDatabase() {
        assertEquals("SmartMartMaster_20261105",jdbc.queryForObject("SELECT DB_NAME()",String::class.java),
            "Integration tests must never write to the team's SmartMart database")
        assertTrue(jdbc.dataSource!!.connection.use { it.metaData.databaseProductName.contains("SQL Server") })
    }
    @AfterEach fun retireOwnedFixtures() {
        for(id in products) jdbc.update("UPDATE dbo.Products SET IsActive=0 WHERE ProductID=? AND SKU LIKE 'MASTER-TEST-%'",id)
        for(id in categories) assertEquals(1,jdbc.update("UPDATE dbo.Categories SET IsActive=0 WHERE CategoryID=? AND LEFT(CategoryName,6)=N'[TEST]'",id))
        for(id in users) jdbc.update("UPDATE dbo.Users SET IsActive=0 WHERE UserID=? AND Email LIKE '%@master-test.invalid'",id)
        products.clear();categories.clear();users.clear();administrator=null
    }
    private fun user(phone: String?=null): SessionResponse {
        val response=auth.register(RegisterRequest("[TEST] SQL integration",UUID.randomUUID().toString()+"@master-test.invalid",password,phone))
        users+=response.user.id
        return response
    }
    private fun actor(): Long {
        if(administrator==null) {
            administrator=user()
            jdbc.update("UPDATE dbo.Users SET Role='ADMIN' WHERE UserID=?",administrator!!.user.id)
        }
        return administrator!!.user.id
    }
    private fun product(stock: Int=5): Fixture {
        val suffix=UUID.randomUUID().toString().take(15)
        val categoryId=(admin.saveCategory(null,CategoryWriteRequest("[TEST] $suffix","SQL integration fixture"))["id"] as Number).toInt()
        categories+=categoryId
        val sku="MASTER-TEST-$suffix"
        val id=(admin.saveProduct(null,ProductWriteRequest(categoryId=categoryId,sku=sku,name="[TEST] $suffix",description="Isolated SQL fixture",unit="hộp",price=BigDecimal("12000.50"),costPrice=BigDecimal("4000.00"),brand="Test",imageUrl="/assets/products/apple.jpg"))["id"] as Number).toLong()
        products+=id
        admin.count(actor(),id,InventoryCount(stock,0,"[TEST] fixture stock"),UUID.randomUUID().toString())
        return Fixture(id,categoryId,sku)
    }
    private fun address(userId: Long): Long = (commerce.saveAddress(userId,null,AddressRequest("[TEST] Recipient","0901234567","1 Đường kiểm thử",null,"Quận kiểm thử","TP kiểm thử",true))["id"] as Number).toLong()
    private fun stock(productId: Long)=jdbc.queryForObject("SELECT QuantityOnHand FROM dbo.Inventory WHERE ProductID=?",Int::class.java,productId)!!
    private fun orderCount(userId: Long)=jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Orders WHERE CustomerID=?",Int::class.java,userId)!!
    private fun request(token: String)=MockHttpServletRequest().also { it.addHeader("Authorization","Bearer $token") }

    @Test fun `sessions are hashed and profile phone email uniqueness and ownership use real SQL`() {
        val phone="09"+(UUID.randomUUID().mostSignificantBits.toString().filter(Char::isDigit)+"12345678").take(8)
        val customer=user(phone);val other=user()
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.SessionTokens WHERE TokenHash=?",Int::class.java,Passwords.digest(customer.token)))
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.SessionTokens WHERE TokenHash=?",Int::class.java,customer.token))
        assertEquals(customer.user.id,auth.login(LoginRequest(phone,password)).user.id)
        val newEmail=UUID.randomUUID().toString()+"@master-test.invalid"
        assertEquals(newEmail,auth.profile(customer.user.id,ProfileRequest("[TEST] Updated",phone,newEmail)).email)
        assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException::class.java) { auth.profile(other.user.id,ProfileRequest("[TEST] Other",phone)) }.status)
        val aid=address(customer.user.id)
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ApiException::class.java) { commerce.address(other.user.id,aid) }.status)
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ApiException::class.java) { auth.roles(request(customer.token),"ADMIN") }.status)
        auth.logout(request(customer.token))
        assertEquals(HttpStatus.UNAUTHORIZED,assertThrows(ApiException::class.java) { auth.current(request(customer.token)) }.status)
    }

    @Test fun `invalid voucher and blocked online checkout roll back without losing cart or stock`() {
        val customer=user();val item=product(3);val aid=address(customer.user.id)
        commerce.setCart(customer.user.id,item.id,2,true)
        val before=orderCount(customer.user.id)
        assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException::class.java) { commerce.checkout(customer.user.id,UUID.randomUUID().toString(),CheckoutRequest(aid,"COD","MISSING-"+UUID.randomUUID().toString().take(12))) }.status)
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,assertThrows(ApiException::class.java) { commerce.checkout(customer.user.id,UUID.randomUUID().toString(),CheckoutRequest(aid,"CARD")) }.status)
        assertEquals(3,stock(item.id));assertEquals(2,commerce.cart(customer.user.id).itemCount)
        assertEquals(before,orderCount(customer.user.id))
    }

    @Test fun `checkout snapshots price name and address and replay cancel restores stock once`() {
        val customer=user();val other=user();val item=product(3);val aid=address(customer.user.id)
        commerce.setCart(customer.user.id,item.id,2,true)
        val key=UUID.randomUUID().toString();val body=CheckoutRequest(aid,"COD",customerNote="[TEST] snapshots")
        val order=commerce.checkout(customer.user.id,key,body);val oid=(order["id"] as Number).toLong()
        assertEquals("UNPAID",order["paymentStatus"]);assertEquals("COD",order["paymentMethod"])
        assertEquals(1,stock(item.id));assertEquals(0,commerce.cart(customer.user.id).itemCount)
        jdbc.update("UPDATE dbo.Products SET ProductName=N'[TEST] renamed',Price=999 WHERE ProductID=?",item.id)
        commerce.saveAddress(customer.user.id,aid,AddressRequest("[TEST] Modified","0901234567","Changed address",null,"Other district","Other province"))
        val stored=commerce.order(customer.user.id,oid)
        @Suppress("UNCHECKED_CAST") val lines=stored["items"] as List<Map<String,Any?>>
        assertEquals(BigDecimal("12000.50"),(lines.first()["price"] as BigDecimal).setScale(2))
        assertNotEquals("[TEST] renamed",lines.first()["name"])
        assertFalse((stored["addressSnapshot"] as String).contains("Changed address"))
        assertEquals(oid,(commerce.checkout(customer.user.id,key,body)["id"] as Number).toLong())
        assertEquals(1,orderCount(customer.user.id));assertEquals(1,stock(item.id))
        assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException::class.java) { commerce.checkout(customer.user.id,key,body.copy(customerNote="other content")) }.status)
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ApiException::class.java) { commerce.order(other.user.id,oid) }.status)
        commerce.cancel(customer.user.id,oid);commerce.cancel(customer.user.id,oid)
        assertEquals(3,stock(item.id));assertEquals("CANCELLED",commerce.order(customer.user.id,oid)["orderStatus"])
    }

    @Test fun `competing buyers cannot both buy the last stock unit`() {
        val item=product(1);val first=user();val second=user()
        val aid1=address(first.user.id);val aid2=address(second.user.id)
        commerce.setCart(first.user.id,item.id,1,true);commerce.setCart(second.user.id,item.id,1,true)
        val start=CountDownLatch(1);val executor=Executors.newFixedThreadPool(2)
        try {
            val tasks=listOf(first.user.id to aid1,second.user.id to aid2).map { (uid,aid) ->
                executor.submit(Callable {
                    start.await()
                    try { commerce.checkout(uid,UUID.randomUUID().toString(),CheckoutRequest(aid));"SUCCESS" }
                    catch(error: ApiException) { assertEquals(HttpStatus.CONFLICT,error.status);error.code }
                })
            }
            start.countDown()
            val outcomes=tasks.map { it.get(20,TimeUnit.SECONDS) }
            assertEquals(1,outcomes.count { it=="SUCCESS" })
            assertEquals(1,outcomes.count { it=="INSUFFICIENT_STOCK" })
            assertEquals(0,stock(item.id));assertEquals(1,orderCount(first.user.id)+orderCount(second.user.id))
            assertEquals(1,commerce.cart(first.user.id).itemCount+commerce.cart(second.user.id).itemCount)
        } finally { executor.shutdownNow() }
    }

    @Test fun `concurrent retries with one key create only one order and stock debit`() {
        val item=product(3);val customer=user();val aid=address(customer.user.id)
        commerce.setCart(customer.user.id,item.id,1,true)
        val key=UUID.randomUUID().toString();val start=CountDownLatch(1);val executor=Executors.newFixedThreadPool(2)
        try {
            val tasks=(1..2).map { executor.submit(Callable { start.await();(commerce.checkout(customer.user.id,key,CheckoutRequest(aid))["id"] as Number).toLong() }) }
            start.countDown()
            assertEquals(tasks[0].get(20,TimeUnit.SECONDS),tasks[1].get(20,TimeUnit.SECONDS))
            assertEquals(1,orderCount(customer.user.id));assertEquals(2,stock(item.id))
        } finally { executor.shutdownNow() }
    }

    @Test fun `translated deadlock failure rolls back writes before retrying in a fresh SQL transaction`() {
        val item=product(3)
        var attempts=0
        val resources=mutableListOf<Any>()
        val result=checkoutTransactions.execute {
            attempts++
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive())
            resources+=requireNotNull(TransactionSynchronizationManager.getResource(jdbc.dataSource!!))
            jdbc.update("UPDATE dbo.Inventory SET QuantityOnHand=QuantityOnHand-1 WHERE ProductID=?",item.id)
            // Deterministic translated-deadlock injection after a real SQL write. The
            // separate two-buyer test above exercises real competing SQL transactions.
            if(attempts==1) throw CannotAcquireLockException("Simulated translated SQL Server deadlock victim")
            stock(item.id)
        }
        assertEquals(2,attempts)
        assertNotSame(resources[0],resources[1],"Retry must acquire a fresh transaction-bound SQL connection holder")
        assertEquals(2,result)
        assertEquals(2,stock(item.id),"Only the successful transaction may debit stock")
    }

    @Test fun `exhausted deadlock retries roll back every write and return explicit busy conflict`() {
        val item=product(3)
        var attempts=0
        val error=assertThrows(ApiException::class.java) {
            checkoutTransactions.execute<Unit> {
                attempts++
                jdbc.update("UPDATE dbo.Inventory SET QuantityOnHand=QuantityOnHand-1 WHERE ProductID=?",item.id)
                throw CannotAcquireLockException("Simulated repeated translated deadlock victim")
            }
        }
        assertEquals(3,attempts)
        assertEquals(HttpStatus.CONFLICT,error.status)
        assertEquals("CHECKOUT_BUSY",error.code)
        assertEquals(3,stock(item.id),"No failed attempt may leave a stock debit")
    }

    @Test fun `business conflicts roll back once and are never retried as deadlocks`() {
        val item=product(3)
        var attempts=0
        val error=assertThrows(ApiException::class.java) {
            checkoutTransactions.execute<Unit> {
                attempts++
                jdbc.update("UPDATE dbo.Inventory SET QuantityOnHand=QuantityOnHand-1 WHERE ProductID=?",item.id)
                conflict("INSUFFICIENT_STOCK","[TEST] business conflict")
            }
        }
        assertEquals(1,attempts)
        assertEquals("INSUFFICIENT_STOCK",error.code)
        assertEquals(3,stock(item.id))
    }

    @Test fun `inventory retry key is transactional when an adjustment fails`() {
        val item=product(2);val actor=actor();val key=UUID.randomUUID().toString()
        assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException::class.java) { admin.adjust(actor,item.id,InventoryAdjustment(-3,"ADJUSTMENT","[TEST] reject negative"),key) }.status)
        assertEquals(2,stock(item.id))
        admin.adjust(actor,item.id,InventoryAdjustment(1,"IMPORT","[TEST] receive stock"),key)
        admin.adjust(actor,item.id,InventoryAdjustment(1,"IMPORT","[TEST] receive stock"),key)
        assertEquals(3,stock(item.id))
        assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException::class.java) { admin.adjust(actor,item.id,InventoryAdjustment(2,"IMPORT","[TEST] changed retry"),key) }.status)
        assertEquals(3,stock(item.id))
    }

    @Test fun `unknown inventory is unavailable and cannot be added to cart`() {
        val item=product(2);val customer=user()
        jdbc.update("DELETE dbo.InventoryTransactions WHERE InventoryID IN(SELECT InventoryID FROM dbo.Inventory WHERE ProductID=?)",item.id)
        jdbc.update("DELETE dbo.Inventory WHERE ProductID=?",item.id)
        val detail=catalog.detail(item.id)
        assertEquals("UNAVAILABLE",detail.stockStatus);assertNull(detail.quantityOnHand)
        assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException::class.java) { commerce.setCart(customer.user.id,item.id,1,true) }.status)
        assertTrue(commerce.cart(customer.user.id).items.isEmpty())
    }

    @Test fun `assignment retry preserves shipment progress and changing shipper after pickup is rejected`() {
        val customer=user();val item=product(2);val aid=address(customer.user.id)
        val first=user();val second=user();val actor=actor()
        jdbc.update("UPDATE dbo.Users SET Role='SHIPPER' WHERE UserID IN (?,?)",first.user.id,second.user.id)
        commerce.setCart(customer.user.id,item.id,1,true)
        val oid=(commerce.checkout(customer.user.id,UUID.randomUUID().toString(),CheckoutRequest(aid))["id"] as Number).toLong()
        admin.orderStatus(actor,oid,"CONFIRMED")
        val sid=(shipping.assign(actor,oid,first.user.id)["id"] as Number).toLong()
        shipping.status(first.user.id,sid,"PICKED_UP")
        assertEquals("PICKED_UP",shipping.assign(actor,oid,first.user.id)["status"])
        assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException::class.java) { shipping.assign(actor,oid,second.user.id) }.status)
        admin.orderStatus(actor,oid,"PREPARING")
        shipping.status(first.user.id,sid,"IN_TRANSIT")
        assertEquals("IN_TRANSIT",shipping.assign(actor,oid,first.user.id)["status"])
        assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException::class.java) { shipping.assign(actor,oid,second.user.id) }.status)
        assertEquals("DELIVERED",shipping.status(first.user.id,sid,"DELIVERED")["status"])
    }
}
