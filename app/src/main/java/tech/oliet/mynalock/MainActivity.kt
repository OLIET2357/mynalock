package tech.oliet.mynalock

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(getString(R.string.no_nfc))
                .show()
            return
        }

        if (nfcAdapter?.isEnabled == false) {
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(getString(R.string.nfc_is_off))
                .show()
            return
        }

    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, ,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag?) {
        tag ?: return
        val isoDep = IsoDep.get(tag)
        isoDep.connect()

        repeat(10) {
            val command = byteArrayOf()
            val response = isoDep.transceive(command)
        }

        isoDep.close()
    }
}