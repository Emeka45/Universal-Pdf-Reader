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

    val context =
        activity.applicationContext

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
         * Monitor Readium's current locator.
         *
         * This does not intercept touch events.
         * The Readium navigator remains the actual
         * scrolling surface.
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

                    if (
                        fragment
                            .getPublication() != null
                    ) {

                        publicationReady =
                            true
                    }
                }

                delay(500)
            }
        }

        /*
         * Controls.
         *
         * These occupy only the toolbar/progress
         * areas and do not put a clickable layer over
         * the EPUB reading surface.
         */
        if (
            controlsVisible
        ) {

            Surface(
                modifier =
                    Modifier.fillMaxSize()
            ) {

                Box(
                    modifier =
                        Modifier.fillMaxSize()
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

                            IconButton(
                                onClick = {
                                    showSettings =
                                        true
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
            }
        }

        /*
         * Table of contents.
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
         * EPUB search.
         *
         * The publication is checked before
         * passing it to the search panel.
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
         * Reader settings.
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
                    },

                    onClose = {
                        showSettings =
                            false
                    }
                )
            }
        }

        /*
         * Keep the variable referenced so the state
         * remains available for future EPUB controls.
         */
        if (
            !publicationReady
        ) {

            Surface(
                modifier =
                    Modifier.fillMaxSize()
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
}