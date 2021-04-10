package xyz.pokkst.pokket.lite.ui.listener

import org.bitcoinj.core.Transaction

interface TxAdapterListener {
    fun onClickTransaction(tx: Transaction)
}