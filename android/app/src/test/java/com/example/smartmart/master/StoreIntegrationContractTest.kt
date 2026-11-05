package com.example.smartmart.master

import androidx.lifecycle.SavedStateHandle
import com.example.smartmart.domain.model.Product
import com.example.smartmart.domain.model.StockStatus
import com.example.smartmart.testutil.MainDispatcherRule
import com.google.gson.Gson
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

/** Bổ sung đợt hợp nhất, có hỗ trợ AI. Contract tests; không thay thế SQL integration. */
@OptIn(ExperimentalCoroutinesApi::class)
class StoreIntegrationContractTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = mockk<StoreRepository>()
    private val customer = User(1, "Khách kiểm thử", "test@example.invalid", null, "CUSTOMER")
    private val product = Product(1, "Táo kiểm thử", BigDecimal("35000"), description = null, imageUrl = null, stockStatus = StockStatus.IN_STOCK)
    private fun model(saved: SavedStateHandle = SavedStateHandle()): StoreViewModel {
        coEvery { repository.catalog(any()) } returns ProductPage(listOf(product), 0, 1, 1)
        coEvery { repository.filters() } returns Filters()
        coEvery { repository.login(any(), any()) } returns customer
        coEvery { repository.cart() } returns Cart()
        coEvery { repository.favorites() } returns emptyList()
        return StoreViewModel(repository, saved)
    }
    private fun order() = Order(14, "SM-TEST", "PENDING", "UNPAID", BigDecimal("35000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal("35000"), "2026-11-05T08:00:00", paymentMethod = "COD")

    @Test fun `failed cart write never reports added or changes count`() = runTest {
        val vm = model(); advanceUntilIdle(); vm.authenticate(false, "", "test@example.invalid", "password", ""); advanceUntilIdle(); vm.messageConsumed()
        coEvery { repository.add(1, 2) } throws StoreFailure("Tồn kho đã thay đổi", "INSUFFICIENT_STOCK")
        vm.addToCart(product, 2); advanceUntilIdle()
        assertEquals(0, vm.state.value.cart.itemCount); assertEquals("Tồn kho đã thay đổi", vm.state.value.message); assertFalse(vm.state.value.busy)
    }
    @Test fun `checkout retry reuses key and saved order needs no followup cart request`() = runTest {
        val vm = model(); advanceUntilIdle()
        var firstKey: String? = null; var secondKey: String? = null
        coEvery { repository.checkout(any(), any()) } coAnswers { firstKey = firstArg(); throw StoreFailure("Kết nối gián đoạn", "NETWORK") }
        vm.checkout(2, "COD", "", ""); advanceUntilIdle(); assertNull(vm.state.value.selectedOrder)
        coEvery { repository.checkout(any(), any()) } coAnswers { secondKey = firstArg(); order() }
        vm.checkout(2, "COD", "", ""); advanceUntilIdle()
        assertEquals(firstKey, secondKey); assertEquals(14L, vm.state.value.selectedOrder?.id); assertEquals("UNPAID", vm.state.value.selectedOrder?.paymentStatus)
        assertEquals(Destination.ORDER_DETAIL, vm.state.value.destination); coVerify(exactly = 0) { repository.cart() }
    }
    @Test fun `late old voucher quote cannot overwrite latest total`() = runTest {
        val vm = model(); advanceUntilIdle(); val old = CompletableDeferred<CheckoutQuote>()
        val fresh = CheckoutQuote(BigDecimal(100), BigDecimal(10), BigDecimal(2), BigDecimal(92))
        coEvery { repository.quote(match { it.voucherCode == "OLD" }) } coAnswers { withContext(NonCancellable) { old.await() } }
        coEvery { repository.quote(match { it.voucherCode == "NEW" }) } returns fresh
        vm.quote(7, "OLD"); advanceTimeBy(301); runCurrent(); vm.quote(7, "NEW"); advanceTimeBy(301); runCurrent()
        assertEquals(fresh, vm.state.value.quote)
        old.complete(CheckoutQuote(BigDecimal(100), BigDecimal.ZERO, BigDecimal(2), BigDecimal(102))); advanceUntilIdle()
        assertEquals(fresh, vm.state.value.quote); assertEquals(quoteSignature(7, "NEW"), vm.state.value.quoteSignature)
    }
    @Test fun `combined filters and selected ID survive recreation`() = runTest {
        coEvery { repository.reviews(any(), any(), any()) } returns ReviewPage()
        val saved = SavedStateHandle(); val vm = model(saved); advanceUntilIdle()
        val query = CatalogQuery(query = "táo", categoryId = 3, minPrice = "10000", maxPrice = "50000", minRating = 4, inStock = true, brand = "Demo", sort = "price_asc", page = 2)
        vm.setQuery(query); vm.openProduct(91); advanceUntilIdle(); val restored = model(saved); advanceUntilIdle()
        assertEquals(query, restored.state.value.query); assertEquals(91L, restored.state.value.selectedProductId); assertEquals(Destination.DETAIL, restored.state.value.destination)
    }
    @Test fun `inventory retry keeps mutation key`() = runTest {
        val vm = model(); advanceUntilIdle(); coEvery { repository.management(any()) } returns ManagementState()
        var key1: Any? = null; var key2: Any? = null
        coEvery { repository.adminAction("inventory", 4, any()) } coAnswers { key1 = thirdArg<Map<String, Any?>>() ["_idempotencyKey"]; throw StoreFailure("Mất kết nối", "NETWORK") }
        val body = mapOf<String, Any?>("quantityChange" to 2, "type" to "IMPORT", "note" to "test")
        vm.adminAction("inventory", 4, body); advanceUntilIdle()
        coEvery { repository.adminAction("inventory", 4, any()) } coAnswers { key2 = thirdArg<Map<String, Any?>>() ["_idempotencyKey"]; Unit }
        vm.adminAction("inventory", 4, body); advanceUntilIdle(); assertNotNull(key1); assertEquals(key1, key2)
    }
    @Test fun `profile forwards updated email and phone`() = runTest {
        val vm = model(); advanceUntilIdle()
        coEvery { repository.profile("Tên mới", "0901234567", "new@example.invalid") } returns customer.copy(fullName = "Tên mới", email = "new@example.invalid", phone = "0901234567")
        vm.updateProfile("Tên mới", "0901234567", "new@example.invalid"); advanceUntilIdle(); assertEquals("new@example.invalid", vm.state.value.user?.email)
    }
    @Test fun `address write excludes response id from strict server request`() = runTest {
        val api = mockk<StoreApi>(); val request = slot<AddressWriteRequest>()
        val address = Address(recipientName = "Người nhận", phone = "0901234567", addressLine = "1 Đường thử", district = "Quận thử", province = "Thành phố thử")
        coEvery { api.saveAddress(capture(request)) } returns address.copy(id = 12)
        assertEquals(12L, ApiStoreRepository(api, SessionToken()).saveAddress(address).id)
        val json = Gson().toJsonTree(request.captured).asJsonObject; assertFalse(json.has("id")); assertEquals("Người nhận", json["recipientName"].asString)
    }
    @Test fun `cart uses committed response when followup read would fail`() = runTest {
        val api = mockk<StoreApi>(); val committed = Cart(itemCount = 2, total = BigDecimal("70000"))
        coEvery { api.addToCart(CartRequest(1, 2)) } returns committed; coEvery { api.cart() } throws java.io.IOException("read after write failed")
        assertEquals(committed, ApiStoreRepository(api, SessionToken()).add(1, 2)); coVerify(exactly = 0) { api.cart() }
    }
    @Test fun `missing unit never invents kg and money uses half up`() { assertEquals("35.001đ", money(BigDecimal("35000.5"))); assertEquals("35.000đ/hộp", money(BigDecimal("35000"), "hộp")) }
    @Test fun `review forwards owned order and trimmed comment then uses returned aggregate`() = runTest {
        val vm = model(); coEvery { repository.reviews(1, null, 0) } returns ReviewPage(); advanceUntilIdle(); vm.openProduct(1); advanceUntilIdle()
        coEvery { repository.review(1, 71, 4, "Tươi ngon") } returns Unit
        val savedReviews = ReviewPage(averageRating = BigDecimal(4), reviewCount = 1)
        coEvery { repository.reviews(1, null, 0) } returns savedReviews
        vm.review(1, 71, 4, "  Tươi ngon  "); advanceUntilIdle()
        coVerify(exactly = 1) { repository.review(1, 71, 4, "Tươi ngon") }; assertEquals(savedReviews, vm.state.value.reviews)
    }
    @Test fun `late review completion cannot replace reviews of newly selected product`() = runTest {
        val vm = model(); coEvery { repository.reviews(any(), any(), any()) } returns ReviewPage(); advanceUntilIdle(); vm.openProduct(1); advanceUntilIdle()
        val writing = CompletableDeferred<Unit>()
        coEvery { repository.review(1, 71, 5, "Tươi") } coAnswers { writing.await() }
        vm.review(1, 71, 5, "Tươi"); runCurrent(); vm.openProduct(2); advanceUntilIdle()
        coEvery { repository.reviews(1, null, 0) } returns ReviewPage(reviewCount = 100)
        writing.complete(Unit); advanceUntilIdle()
        assertEquals(2L, vm.state.value.selectedProductId); assertEquals(0, vm.state.value.reviews.reviewCount)
    }
}
