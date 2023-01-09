package tech.oliet.mynalock

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    private val handler = Handler(Looper.getMainLooper())
    private var nfcAdapter: NfcAdapter? = null

    private var lock: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(getString(R.string.no_nfc))
                .setCancelable(false)
                .show()
            return
        }

        if (nfcAdapter?.isEnabled == false) {
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(getString(R.string.nfc_is_off))
                .setCancelable(false)
                .show()
            return
        }

        AlertDialog.Builder(this).apply {
            setTitle(getString(R.string.warning))
            setMessage(getString(R.string.warning_text))
            setPositiveButton("OK") { _, _ -> lock = true }
            setNegativeButton("NO") { _, _ -> finish() }
            setCancelable(false)
        }.show()
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
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

        val result = if (lock) {
            lockmyna(isoDep)
        } else {
            false
        }

        isoDep.close()

        handler.post {
            AlertDialog.Builder(this).apply {
                setTitle(getString(R.string.result))
                setMessage(
                    if (result) {
                        getString(R.string.succeeded)
                    } else {
                        getString(R.string.failed)
                    }
                )
            }.show()
        }
    }

    private fun lockmyna(isoDep: IsoDep): Boolean {
        // SELECT FILE 券面事項入力補助AP
        val command1 = byteArrayOf(
            0x00, 0xA4.toByte(), 0x04, 0x0C,
            0x0A, 0xD3.toByte(), 0x92.toByte(),
            0x10, 0x00, 0x31, 0x00, 0x01,
            0x01, 0x04, 0x08
        )
        val response1 = isoDep.transceive(command1)

        if (!(response1[0] == 0x90.toByte() && response1[1] == 0x00.toByte())) {
            return false
        }

        // SELECT FILE 暗証番号
        val command2 = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x0C, 0x02, 0x00, 0x11)
        val response2 = isoDep.transceive(command2)

        if (!(response2[0] == 0x90.toByte() && response2[1] == 0x00.toByte())) {
            return false
        }

        repeat(10) {
            // VERIFY
            val command3 =
                byteArrayOf(0x00, 0x20, 0x00, 0x80.toByte(), 0x04, 0x4c, 0x4f, 0x43, 0x4b)
            val response3 = isoDep.transceive(command3)
            if (response3[0] == 0x69.toByte() && response3[1] == 0x84.toByte()) {
                return true
            }
        }

        return false
    }
}