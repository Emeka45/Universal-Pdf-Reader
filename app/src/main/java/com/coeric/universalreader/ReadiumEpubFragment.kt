package com.coeric.universalreader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.readium.r2.navigator.epub.EpubDefaults
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.publication.Publication

class ReadiumEpubFragment(
    private val publication: Publication
) : Fragment(),
    EpubNavigatorFragment.Listener {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        childFragmentManager.fragmentFactory =
            EpubNavigatorFactory(
                publication = publication,
                configuration =
                    EpubNavigatorFactory.Configuration(
                        defaults =
                            EpubDefaults(
                                scroll = true
                            )
                    )
            ).createFragmentFactory(
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

        val view =
            inflater.inflate(
                android.R.layout.simple_list_item_1,
                container,
                false
            )

        if (savedInstanceState == null) {

            childFragmentManager
                .beginTransaction()
                .add(
                    android.R.id.text1,
                    EpubNavigatorFragment::class.java,
                    Bundle(),
                    "readium_epub_navigator"
                )
                .commitNow()
        }

        return view
    }
}