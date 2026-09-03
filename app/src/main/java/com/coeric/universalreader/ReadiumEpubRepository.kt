package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

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
                    assetRetriever = assetRetriever,
                    pdfFactory = null
                )

            val publicationOpener =
                PublicationOpener(
                    publicationParser =
                        publicationParser
                )

            val url =
                AbsoluteUrl(
                    uri.toString()
                )

            val asset =
                assetRetriever
                    .retrieve(url)
                    .getOrElse {
                        return Result.failure(it)
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