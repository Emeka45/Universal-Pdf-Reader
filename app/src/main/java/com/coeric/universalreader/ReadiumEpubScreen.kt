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
fun ReadiumEpubScreen(
    uri: Uri,
    activity: androidx.fragment.app.FragmentActivity,
    modifier: Modifier = Modifier
) {

    var epubFragment by remember {
        mutableStateOf<ReadiumEpubFragment?>(null)
    }

    var publicationReady by remember {
        mutableStateOf(false)
    }

    var currentLocator by remember {
        mutableStateOf<Locator?>(null)
    }

    var progress by remember {
        mutableFloatStateOf(0f)
    }

    var controlsVisible by remember {
        mutableStateOf(true)
    }

    var showToc by remember {
        mutableStateOf(false)
    }

    var showSearch by remember {
        mutableStateOf(false)
    }

    var showSettings by remember {
        mutableStateOf(false)
    }

    var isBookmarked by remember {
        mutableStateOf(false)
    }

    var openError by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * ---------------------------------------------------------
     * BACK BUTTON
     * ---------------------------------------------------------
     */

    BackHandler {

        when {

            showToc -> {
                showToc = false
            }

            showSearch -> {
                showSearch = false
            }

            showSettings -> {
                showSettings = false
            }

            else -> {
                activity.finish()
            }
        }
    }

    /*
     * ---------------------------------------------------------
     * READIUM HOST
     *
     * AndroidView only provides the container.
     * ReadiumEpubFragment owns the actual navigator.
     * ---------------------------------------------------------
     */

    AndroidView(
        modifier =
            modifier.fillMaxSize(),

        factory = { context ->

            FrameLayout(context).apply {

                id =
                    android.view.View.generateViewId()

                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                post {

                    if (
                        activity.isFinishing ||
                        activity.isDestroyed
                    ) {
                        return@post
                    }

                    val existing =
                        activity.supportFragmentManager
                            .findFragmentByTag(
                                READIUM_FRAGMENT_TAG
                            ) as? ReadiumEpubFragment

                    if (existing != null) {

                        epubFragment =
                            existing

                        return@post
                    }

                    val fragment =
                        ReadiumEpubFragment.newInstance(
                            uri
                        )

                    epubFragment =
                        fragment

                    activity.supportFragmentManager
                        .beginTransaction()
                        .replace(
                            id,
                            fragment,
                            READIUM_FRAGMENT_TAG
                        )
                        .commit()
                }
            }
        },

        update = {
            /*
             * Nothing here.
             *
             * The Readium fragment owns its lifecycle.
             */
        }
    )

    /*
     * ---------------------------------------------------------
     * WAIT FOR THE REAL READIUM PUBLICATION
     * ---------------------------------------------------------
     */

    LaunchedEffect(epubFragment) {

        val fragment =
            epubFragment
                ?: return@LaunchedEffect

        while (true) {

            if (
                !activity.isFinishing &&
                !activity.isDestroyed
            ) {

                val publication =
                    fragment.getPublication()

                val ready =
                    fragment.isReady()

                publicationReady =
                    ready

                openError =
                    fragment.getOpenError()

                val locator =
                    fragment.getCurrentLocator()

                if (locator != null) {

                    currentLocator =
                        locator

                    progress =
                        locator.locations
                            .totalProgression
                            ?.toFloat()
                            ?.coerceIn(
                                0f,
                                1f
                            )
                            ?: 0f

                    ReadiumReadingPositionRepository
                        .save(
                            context =
                                activity,

                            documentUri =
                                uri.toString(),

                            locator =
                                locator
                        )

                    // Check if bookmark exists in the list
                    val bookmarks =
                        ReadiumBookmarkRepository
                            .getForDocument(
                                context = activity,
                                documentUri = uri.toString()
                            )

                    isBookmarked =
                        bookmarks.any { bookmark ->
                            bookmark.locator.href == locator.href
                        }
                }

                /*
                 * Publication being non-null means the EPUB
                 * was successfully opened.
                 */
                if (publication != null) {
                    publicationReady =
                        ready
                }
            }

            delay(300)
        }
    }

    /*
     * ---------------------------------------------------------
     * ERROR SCREEN
     * ---------------------------------------------------------
     */

    if (
        openError != null &&
        !publicationReady
    ) {

        Surface(
            modifier =
                Modifier.fillMaxSize()
        ) {

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        "Unable to open EPUB\n\n$openError",

                    style =
                        MaterialTheme.typography.bodyLarge
                )
            }
        }

        return
    }

    /*
     * ---------------------------------------------------------
     * READER UI
     * ---------------------------------------------------------
     */

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.background
                )
    ) {

        /*
         * -----------------------------------------------------
         * TOP BAR
         * -----------------------------------------------------
         */

        if (controlsVisible) {

            TopAppBar(

                title = {
                    Text(
                        text = "EPUB Reader"
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = {
                            activity.finish()
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ArrowBack,

                            contentDescription =
                                "Back"
                        )
                    }
                },

                actions = {

                    /*
                     * TOC
                     */
                    IconButton(
                        enabled =
                            publicationReady,

                        onClick = {
                            showToc = true
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Menu,

                            contentDescription =
                                "Table of contents"
                        )
                    }

                    /*
                     * SEARCH
                     */
                    IconButton(
                        enabled =
                            publicationReady,

                        onClick = {
                            showSearch = true
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Search,

                            contentDescription =
                                "Search"
                        )
                    }

                    /*
                     * BOOKMARK
                     */
                    IconButton(
                        enabled =
                            publicationReady &&
                                currentLocator != null,

                        onClick = {

                            val locator =
                                currentLocator
                                    ?: return@IconButton

                            if (isBookmarked) {

                                ReadiumBookmarkRepository
                                    .remove(
                                        context =
                                            activity,

                                        bookmarkId =
                                            locator.href ?: UUID.randomUUID().toString()
                                    )

                                isBookmarked =
                                    false

                            } else {

                                ReadiumBookmarkRepository
                                    .add(
                                        context =
                                            activity,

                                        bookmark =
                                            ReadiumBookmark(
                                                id = UUID.randomUUID().toString(),
                                                documentUri = uri.toString(),
                                                locator = locator,
                                                title = "Bookmark"
                                            )
                                    )

                                isBookmarked =
                                    true
                            }
                        }
                    ) {

                        Icon(
                            imageVector =
                                if (isBookmarked) {
                                    Icons.Default.Bookmark
                                } else {
                                    Icons.Default.BookmarkBorder
                                },

                            contentDescription =
                                if (isBookmarked) {
                                    "Remove bookmark"
                                } else {
                                    "Bookmark"
                                }
                        )
                    }

                    /*
                     * SETTINGS
                     */
                    IconButton(
                        enabled =
                            publicationReady,

                        onClick = {
                            showSettings = true
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Settings,

                            contentDescription =
                                "Reader settings"
                        )
                    }
                }
            )
        }

        /*
         * -----------------------------------------------------
         * PROGRESS
         * -----------------------------------------------------
         */

        if (controlsVisible && publicationReady) {

            LinearProgressIndicator(

                progress = {
                    progress
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(
                            Alignment.BottomCenter
                        )
            )
        }

        /*
         * -----------------------------------------------------
         * LOADING
         * -----------------------------------------------------
         */

        if (!publicationReady && openError == null) {

            Box(
                modifier =
                    Modifier.fillMaxSize(),

                contentAlignment =
                    Alignment.Center
            ) {

                CircularProgressIndicator()
            }
        }
    }

    /*
     * ---------------------------------------------------------
     * TOC
     * ---------------------------------------------------------
     */

    if (showToc) {

        val fragment =
            epubFragment

        if (fragment != null) {

            ReadiumTocPanel(

                items =
                    fragment.getTableOfContents(),

                onItemSelected = { item ->

                    fragment.openTocItem(
                        item
                    )

                    showToc = false
                }
            )
        }
    }

    /*
     * ---------------------------------------------------------
     * SEARCH
     * ---------------------------------------------------------
     */

    if (showSearch) {

        val publication =
            epubFragment
                ?.getPublication()

        if (publication != null) {

            ReadiumEpubSearchPanel(

                publication =
                    publication,

                onResultSelected = { result ->

                    epubFragment
                        ?.openSearchResult(
                            result
                        )

                    showSearch = false
                },

                onClose = {
                    showSearch = false
                }
            )
        }
    }

    /*
     * ---------------------------------------------------------
     * SETTINGS
     * ---------------------------------------------------------
     */

    if (showSettings) {

        val settings =
            ReaderSettingsRepository.get(
                context =
                    activity,

                documentUri =
                    uri.toString()
            )

        ReaderSettingsDialog(
            settings = settings,

            onSave = { newSettings ->

                ReaderSettingsRepository.save(
                    context =
                        activity,

                    documentUri =
                        uri.toString(),

                    settings =
                        newSettings
                )

                epubFragment
                    ?.applyReaderSettings(
                        newSettings
                    )

                showSettings = false
            },

            onDismiss = {
                showSettings = false
            }
        )
    }
}

private const val READIUM_FRAGMENT_TAG =
    "universal_reader_readium_epub"
