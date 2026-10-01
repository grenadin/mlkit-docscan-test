package org.microg.test.docscan

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

private const val TAG = "DocScanTest"
private const val THUMBNAIL_SIZE = 800

class MainActivity : ComponentActivity() {
    private lateinit var output: TextView
    private lateinit var images: LinearLayout

    private val launcher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        images.removeAllViews()
        if (result.resultCode != Activity.RESULT_OK) {
            show("resultCode=${result.resultCode} (cancelled)")
            return@registerForActivityResult
        }
        val scan = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
        val lines = mutableListOf("resultCode=OK")
        scan?.pages?.forEachIndexed { i, page ->
            val size = runCatching { contentResolver.openInputStream(page.imageUri)!!.use { it.readBytes().size } }.getOrElse { "ERR ${it.message}" }
            lines += "page[$i]=${page.imageUri} size=$size"
            addImage("JPEG page ${i + 1}", decodeImage(page.imageUri))
        }
        scan?.pdf?.let { pdf ->
            val size = runCatching { contentResolver.openInputStream(pdf.uri)!!.use { it.readBytes().size } }.getOrElse { "ERR ${it.message}" }
            lines += "pdf=${pdf.uri} pages=${pdf.pageCount} size=$size"
            renderPdf(pdf.uri).forEachIndexed { i, bitmap -> addImage("PDF page ${i + 1}", bitmap) }
        }
        if (scan == null) lines += "scan result is null"
        show(lines.joinToString("\n"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        output = TextView(this).apply { setPadding(32, 32, 32, 32) }
        images = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val button = Button(this).apply {
            text = "Scan document"
            setOnClickListener { startScan() }
        }
        setContentView(ScrollView(this).apply {
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(button)
                addView(output)
                addView(images)
            })
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

    private fun decodeImage(uri: Uri): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= THUMBNAIL_SIZE) sampleSize *= 2
        contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize }) }
    }.onFailure { Log.w(TAG, "Failed to decode $uri", it) }.getOrNull()

    private fun renderPdf(uri: Uri): List<Bitmap?> = runCatching {
        contentResolver.openFileDescriptor(uri, "r")!!.use { descriptor: ParcelFileDescriptor ->
            PdfRenderer(descriptor).use { renderer ->
                (0 until renderer.pageCount).map { index ->
                    renderer.openPage(index).use { page ->
                        val scale = THUMBNAIL_SIZE.toFloat() / maxOf(page.width, page.height)
                        Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888).also {
                            it.eraseColor(Color.WHITE)
                            page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                }
            }
        }
    }.onFailure { Log.w(TAG, "Failed to render PDF $uri", it) }.getOrElse { listOf(null) }

    private fun addImage(label: String, bitmap: Bitmap?) {
        images.addView(TextView(this).apply {
            text = if (bitmap != null) "$label (${bitmap.width}x${bitmap.height})" else "$label: failed to load"
            setPadding(32, 24, 32, 8)
        })
        if (bitmap != null) {
            images.addView(ImageView(this).apply {
                setImageBitmap(bitmap)
                adjustViewBounds = true
                setPadding(32, 0, 32, 0)
            })
        }
    }

    private fun show(text: String) {
        Log.i(TAG, text)
        output.text = text
    }
}
