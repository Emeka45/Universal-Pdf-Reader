package com.coeric.universalreader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import org.readium.r2.navigator.epub.EpubDefaults
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.publication.Publication

class ReadiumEpubFragment(
    private val publication: Publication
) : Fragment(),
    EpubNavigatorFragment.Listener {

    private val navigatorContainerId =
        View.generateViewId()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        val navigatorFactory =
            EpubNavigatorFactory(
                publication = publication,
                configuration =
                    EpubNavigatorFactory.Configuration(
                        defaults =
                            EpubDefaults(
                                scroll = true
                            )
                    )
            )

        childFragmentManager.fragmentFactory =
            navigatorFactory.createFragmentFactory(
                initialLocator = null,
                listener = this
            )

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return FrameLayout(
            requireContext()
        ).apply {

            id = navigatorContainerId

            layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        if (
            childFragmentManager
                .findFragmentByTag(
                    "readium_epub_navigator"
                ) == null
        ) {

            childFragmentManager
                .beginTransaction()
                .replace(
                    navigatorContainerId,
                    EpubNavigatorFragment::class.java,
                    Bundle(),
                    "readium_epub_navigator"
                )
                .commitNow()
        }
    }
}