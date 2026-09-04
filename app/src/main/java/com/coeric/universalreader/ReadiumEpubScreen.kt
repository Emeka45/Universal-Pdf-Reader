package com.coeric.universalreader

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.readium.r2.shared.publication.Locator

@Composable
fun ReadiumEpubScreen(
    uri: Uri,
    activity: FragmentActivity,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current

    var epubFragment by remember {
        mutableStateOf<ReadiumEpubFragment?>(null)
    }

    var publicationReady by remember {
        mutableStateOf(false)
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

    var showBookmarks by remember {
        mutableStateOf(false)
    }

    var isBookmarked by remember {
        mutableStateOf(false)
    }

    var currentLocator by remember {
        mutableStateOf<Locator?>(null)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    BackHandler {

        if (showToc) {
            showToc = false
            return@BackHandler
        }

        if (showSearch) {
            showSearch = false
            return@BackHandler
        }

        if (showSettings) {
            showSettings = false
            return@BackHandler
        }

        if (showBookmarks) {
            showBookmarks = false
            return@BackHandler
        }

        activity.finish()
    }

    Box(
        modifier = modifier.fillMaxSize()
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

                        val fragment =
                            ReadiumEpubFragment
                                .newInstance(uri)

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
        )

        LaunchedEffect(
            epubFragment,
            publicationReady
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

                        val value =
                            locator
                                .locations
                                .progression
                                ?.toFloat()

                        if (
                            value != null
                        ) {

                            progress =
                                value
                                    .coerceIn(
                                        0f,
                                        1f
                                    )

                            ReadiumReadingPositionRepository
                                .save(
                                    context =
                                        context,
                                    documentUri =
                                        uri.toString(),
                                    locator =
                                        locator
                                )
                        }

                        isBookmarked =
                            ReadiumBookmarkRepository
                                .getForDocument(
                                    context =
                                        context,
                                    documentUri =
                                        uri.toString()
                                )
                                .any {
                                    it.locator.href ==
                                        locator.href &&
                                        it.locator
                                            .locations
                                            .progression ==
                                        locator.locations
                                            .progression
                                }
                    }

                    if (
                        fragment.getPublication() != null
                    ) {
                        publicationReady =
                            true
                    }
                }

                delay(500)
            }
        }

        if (
            error != null
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
                            error
                                ?: "Unable to open EPUB."
                    )
                }
            }
        }

        if (
            controlsVisible
        ) {

            Surface(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            bottom = 4.dp
                        ),
                color =
                    Color.Transparent
            ) {

                Box(
                    modifier =
                        Modifier.fillMaxSize()
                ) {

                    TopAppBar(
                        title = {
                            Text(
                                text = "Universal Reader"
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
                                    showBookmarks = true
                                }
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Bookmark,
                                    contentDescription =
                                        "Bookmarks"
                                )
                            }

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

                    Box(
                        modifier =
                            Modifier
                                .align(
                                    Alignment.BottomCenter
                                )
                                .fillMaxSize()
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
                }
            }
        }

        if (
            showToc &&
            epubFragment != null
        ) {

            Surface(
                modifier =
                    Modifier.fillMaxSize(),
                color =
                    MaterialTheme
                        .colorScheme
                        .surface
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

        if (
            showSearch &&
            epubFragment != null
        ) {

            ReadiumEpubSearchPanel(
                publication =
                    epubFragment
                        ?.getPublication(),

                onResultSelected = { result ->

                    epubFragment
                        ?.openSearchResult(
                            result
                        )

                    showSearch =
                        false
                },

                onDismiss = {
                    showSearch = false
                }
            )
        }

        if (
            showSettings &&
            epubFragment != null
        ) {

            ReaderSettingsPanel(
                settings =
                    ReaderSettingsRepository.get(
                        context =
                            context,
                        documentUri =
                            uri.toString()
                    ),

                onSettingsChanged = { settings ->

                    ReaderSettingsRepository.save(
                        context =
                            context,
                        documentUri =
                            uri.toString(),
                        settings =
                            settings
                    )

                    epubFragment
                        ?.applyReaderSettings(
                            settings
                        )
                },

                onDismiss = {
                    showSettings = false
                }
            )
        }

        if (
            showBookmarks
        ) {

            val bookmarks =
                ReadiumBookmarkRepository
                    .getForDocument(
                        context =
                            context,
                        documentUri =
                            uri.toString()
                    )

            Surface(
                modifier =
                    Modifier.fillMaxSize(),
                color =
                    MaterialTheme
                        .colorScheme
                        .surface
            ) {

                ReadiumBookmarkList(
                    bookmarks =
                        bookmarks,

                    onBookmarkSelected = { bookmark ->

                        epubFragment
                            ?.getPublication()

                        epubFragment
                            ?.openSearchResult(
                                ReadiumEpubSearchResult(
                                    locator =
                                        bookmark.locator,
                                    title =
                                        bookmark.title
                                )
                            )

                        showBookmarks =
                            false
                    },

                    onDelete = { bookmark ->

                        ReadiumBookmarkRepository
                            .remove(
                                context =
                                    context,
                                bookmarkId =
                                    bookmark.id
                            )
                    },

                    onDismiss = {
                        showBookmarks = false
                    }
                )
            }
        }

        if (
            !publicationReady &&
            error == null
        ) {

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Color.Transparent
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                CircularProgressIndicator()
            }
        }
    }
}