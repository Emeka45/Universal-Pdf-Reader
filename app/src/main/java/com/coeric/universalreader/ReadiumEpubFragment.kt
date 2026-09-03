package com.coeric.universalreader

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubDefaults
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.TextAlign
import org.readium.r2.navigator.epub.Theme

class ReadiumEpubFragment :
    Fragment(),
    EpubNavigatorFragment.Listener {

    private var navigatorContainerId: Int = 0

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
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
            observeNavigator()
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

        viewLifecycleOwner.lifecycleScope.launch {

            val result =
                ReadiumEpubRepository.open(
                    context = requireContext(),
                    uri = uri
                )

            result.onSuccess { publication ->

                if (!isAdded) {
                    return@onSuccess
                }

                val savedLocator =
                    ReadiumReadingPositionRepository.get(
                        context = requireContext(),
                        documentUri = uri.toString()
                    )

                val savedSettings =
                    ReaderSettingsRepository.get(
                        context = requireContext(),
                        documentUri = uri.toString()
                    )

                val preferences =
                    EpubPreferences(

                        fontSize =
                            savedSettings
                                .fontSize
                                .toDouble(),

                        lineHeight =
                            savedSettings
                                .lineSpacing
                                .toDouble(),

                        pageMargins = 1.2,

                        scroll = true,

                        publisherStyles = true,

                        theme =
                            when (
                                savedSettings.theme
                            ) {

                                ReaderTheme.LIGHT ->
                                    Theme.LIGHT

                                ReaderTheme.DARK ->
                                    Theme.DARK

                                ReaderTheme.SEPIA ->
                                    Theme.SEPIA
                            },

                        textAlign =
                            when (
                                savedSettings.textAlignment
                            ) {

                                ReaderTextAlignment.LEFT ->
                                    TextAlign.LEFT

                                ReaderTextAlignment.JUSTIFY ->
                                    TextAlign.JUSTIFY
                            }
                    )

                val navigatorFactory =
                    EpubNavigatorFactory(
                        publication = publication,

                        configuration =
                            EpubNavigatorFactory.Configuration(

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

                observeNavigator()
            }
        }
    }

    private fun observeNavigator() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner
                .repeatOnLifecycle(
                    Lifecycle.State.STARTED
                ) {

                    val navigator =
                        childFragmentManager
                            .findFragmentByTag(
                                NAVIGATOR_TAG
                            )
                            as? EpubNavigatorFragment
                            ?: return@repeatOnLifecycle

                    launch {

                        navigator
                            .currentLocator
                            .collect { locator ->

                                val uriString =
                                    arguments
                                        ?.getString(
                                            ARG_URI
                                        )
                                        ?: return@collect

                                ReadiumReadingPositionRepository
                                    .save(
                                        context =
                                            requireContext(),
                                        documentUri =
                                            uriString,
                                        locator =
                                            locator
                                    )
                            }
                    }
                }
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