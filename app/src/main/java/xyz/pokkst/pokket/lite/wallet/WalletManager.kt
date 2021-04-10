package xyz.pokkst.pokket.lite.wallet

import android.app.Activity
import android.os.Handler
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.bitcoinj.core.Coin
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.PeerAddress
import org.bitcoinj.core.listeners.DownloadProgressTracker
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.Script
import org.bitcoinj.utils.BriefLogFormatter
import org.bitcoinj.utils.Threading
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.KeyChainGroupStructure
import org.bitcoinj.wallet.Wallet
import xyz.pokkst.pokket.lite.livedata.Event
import xyz.pokkst.pokket.lite.util.PrefsHelper
import java.io.File
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.Executor

class WalletManager {
    companion object {
        lateinit var walletDir: File
        var walletKit: WalletAppKit? = null
        val wallet: Wallet?
            get() {
                return walletKit?.wallet()
            }
        val kit: WalletAppKit?
            get() {
                return walletKit
            }
        val parameters: NetworkParameters = MainNetParams.get()
        private val _syncPercentage: MutableLiveData<Int> = MutableLiveData(0)
        val syncPercentage: LiveData<Int> = _syncPercentage
        private val _refreshEvents: MutableLiveData<Event<String>> = MutableLiveData()
        val refreshEvents: LiveData<Event<String>> = _refreshEvents
        private val _peerCount: MutableLiveData<Int> = MutableLiveData(0)
        val peerCount: LiveData<Int> = _peerCount
        const val walletFileName = "pokket"
        fun startWallet(activity: Activity, seed: String?, newUser: Boolean, passphrase: String?) {
            setBitcoinSDKThread()
            BriefLogFormatter.init()
            walletKit = object : WalletAppKit(
                parameters,
                Script.ScriptType.P2WPKH,
                KeyChainGroupStructure.DEFAULT,
                walletDir,
                walletFileName
            ) {
                override fun onSetupCompleted() {
                    wallet().isAcceptRiskyTransactions = true
                    _syncPercentage.postValue(0)
                    _refreshEvents.postValue(Event(""))
                    wallet().addCoinsReceivedEventListener { wallet, tx, prevBalance, newBalance ->
                        _refreshEvents.postValue(Event(tx.txId.toString()))
                    }
                    wallet().addCoinsSentEventListener { wallet, tx, prevBalance, newBalance ->
                        _refreshEvents.postValue(Event(tx.txId.toString()))
                    }
                    peerGroup()?.addConnectedEventListener { peer, peerCount ->
                        _peerCount.postValue(peerCount)
                    }
                    peerGroup()?.addDisconnectedEventListener { peer, peerCount ->
                        _peerCount.postValue(peerCount)
                    }
                    wallet().saveToFile(vWalletFile)
                }
            }

            walletKit?.setDownloadListener(object : DownloadProgressTracker() {
                override fun doneDownload() {
                    super.doneDownload()
                    _syncPercentage.postValue(100)
                }

                override fun progress(pct: Double, blocksSoFar: Int, date: Date?) {
                    super.progress(pct, blocksSoFar, date)
                    _syncPercentage.postValue(pct.toInt())
                }
            })

            val creationDate = if (newUser) System.currentTimeMillis() / 1000L else 1560281760L
            if (seed != null) {
                val deterministicSeed = DeterministicSeed(
                    seed, null, passphrase
                        ?: "", creationDate
                )
                walletKit?.restoreWalletFromSeed(deterministicSeed)
            }

            walletKit?.setBlockingStartup(false)
            val checkpointsInputStream = activity.assets.open("checkpoints.txt")
            walletKit?.setCheckpoints(checkpointsInputStream)
            setupNodeOnStart()
            walletKit?.startAsync()
        }

        private fun setupNodeOnStart() {
            val nodeIP = PrefsHelper.instance(null)?.getString("node_ip", null)
            if (nodeIP?.isNotEmpty() == true) {
                var node1: InetAddress? = null
                try {
                    node1 = InetAddress.getByName(nodeIP)
                } catch (e: UnknownHostException) {
                    e.printStackTrace()
                }

                this.kit?.setPeerNodes(null)
                this.kit?.setPeerNodes(PeerAddress(parameters, node1))
            }
        }

        fun getBalance(wallet: Wallet): Coin {
            return wallet.getBalance(Wallet.BalanceType.ESTIMATED)
        }


        fun setBitcoinSDKThread() {
            val handler = Handler()
            Threading.USER_THREAD = Executor { handler.post(it) }
        }

        fun stopWallets() {
            kit?.stopAsync()
            kit?.awaitTerminated()
            walletKit = null
        }
    }
}