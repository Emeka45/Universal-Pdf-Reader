package com.coeric.universalreader

import android.net.Uri
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit

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
                activity.supportFragmentManager
                    .findFragmentByTag(tag) == null
            ) {

                activity.supportFragmentManager
                    .commit {

                        replace(
                            containerId,
                            ReadiumEpubFragment.newInstance(
                                uri
                            ),
                            tag
                        )
                    }
            }
        }
    )

    DisposableEffect(
        activity,
        containerId
    ) {

        onDispose {

            activity.supportFragmentManager
                .findFragmentByTag(
                    "readium_epub_screen"
                )
                ?.let { fragment ->

                    if (!fragment.isRemoving) {

                        activity.supportFragmentManager
                            .commit {

                                remove(fragment)
                            }
                    }
                }
        }
    }
}