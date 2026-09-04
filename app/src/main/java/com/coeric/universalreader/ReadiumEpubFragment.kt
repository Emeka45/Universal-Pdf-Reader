package com.coeric.universalreader

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubDefaults
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.publication.Publication

class ReadiumEpubFragment :
    Fragment(),
    EpubNavigatorFragment.Listener {

    private var navigatorContainerId: Int = 0

    private var tocItems: List<ReadiumTocItem> =
        emptyList()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        navigatorContainerId =
            View.generateViewId()

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
                    NAVIGATOR_TAG
                ) != null
        ) {
            return
        }

        val uriString =
            arguments
                ?.getString(ARG_URI)

        if (uriString.isNullOrBlank()) {
            return
        }

        val uri =
            Uri.parse(uriString)

        viewLifecycleOwner
            .lifecycleScope
            .launch {

                val result =
                    ReadiumEpubRepository.open(
                        context =
                            requireContext(),
                        uri = uri
                    )

                result.onSuccess { publication ->

                    if (!isAdded) {
                        return@onSuccess
                    }

                    tocItems =
                        publication
                            .tableOfContents
                            .map {
                                it.toReadiumTocItem()
                            }

                    val savedLocator =
                        ReadiumReadingPositionRepository
                            .get(
                                context =
                                    requireContext(),
                                documentUri =
                                    uri.toString()
                            )

                    val savedSettings =
                        ReaderSettingsRepository
                            .get(
                                context =
                                    requireContext(),
                                documentUri =
                                    uri.toString()
                            )

                    val preferences =
                        ReadiumEpubPreferences
                            .fromReaderSettings(
                                savedSettings
                            )

                    val navigatorFactory =
                        EpubNavigatorFactory(
                            publication =
                                publication,

                            configuration =
                                EpubNavigatorFactory
                                    .Configuration(

                                        defaults =
                                            EpubDefaults(
                                                scroll = true,
                                                pageMargins = 1.2
                                            )
                                    )
                        )

                    childFragmentManager
                        .fragmentFactory =
                        navigatorFactory
                            .createFragmentFactory(

                                initialLocator =
                                    savedLocator,

                                initialPreferences =
                                    preferences,

                                listener =
                                    this@ReadiumEpubFragment
                            )

                    if (
                        childFragmentManager
                            .findFragmentByTag(
                                NAVIGATOR_TAG
                            ) == null
                    ) {

                        childFragmentManager
                            .beginTransaction()
                            .replace(
                                navigatorContainerId,
                                EpubNavigatorFragment::class.java,
                                Bundle(),
                                NAVIGATOR_TAG
                            )
                            .commitNow()
                    }
                }
            }
    }

    /**
     * Apply new reader settings directly
     * to the active Readium navigator.
     */
    fun applyReaderSettings(
        settings: ReaderSettings
    ) {

        val navigator =
            childFragmentManager
                .findFragmentByTag(
                    NAVIGATOR_TAG
                ) as? EpubNavigatorFragment
                ?: return

        val preferences =
            ReadiumEpubPreferences
                .fromReaderSettings(
                    settings
                )

        navigator.submitPreferences(
            preferences
        )
    }

    /**
     * Returns the EPUB's real hierarchical TOC.
     */
    fun getTableOfContents():
        List<ReadiumTocItem> {

        return tocItems
    }

    /**
     * Returns the active Readium publication.
     */
    fun getPublication():
        Publication? {

        return (
            childFragmentManager
                .findFragmentByTag(
                    NAVIGATOR_TAG
                ) as? EpubNavigatorFragment
            )?.publication
    }

    /**
     * Navigate directly to a TOC link.
     */
    fun openTocItem(
        item: ReadiumTocItem
    ) {

        val navigator =
            childFragmentManager
                .findFragmentByTag(
                    NAVIGATOR_TAG
                ) as? EpubNavigatorFragment
                ?: return

        try {

            navigator.go(
                item.link
            )

        } catch (
            exception: Exception
        ) {
            // Ignore invalid TOC targets.
        }
    }

    /**
     * Navigate directly to a Readium search result.
     */
    fun openSearchResult(
        result: ReadiumEpubSearchResult
    ) {

        val navigator =
            childFragmentManager
                .findFragmentByTag(
                    NAVIGATOR_TAG
                ) as? EpubNavigatorFragment
                ?: return

        try {

            navigator.go(
                result.locator
            )

        } catch (
            exception: Exception
        ) {
            // Ignore invalid search locations.
        }
    }

    companion object {

        private const val ARG_URI =
            "readium_epub_uri"

        private const val NAVIGATOR_TAG =
            "readium_epub_navigator"

        fun newInstance(
            uri: Uri
        ): ReadiumEpubFragment {

            return ReadiumEpubFragment().apply {

                arguments =
                    Bundle().apply {

                        putString(
                            ARG_URI,
                            uri.toString()
                        )
                    }
            }
        }
    }
}