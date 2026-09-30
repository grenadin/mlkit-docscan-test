package org.microg.test.docscan

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

private const val TAG = "DocScanTest"

class MainActivity : ComponentActivity() {
    private lateinit var output: TextView

    private val launcher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            show("resultCode=${result.resultCode} (cancelled)")
            return@registerForActivityResult
        }
        val scan = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
        val lines = mutableListOf("resultCode=OK")
        scan?.pages?.forEachIndexed { i, page ->
            val size = runCatching { contentResolver.openInputStream(page.imageUri)!!.use { it.available() } }.getOrElse { "ERR ${it.message}" }
            lines += "page[$i]=${page.imageUri} size=$size"
        }
        scan?.pdf?.let { pdf ->
            val size = runCatching { contentResolver.openInputStream(pdf.uri)!!.use { it.readBytes().size } }.getOrElse { "ERR ${it.message}" }
            lines += "pdf=${pdf.uri} pages=${pdf.pageCount} size=$size"
        }
        if (scan == null) lines += "scan result is null"
        show(lines.joinToString("\n"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        output = TextView(this).apply { setPadding(32, 32, 32, 32) }
        val button = Button(this).apply {
            text = "Scan document"
            setOnClickListener { startScan() }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(button)
            addView(output)
        })
    }

    private fun startScan() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(5)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG, GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options).getStartScanIntent(this)
            .addOnSuccessListener { launcher.launch(IntentSenderRequest.Builder(it).build()) }
            .addOnFailureListener { show("getStartScanIntent failed: $it") }
    }

    private fun show(text: String) {
        Log.i(TAG, text)
        output.text = text
    }
}
