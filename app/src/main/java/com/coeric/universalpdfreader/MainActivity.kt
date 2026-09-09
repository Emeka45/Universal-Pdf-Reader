package com.coeric.universalpdfreader

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var titleLabel: TextView
    private lateinit var pageImage: ImageView
    private lateinit var pageLabel: TextView
    private lateinit var searchBox: EditText
    private lateinit var searchPanel: LinearLayout
    private lateinit var toolsPanel: LinearLayout
    private lateinit var libraryPanel: LinearLayout
    private lateinit var readerPanel: LinearLayout
    private lateinit var libraryList: LinearLayout
    private lateinit var emptyLibrary: TextView

    private var renderer: PdfRenderer? = null
    private var descriptor: ParcelFileDescriptor? = null
    private var currentPage = 0
    private var pdfFile: File? = null
    private var currentBitmap: Bitmap? = null
    private var zoom = 1f
    private var rotation = 0f

    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        buildUi()
        showLibrary()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(244, 246, 250))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(10), dp(10))
            background(Color.WHITE, 0f)
        }

        val logo = TextView(this).apply {
            text = "U"
            textSize = 22f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background(Color.rgb(69, 57, 190), 14f)
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(44), dp(44)))

        val identity = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, dp(8), 0)
        }
        titleLabel = TextView(this).apply {
            text = "Universal PDF Reader"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(25, 27, 35))
            maxLines = 1
        }
        identity.addView(titleLabel)
        identity.addView(TextView(this).apply {
            text = "Read beautifully. Anywhere."
            textSize = 11f
            setTextColor(Color.rgb(110, 114, 128))
        })
        header.addView(identity, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(action("LIBRARY") { showLibrary() }, LinearLayout.LayoutParams(dp(78), dp(42)))
        header.addView(action("OPEN") { openPicker() }, LinearLayout.LayoutParams(dp(64), dp(42)))
        header.addView(action("TOOLS") { toggleTools() }, LinearLayout.LayoutParams(dp(64), dp(42)))
        root.addView(header)

        searchPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background(Color.rgb(250, 250, 252), 0f)
        }
        searchBox = EditText(this).apply {
            hint = "Search inside this PDF"
            textSize = 14f
            singleLine = true
            setPadding(dp(14), 0, dp(10), 0)
            background(Color.WHITE, 12f)
        }
        searchPanel.addView(searchBox, LinearLayout.LayoutParams(0, dp(46), 1f))
        searchPanel.addView(action("SEARCH") { searchPdf(searchBox.text.toString()) }, LinearLayout.LayoutParams(dp(82), dp(46)).apply { leftMargin = dp(8) })
        root.addView(searchPanel)

        toolsPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(7), dp(8), dp(7))
            background(Color.WHITE, 0f)
        }
        addTool("‹", "PREV") { showPage(currentPage - 1) }
        addTool("−", "ZOOM OUT") { setZoom(zoom - 0.2f) }
        addTool("FIT", "FIT") { fitPage() }
        addTool("+", "ZOOM IN") { setZoom(zoom + 0.2f) }
        addTool("↻", "ROTATE") { rotation = (rotation + 90f) % 360f; applyTransform() }
        addTool("SHARE", "SHARE") { sharePdf() }
        addTool("›", "NEXT") { showPage(currentPage + 1) }
        root.addView(toolsPanel)

        readerPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background(Color.rgb(226, 228, 234), 0f)
        }
        val viewer = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background(Color.rgb(226, 228, 234), 0f)
        }
        pageImage = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.WHITE)
            elevation = dp(5).toFloat()
            contentDescription = "PDF page"
        }
        viewer.addView(pageImage, LinearLayout.LayoutParams(-1, -1))
        readerPanel.addView(viewer, LinearLayout.LayoutParams(-1, 0, 1f))
        pageLabel = TextView(this).apply {
            text = "No document open"
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(Color.rgb(88, 91, 103))
            setPadding(dp(8), dp(8), dp(8), dp(10))
            background(Color.WHITE, 0f)
        }
        readerPanel.addView(pageLabel)
        root.addView(readerPanel, LinearLayout.LayoutParams(-1, 0, 1f))

        libraryPanel = buildLibraryPanel()
        root.addView(libraryPanel, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun buildLibraryPanel(): LinearLayout {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(20))
            background(Color.rgb(244, 246, 250), 0f)
        }
        panel.addView(TextView(this).apply {
            text = "Your Library"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(25, 27, 35))
        })
        panel.addView(TextView(this).apply {
            text = "Keep your PDFs close and pick up where you left off."
            textSize = 13f
            setTextColor(Color.rgb(105, 109, 123))
            setPadding(0, dp(3), 0, dp(16))
        })

        val quick = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background(Color.WHITE, 16f)
            elevation = dp(2).toFloat()
        }
        val quickText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        quickText.addView(TextView(this).apply {
            text = "Open a new PDF"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(35, 37, 47))
        })
        quickText.addView(TextView(this).apply {
            text = "Browse files on your device"
            textSize = 11f
            setTextColor(Color.rgb(112, 115, 128))
        })
        quick.addView(quickText, LinearLayout.LayoutParams(0, -2, 1f))
        quick.addView(action("OPEN PDF") { openPicker() }, LinearLayout.LayoutParams(dp(94), dp(44)))
        panel.addView(quick)

        panel.addView(TextView(this).apply {
            text = "RECENT DOCUMENTS"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(105, 108, 122))
            setPadding(dp(2), dp(22), 0, dp(8))
        })
        val scroll = ScrollView(this).apply { isFillViewport = true }
        libraryList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(libraryList)
        panel.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        emptyLibrary = TextView(this).apply {
            text = "Your library is empty.\nOpen a PDF and it will appear here."
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(105, 108, 122))
            setPadding(dp(20), dp(40), dp(20), dp(40))
        }
        return panel
    }

    private fun addTool(label: String, description: String, click: () -> Unit) {
        val button = action(label) { click() }
        button.contentDescription = description
        button.textSize = if (label.length == 1) 20f else 10f
        toolsPanel.addView(button, LinearLayout.LayoutParams(if (label.length > 2) dp(72) else dp(48), dp(44)).apply {
            leftMargin = dp(3)
            rightMargin = dp(3)
        })
    }

    private fun action(label: String, click: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 10f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(67, 56, 180))
        background(Color.rgb(244, 243, 252), 11f)
        setPadding(0, 0, 0, 0)
        setOnClickListener { click() }
    }

    private fun toggleTools() {
        val visible = toolsPanel.visibility != View.VISIBLE
        toolsPanel.visibility = if (visible) View.VISIBLE else View.GONE
        searchPanel.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun showLibrary() {
        libraryPanel.visibility = View.VISIBLE
        readerPanel.visibility = View.GONE
        toolsPanel.visibility = View.GONE
        searchPanel.visibility = View.GONE
        refreshLibrary()
    }

    private fun showReader() {
        libraryPanel.visibility = View.GONE
        readerPanel.visibility = View.VISIBLE
        toolsPanel.visibility = View.VISIBLE
        searchPanel.visibility = View.VISIBLE
        pageImage.post { applyTransform() }
    }

    private fun refreshLibrary() {
        libraryList.removeAllViews()
        val entries = loadLibrary()
        if (entries.isEmpty()) {
            libraryList.addView(emptyLibrary)
            return
        }
        entries.forEach { entry ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(12), dp(10), dp(12))
                background(Color.WHITE, 16f)
                elevation = dp(2).toFloat()
            }
            card.addView(TextView(this).apply {
                text = "PDF"
                textSize = 10f
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                background(Color.rgb(67, 56, 180), 10f)
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, dp(8), 0)
            }
            info.addView(TextView(this).apply {
                text = entry.first
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.rgb(35, 37, 47))
                maxLines = 2
            })
            info.addView(TextView(this).apply {
                text = "PDF document"
                textSize = 11f
                setTextColor(Color.rgb(112, 115, 128))
            })
            card.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
            card.addView(action("OPEN") { openLibraryEntry(entry.second) }, LinearLayout.LayoutParams(dp(68), dp(42)))
            libraryList.addView(card, LinearLayout.LayoutParams(-1, dp(76)).apply { bottomMargin = dp(10) })
        }
    }

    private fun loadLibrary(): List<Pair<String, String>> {
        val raw = prefs.getString(KEY_LIBRARY, "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(ENTRY_SEPARATOR).mapNotNull { item ->
            val p = item.split(FIELD_SEPARATOR, limit = 2)
            if (p.size == 2 && p[0].isNotBlank() && p[1].isNotBlank()) p[0] to p[1] else null
        }
    }

    private fun rememberInLibrary(uri: Uri, name: String) {
        val safeName = name.replace(FIELD_SEPARATOR, " ").replace(ENTRY_SEPARATOR, " ")
        val entries = loadLibrary().filterNot { it.second == uri.toString() }.toMutableList()
        entries.add(0, safeName to uri.toString())
        prefs.edit().putString(KEY_LIBRARY, entries.take(20).joinToString(ENTRY_SEPARATOR) { "${it.first}$FIELD_SEPARATOR${it.second}" }).apply()
    }

    private fun openLibraryEntry(uriString: String) {
        openPdf(Uri.parse(uriString))
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
        if (requestCode != REQUEST_OPEN || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        if ((data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) { }
        }
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Untitled PDF"
        rememberInLibrary(uri, name)
        openPdf(uri)
    }

    private fun openPdf(uri: Uri) {
        try {
            closePdf()
            val destination = File.createTempFile("reader_", ".pdf", cacheDir)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output) }
            } ?: throw IllegalStateException("Unable to read PDF")
            pdfFile = destination
            descriptor = ParcelFileDescriptor.open(destination, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(descriptor!!)
            currentPage = 0
            zoom = 1f
            rotation = 0f
            titleLabel.text = uri.lastPathSegment?.substringAfterLast('/') ?: "Universal PDF Reader"
            searchBox.setText("")
            showReader()
            showPage(0)
        } catch (e: Exception) {
            toast("Could not open PDF: ${e.message ?: "unknown error"}")
        }
    }

    private fun showPage(index: Int) {
        val r = renderer ?: return
        if (index !in 0 until r.pageCount) return
        try {
            val page = r.openPage(index)
            val width = (page.width * resources.displayMetrics.density).toInt().coerceIn(900, 2200)
            val height = (page.height.toFloat() / page.width * width).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            currentBitmap?.recycle()
            currentBitmap = bitmap
            currentPage = index
            pageImage.setImageBitmap(bitmap)
            applyTransform()
            updateLabel()
        } catch (e: Exception) {
            toast("Could not render page: ${e.message ?: "unknown error"}")
        }
    }

    private fun setZoom(value: Float) {
        zoom = value.coerceIn(0.5f, 3f)
        applyTransform()
        updateLabel()
    }

    private fun fitPage() {
        zoom = 1f
        applyTransform()
        updateLabel()
    }

    private fun applyTransform() {
        pageImage.scaleX = zoom
        pageImage.scaleY = zoom
        pageImage.rotation = rotation
    }

    private fun updateLabel() {
        val r = renderer ?: return
        pageLabel.text = "Page ${currentPage + 1} of ${r.pageCount}  •  ${(zoom * 100).toInt()}%"
    }

    private fun searchPdf(query: String) {
        val file = pdfFile ?: run { toast("Open a PDF first"); return }
        val normalized = query.trim()
        if (normalized.isEmpty()) return
        Thread {
            try {
                PDDocument.load(file).use { document ->
                    val stripper = PDFTextStripper()
                    var found = -1
                    for (page in 1..document.numberOfPages) {
                        stripper.startPage = page
                        stripper.endPage = page
                        if (stripper.getText(document).contains(normalized, ignoreCase = true)) {
                            found = page - 1
                            break
                        }
                    }
                    runOnUiThread {
                        if (found >= 0) showPage(found) else toast("No matches found")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { toast("Search failed: ${e.message ?: "unknown error"}") }
            }
        }.start()
    }

    private fun sharePdf() {
        val file = pdfFile ?: run { toast("Open a PDF first"); return }
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Share PDF"))
        } catch (e: Exception) {
            toast("Unable to share PDF")
        }
    }

    private fun closePdf() {
        try { renderer?.close() } catch (_: Exception) { }
        try { descriptor?.close() } catch (_: Exception) { }
        renderer = null
        descriptor = null
        currentBitmap?.recycle()
        currentBitmap = null
        pdfFile?.delete()
        pdfFile = null
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun background(color: Int, radius: Int) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun View.background(color: Int, radius: Int) {
        background = this@MainActivity.background(color, radius)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        closePdf()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_OPEN = 42
        private const val PREFS = "universal_pdf_reader"
        private const val KEY_LIBRARY = "library"
        private const val ENTRY_SEPARATOR = "\u001e"
        private const val FIELD_SEPARATOR = "\u001f"
    }
}
