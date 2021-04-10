package xyz.pokkst.pokket.lite.util

import com.google.common.util.concurrent.ListenableFuture
import org.bitcoinj.protocols.payments.PaymentSession

class BIP70Helper {
    companion object {
        fun getBtcPaymentSession(url: String?): PaymentSession {
            val future: ListenableFuture<PaymentSession> = PaymentSession.createFromUrl(url)
            return future.get()
        }
    }
}