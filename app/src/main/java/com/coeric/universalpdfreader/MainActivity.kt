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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
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
            setBackgroundResource(R.drawable.u_logo)
        }
        header.addView(logo, LinearLayout.LayoutParams(48.dp(), 48.dp()))

        val names = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12.dp(), 0, 0, 0)
        }
        titleLabel = TextView(this).apply {
            text = "Universal PDF Reader"
            textSize = 19f
            setTextColor(Color.rgb(25, 27, 35))
        }
        names.addView(titleLabel)
        names.addView(TextView(this).apply {
            text = "Your documents. Your way."
            textSize = 12f
            setTextColor(Color.rgb(105, 108, 120))
        })
        header.addView(names, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(
            actionButton("OPEN") { openPicker() },
            LinearLayout.LayoutParams(76.dp(), 44.dp())
        )

        val searchRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14.dp(), 8.dp(), 14.dp(), 8.dp())
        }
        searchBox = EditText(this).apply {
            hint = "Search this PDF"
            textSize = 14f
            setSingleLine(true)
            setPadding(16.dp(), 0, 10.dp(), 0)
        }
        searchRow.addView(searchBox, LinearLayout.LayoutParams(0, 48.dp(), 1f))
        searchRow.addView(
            actionButton("SEARCH") { searchPdf(searchBox.text.toString()) },
            LinearLayout.LayoutParams(88.dp(), 48.dp()).apply {
                leftMargin = 8.dp()
            }
        )

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

        val buttons = listOf(
            iconButton("‹") { showPage(currentPage - 1) },
            iconButton("−") { setZoom(zoom - 0.25f) },
            actionButton("FIT") { setZoom(1f) },
            iconButton("+") { setZoom(zoom + 0.25f) },
            actionButton("ROTATE") {
                rotation = (rotation + 90f) % 360f
                applyMatrix()
            },
            actionButton("SHARE") { sharePdf() },
            iconButton("›") { showPage(currentPage + 1) }
        )

        buttons.forEachIndexed { index, view ->
            val width = when {
                index == 2 -> 64.dp()
                index in 4..5 -> 86.dp()
                else -> 50.dp()
            }
            controls.addView(view, LinearLayout.LayoutParams(width, 48.dp()).apply {
                leftMargin = 4.dp()
                rightMargin = 4.dp()
            })
        }

        root.addView(header)
        root.addView(searchRow)
        root.addView(pageImage, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(pageLabel)
        root.addView(controls)
        setContentView(root)
    }

    private fun actionButton(label: String, click: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 11f
            setTextColor(Color.rgb(52, 39, 170))
            setOnClickListener { click() }
        }

    private fun iconButton(label: String, click: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 22f
            setOnClickListener { click() }
        }

    private fun openPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
        startActivityForResult(intent, REQUEST_OPEN)
    }

    @Deprecated("Deprecated in Android API 33")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_OPEN || resultCode != RESULT_OK) return

        val uri = data?.data ?: return
        if ((data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers do not offer persistable permissions.
            }
        }
        openPdf(uri)
    }

    private fun openPdf(uri: Uri) {
        try {
            closePdf()

            val destination = File.createTempFile("reader_", ".pdf", cacheDir)
            val input = contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Unable to read PDF")

            input.use { stream ->
                FileOutputStream(destination).use { output ->
                    stream.copyTo(output)
                }
            }

            pdfFile?.delete()
            pdfFile = destination
            descriptor = ParcelFileDescriptor.open(
                destination,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            renderer = PdfRenderer(descriptor!!)
            currentPage = 0
            zoom = 1f
            rotation = 0f
            titleLabel.text = uri.lastPathSegment?.substringAfterLast('/')
                ?: "Universal PDF Reader"
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
            val width = (page.width * resources.displayMetrics.density)
                .toInt()
                .coerceIn(800, 2048)
            val height = (page.height.toFloat() / page.width * width)
                .toInt()
                .coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.eraseColor(Color.WHITE)
            page.render(
                bitmap,
                null,
                null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            )
            page.close()

            currentBitmap?.recycle()
            currentBitmap = bitmap
            pageImage.setImageBitmap(bitmap)
            currentPage = index
            updateLabel()
            pageImage.post { applyMatrix() }
        } catch (e: Exception) {
            toast("Could not render page: ${e.message ?: "unknown error"}")
        }
    }

    private fun setZoom(value: Float) {
        zoom = value.coerceIn(0.5f, 3f)
        applyMatrix()
        updateLabel()
    }

    private fun applyMatrix() {
        if (!::pageImage.isInitialized || currentBitmap == null) return

        val matrix = Matrix()
        matrix.postScale(
            zoom,
            zoom,
            pageImage.width / 2f,
            pageImage.height / 2f
        )
        matrix.postRotate(
            rotation,
            pageImage.width / 2f,
            pageImage.height / 2f
        )
        pageImage.imageMatrix = matrix
    }

    private fun updateLabel() {
        val currentRenderer = renderer ?: return
        pageLabel.text = "Page ${currentPage + 1} of ${currentRenderer.pageCount}  •  ${zoomPercent()}"
    }

    private fun zoomPercent(): String = "${(zoom * 100).toInt()}%"

    private fun searchPdf(query: String) {
        val file = pdfFile
        if (file == null) {
            toast("Open a PDF first")
            return
        }

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
                        if (found >= 0) {
                            showPage(found)
                            toast("Found on page ${found + 1}")
                        } else {
                            toast("No match found")
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    toast("Search failed: ${e.message ?: "unknown error"}")
                }
            }
        }.start()
    }

    private fun sharePdf() {
        val file = pdfFile
        if (file == null) {
            toast("Open a PDF first")
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                this,
                "com.coeric.universalpdfreader.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share PDF"))
        } catch (_: Exception) {
            toast("Unable to share PDF")
        }
    }

    private fun closePdf() {
        renderer?.close()
        renderer = null
        descriptor?.close()
        descriptor = null
        currentBitmap?.recycle()
        currentBitmap = null
        if (::pageImage.isInitialized) {
            pageImage.setImageDrawable(null)
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        closePdf()
        pdfFile?.delete()
        pdfFile = null
        super.onDestroy()
    }

    private fun Int.dp(): Int =
        (this * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_OPEN = 42
    }
}
