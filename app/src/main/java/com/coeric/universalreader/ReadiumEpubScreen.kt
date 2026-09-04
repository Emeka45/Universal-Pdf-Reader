package com.coeric.universalreader

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.readium.r2.shared.publication.Locator
import java.util.UUID

@androidx.compose.material3.ExperimentalMaterial3Api
@androidx.compose.runtime.Composable
fun ReadiumEpubScreen(uri: Uri, activity: androidx.fragment.app.FragmentActivity, modifier: Modifier = Modifier) {
    var epubFragment by remember { mutableStateOf<ReadiumEpubFragment?>(null) }
    var publicationReady by remember { mutableStateOf(false) }
    var currentLocator by remember { mutableStateOf<Locator?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var controlsVisible by remember { mutableStateOf(true) }
    var showToc by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var isBookmarked by remember { mutableStateOf(false) }
    var openError by remember { mutableStateOf<String?>(null) }

    BackHandler {
        when {
            showToc -> showToc = false
            showSearch -> showSearch = false
            showSettings -> showSettings = false
            else -> activity.finish()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            FrameLayout(context).apply {
                id = android.view.View.generateViewId()
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                post {
                    if (activity.isFinishing || activity.isDestroyed) return@post
                    val existing = activity.supportFragmentManager.findFragmentByTag(READIUM_FRAGMENT_TAG) as? ReadiumEpubFragment
                    if (existing != null) {
                        epubFragment = existing
                        return@post
                    }
                    val fragment = ReadiumEpubFragment.newInstance(uri)
                    epubFragment = fragment
                    activity.supportFragmentManager.beginTransaction().replace(id, fragment, READIUM_FRAGMENT_TAG).commit()
                }
            }
        },
        update = { }
    )

    LaunchedEffect(epubFragment) {
        val fragment = epubFragment ?: return@LaunchedEffect
        while (true) {
            if (!activity.isFinishing && !activity.isDestroyed) {
                val publication = fragment.getPublication()
                val ready = fragment.isReady()
                publicationReady = ready
                openError = fragment.getOpenError()
                val locator = fragment.getCurrentLocator()
                if (locator != null) {
                    currentLocator = locator
                    progress = locator.locations.totalProgression?.toFloat()?.coerceIn(0f, 1f) ?: 0f
                    ReadiumReadingPositionRepository.save(activity, uri.toString(), locator)
                    val bookmarks = ReadiumBookmarkRepository.getForDocument(activity, uri.toString())
                    isBookmarked = bookmarks.any { bookmark -> bookmark.locator.href?.toString() == locator.href?.toString() }
                }
                if (publication != null) publicationReady = ready
            }
            delay(300)
        }
    }

    if (openError != null && !publicationReady) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(text = "Unable to open EPUB\n\n$openError", style = MaterialTheme.typography.bodyLarge)
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (controlsVisible) {
            TopAppBar(
                title = { Text("EPUB Reader") },
                navigationIcon = {
                    IconButton(onClick = { activity.finish() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(enabled = publicationReady, onClick = { showToc = true }) {
                        Icon(Icons.Default.Menu, "Table of contents")
                    }
                    IconButton(enabled = publicationReady, onClick = { showSearch = true }) {
                        Icon(Icons.Default.Search, "Search")
                    }
                    IconButton(enabled = publicationReady && currentLocator != null, onClick = {
                        val locator = currentLocator ?: return@IconButton
                        val bookmarks = ReadiumBookmarkRepository.getForDocument(activity, uri.toString())
                        val matching = bookmarks.firstOrNull { bookmark ->
                            bookmark.locator.href?.toString() == locator.href?.toString()
                        }
                        if (matching != null) {
                            ReadiumBookmarkRepository.remove(activity, matching.id)
                            isBookmarked = false
                        } else {
                            ReadiumBookmarkRepository.add(
                                activity,
                                ReadiumBookmark(
                                    id = UUID.randomUUID().toString(),
                                    documentUri = uri.toString(),
                                    locator = locator,
                                    title = "Bookmark"
                                )
                            )
                            isBookmarked = true
                        }
                    }) {
                        Icon(if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, if (isBookmarked) "Remove bookmark" else "Bookmark")
                    }
                    IconButton(enabled = publicationReady, onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, "Reader settings")
                    }
                }
            )
        }
        if (controlsVisible && publicationReady) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter))
        }
        if (!publicationReady && openError == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }

    if (showToc) {
        epubFragment?.let { fragment ->
            ReadiumTocPanel(
                items = fragment.getTableOfContents(),
                onItemSelected = { item ->
                    fragment.openTocItem(item)
                    showToc = false
                }
            )
        }
    }

    if (showSearch) {
        epubFragment?.getPublication()?.let { publication ->
            ReadiumEpubSearchPanel(
                publication = publication,
                onResultSelected = { result ->
                    epubFragment?.openSearchResult(result)
                    showSearch = false
                },
                onClose = { showSearch = false }
            )
        }
    }

    if (showSettings) {
        val settings = ReaderSettingsRepository.get(activity, uri.toString())
        ReaderSettingsDialog(
            settings = settings,
            onSave = { newSettings ->
                ReaderSettingsRepository.save(activity, uri.toString(), newSettings)
                epubFragment?.applyReaderSettings(newSettings)
                showSettings = false
            },
            onDismiss = { showSettings = false }
        )
    }
}

private const val READIUM_FRAGMENT_TAG = "universal_reader_readium_epub"
