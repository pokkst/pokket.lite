package xyz.pokkst.pokket.lite.util

import org.bitcoinj.core.Coin

class PaymentContent(val addressOrPayload: String?, val amount: Coin?, val paymentType: PaymentType)

enum class PaymentType {
    ADDRESS,
    BIP70
}