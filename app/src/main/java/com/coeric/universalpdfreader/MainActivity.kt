package com.coeric.universalpdfreader

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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
    private lateinit var topBar: LinearLayout
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
    private var currentUri: Uri? = null
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

        topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(12), dp(12))
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
        topBar.addView(logo, LinearLayout.LayoutParams(dp(44), dp(44)))

        val identity = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), 0, dp(8), 0)
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
        topBar.addView(identity, LinearLayout.LayoutParams(0, -2, 1f))

        topBar.addView(topAction("LIBRARY") { showLibrary() }, LinearLayout.LayoutParams(dp(82), dp(42)))
        topBar.addView(topAction("OPEN") { openPicker() }, LinearLayout.LayoutParams(dp(68), dp(42)))
        topBar.addView(topAction("TOOLS") { toggleTools() }, LinearLayout.LayoutParams(dp(68), dp(42)))
        root.addView(topBar)

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
        searchPanel.addView(topAction("SEARCH") { searchPdf(searchBox.text.toString()) }, LinearLayout.LayoutParams(dp(82), dp(46)).apply {
            leftMargin = dp(8)
        })
        root.addView(searchPanel)

        toolsPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(7), dp(10), dp(7))
            background(Color.WHITE, 0f)
        }
        addTool("‹", "PREV") { showPage(currentPage - 1) }
        addTool("−", "ZOOM OUT") { setZoom(zoom - 0.2f) }
        addTool("FIT", "FIT") { fitPage() }
        addTool("+", "ZOOM IN") { setZoom(zoom + 0.2f) }
        addTool("↻", "ROTATE") { rotation = (rotation + 90f) % 360f; applyMatrix() }
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
            adjustViewBounds = true
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

        val heading = TextView(this).apply {
            text = "Your Library"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(25, 27, 35))
        }
        panel.addView(heading)
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
        quick.addView(topAction("OPEN PDF") { openPicker() }, LinearLayout.LayoutParams(dp(94), dp(44)))
        panel.addView(quick)

        val recent = TextView(this).apply {
            text = "RECENT DOCUMENTS"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(105, 108, 122))
            setPadding(dp(2), dp(22), 0, dp(8))
        }
        panel.addView(recent)

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
        libraryList.addView(emptyLibrary)
        return panel
    }

    private fun addTool(label: String, accessibility: String, click: () -> Unit) {
        val button = toolButton(label, click)
        button.contentDescription = accessibility
        val width = if (label.length > 2) dp(72) else dp(48)
        toolsPanel.addView(button, LinearLayout.LayoutParams(width, dp(44)).apply {
            leftMargin = dp(3)
            rightMargin = dp(3)
        })
    }

    private fun toolButton(label: String, click: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = if (label.length == 1) 20f else 10f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(67, 56, 180))
        background(Color.rgb(244, 243, 252), 12f)
        setOnClickListener { click() }
    }

    private fun topAction(label: String, click: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 10f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(67, 56, 180))
        background(Color.rgb(244, 243, 252), 11f)
        setPadding(0, 0, 0, 0)
        setOnClickListener { click() }
    }

    private fun toggleTools() {
        toolsPanel.visibility = if (toolsPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        searchPanel.visibility = if (toolsPanel.visibility == View.VISIBLE) View.VISIBLE else View.GONE
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
        pageImage.post { applyMatrix() }
    }

    private fun refreshLibrary() {
        if (!::libraryList.isInitialized) return
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

            val icon = TextView(this).apply {
                text = "PDF"
                textSize = 10f
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                background(Color.rgb(67, 56, 180), 10f)
            }
            card.addView(icon, LinearLayout.LayoutParams(dp(48), dp(48)))

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
                setPadding(0, dp(3), 0, 0)
            })
            card.addView(info, LinearLayout.LayoutParams(0, -2, 1f))

            card.addView(topAction("OPEN") { openLibraryEntry(entry.second) }, LinearLayout.LayoutParams(dp(68), dp(42)))
            libraryList.addView(card, LinearLayout.LayoutParams(-1, dp(76)).apply {
                bottomMargin = dp(10)
            })
        }
    }

    private fun loadLibrary(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val raw = prefs.getString(KEY_LIBRARY, "") ?: ""
        if (raw.isBlank()) return result
        raw.split(ENTRY_SEPARATOR).forEach { item ->
            val parts = item.split(FIELD_SEPARATOR)
            if (parts.size >= 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                result.add(parts[0] to parts[1])
            }
        }
        return result
    }

    private fun rememberInLibrary(uri: Uri, name: String) {
        val encoded = "${name.replace(FIELD_SEPARATOR, " ")}\u001f${uri}"
        val entries = loadLibrary().filterNot { it.second == uri.toString() }.toMutableList()
        entries.add(0, name.replace(FIELD_SEPARATOR, " ") to uri.toString())
        val trimmed = entries.take(20)
        prefs.edit().putString(
            KEY_LIBRARY,
            trimmed.joinToString(ENTRY_SEPARATOR) { "${it.first}\u001f${it.second}" }
        ).apply()
    }

    private fun openLibraryEntry(uriString: String) {
        try {
            openPdf(Uri.parse(uriString))
        } catch (e: Exception) {
            toast("This PDF is no longer available")
        }
    }

    private fun openPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, REQUEST_OPEN)
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
            currentUri = uri
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
        val currentRenderer = renderer ?: return
        if (index !in 0 until currentRenderer.pageCount) return
        try {
            val page = currentRenderer.openPage(index)
            val width = (page.width * resources.displayMetrics.density).toInt().coerceIn(900, 2200)
            val height = (page.height.toFloat() / page.width * width).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            currentBitmap?.recycle()
            currentBitmap = bitmap
            pageImage.setImageBitmap(bitmap)
            currentPage = index
            updateLabel()
            pageImage.post { fitPage() }
        } catch (e: Exception) {
            toast("Could not render page: ${e.message ?: "unknown error"}")
        }
    }

    private fun fitPage() {
        zoom = 1f
        applyMatrix()
        updateLabel()
    }

    private fun setZoom(value: Float) {
        zoom = value.coerceIn(0.5f, 3f)
        applyMatrix()
        updateLabel()
    }

    private fun applyMatrix() {
        if (!::pageImage.isInitialized || currentBitmap == null) return
        val matrix = Matrix()
        matrix.postScale(zoom, zoom, pageImage.width / 2f, pageImage.height / 2f)
        matrix.postRotate(rotation, pageImage.width / 2f, pageImage.height / 2f)
        pageImage.imageMatrix = matrix
    }

    private fun updateLabel() {
        val count = renderer?.pageCount ?: 0
        if (count > 0) pageLabel.text = "Page ${currentPage + 1} of $count  •  ${(zoom * 100).toInt()}%"
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
                        if (stripper.getText(document).contains(normalized, ignoreCase = true)) { found = page - 1; break }
                    }
                    runOnUiThread {
                        if (found >= 0) { showPage(found); toast("Found on page ${found + 1}") }
                        else toast("No match found")
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
            val uri = FileProvider.getUriForFile(this, "com.coeric.universalpdfreader.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share PDF"))
        } catch (_: Exception) { toast("Unable to share PDF") }
    }

    private fun closePdf() {
        renderer?.close()
        renderer = null
        descriptor?.close()
        descriptor = null
        currentBitmap?.recycle()
        currentBitmap = null
        currentUri = null
        if (::pageImage.isInitialized) pageImage.setImageDrawable(null)
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        closePdf()
        pdfFile?.delete()
        pdfFile = null
        super.onDestroy()
    }

    private fun background(color: Int, radius: Float): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply { setColor(color); cornerRadius = dp(radius.toInt()).toFloat() }

    private fun View.background(color: Int, radius: Float) { background = background(color, radius) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_OPEN = 42
        private const val PREFS = "universal_pdf_reader"
        private const val KEY_LIBRARY = "library"
        private const val ENTRY_SEPARATOR = "\u001e"
        private const val FIELD_SEPARATOR = "\u001f"
    }
}
