package xyz.pokkst.pokket.lite.ui.main.fragment.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_settings_transactions.view.*
import org.bitcoinj.core.Transaction
import org.bitcoinj.wallet.Wallet
import xyz.pokkst.pokket.lite.R
import xyz.pokkst.pokket.lite.SettingsActivity
import xyz.pokkst.pokket.lite.ui.adapter.TransactionAdapter
import xyz.pokkst.pokket.lite.ui.listener.TxAdapterListener
import xyz.pokkst.pokket.lite.wallet.WalletManager


/**
 * A placeholder fragment containing a simple view.
 */
class SettingsTransactionsFragment : Fragment(), TxAdapterListener {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings_transactions, container, false)
        this.setArrayAdapter(root, WalletManager.wallet)
        return root
    }

    private fun setArrayAdapter(root: View, wallet: Wallet?) {
        setListViewShit(root, wallet)
    }

    private fun setListViewShit(root: View, wallet: Wallet?) {
        val transactions = wallet?.getRecentTransactions(0, false)
        val txAdapter = transactions?.let { TransactionAdapter(it) }
        txAdapter?.listener = this
        root.transactions_list.adapter = txAdapter
        root.transactions_list.layoutManager = LinearLayoutManager(context)
    }

    override fun onClickTransaction(tx: Transaction) {
        (activity as? SettingsActivity)?.adjustDeepMenu(1)
        val txid = tx.txId.toString()
        val amount = tx.getValue(WalletManager.wallet)
        if (amount?.isPositive == true) {
            findNavController().navigate(
                SettingsHomeFragmentDirections.navToTxReceived(
                    txid,
                    false
                )
            )
        } else if (amount?.isNegative == true) {
            findNavController().navigate(
                SettingsHomeFragmentDirections.navToTxSent(
                    txid,
                    false
                )
            )
        }
    }
}