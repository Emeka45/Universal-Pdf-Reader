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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {
    private lateinit var pageImage: ImageView
    private lateinit var pageLabel: TextView
    private lateinit var searchBox: EditText
    private var renderer: PdfRenderer? = null
    private var descriptor: ParcelFileDescriptor? = null
    private var currentPage = 0
    private var pdfFile: File? = null
    private var zoom = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(245, 245, 248)) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(8, 6, 8, 6) }
        val open = Button(this).apply { text = "Open PDF"; setOnClickListener { openPicker() } }
        searchBox = EditText(this).apply { hint = "Search in PDF"; setSingleLine(true); setPadding(14, 0, 14, 0) }
        val search = Button(this).apply { text = "Search"; setOnClickListener { searchPdf(searchBox.text.toString()) } }
        top.addView(open, LinearLayout.LayoutParams(0, 50.dp(), 1f))
        top.addView(searchBox, LinearLayout.LayoutParams(0, 50.dp(), 1.5f))
        top.addView(search, LinearLayout.LayoutParams(0, 50.dp(), 0.8f))

        pageImage = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.MATRIX
            setBackgroundColor(Color.WHITE)
        }
        pageLabel = TextView(this).apply { text = "Open a PDF to begin"; gravity = Gravity.CENTER; setPadding(8, 8, 8, 8) }

        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(8, 6, 8, 10) }
        val previous = Button(this).apply { text = "‹"; setOnClickListener { showPage(currentPage - 1) } }
        val zoomOut = Button(this).apply { text = "−"; setOnClickListener { setZoom(zoom - 0.25f) } }
        val reset = Button(this).apply { text = "100%"; setOnClickListener { setZoom(1f) } }
        val zoomIn = Button(this).apply { text = "+"; setOnClickListener { setZoom(zoom + 0.25f) } }
        val next = Button(this).apply { text = "›"; setOnClickListener { showPage(currentPage + 1) } }
        controls.addView(previous, LinearLayout.LayoutParams(0, 52.dp(), 1f))
        controls.addView(zoomOut, LinearLayout.LayoutParams(0, 52.dp(), 0.8f))
        controls.addView(reset, LinearLayout.LayoutParams(0, 52.dp(), 1f))
        controls.addView(zoomIn, LinearLayout.LayoutParams(0, 52.dp(), 0.8f))
        controls.addView(next, LinearLayout.LayoutParams(0, 52.dp(), 1f))

        val pageRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        pageRow.addView(pageLabel, LinearLayout.LayoutParams(-1, 40.dp()))
        root.addView(top)
        root.addView(pageImage, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(pageRow)
        root.addView(controls)
        setContentView(root)
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
        if (requestCode == REQUEST_OPEN && resultCode == RESULT_OK) data?.data?.let { openPdf(it) }
    }

    private fun openPdf(uri: Uri) {
        try {
            closePdf()
            pdfFile = File(cacheDir, "current.pdf")
            contentResolver.openInputStream(uri)!!.use { input -> FileOutputStream(pdfFile!!).use { output -> input.copyTo(output) } }
            descriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(descriptor!!)
            currentPage = 0
            zoom = 1f
            showPage(0)
            Toast.makeText(this, "${renderer!!.pageCount} pages", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showPage(index: Int) {
        val r = renderer ?: return
        if (index !in 0 until r.pageCount) return
        val page = r.openPage(index)
        val width = (page.width * resources.displayMetrics.density).toInt().coerceAtLeast(800)
        val height = (page.height.toFloat() / page.width * width).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        pageImage.setImageBitmap(bitmap)
        currentPage = index
        pageLabel.text = "Page ${index + 1} / ${r.pageCount}"
        applyMatrix()
    }

    private fun setZoom(value: Float) {
        zoom = value.coerceIn(0.5f, 3f)
        applyMatrix()
    }

    private fun applyMatrix() {
        pageImage.imageMatrix = Matrix().apply { postScale(zoom, zoom, pageImage.width / 2f, pageImage.height / 2f) }
    }

    private fun searchPdf(query: String) {
        val file = pdfFile ?: run { Toast.makeText(this, "Open a PDF first", Toast.LENGTH_SHORT).show(); return }
        if (query.trim().isEmpty()) return
        Thread {
            try {
                PDDocument.load(file).use { document ->
                    val stripper = PDFTextStripper()
                    var found = -1
                    for (page in 1..document.numberOfPages) {
                        stripper.startPage = page
                        stripper.endPage = page
                        if (stripper.getText(document).contains(query, ignoreCase = true)) { found = page - 1; break }
                    }
                    runOnUiThread {
                        if (found >= 0) {
                            showPage(found)
                            Toast.makeText(this, "Found on page ${found + 1}", Toast.LENGTH_SHORT).show()
                        } else Toast.makeText(this, "No match found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Search failed: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun closePdf() { renderer?.close(); renderer = null; descriptor?.close(); descriptor = null }
    override fun onDestroy() { closePdf(); super.onDestroy() }
    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
    companion object { private const val REQUEST_OPEN = 42 }
}
