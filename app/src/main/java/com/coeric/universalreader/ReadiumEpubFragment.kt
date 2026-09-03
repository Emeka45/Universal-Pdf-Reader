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

class ReadiumEpubFragment : Fragment(),
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
                    "readium_epub_navigator"
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

        lifecycleScope.launch {

            val result =
                ReadiumEpubRepository.open(
                    context = requireContext(),
                    uri = uri
                )

            result
                .onSuccess { publication ->

                    if (!isAdded) {
                        return@onSuccess
                    }

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
                                initialLocator = null,
                                listener = this@ReadiumEpubFragment
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
    }

    companion object {

        private const val ARG_URI =
            "readium_epub_uri"

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