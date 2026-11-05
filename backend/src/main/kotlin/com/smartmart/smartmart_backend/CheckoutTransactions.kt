package com.smartmart.smartmart_backend

import org.slf4j.LoggerFactory
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.ThreadLocalRandom

/** Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
 * SQL Server có thể chọn một checkout làm deadlock victim dù dữ liệu/tồn vẫn hợp lệ.
 * Thử lại toàn bộ thao tác trong transaction mới sau khi transaction lỗi đã rollback;
 * không tiếp tục chạy câu SQL trong transaction đã bị SQL Server hủy.
 */
@Component
class CheckoutTransactions(transactionManager: PlatformTransactionManager) {
    private val logger=LoggerFactory.getLogger(javaClass)
    private val transaction=TransactionTemplate(transactionManager).apply {
        propagationBehavior=TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun <T: Any> execute(operation: () -> T): T {
        for(attempt in 1..MAX_ATTEMPTS) {
            try {
                return transaction.execute { operation() }
                    ?: error("Checkout transaction returned no result")
            } catch(error: PessimisticLockingFailureException) {
                // TransactionTemplate has finished rollback before this catch/backoff.
                // Keep request/body/idempotency key unchanged across every attempt.
                logger.warn("Checkout lock conflict; attempt {}/{}",attempt,MAX_ATTEMPTS)
                if(attempt==MAX_ATTEMPTS)
                    conflict("CHECKOUT_BUSY","Hệ thống đang xử lý nhiều đơn cùng lúc. Vui lòng thử đặt hàng lại")
                try {
                    Thread.sleep(25L*attempt+ThreadLocalRandom.current().nextLong(25L))
                } catch(interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw ApiException(HttpStatus.SERVICE_UNAVAILABLE,"CHECKOUT_INTERRUPTED","Yêu cầu đặt hàng bị gián đoạn. Vui lòng thử lại")
                }
            }
        }
        error("Unreachable checkout retry state")
    }

    companion object { private const val MAX_ATTEMPTS=3 }
}
