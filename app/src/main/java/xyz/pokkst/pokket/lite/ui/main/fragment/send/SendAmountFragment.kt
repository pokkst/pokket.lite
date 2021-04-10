package xyz.pokkst.pokket.lite.ui.main.fragment.send

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.android.synthetic.main.component_input_numpad.view.*
import kotlinx.android.synthetic.main.fragment_send_amount.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.glxn.qrgen.android.QRCode
import org.bitcoinj.core.*
import org.bitcoinj.protocols.payments.PaymentProtocol
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import xyz.pokkst.pokket.lite.MainActivity
import xyz.pokkst.pokket.lite.R
import xyz.pokkst.pokket.lite.util.*
import xyz.pokkst.pokket.lite.wallet.WalletManager
import java.util.*


/**
 * A placeholder fragment containing a simple view.
 */
class SendAmountFragment : Fragment() {
    var root: View? = null
    var paymentContent: PaymentContent? = null

    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Constants.ACTION_MAIN_ENABLE_PAGER == intent.action) {
                this@SendAmountFragment.findNavController()
                    .popBackStack(R.id.sendHomeFragment, false)
            } else if (Constants.ACTION_FRAGMENT_SEND_SEND == intent.action) {
                if (getCoinAmount() != Coin.ZERO) {
                    this@SendAmountFragment.send()
                } else {
                    showToast("enter an amount")
                }
            } else if (Constants.ACTION_FRAGMENT_SEND_MAX == intent.action) {
                val balance = WalletManager.kit?.wallet()?.getBalance(Wallet.BalanceType.ESTIMATED)
                    ?.toPlainString()
                val coinBalance = Coin.parseCoin(balance)
                setCoinAmount(coinBalance)
            }
        }
    }

    var bchIsSendType = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (activity as? MainActivity)?.toggleSendScreen(false)
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        root = inflater.inflate(R.layout.fragment_send_amount, container, false)
        prepareViews()
        setListeners()
        return root
    }

    private fun prepareViews() {
        (activity as? MainActivity)?.toggleSendScreen(true)

        root?.input_type_toggle?.isChecked = true

        paymentContent = arguments?.getString("address", null)?.let { UriHelper.parse(it) }
        if (paymentContent != null) {
            when (paymentContent?.paymentType) {
                PaymentType.BIP70 -> this.getBIP70Data(root, paymentContent?.addressOrPayload)
                else -> root?.to_field_text?.text = "to: ${paymentContent?.addressOrPayload}"
            }

            if (paymentContent?.amount != null) {
                root?.send_amount_input?.isEnabled = false
                setCoinAmount(paymentContent?.amount)
            }
        } else {
            root?.to_field_text?.visibility = View.GONE
            root?.to_field_edit_text?.visibility = View.VISIBLE
        }
    }

    private fun setListeners() {
        root?.input_type_toggle?.setOnClickListener {
            if (PriceHelper.price != 0.0) {
                bchIsSendType = !bchIsSendType
                swapSendTypes(root)
            }
        }

        val charInputListener = View.OnClickListener { v ->
            if (root?.send_amount_input?.isEnabled == true) {
                val view = v as Button
                appendCharacterToInput(root, view.text.toString())
                updateAltCurrencyDisplay(root)
            }
        }

        val decimalListener = View.OnClickListener { v ->
            if (root?.send_amount_input?.isEnabled == true) {
                val view = v as Button
                appendCharacterToInput(root, view.text.toString())
                updateAltCurrencyDisplay(root)
            }
        }

        root?.input_0?.setOnClickListener(charInputListener)
        root?.input_1?.setOnClickListener(charInputListener)
        root?.input_2?.setOnClickListener(charInputListener)
        root?.input_3?.setOnClickListener(charInputListener)
        root?.input_4?.setOnClickListener(charInputListener)
        root?.input_5?.setOnClickListener(charInputListener)
        root?.input_6?.setOnClickListener(charInputListener)
        root?.input_7?.setOnClickListener(charInputListener)
        root?.input_8?.setOnClickListener(charInputListener)
        root?.input_9?.setOnClickListener(charInputListener)
        root?.decimal_button?.setOnClickListener(decimalListener)
        root?.delete_button?.setOnClickListener {
            if (root?.send_amount_input?.isEnabled == true) {
                val newValue = root?.send_amount_input?.text.toString().dropLast(1)
                root?.send_amount_input?.setText(newValue)
                updateAltCurrencyDisplay(root)
            }
        }

        val filter = IntentFilter()
        filter.addAction(Constants.ACTION_MAIN_ENABLE_PAGER)
        filter.addAction(Constants.ACTION_FRAGMENT_SEND_SEND)
        filter.addAction(Constants.ACTION_FRAGMENT_SEND_MAX)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
    }

    private fun send() {
        if (WalletManager.wallet?.getBalance(Wallet.BalanceType.ESTIMATED)?.isZero == false) {
            if (paymentContent != null || root?.to_field_edit_text?.text?.isNotEmpty() == true) {
                if (paymentContent == null) paymentContent =
                    root?.to_field_edit_text?.text?.toString()?.let { UriHelper.parse(it) }
                val destination = paymentContent?.addressOrPayload
                when (paymentContent?.paymentType) {
                    PaymentType.BIP70 -> destination?.let { this.processBIP70(it) }
                    PaymentType.ADDRESS -> this.processNormalTransaction()
                    null -> showToast("please enter a valid destination")
                }
            } else {
                showToast("please enter an address")
            }
        } else {
            showToast("wallet balance is zero")
        }
    }

    private fun swapSendTypes(root: View?) {
        if (root != null) {
            if (bchIsSendType) {
                //We are changing from BCH as the alt currency.
                val bchValue = root.alt_currency_display.text.toString()
                val fiatValue = root.send_amount_input.text.toString()
                root.main_currency_symbol.text = resources.getString(R.string.b_symbol)
                root.alt_currency_symbol.text = resources.getString(R.string.fiat_symbol)
                root.send_amount_input.setText(bchValue)
                root.alt_currency_display.text = fiatValue
            } else {
                //We are changing from fiat as the alt currency.
                val bchValue = root.send_amount_input.text.toString()
                val fiatValue = root.alt_currency_display.text.toString()
                root.main_currency_symbol.text = resources.getString(R.string.fiat_symbol)
                root.alt_currency_symbol.text = resources.getString(R.string.b_symbol)
                root.alt_currency_display.text = bchValue
                root.send_amount_input.setText(fiatValue)
            }
        }
    }

    private fun appendCharacterToInput(root: View?, char: String) {
        if (root != null) {
            if (char == "." && !root.send_amount_input.text.toString().contains(".")) {
                root.send_amount_input.append(char)
            } else if (char != ".") {
                root.send_amount_input.append(char)
            }
        }
    }

    private fun updateAltCurrencyDisplay(root: View?) {
        if (root != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                if (root.send_amount_input.text.toString().isNotEmpty()) {
                    val value = if (root.send_amount_input.text.toString() == ".")
                        0.0
                    else
                        java.lang.Double.parseDouble(root.send_amount_input.text.toString())

                    val price = PriceHelper.price

                    activity?.runOnUiThread {
                        root.alt_currency_display.text = if (bchIsSendType) {
                            val fiatValue = value * price
                            BalanceFormatter.formatBalance(fiatValue, "0.00")
                        } else {
                            val bchValue = value / price
                            BalanceFormatter.formatBalance(bchValue, "#.########")
                        }
                    }
                } else {
                    activity?.runOnUiThread {
                        root.alt_currency_display.text = null
                    }
                }
            }
        }
    }

    private fun processBIP70(url: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                processBchBIP70(url)
            } catch (e: InsufficientMoneyException) {
                e.printStackTrace()
                showToast("not enough coins in wallet")
            } catch (e: Wallet.CouldNotAdjustDownwards) {
                e.printStackTrace()
                showToast("error adjusting downwards")
            } catch (e: Wallet.ExceededMaxTransactionSize) {
                e.printStackTrace()
                showToast("transaction is too big")
            } catch (e: Exception) {
                e.printStackTrace()
                e.message?.let {
                    showToast(it)
                }
            }
        }
    }

    private fun processBchBIP70(url: String) {
        val session = BIP70Helper.getBtcPaymentSession(url)
        if (session.isExpired) {
            showToast("invoice is expired")
            return
        }

        val req = session.sendRequest
        req.allowUnconfirmed()
        WalletManager.wallet?.completeTx(req)

        val ack = session.sendPayment(
            ImmutableList.of(req.tx),
            WalletManager.wallet?.freshReceiveAddress(),
            null
        )
        if (ack != null) {
            Futures.addCallback<PaymentProtocol.Ack>(
                ack,
                object : FutureCallback<PaymentProtocol.Ack> {
                    override fun onSuccess(ack: PaymentProtocol.Ack?) {
                        WalletManager.wallet?.commitTx(req.tx)
                        showToast("coins sent!")
                        activity?.runOnUiThread {
                            (activity as? MainActivity)?.toggleSendScreen(false)
                        }
                    }

                    override fun onFailure(throwable: Throwable) {
                        showToast("an error occurred")
                    }
                },
                MoreExecutors.directExecutor()
            )
        }
    }

    private fun processNormalTransaction() {
        paymentContent?.addressOrPayload?.let { this.processNormalTransaction(it) }
    }

    private fun processNormalTransaction(address: String?) {
        val bchToSend = getCoinAmount()
        address?.let { this.processNormalTransaction(it, bchToSend) }
    }

    private fun processNormalTransaction(address: String, bchAmount: Coin) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val addressAsObject = Address.fromString(WalletManager.parameters, address)
                val req: SendRequest =
                    if (bchAmount == WalletManager.wallet?.let { WalletManager.getBalance(it) }) {
                        SendRequest.emptyWallet(addressAsObject)
                    } else {
                        SendRequest.to(addressAsObject, bchAmount)
                    }

                req.allowUnconfirmed()
                req.ensureMinRequiredFee = false
                req.feePerKb = Coin.valueOf(2L * 1000L)
                val sendResult = WalletManager.wallet?.sendCoins(req)
                Futures.addCallback(
                    sendResult?.broadcastComplete,
                    object : FutureCallback<Transaction?> {
                        override fun onSuccess(@Nullable result: Transaction?) {
                            showToast("coins sent!")
                            (activity as? MainActivity)?.toggleSendScreen(false)
                        }

                        override fun onFailure(t: Throwable) { // We died trying to empty the wallet.

                        }
                    },
                    MoreExecutors.directExecutor()
                )
            } catch (e: InsufficientMoneyException) {
                e.printStackTrace()
                showToast("not enough coins in wallet")
            } catch (e: Wallet.CouldNotAdjustDownwards) {
                e.printStackTrace()
                showToast("error adjusting downwards")
            } catch (e: Wallet.ExceededMaxTransactionSize) {
                e.printStackTrace()
                showToast("transaction is too big")
            } catch (e: NullPointerException) {
                e.printStackTrace()
                e.message?.let {
                    showToast(it)
                }
            }
        }
    }

    fun getBIP70Data(root: View?, url: String?) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val session = BIP70Helper.getBtcPaymentSession(url)
                val amountWanted = session.value
                setCoinAmount(amountWanted)
                activity?.runOnUiThread {
                    root?.send_amount_input?.isEnabled = false
                    root?.to_field_text?.text = session.memo
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getCoinAmount(): Coin {
        val bchToSend = if (bchIsSendType) {
            root?.send_amount_input?.text.toString()
        } else {
            root?.alt_currency_display?.text.toString()
        }

        if (bchToSend.isEmpty())
            return Coin.ZERO

        return Coin.parseCoin(bchToSend)
    }

    private fun setCoinAmount(coin: Coin?) {
        val amountFormatted = coin?.toPlainString()?.toDouble()
            ?.let { BalanceFormatter.formatBalance(it, "#.########") }
        activity?.runOnUiThread {
            val bchValue = amountFormatted?.toDouble() ?: 0.0
            val price = PriceHelper.price
            val fiatValue = bchValue * price
            if (bchIsSendType) {
                root?.send_amount_input?.setText(
                    BalanceFormatter.formatBalance(
                        bchValue,
                        "#.########"
                    )
                )
                root?.alt_currency_display?.text = BalanceFormatter.formatBalance(fiatValue, "0.00")
            } else {
                root?.send_amount_input?.setText(BalanceFormatter.formatBalance(fiatValue, "0.00"))
                root?.alt_currency_display?.text =
                    BalanceFormatter.formatBalance(bchValue, "#.########")
            }
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            (activity as? MainActivity)?.enablePayButton()
        }
        (activity as? MainActivity)?.let { Toaster.showMessage(it, message) }
    }

    private fun generateQR(payload: String?): Bitmap? {
        return try {
            val encoder = QRCode.from(payload).withSize(1024, 1024).withErrorCorrection(
                ErrorCorrectionLevel.L
            )
            encoder.bitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}