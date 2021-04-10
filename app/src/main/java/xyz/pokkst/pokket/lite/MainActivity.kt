package xyz.pokkst.pokket.lite

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.pokkst.pokket.lite.ui.ToggleViewPager
import xyz.pokkst.pokket.lite.ui.main.SectionsPagerAdapter
import xyz.pokkst.pokket.lite.util.*
import xyz.pokkst.pokket.lite.wallet.WalletManager
import java.io.File

class MainActivity : AppCompatActivity() {
    var inFragment = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        PrefsHelper.instance(this)
        WalletManager.walletDir = File(applicationInfo.dataDir)
        val extras = intent.extras
        var seed: String? = null
        var newUser: Boolean = false
        var passphrase: String? = null
        if (extras != null) {
            seed = extras.getString("seed")
            newUser = extras.getBoolean("new")
        }

        prepareViews(newUser)
        setListeners()

        if (extras?.containsKey("passphrase") == true) {
            passphrase = extras.getString("passphrase")
        }
        WalletManager.startWallet(this, seed, newUser, passphrase)
    }

    private fun prepareViews(newUser: Boolean) {
        StatusBarHelper.prepareLightStatusBar(this)
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        view_pager.adapter = sectionsPagerAdapter
        if (newUser) {
            view_pager.currentItem = 2
        }
        tabs.setupWithViewPager(view_pager)
    }

    private fun setListeners() {
        pay_button.setOnClickListener {
            pay_button.isEnabled = false
            val intent = Intent(Constants.ACTION_FRAGMENT_SEND_SEND)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }

        appbar_title.setOnClickListener {
            val intent = Intent(Constants.ACTION_FRAGMENT_SEND_MAX)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }

        settings_button.setOnClickListener {
            if (view_pager.isPagingEnabled()) {
                val intentSettings = Intent(this, SettingsActivity::class.java)
                startActivity(intentSettings)
            } else {
                toggleSendScreen(false)
            }
        }

        WalletManager.syncPercentage.observe(this, Observer { pct ->
            refresh(pct)
        })

        WalletManager.refreshEvents.observe(this, Observer { event ->
            if (event != null) {
                refresh()
            }
        })

        WalletManager.peerCount.observe(this, Observer { peers ->
            when {
                peers == 0 -> {
                    appbar_title.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_disconnected,
                        0
                    )
                }
                peers in 1..2 -> {
                    appbar_title.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_connected_partial,
                        0
                    )
                }
                peers >= 3 -> {
                    appbar_title.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_connected,
                        0
                    )
                }
            }
        })
    }

    override fun onBackPressed() {
        if (!inFragment) {
            val a = Intent(Intent.ACTION_MAIN)
            a.addCategory(Intent.CATEGORY_HOME)
            a.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(a)
        } else {
            super.onBackPressed()
        }
    }

    private fun refresh(sync: Int?) {
        refresh()
        if (sync != null) {
            sync_progress_bar.visibility = if (sync == 100) View.INVISIBLE else View.VISIBLE
            sync_progress_bar.progress = sync
        }
    }

    private fun refresh() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bch =
                    WalletManager.wallet?.let { WalletManager.getBalance(it).toPlainString() }
                bch?.let {
                    val fiat = bch.toDouble() * PriceHelper.price
                    val fiatStr = BalanceFormatter.formatBalance(fiat, "0.00")
                    this@MainActivity.runOnUiThread {
                        appbar_title.text =
                            "${resources.getString(R.string.appbar_title, bch)} ($${fiatStr})"
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    fun toggleSendScreen(status: Boolean) {
        val viewPager: ToggleViewPager = findViewById(R.id.view_pager)
        val tabs: TabLayout = findViewById(R.id.tabs)
        viewPager.setPagingEnabled(!status)
        val imgResId = if (status) R.drawable.navigationback else R.drawable.burger
        settings_button.setImageResource(imgResId)
        pay_button.visibility = if (status) View.VISIBLE else View.INVISIBLE
        pay_button.isEnabled = status
        tabs.visibility = if (status) View.GONE else View.VISIBLE
        inFragment = status

        if (!status) {
            val intent = Intent(Constants.ACTION_MAIN_ENABLE_PAGER)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }

    fun enablePayButton() {
        pay_button.isEnabled = true
    }

    fun enableTokensScreen() {
        val viewPager: ToggleViewPager = findViewById(R.id.view_pager)
        val tabs: TabLayout = findViewById(R.id.tabs)
        viewPager.setPagingEnabled(false)
        settings_button.setImageResource(R.drawable.navigationback)
        tabs.visibility = View.GONE
        inFragment = true
    }
}