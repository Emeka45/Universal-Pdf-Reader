package com.coeric.universalreader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

object EpubImageResolver {

    fun findImage(
        document: EpubDocument,
        path: String
    ): EpubImage? {

        val normalized =
            normalize(path)

        return document.images.firstOrNull {
            normalize(it.path) == normalized ||
                normalize(
                    it.path.substringAfterLast("/")
                ) == normalized.substringAfterLast("/")
        }
    }

    fun decode(
        image: EpubImage
    ): Bitmap? {

        return try {

            BitmapFactory.decodeByteArray(
                image.data,
                0,
                image.data.size
            )

        } catch (
            exception: Exception
        ) {
            null
        }
    }

    private fun normalize(
        path: String
    ): String {

        return path
            .replace('\\', '/')
            .removePrefix("./")
            .trim()
    }
}

@Composable
fun rememberEpubBitmap(
    image: EpubImage?
): Bitmap? {

    return remember(
        image?.path
    ) {

        image?.let {
            EpubImageResolver.decode(it)
        }
    }
}