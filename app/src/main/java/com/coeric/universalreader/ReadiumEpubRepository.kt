package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import com.github.readium.kotlin.toolkit.extensions.toUrl
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import org.readium.r2.streamer.parser.PublicationParser
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.asset.FileAsset

object ReadiumEpubRepository {

    suspend fun open(
        context: Context,
        uri: Uri
    ): Result<Publication> {

        return try {

            val httpClient =
                DefaultHttpClient()

            val assetRetriever =
                AssetRetriever(
                    contentResolver =
                        context.contentResolver,
                    httpClient =
                        httpClient
                )

            val publicationParser =
                DefaultPublicationParser(
                    context = context,
                    httpClient = httpClient,
                    assetRetriever = assetRetriever
                )

            val publicationOpener =
                PublicationOpener(
                    publicationParser =
                        publicationParser
                )

            val url =
                uri.toString()

            val asset =
                assetRetriever
                    .retrieve(
                        url
                    )
                    .getOrElse {
                        return Result.failure(
                            it
                        )
                    }

            publicationOpener
                .open(
                    asset,
                    allowUserInteraction = true
                )
                .map {
                    it
                }

        } catch (
            exception: Exception
        ) {

            Result.failure(
                exception
            )
        }
    }
}