package com.coeric.universalreader

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.readium.r2.shared.publication.Locator

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun ReadiumEpubScreen(
    uri: Uri,
    activity: FragmentActivity,
    modifier: Modifier = Modifier
) {

    val context =
        activity.applicationContext

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
        mutableStateOf(0f)
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

    Box(
        modifier =
            modifier.fillMaxSize()
    ) {

        /*
         * READIUM NAVIGATOR
         *
         * This is the actual reading surface.
         * No full-screen clickable Compose layer is
         * placed over it.
         */
        AndroidView(

            modifier =
                Modifier.fillMaxSize(),

            factory = { androidContext ->

                FrameLayout(
                    androidContext
                ).apply {

                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                    id =
                        android.view.View.generateViewId()

                    post {

                        if (
                            epubFragment == null
                        ) {

                            val fragment =
                                ReadiumEpubFragment
                                    .newInstance(
                                        uri
                                    )

                            epubFragment =
                                fragment

                            activity
                                .supportFragmentManager
                                .beginTransaction()
                                .replace(
                                    id,
                                    fragment,
                                    "readium_epub_screen_fragment"
                                )
                                .commitNow()

                            publicationReady =
                                true
                        }
                    }
                }
            }
        )

        /*
         * Monitor the Readium navigator.
         *
         * This saves the current Locator and updates
         * the progress indicator without touching or
         * intercepting the reading surface.
         */
        LaunchedEffect(
            epubFragment
        ) {

            while (
                isActive
            ) {

                val fragment =
                    epubFragment

                if (
                    fragment != null
                ) {

                    val locator =
                        fragment
                            .getCurrentLocator()

                    currentLocator =
                        locator

                    if (
                        locator != null
                    ) {

                        val progression =
                            locator
                                .locations
                                .progression

                        if (
                            progression != null
                        ) {

                            progress =
                                progression
                                    .toFloat()
                                    .coerceIn(
                                        0f,
                                        1f
                                    )
                        }

                        ReadiumReadingPositionRepository
                            .save(
                                context =
                                    context,
                                documentUri =
                                    uri.toString(),
                                locator =
                                    locator
                            )

                        val bookmarks =
                            ReadiumBookmarkRepository
                                .getForDocument(
                                    context =
                                        context,
                                    documentUri =
                                        uri.toString()
                                )

                        isBookmarked =
                            bookmarks.any {

                                it.locator.href ==
                                    locator.href &&
                                    it.locator
                                        .locations
                                        .progression ==
                                    locator
                                        .locations
                                        .progression
                            }
                    }

                    publicationReady =
                        fragment
                            .getPublication() != null
                }

                delay(500)
            }
        }

        /*
         * TOP READER BAR
         *
         * Only the actual toolbar occupies this layer.
         * The EPUB reading area remains available for
         * Readium scrolling.
         */
        if (
            controlsVisible
        ) {

            TopAppBar(

                title = {

                    Text(
                        text =
                            "Universal Reader"
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
                     * Table of contents
                     */
                    IconButton(
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
                     * Search
                     */
                    IconButton(
                        onClick = {

                            if (
                                epubFragment
                                    ?.getPublication() != null
                            ) {

                                showSearch =
                                    true
                            }
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
                     * Bookmark
                     */
                    IconButton(
                        onClick = {

                            val locator =
                                currentLocator
                                    ?: return@IconButton

                            val existing =
                                ReadiumBookmarkRepository
                                    .getForDocument(
                                        context =
                                            context,
                                        documentUri =
                                            uri.toString()
                                    )
                                    .firstOrNull {

                                        it.locator.href ==
                                            locator.href &&
                                            it.locator
                                                .locations
                                                .progression ==
                                            locator
                                                .locations
                                                .progression
                                    }

                            if (
                                existing != null
                            ) {

                                ReadiumBookmarkRepository
                                    .remove(
                                        context =
                                            context,
                                        bookmarkId =
                                            existing.id
                                    )

                                isBookmarked =
                                    false

                            } else {

                                ReadiumBookmarkRepository
                                    .add(
                                        context =
                                            context,

                                        bookmark =
                                            ReadiumBookmark(

                                                id =
                                                    java.util.UUID
                                                        .randomUUID()
                                                        .toString(),

                                                documentUri =
                                                    uri.toString(),

                                                locator =
                                                    locator,

                                                title =
                                                    "Bookmark"
                                            )
                                    )

                                isBookmarked =
                                    true
                            }
                        }
                    ) {

                        Icon(
                            imageVector =
                                if (
                                    isBookmarked
                                ) {
                                    Icons.Default.Bookmark
                                } else {
                                    Icons.Default.BookmarkBorder
                                },

                            contentDescription =
                                "Bookmark"
                        )
                    }

                    /*
                     * Settings
                     */
                    IconButton(
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
         * Bottom reading progress.
         */
        if (
            controlsVisible
        ) {

            LinearProgressIndicator(

                progress = {
                    progress
                },

                modifier =
                    Modifier
                        .align(
                            Alignment.BottomCenter
                        )
                        .fillMaxSize()
                        .height(3.dp)
            )
        }

        /*
         * TABLE OF CONTENTS
         */
        if (
            showToc &&
            epubFragment != null
        ) {

            Surface(
                modifier =
                    Modifier.fillMaxSize()
            ) {

                ReadiumTocPanel(

                    items =
                        epubFragment
                            ?.getTableOfContents()
                            ?: emptyList(),

                    onItemSelected = { item ->

                        epubFragment
                            ?.openTocItem(
                                item
                            )

                        showToc =
                            false
                    }
                )
            }
        }

        /*
         * EPUB SEARCH
         */
        if (
            showSearch
        ) {

            val publication =
                epubFragment
                    ?.getPublication()

            if (
                publication != null
            ) {

                Surface(
                    modifier =
                        Modifier.fillMaxSize()
                ) {

                    ReadiumEpubSearchPanel(

                        publication =
                            publication,

                        onResultSelected = { result ->

                            epubFragment
                                ?.openSearchResult(
                                    result
                                )

                            showSearch =
                                false
                        },

                        onClose = {

                            showSearch =
                                false
                        }
                    )
                }
            }
        }

        /*
         * READER SETTINGS
         *
         * Your actual ReaderSettingsPanel only
         * accepts settings and onSettingsChanged.
         * It does NOT have onDismiss or onClose.
         */
        if (
            showSettings &&
            epubFragment != null
        ) {

            val settings =
                ReaderSettingsRepository.get(
                    context =
                        context,
                    documentUri =
                        uri.toString()
                )

            Surface(
                modifier =
                    Modifier.fillMaxSize()
            ) {

                ReaderSettingsPanel(

                    settings =
                        settings,

                    onSettingsChanged = { newSettings ->

                        ReaderSettingsRepository
                            .save(
                                context =
                                    context,
                                documentUri =
                                    uri.toString(),
                                settings =
                                    newSettings
                            )

                        epubFragment
                            ?.applyReaderSettings(
                                newSettings
                            )
                    }
                )
            }
        }

        /*
         * Loading indicator.
         */
        if (
            !publicationReady &&
            !showToc &&
            !showSearch &&
            !showSettings
        ) {

            Box(
                modifier =
                    Modifier.fillMaxSize(),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        "Opening EPUB..."
                )
            }
        }
    }
}