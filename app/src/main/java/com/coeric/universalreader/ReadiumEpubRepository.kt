package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import org.readium.r2.shared.util.toAbsoluteUrl
import org.readium.r2.shared.publication.Publication

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
                    context.contentResolver,
                    httpClient
                )

            val url =
                uri.toAbsoluteUrl()
                    ?: return Result.failure(
                        IllegalArgumentException(
                            "Unable to convert EPUB URI to a Readium URL."
                        )
                    )

            val assetResult =
                assetRetriever.retrieve(url)

            val asset =
                assetResult.getOrElse {
                    return Result.failure(
                        IllegalStateException(
                            it.toString()
                        )
                    )
                }

            val parser =
                DefaultPublicationParser(
                    context = context,
                    httpClient = httpClient,
                    assetRetriever = assetRetriever,
                    pdfFactory = null
                )

            val opener =
                PublicationOpener(
                    publicationParser = parser
                )

            val publicationResult =
                opener.open(
                    asset,
                    allowUserInteraction = true
                )

            publicationResult
                .fold(
                    onSuccess = {
                        Result.success(it)
                    },
                    onFailure = {
                        Result.failure(
                            IllegalStateException(
                                it.toString()
                            )
                        )
                    }
                )

        } catch (
            exception: Exception
        ) {

            Result.failure(
                exception
            )
        }
    }
}