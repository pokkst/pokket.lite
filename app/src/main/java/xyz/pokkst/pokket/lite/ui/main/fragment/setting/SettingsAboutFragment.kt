package xyz.pokkst.pokket.lite.ui.main.fragment.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import xyz.pokkst.pokket.lite.R


/**
 * A placeholder fragment containing a simple view.
 */
class SettingsAboutFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings_about, container, false)
        return root
    }
}