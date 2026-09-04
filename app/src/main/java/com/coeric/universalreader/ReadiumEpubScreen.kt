package com.coeric.universalreader

import android.net.Uri
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

@Composable
fun ReadiumEpubScreen(
    uri: Uri,
    activity: FragmentActivity,
    modifier: Modifier = Modifier
) {

    val containerId =
        remember {
            android.view.View.generateViewId()
        }

    var progress by remember {
        mutableFloatStateOf(0f)
    }

    var currentLocator by remember {
        mutableStateOf<Locator?>(null)
    }

    var controlsVisible by remember {
        mutableStateOf(true)
    }

    var tocItems by remember {
        mutableStateOf(
            emptyList<ReadiumTocItem>()
        )
    }

    var publication by remember {
        mutableStateOf<Publication?>(null)
    }

    var showSearch by remember {
        mutableStateOf(false)
    }

    var isBookmarked by remember {
        mutableStateOf(false)
    }

    val drawerState =
        rememberDrawerState(
            initialValue =
                DrawerValue.Closed
        )

    val scope =
        rememberCoroutineScope()

    AndroidView(
        modifier = modifier,

        factory = { context ->

            FrameLayout(context).apply {
                id = containerId
            }
        },

        update = {

            val tag =
                "readium_epub_screen"

            if (
                activity
                    .supportFragmentManager
                    .findFragmentByTag(tag) == null
            ) {

                activity
                    .supportFragmentManager
                    .commit {

                        replace(
                            containerId,
                            ReadiumEpubFragment
                                .newInstance(uri),
                            tag
                        )
                    }
            }
        }
    )

    LaunchedEffect(
        activity,
        uri
    ) {

        while (true) {

            val readerFragment =
                activity
                    .supportFragmentManager
                    .findFragmentByTag(
                        "readium_epub_screen"
                    ) as? ReadiumEpubFragment

            if (readerFragment != null) {

                val items =
                    readerFragment
                        .getTableOfContents()

                val epubPublication =
                    readerFragment
                        .getPublication()

                if (
                    items.isNotEmpty() ||
                    epubPublication != null
                ) {

                    tocItems = items
                    publication = epubPublication

                    if (
                        epubPublication != null
                    ) {
                        break
                    }
                }
            }

            delay(100)
        }
    }

    LaunchedEffect(
        activity,
        uri
    ) {

        while (true) {

            val readerFragment =
                activity
                    .supportFragmentManager
                    .findFragmentByTag(
                        "readium_epub_screen"
                    ) as? ReadiumEpubFragment

            val navigator =
                readerFragment
                    ?.childFragmentManager
                    ?.findFragmentByTag(
                        "readium_epub_navigator"
                    ) as? EpubNavigatorFragment

            if (navigator != null) {

                navigator
                    .currentLocator
                    .collect { locator ->

                        currentLocator =
                            locator

                        progress =
                            locator
                                .locations
                                .totalProgression
                                ?.toFloat()
                                ?.coerceIn(
                                    0f,
                                    1f
                                )
                                ?: 0f

                        val bookmarks =
                            ReadiumBookmarkRepository
                                .getForDocument(
                                    context =
                                        activity,
                                    documentUri =
                                        uri.toString()
                                )

                        isBookmarked =
                            bookmarks.any {
                                it.locator
                                    .href ==
                                    locator.href &&
                                it.locator
                                    .locations
                                    .progression ==
                                    locator
                                        .locations
                                        .progression
                            }
                    }

                break
            }

            delay(100)
        }
    }

    BackHandler {

        when {

            showSearch -> {
                showSearch = false
            }

            drawerState.isOpen -> {

                scope.launch {
                    drawerState.close()
                }
            }

            else -> {
                activity.finish()
            }
        }
    }

    if (
        showSearch &&
        publication != null
    ) {

        ReadiumEpubSearchPanel(

            publication =
                publication!!,

            onResultSelected = { result ->

                val readerFragment =
                    activity
                        .supportFragmentManager
                        .findFragmentByTag(
                            "readium_epub_screen"
                        ) as? ReadiumEpubFragment

                readerFragment
                    ?.openSearchResult(
                        result
                    )

                showSearch = false
                controlsVisible = false
            },

            onClose = {
                showSearch = false
            },

            modifier =
                modifier
        )

        return
    }

    ModalNavigationDrawer(

        drawerState =
            drawerState,

        drawerContent = {

            ModalDrawerSheet {

                Text(
                    text = "Contents",

                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall,

                    modifier =
                        Modifier.padding(
                            20.dp
                        )
                )

                if (tocItems.isEmpty()) {

                    Text(
                        text =
                            "No table of contents available.",

                        modifier =
                            Modifier.padding(
                                20.dp
                            )
                    )

                } else {

                    ReadiumTocPanel(

                        items =
                            tocItems,

                        onItemSelected = { item ->

                            val readerFragment =
                                activity
                                    .supportFragmentManager
                                    .findFragmentByTag(
                                        "readium_epub_screen"
                                    ) as? ReadiumEpubFragment

                            readerFragment
                                ?.openTocItem(
                                    item
                                )

                            scope.launch {
                                drawerState.close()
                            }

                            controlsVisible = false
                        }
                    )
                }
            }
        }
    ) {

        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {

            if (controlsVisible) {

                Surface(
                    modifier =
                        Modifier
                            .align(
                                Alignment.TopCenter
                            )
                            .fillMaxWidth(),

                    tonalElevation = 3.dp,

                    shadowElevation = 4.dp
                ) {

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 4.dp
                                ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        IconButton(
                            onClick = {
                                activity.finish()
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons
                                        .AutoMirrored
                                        .Filled
                                        .ArrowBack,

                                contentDescription =
                                    "Back"
                            )
                        }

                        Text(
                            text = "Reading",

                            modifier =
                                Modifier.weight(
                                    1f
                                ),

                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium
                        )

                        IconButton(
                            onClick = {

                                val locator =
                                    currentLocator
                                        ?: return@IconButton

                                val existing =
                                    ReadiumBookmarkRepository
                                        .getForDocument(
                                            context =
                                                activity,
                                            documentUri =
                                                uri.toString()
                                        )
                                        .firstOrNull {
                                            it.locator
                                                .href ==
                                                locator.href &&
                                            it.locator
                                                .locations
                                                .progression ==
                                                locator
                                                    .locations
                                                    .progression
                                        }

                                if (existing != null) {

                                    ReadiumBookmarkRepository
                                        .remove(
                                            context =
                                                activity,
                                            bookmarkId =
                                                existing.id
                                        )

                                    isBookmarked = false

                                } else {

                                    val progression =
                                        locator
                                            .locations
                                            .totalProgression
                                            ?.let {
                                                "${(it * 100).toInt()}%"
                                            }
                                            ?: "Current position"

                                    val bookmark =
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
                                                "Bookmark • $progression"
                                        )

                                    ReadiumBookmarkRepository
                                        .add(
                                            context =
                                                activity,
                                            bookmark =
                                                bookmark
                                        )

                                    isBookmarked = true
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
                                        "Add bookmark"
                                    }
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
                                // Reader settings connection next.
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Settings,

                                contentDescription =
                                    "Settings"
                            )
                        }

                        IconButton(
                            onClick = {

                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Menu,

                                contentDescription =
                                    "Table of contents"
                            )
                        }
                    }
                }

                Surface(
                    modifier =
                        Modifier
                            .align(
                                Alignment.BottomCenter
                            )
                            .fillMaxWidth()
                            .navigationBarsPadding(),

                    tonalElevation = 3.dp
                ) {

                    Column(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        LinearProgressIndicator(
                            progress = {
                                progress
                            },

                            modifier =
                                Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    ),

                            horizontalArrangement =
                                Arrangement.Center
                        ) {

                            Text(
                                text =
                                    "${(progress * 100).toInt()}%",

                                style =
                                    MaterialTheme
                                        .typography
                                        .labelMedium
                            )
                        }
                    }
                }
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {

                            detectTapGestures(
                                onTap = {

                                    controlsVisible =
                                        !controlsVisible
                                }
                            )
                        }
            )
        }
    }

    DisposableEffect(
        activity,
        containerId
    ) {

        onDispose {

            activity
                .supportFragmentManager
                .findFragmentByTag(
                    "readium_epub_screen"
                )
                ?.let { fragment ->

                    if (!fragment.isRemoving) {

                        activity
                            .supportFragmentManager
                            .commit {

                                remove(fragment)
                            }
                    }
                }
        }
    }
}