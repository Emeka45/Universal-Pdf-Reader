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
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl

class ReadiumEpubFragment :
    Fragment(),
    EpubNavigatorFragment.Listener {

    private var navigatorContainerId: Int = 0

    private var tocItems:
        List<ReadiumTocItem> =
        emptyList()

    private var activePublication:
        Publication? = null

    private var initializationError:
        String? = null

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

            initializationError =
                "No EPUB document was supplied."

            return
        }

        val uri =
            Uri.parse(
                uriString
            )

        viewLifecycleOwner
            .lifecycleScope
            .launch {

                val result =
                    ReadiumEpubRepository.open(
                        context =
                            requireContext(),
                        uri =
                            uri
                    )

                if (!isAdded) {
                    return@launch
                }

                result.fold(

                    onSuccess = { publication ->

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

                        val initialPreferences =
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

                        childFragmentManager
                            .fragmentFactory =
                            navigatorFactory
                                .createFragmentFactory(

                                    initialLocator =
                                        savedLocator,

                                    initialPreferences =
                                        initialPreferences,

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
                    },

                    onFailure = { exception ->

                        initializationError =
                            exception.message
                                ?: exception.toString()
                    }
                )
            }
    }

    private fun createPreferences(
        settings: ReaderSettings
    ): EpubPreferences {

        val theme =
            when (settings.theme) {

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

    fun getInitializationError():
        String? {

        return initializationError
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
                item.link
            )

        } catch (
            exception: Exception
        ) {

            initializationError =
                exception.message
                    ?: "Unable to open this chapter."
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
                result.locator
            )

        } catch (
            exception: Exception
        ) {

            initializationError =
                exception.message
                    ?: "Unable to open this search result."
        }
    }

    override fun onExternalLinkActivated(
        url: AbsoluteUrl
    ) {
        // External EPUB links remain inactive for now.
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
}package com.coeric.universalreader

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
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl

class ReadiumEpubFragment :
    Fragment(),
    EpubNavigatorFragment.Listener {

    private var navigatorContainerId: Int = 0

    private var tocItems:
        List<ReadiumTocItem> =
        emptyList()

    private var activePublication:
        Publication? = null

    private var initializationError:
        String? = null

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

            initializationError =
                "No EPUB document was supplied."

            return
        }

        val uri =
            Uri.parse(
                uriString
            )

        viewLifecycleOwner
            .lifecycleScope
            .launch {

                val result =
                    ReadiumEpubRepository.open(
                        context =
                            requireContext(),
                        uri =
                            uri
                    )

                if (!isAdded) {
                    return@launch
                }

                result.fold(

                    onSuccess = { publication ->

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

                        val initialPreferences =
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

                        childFragmentManager
                            .fragmentFactory =
                            navigatorFactory
                                .createFragmentFactory(

                                    initialLocator =
                                        savedLocator,

                                    initialPreferences =
                                        initialPreferences,

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
                    },

                    onFailure = { exception ->

                        initializationError =
                            exception.message
                                ?: exception.toString()
                    }
                )
            }
    }

    private fun createPreferences(
        settings: ReaderSettings
    ): EpubPreferences {

        val theme =
            when (settings.theme) {

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

    fun getInitializationError():
        String? {

        return initializationError
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
                item.link
            )

        } catch (
            exception: Exception
        ) {

            initializationError =
                exception.message
                    ?: "Unable to open this chapter."
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
                result.locator
            )

        } catch (
            exception: Exception
        ) {

            initializationError =
                exception.message
                    ?: "Unable to open this search result."
        }
    }

    override fun onExternalLinkActivated(
        url: AbsoluteUrl
    ) {
        // External EPUB links remain inactive for now.
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