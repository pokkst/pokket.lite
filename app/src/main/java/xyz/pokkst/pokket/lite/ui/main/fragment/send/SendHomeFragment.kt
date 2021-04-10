package xyz.pokkst.pokket.lite.ui.main.fragment.send

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_send_home.view.*
import xyz.pokkst.pokket.lite.R
import xyz.pokkst.pokket.lite.qr.QRHelper
import xyz.pokkst.pokket.lite.util.Constants
import xyz.pokkst.pokket.lite.util.UriHelper

/**
 * A placeholder fragment containing a simple view.
 */
class SendHomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_send_home, container, false)

        root.scan_qr_code_button.setOnClickListener {
            QRHelper().startQRScan(this, Constants.REQUEST_CODE_SCAN_QR)
        }

        root.paste_address_button.setOnClickListener {
            val clipBoard =
                requireActivity().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val pasteData = clipBoard.primaryClip?.getItemAt(0)?.text.toString()
            if (isValidPaymentType(pasteData)) {
                findNavController().navigate(
                    SendHomeFragmentDirections.navToSend(
                        pasteData
                    )
                )
            }
        }

        root.donate_button.setOnClickListener {
            findNavController().navigate(
                    SendHomeFragmentDirections.navToSend(
                            Constants.DONATION_ADDRESS
                    )
            )
        }



        return root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constants.REQUEST_CODE_SCAN_QR) {
                if (data != null) {
                    val scanData = data.getStringExtra(Constants.QR_SCAN_RESULT)
                    if (scanData != null) {
                        if (isValidPaymentType(scanData)) {
                            findNavController().navigate(
                                SendHomeFragmentDirections.navToSend(
                                    scanData
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isValidPaymentType(address: String): Boolean {
        return UriHelper.parse(address) != null
    }
}