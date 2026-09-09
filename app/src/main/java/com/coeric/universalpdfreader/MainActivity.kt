package com.coeric.universalpdfreader

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Gravity
import android.view.View
import android.widget.*
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {
    private lateinit var pageImage: ImageView
    private lateinit var pageLabel: TextView
    private lateinit var titleLabel: TextView
    private lateinit var searchBox: EditText
    private var renderer: PdfRenderer? = null
    private var descriptor: ParcelFileDescriptor? = null
    private var currentPage = 0
    private var pdfFile: File? = null
    private var currentBitmap: Bitmap? = null
    private var zoom = 1f
    private var rotation = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(246, 247, 251))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(18.dp(), 14.dp(), 18.dp(), 10.dp())
            setBackgroundColor(Color.WHITE)
        }
        val logo = TextView(this).apply {
            text = "U"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundResource(com.coeric.universalpdfreader.R.drawable.u_logo)
        }
        header.addView(logo, LinearLayout.LayoutParams(48.dp(), 48.dp()))
        val names = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(12.dp(), 0, 0, 0) }
        titleLabel = TextView(this).apply { text = "Universal PDF Reader"; textSize = 19f; setTextColor(Color.rgb(25, 27, 35)) }
        val subtitle = TextView(this).apply { text = "Your documents. Your way."; textSize = 12f; setTextColor(Color.rgb(105, 108, 120)) }
        names.addView(titleLabel)
        names.addView(subtitle)
        header.addView(names, LinearLayout.LayoutParams(0, -2, 1f))
        val open = actionButton("OPEN") { openPicker() }
        header.addView(open, LinearLayout.LayoutParams(76.dp(), 44.dp()))

        val searchRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(14.dp(), 8.dp(), 14.dp(), 8.dp()) }
        searchBox = EditText(this).apply {
            hint = "Search this PDF"
            textSize = 14f
            singleLine = true
            setPadding(16.dp(), 0, 10.dp(), 0)
            setBackgroundResource(android.R.drawable.editbox_background)
        }
        val search = actionButton("SEARCH") { searchPdf(searchBox.text.toString()) }
        searchRow.addView(searchBox, LinearLayout.LayoutParams(0, 48.dp(), 1f))
        searchRow.addView(search, LinearLayout.LayoutParams(88.dp(), 48.dp()).apply { leftMargin = 8.dp() })

        pageImage = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.MATRIX
            setBackgroundColor(Color.rgb(224, 226, 232))
            setPadding(12.dp(), 8.dp(), 12.dp(), 8.dp())
            contentDescription = "PDF page"
        }

        pageLabel = TextView(this).apply {
            text = "Open a PDF to begin"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(88, 91, 102))
            setPadding(8.dp(), 8.dp(), 8.dp(), 8.dp())
        }

        val controls = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(12.dp(), 6.dp(), 12.dp(), 16.dp())
            setBackgroundColor(Color.WHITE)
        }
        val previous = iconButton("‹") { showPage(currentPage - 1) }
        val zoomOut = iconButton("−") { setZoom(zoom - .25f) }
        val fit = actionButton("FIT") { setZoom(1f) }
        val zoomIn = iconButton("+") { setZoom(zoom + .25f) }
        val rotate = actionButton("ROTATE") { rotation = (rotation + 90f) % 360f; applyMatrix() }
        val share = actionButton("SHARE") { sharePdf() }
        val next = iconButton("›") { showPage(currentPage + 1) }
        listOf(previous, zoomOut, fit, zoomIn, rotate, share, next).forEachIndexed { i, v ->
            controls.addView(v, LinearLayout.LayoutParams(if (i == 2) 64.dp() else if (i in 4..5) 86.dp() else 50.dp(), 48.dp()).apply { leftMargin = 4.dp(); rightMargin = 4.dp() })
        }

        root.addView(header)
        root.addView(searchRow)
        root.addView(pageImage, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(pageLabel)
        root.addView(controls)
        setContentView(root)
    }

    private fun actionButton(label: String, click: () -> Unit) = Button(this).apply {
        text = label
        textSize = 11f
        setTextColor(Color.rgb(52, 39, 170))
        setOnClickListener { click() }
    }

    private fun iconButton(label: String, click: () -> Unit) = Button(this).apply {
        text = label
        textSize = 22f
        setOnClickListener { click() }
    }

    private fun openPicker() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }, REQUEST_OPEN)
    }

    @Deprecated("Deprecated in Android API 33")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OPEN && resultCode == RESULT_OK) data?.data?.let { uri ->
            if ((data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) {}
            openPdf(uri)
        }
    }

    private fun openPdf(uri: Uri) {
        try {
            closePdf()
            val destination = File.createTempFile("reader_", ".pdf", cacheDir)
            contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Unable to read PDF" }
                FileOutputStream(destination).use { output -> input.copyTo(output) }
            }
            pdfFile?.delete(); pdfFile = destination
            descriptor = ParcelFileDescriptor.open(destination, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(descriptor!!)
            currentPage = 0; zoom = 1f; rotation = 0f
            titleLabel.text = uri.lastPathSegment?.substringAfterLast('/') ?: "Universal PDF Reader"
            showPage(0)
        } catch (e: Exception) { toast("Could not open PDF: ${e.message ?: "unknown error"}") }
    }

    private fun showPage(index: Int) {
        val r = renderer ?: return
        if (index !in 0 until r.pageCount) return
        try {
            val page = r.openPage(index)
            val width = (page.width * resources.displayMetrics.density).toInt().coerceIn(800, 2048)
            val height = (page.height.toFloat() / page.width * width).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            currentBitmap?.recycle(); currentBitmap = bitmap
            pageImage.setImageBitmap(bitmap)
            currentPage = index
            pageLabel.text = "Page ${index + 1} of ${r.pageCount}  •  ${zoomPercent()}"
            pageImage.post { applyMatrix() }
        } catch (e: Exception) { toast("Could not render page: ${e.message ?: "unknown error"}") }
    }

    private fun setZoom(value: Float) { zoom = value.coerceIn(.5f, 3f); applyMatrix(); updateLabel() }

    private fun applyMatrix() {
        if (!::pageImage.isInitialized || currentBitmap == null) return
        val matrix = Matrix()
        matrix.postScale(zoom, zoom, pageImage.width / 2f, pageImage.height / 2f)
        matrix.postRotate(rotation, pageImage.width / 2f, pageImage.height / 2f)
        pageImage.imageMatrix = matrix
    }

    private fun updateLabel() { if (renderer != null) pageLabel.text = "Page ${currentPage + 1} of ${renderer!!.pageCount}  •  ${zoomPercent()}" }
    private fun zoomPercent() = "${(zoom * 100).toInt()}%"

    private fun searchPdf(query: String) {
        val file = pdfFile ?: run { toast("Open a PDF first"); return }
        val normalized = query.trim(); if (normalized.isEmpty()) return
        Thread {
            try {
                PDDocument.load(file).use { document ->
                    val stripper = PDFTextStripper(); var found = -1
                    for (page in 1..document.numberOfPages) {
                        stripper.startPage = page; stripper.endPage = page
                        if (stripper.getText(document).contains(normalized, true)) { found = page - 1; break }
                    }
                    runOnUiThread { if (found >= 0) { showPage(found); toast("Found on page ${found + 1}") } else toast("No match found") }
                }
            } catch (e: Exception) { runOnUiThread { toast("Search failed: ${e.message ?: "unknown error"}") } }
        }.start()
    }

    private fun sharePdf() {
        val file = pdfFile ?: run { toast("Open a PDF first"); return }
        val uri = Uri.fromFile(file)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share PDF"))
    }

    private fun closePdf() {
        renderer?.close(); renderer = null
        descriptor?.close(); descriptor = null
        currentBitmap?.recycle(); currentBitmap = null
        if (::pageImage.isInitialized) pageImage.setImageDrawable(null)
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    override fun onDestroy() { closePdf(); pdfFile?.delete(); pdfFile = null; super.onDestroy() }
    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
    companion object { private const val REQUEST_OPEN = 42 }
}
