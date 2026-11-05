package com.smartmart.smartmart_backend

import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

/** Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI. */
class DomainRulesTest {
    @Test fun `password hashing is salted and rejects wrong or malformed values`() {
        val first=Passwords.hash("A secret used only inside this test")
        val second=Passwords.hash("A secret used only inside this test")
        assertNotEquals(first,second)
        assertFalse(first.contains("A secret"))
        assertTrue(Passwords.verify("A secret used only inside this test",first))
        assertFalse(Passwords.verify("incorrect",first))
        for(invalid in listOf("", "plain:password", "pbkdf2-sha256:0:YQ==:Yg==", "pbkdf2-sha256:nope:@@:@@"))
            assertFalse(Passwords.verify("incorrect",invalid))
    }
    @Test fun `catalog rejects invalid ranges and non allowlisted SQL sort`() {
        val valid=CatalogFilter(null,null,null,null,null,null,null,"newest",0,12)
        for(invalid in listOf(valid.copy(page=-1),valid.copy(size=0),valid.copy(size=101),
            valid.copy(minPrice=BigDecimal("-1")),valid.copy(minPrice=BigDecimal.TEN,maxPrice=BigDecimal.ONE),
            valid.copy(minRating=BigDecimal("5.01")),valid.copy(sort="CostPrice; DROP TABLE Products"))) {
            assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ApiException::class.java) { invalid.validate() }.status)
        }
        for(sort in CatalogFilter.SORTS.keys) valid.copy(sort=sort).validate()
    }
    @Test fun `online adapter never treats unverified data as successful payment`() {
        val provider=UnconfiguredPaymentProvider()
        for(method in listOf("CARD","EWALLET")) {
            val error=assertThrows(ApiException::class.java) { provider.createSession(1,method) }
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE,error.status)
            assertEquals("PAYMENT_PROVIDER_UNCONFIGURED",error.code)
        }
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,assertThrows(ApiException::class.java) {
            provider.verifyWebhook("{\"paid\":true}".toByteArray(),"untrusted")
        }.status)
    }
    @Test fun `unexpected errors never expose raw exception text`() {
        val response=ApiErrorHandler().unexpected(IllegalStateException("sensitive connection details"))
        assertEquals(500,response.statusCode.value())
        assertEquals("INTERNAL_ERROR",response.body!!.code)
        assertFalse(response.body!!.message.contains("sensitive"))
    }
}
