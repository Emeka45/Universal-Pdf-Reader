package com.coeric.universalreader

import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl

class ReadiumEpubFragment :
    Fragment(),
    EpubNavigatorFragment.Listener {

    private var navigatorContainerId: Int = 0

    private var activePublication: Publication? = null

    private var tocItems: List<ReadiumTocItem> =
        emptyList()

    private var openError: String? = null

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

            id =
                navigatorContainerId

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

        if (
            uriString.isNullOrBlank()
        ) {

            openError =
                "No EPUB file was provided."

            showError()

            return
        }

        val uri =
            Uri.parse(
                uriString
            )

        openPublication(
            uri
        )
    }

    private fun openPublication(
        uri: Uri
    ) {

        viewLifecycleOwner
            .lifecycleScope
            .launch {

                try {

                    Log.d(
                        TAG,
                        "Opening EPUB: $uri"
                    )

                    val result =
                        ReadiumEpubRepository.open(
                            context =
                                requireContext(),
                            uri =
                                uri
                        )

                    result.fold(

                        onSuccess = {
                            publication ->

                            if (
                                !isAdded ||
                                view == null
                            ) {
                                return@fold
                            }

                            Log.d(
                                TAG,
                                "EPUB opened successfully."
                            )

                            activePublication =
                                publication

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
                                createPreferences(
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

                            val fragmentFactory =
                                navigatorFactory
                                    .createFragmentFactory(

                                        initialLocator =
                                            savedLocator,

                                        initialPreferences =
                                            preferences,

                                        listener =
                                            this@ReadiumEpubFragment
                                    )

                            /*
                             * IMPORTANT:
                             *
                             * The factory is installed BEFORE
                             * EpubNavigatorFragment is created.
                             */
                            childFragmentManager
                                .fragmentFactory =
                                fragmentFactory

                            val existingNavigator =
                                childFragmentManager
                                    .findFragmentByTag(
                                        NAVIGATOR_TAG
                                    )

                            if (
                                existingNavigator == null &&
                                !childFragmentManager
                                    .isStateSaved
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

                        },

                        onFailure = {
                            exception ->

                            openError =
                                exception.message
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?: exception.toString()

                            Log.e(
                                TAG,
                                "EPUB opening failed.",
                                exception
                            )

                            showError()
                        }
                    )

                } catch (
                    exception: Exception
                ) {

                    openError =
                        exception.message
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: exception.toString()

                    Log.e(
                        TAG,
                        "Unexpected EPUB error.",
                        exception
                    )

                    showError()
                }
            }
    }

    private fun showError() {

        val container =
            view as? ViewGroup
                ?: return

        container.removeAllViews()

        val errorView =
            android.widget.TextView(
                requireContext()
            ).apply {

                text =
                    buildString {

                        append(
                            "Unable to open EPUB"
                        )

                        append(
                            "\n\n"
                        )

                        append(
                            openError
                                ?: "Unknown EPUB error."
                        )
                    }

                textSize =
                    16f

                setPadding(
                    40,
                    40,
                    40,
                    40
                )

                gravity =
                    android.view.Gravity.CENTER

                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
            }

        container.addView(
            errorView
        )
    }

    private fun createPreferences(
        settings: ReaderSettings
    ): EpubPreferences {

        val theme =
            when (
                settings.theme
            ) {

                ReaderTheme.LIGHT ->
                    Theme.LIGHT

                ReaderTheme.DARK ->
                    Theme.DARK

                ReaderTheme.SEPIA ->
                    Theme.LIGHT
            }

        val textAlign =
            when (
                settings.textAlignment
            ) {

                ReaderTextAlignment.LEFT ->
                    TextAlign.START

                ReaderTextAlignment.JUSTIFY ->
                    TextAlign.JUSTIFY
            }

        return EpubPreferences(

            fontSize =
                settings.fontSize
                    .toDouble(),

            lineHeight =
                settings.lineSpacing
                    .toDouble(),

            pageMargins =
                1.2,

            scroll =
                true,

            textAlign =
                textAlign,

            theme =
                theme,

            publisherStyles =
                false
        )
    }

    fun applyReaderSettings(
        settings: ReaderSettings
    ) {

        val navigator =
            childFragmentManager
                .findFragmentByTag(
                    NAVIGATOR_TAG
                ) as? EpubNavigatorFragment
                ?: return

        navigator.submitPreferences(
            createPreferences(
                settings
            )
        )
    }

    fun getTableOfContents():
        List<ReadiumTocItem> {

        return tocItems
    }

    fun getPublication():
        Publication? {

        return activePublication
    }

    fun getOpenError():
        String? {

        return openError
    }

    fun getCurrentLocator():
        Locator? {

        val navigator =
            childFragmentManager
                .findFragmentByTag(
                    NAVIGATOR_TAG
                ) as? EpubNavigatorFragment

        return navigator
            ?.currentLocator
            ?.value
    }

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
                item.link,
                animated = true
            )

        } catch (
            exception: Exception
        ) {

            Log.e(
                TAG,
                "Failed to open TOC item.",
                exception
            )
        }
    }

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
                result.locator,
                animated = true
            )

        } catch (
            exception: Exception
        ) {

            Log.e(
                TAG,
                "Failed to open EPUB search result.",
                exception
            )
        }
    }

    override fun onExternalLinkActivated(
        url: AbsoluteUrl
    ) {

        Log.d(
            TAG,
            "External EPUB link blocked: $url"
        )
    }

    companion object {

        private const val TAG =
            "ReadiumEpubFragment"

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
