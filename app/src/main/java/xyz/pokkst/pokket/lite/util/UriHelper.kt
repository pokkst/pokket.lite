package xyz.pokkst.pokket.lite.util

import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.NetworkParameters
import xyz.pokkst.pokket.lite.wallet.WalletManager
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.*

class UriHelper {
    companion object {
        private var addressOrPayload: String? = null
        private var amount: Coin? = null

        private fun getQueryParams(url: String): Map<String, List<String>> {
            try {
                val params = HashMap<String, List<String>>()
                val urlParts =
                    url.split("\\?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (urlParts.size > 1) {
                    val query = urlParts[1]
                    for (param in query.split("&".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()) {
                        val pair =
                            param.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val key = URLDecoder.decode(pair[0], "UTF-8")
                        var value = ""
                        if (pair.size > 1) {
                            value = URLDecoder.decode(pair[1], "UTF-8")
                        }

                        var values: MutableList<String>? = params[key] as MutableList<String>?
                        if (values == null) {
                            values = ArrayList()
                            params[key] = values
                        }
                        values.add(value)
                    }
                }

                return params
            } catch (ex: UnsupportedEncodingException) {
                throw AssertionError(ex)
            }
        }

        private fun getQueryBaseAddress(url: String): String {
            val urlParts = url.split("\\?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return if (urlParts.size > 1) {
                urlParts[0]
            } else {
                url
            }
        }

        fun parse(uri: String): PaymentContent? {
            val params = WalletManager.parameters
            val mappedVariables = getQueryParams(uri)
            val destinationWithoutPrefix =
                uri.replace("bitcoin:", "")
                    .replace("bitcoin:", "")
            if (destinationWithoutPrefix.contains("http")) {
                addressOrPayload = if (mappedVariables["r"] != null) {
                    (mappedVariables["r"] ?: error(""))[0]
                } else {
                    uri
                }
                return PaymentContent(addressOrPayload, null, PaymentType.BIP70)
            } else {
                amount = if (mappedVariables["amount"] != null) {
                    val amountVariable = (mappedVariables["amount"] ?: error(""))[0]
                    Coin.parseCoin(amountVariable)
                } else {
                    null
                }

                addressOrPayload = when {
                    uri.startsWith("bitcoin") -> getQueryBaseAddress(
                        uri
                    ).replace("bitcoin:", "")
                    else -> getQueryBaseAddress(uri)
                }

                if (isValidAddress(
                        WalletManager.parameters,
                        addressOrPayload
                    )
                ) {
                    return PaymentContent(addressOrPayload, amount, PaymentType.ADDRESS)
                }

            }

            return null
        }

        private fun isValidAddress(
            networkParameters: NetworkParameters,
            address: String?
        ): Boolean {
            return try {
                Address.fromString(networkParameters, address)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}