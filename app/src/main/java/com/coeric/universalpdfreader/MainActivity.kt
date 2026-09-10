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
import android.view.MotionEvent
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
import kotlin.math.hypot

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
    private lateinit var emptyLibrary: LinearLayout

    private var renderer: PdfRenderer? = null
    private var descriptor: ParcelFileDescriptor? = null
    private var currentPage = 0
    private var pdfFile: File? = null
    private var currentBitmap: Bitmap? = null
    private var zoom = 1f
    private var rotation = 0f
    private var currentUri: Uri? = null

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var pinchDistance = 0f
    private var pinchFocusX = 0f
    private var pinchFocusY = 0f
    private var isPanning = false
    private var isPinching = false

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
            setBackgroundColor(Color.rgb(246, 247, 251))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(12), dp(12))
            background(Color.WHITE, 0)
        }
        val logo = TextView(this).apply {
            text = "U"
            textSize = 22f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background(Color.rgb(67, 56, 180), 14)
            contentDescription = "Universal PDF Reader logo"
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(46), dp(46)))

        val identity = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), 0, dp(6), 0)
        }
        titleLabel = TextView(this).apply {
            text = "Universal PDF Reader"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(28, 29, 38))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        identity.addView(titleLabel)
        identity.addView(TextView(this).apply {
            text = "Read beautifully. Anywhere."
            textSize = 11f
            setTextColor(Color.rgb(112, 115, 129))
        })
        header.addView(identity, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(action("LIBRARY") { showLibrary() }, LinearLayout.LayoutParams(dp(78), dp(42)))
        header.addView(action("OPEN") { openPicker() }, LinearLayout.LayoutParams(dp(64), dp(42)).apply { leftMargin = dp(5) })
        header.addView(action("TOOLS") { toggleTools() }, LinearLayout.LayoutParams(dp(64), dp(42)).apply { leftMargin = dp(5) })
        root.addView(header)

        searchPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background(Color.rgb(249, 249, 252), 0)
        }
        searchBox = EditText(this).apply {
            hint = "Search inside this PDF"
            textSize = 14f
            singleLine = true
            setTextColor(Color.rgb(35, 36, 45))
            setHintTextColor(Color.rgb(145, 147, 158))
            setPadding(dp(14), 0, dp(10), 0)
            background(Color.WHITE, 14)
        }
        searchPanel.addView(searchBox, LinearLayout.LayoutParams(0, dp(46), 1f))
        searchPanel.addView(action("SEARCH") { searchPdf(searchBox.text.toString()) }, LinearLayout.LayoutParams(dp(82), dp(46)).apply { leftMargin = dp(8) })
        root.addView(searchPanel)

        toolsPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(7), dp(8), dp(7))
            background(Color.WHITE, 0)
        }
        addTool("‹", "PREVIOUS PAGE") { showPage(currentPage - 1) }
        addTool("−", "ZOOM OUT") { setZoom(zoom - 0.2f) }
        addTool("FIT", "FIT PAGE") { fitPage() }
        addTool("+", "ZOOM IN") { setZoom(zoom + 0.2f) }
        addTool("↻", "ROTATE") { rotation = (rotation + 90f) % 360f; applyTransform() }
        addTool("SHARE", "SHARE PDF") { sharePdf() }
        addTool("›", "NEXT PAGE") { showPage(currentPage + 1) }
        root.addView(toolsPanel)

        readerPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background(Color.rgb(229, 231, 237), 0)
        }
        val viewer = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(18), dp(18), dp(12))
            background(Color.rgb(229, 231, 237), 0)
        }
        pageImage = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.WHITE)
            elevation = dp(6).toFloat()
            contentDescription = "PDF page"
            pivotX = 0f
            pivotY = 0f
            setOnTouchListener { view, event -> handlePageTouch(view, event) }
        }
        viewer.addView(pageImage, LinearLayout.LayoutParams(-1, -1))
        readerPanel.addView(viewer, LinearLayout.LayoutParams(-1, 0, 1f))
        pageLabel = TextView(this).apply {
            text = "No document open"
            gravity = Gravity.CENTER
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(78, 81, 94))
            setPadding(dp(8), dp(9), dp(8), dp(11))
            background(Color.WHITE, 0)
        }
        readerPanel.addView(pageLabel)
        root.addView(readerPanel, LinearLayout.LayoutParams(-1, 0, 1f))

        libraryPanel = buildLibraryPanel()
        root.addView(libraryPanel, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun handlePageTouch(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isPanning = false
                isPinching = false
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    pinchDistance = distance(event)
                    pinchFocusX = midpointX(event)
                    pinchFocusY = midpointY(event)
                    isPinching = pinchDistance > 0f
                    isPanning = false
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2 && pinchDistance > 0f) {
                    val newDistance = distance(event)
                    if (newDistance > 0f) {
                        val factor = newDistance / pinchDistance
                        val oldZoom = zoom
                        val newZoom = (oldZoom * factor).coerceIn(0.5f, 5f)
                        val actualFactor = if (oldZoom > 0f) newZoom / oldZoom else 1f
                        val focusX = midpointX(event)
                        val focusY = midpointY(event)
                        pageImage.translationX = focusX - (focusX - pageImage.translationX) * actualFactor
                        pageImage.translationY = focusY - (focusY - pageImage.translationY) * actualFactor
                        zoom = newZoom
                        pinchDistance = newDistance
                        updateLabel()
                    }
                    return true
                }

                if (event.pointerCount == 1 && zoom > 1.01f && !isPinching) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    pageImage.translationX += dx
                    pageImage.translationY += dy
                    isPanning = true
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount <= 2) {
                    pinchDistance = 0f
                    isPinching = false
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pinchDistance = 0f
                isPinching = false
                isPanning = false
                return true
            }
        }
        return true
    }

    private fun distance(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        return hypot(
            event.getX(0) - event.getX(1),
            event.getY(0) - event.getY(1)
        )
    }

    private fun midpointX(event: MotionEvent): Float =
        if (event.pointerCount >= 2) (event.getX(0) + event.getX(1)) / 2f else event.x

    private fun midpointY(event: MotionEvent): Float =
        if (event.pointerCount >= 2) (event.getY(0) + event.getY(1)) / 2f else event.y

    private fun buildLibraryPanel(): LinearLayout {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(14))
            background(Color.rgb(246, 247, 251), 0)
        }
        val titleRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val heading = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        heading.addView(TextView(this).apply {
            text = "Your Library"
            textSize = 29f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(27, 29, 38))
        })
        heading.addView(TextView(this).apply {
            text = "Your reading space, organised."
            textSize = 12f
            setTextColor(Color.rgb(112, 115, 129))
            setPadding(0, dp(2), 0, 0)
        })
        titleRow.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))
        titleRow.addView(action("OPEN PDF") { openPicker() }, LinearLayout.LayoutParams(dp(100), dp(44)))
        panel.addView(titleRow)

        val accent = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(13), dp(15), dp(13))
            background(Color.rgb(238, 236, 252), 16)
        }
        val mark = TextView(this).apply {
            text = "U"
            textSize = 18f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background(Color.rgb(67, 56, 180), 12)
        }
        accent.addView(mark, LinearLayout.LayoutParams(dp(42), dp(42)))
        val textContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), 0, dp(8), 0) }
        textContainer.addView(TextView(this).apply {
            text = "Continue reading"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(43, 40, 76))
        })
        textContainer.addView(TextView(this).apply {
            text = "Open your latest document and continue where you stopped."
            textSize = 11f
            setTextColor(Color.rgb(93, 90, 119))
        })
        accent.addView(textContainer, LinearLayout.LayoutParams(0, -2, 1f))
        panel.addView(accent, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })

        panel.addView(TextView(this).apply {
            text = "RECENT DOCUMENTS"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(103, 106, 120))
            setPadding(dp(2), dp(22), 0, dp(9))
        })
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        libraryList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(libraryList)
        panel.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        emptyLibrary = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(40), dp(24), dp(40))
        }
        val emptyIcon = TextView(this).apply {
            text = "PDF"
            textSize = 12f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background(Color.rgb(67, 56, 180), 16)
        }
        emptyLibrary.addView(emptyIcon, LinearLayout.LayoutParams(dp(64), dp(64)).apply { bottomMargin = dp(12) })
        emptyLibrary.addView(TextView(this).apply {
            text = "Your library is waiting"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(44, 45, 55))
        })
        emptyLibrary.addView(TextView(this).apply {
            text = "Open a PDF and it will appear here automatically."
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(112, 115, 129))
            setPadding(0, dp(4), 0, 0)
        })
        return panel
    }

    private fun addTool(label: String, description: String, click: () -> Unit) {
        val button = action(label) { click() }
        button.contentDescription = description
        button.textSize = if (label.length == 1) 20f else 9f
        toolsPanel.addView(button, LinearLayout.LayoutParams(if (label.length > 2) dp(70) else dp(46), dp(44)).apply { leftMargin = dp(3); rightMargin = dp(3) })
    }

    private fun action(label: String, click: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 10f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(67, 56, 180))
        background(Color.rgb(241, 240, 251), 11)
        setPadding(0, 0, 0, 0)
        minHeight = 0
        minimumHeight = 0
        minWidth = 0
        minimumWidth = 0
        stateListAnimator = null
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
        entries.forEachIndexed { index, entry ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(11), dp(11), dp(9), dp(11))
                background(Color.WHITE, 17)
                elevation = dp(2).toFloat()
            }
            val cover = TextView(this).apply {
                text = "PDF"
                textSize = 10f
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                background(if (index == 0) Color.rgb(67, 56, 180) else Color.rgb(88, 84, 157), 12)
            }
            card.addView(cover, LinearLayout.LayoutParams(dp(58), dp(68)))

            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), 0, dp(7), 0)
            }
            info.addView(TextView(this).apply {
                text = entry.first
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.rgb(35, 36, 45))
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            val page = getSavedPage(entry.second)
            info.addView(TextView(this).apply {
                text = if (page > 0) "Continue • page ${page + 1}" else "PDF document"
                textSize = 11f
                setTextColor(Color.rgb(116, 118, 131))
                setPadding(0, dp(3), 0, 0)
            })
            card.addView(info, LinearLayout.LayoutParams(0, -2, 1f))

            val open = action("OPEN") { openLibraryEntry(entry.second) }
            open.contentDescription = "Open ${entry.first}"
            card.addView(open, LinearLayout.LayoutParams(dp(65), dp(42)))

            libraryList.addView(card, LinearLayout.LayoutParams(-1, dp(90)).apply { bottomMargin = dp(10) })
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
        prefs.edit().putString(KEY_LIBRARY, entries.take(30).joinToString(ENTRY_SEPARATOR) { "${it.first}$FIELD_SEPARATOR${it.second}" }).apply()
    }

    private fun getSavedPage(uri: String): Int = prefs.getInt(PAGE_PREFIX + uri.hashCode(), 0)

    private fun savePage(uri: Uri, page: Int) {
        prefs.edit().putInt(PAGE_PREFIX + uri.toString().hashCode(), page).apply()
    }

    private fun openLibraryEntry(uriString: String) = openPdf(Uri.parse(uriString), getSavedPage(uriString))

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
        openPdf(uri, 0)
    }

    private fun openPdf(uri: Uri, requestedPage: Int = 0) {
        try {
            closePdf()
            val destination = File.createTempFile("reader_", ".pdf", cacheDir)
            contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(destination).use { output -> input.copyTo(output) } }
                ?: throw IllegalStateException("Unable to read PDF")
            pdfFile = destination
            currentUri = uri
            descriptor = ParcelFileDescriptor.open(destination, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(descriptor!!)
            currentPage = requestedPage.coerceIn(0, renderer!!.pageCount - 1)
            zoom = 1f
            rotation = 0f
            resetPageTransform()
            titleLabel.text = uri.lastPathSegment?.substringAfterLast('/') ?: "Universal PDF Reader"
            searchBox.setText("")
            showReader()
            showPage(currentPage)
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
            currentUri?.let { savePage(it, index) }
            applyTransform()
            updateLabel()
        } catch (e: Exception) {
            toast("Could not render page: ${e.message ?: "unknown error"}")
        }
    }

    private fun setZoom(value: Float) {
        zoom = value.coerceIn(0.5f, 5f)
        applyTransform(); updateLabel()
    }

    private fun fitPage() {
        zoom = 1f
        resetPageTransform()
        applyTransform()
        updateLabel()
    }

    private fun resetPageTransform() {
        pageImage.translationX = 0f
        pageImage.translationY = 0f
        pageImage.scaleX = 1f
        pageImage.scaleY = 1f
        pageImage.rotation = 0f
    }

    private fun applyTransform() {
        pageImage.scaleX = zoom
        pageImage.scaleY = zoom
        pageImage.rotation = rotation
    }

    private fun updateLabel() {
        val r = renderer ?: return
        pageLabel.text = "Page ${currentPage + 1} of ${r.pageCount}   •   ${(zoom * 100).toInt()}%"
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
                        stripper.startPage = page; stripper.endPage = page
                        if (stripper.getText(document).contains(normalized, ignoreCase = true)) { found = page - 1; break }
                    }
                    runOnUiThread { if (found >= 0) showPage(found) else toast("No matches found") }
                }
            } catch (e: Exception) { runOnUiThread { toast("Search failed: ${e.message ?: "unknown error"}") } }
        }.start()
    }

    private fun sharePdf() {
        val file = pdfFile ?: run { toast("Open a PDF first"); return }
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Share PDF"))
        } catch (_: Exception) { toast("Unable to share PDF") }
    }

    private fun closePdf() {
        try { renderer?.close() } catch (_: Exception) { }
        try { descriptor?.close() } catch (_: Exception) { }
        renderer = null; descriptor = null
        currentBitmap?.recycle(); currentBitmap = null
        pdfFile?.delete(); pdfFile = null; currentUri = null
        if (::pageImage.isInitialized) resetPageTransform()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun background(color: Int, radius: Int) = android.graphics.drawable.GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_OPEN = 1001
        private const val PREFS = "universal_pdf_reader"
        private const val KEY_LIBRARY = "library"
        private const val PAGE_PREFIX = "page_"
        private const val FIELD_SEPARATOR = "|||"
        private const val ENTRY_SEPARATOR = "\n"
    }
}
