package com.coeric.universalreader

import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import org.readium.r2.shared.publication.Publication

@Composable
fun ReadiumEpubScreen(
    publication: Publication,
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
                            ReadiumEpubFragment(
                                publication
                            ),
                            tag
                        )
                    }
            }
        }
    )

    DisposableEffect(
        publication
    ) {

        onDispose {

            activity.supportFragmentManager
                .findFragmentByTag(
                    "readium_epub_screen"
                )
                ?.let { fragment ->

                    activity.supportFragmentManager
                        .commit {

                            remove(fragment)
                        }
                }
        }
    }
}