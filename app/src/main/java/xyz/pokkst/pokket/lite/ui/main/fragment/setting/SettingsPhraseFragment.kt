package xyz.pokkst.pokket.lite.ui.main.fragment.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_settings_phrase.view.*
import xyz.pokkst.pokket.lite.R
import xyz.pokkst.pokket.lite.wallet.WalletManager


/**
 * A placeholder fragment containing a simple view.
 */
class SettingsPhraseFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings_phrase, container, false)
        val mnemonicCode = WalletManager.wallet?.keyChainSeed?.mnemonicCode
        val recoverySeed = StringBuilder()
        if(mnemonicCode != null) {
            for (x in mnemonicCode.indices) {
                recoverySeed.append(mnemonicCode[x]).append(if (x == mnemonicCode.size - 1) "" else " ")
            }
        }
        val seedStr = recoverySeed.toString()
        root.the_phrase.text =
                seedStr
        return root
    }
}