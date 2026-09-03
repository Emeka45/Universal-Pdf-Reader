package com.coeric.universalreader

import android.content.Context
import org.json.JSONObject
import org.readium.r2.shared.publication.Locator

object ReadiumReadingPositionRepository {

    private const val PREFS =
        "readium_reading_positions"

    private fun key(
        documentUri: String
    ): String {
        return "locator_${documentUri.hashCode()}"
    }

    fun save(
        context: Context,
        documentUri: String,
        locator: Locator
    ) {

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                key(documentUri),
                locator.toJSON().toString()
            )
            .apply()
    }

    fun get(
        context: Context,
        documentUri: String
    ): Locator? {

        val jsonString =
            context
                .getSharedPreferences(
                    PREFS,
                    Context.MODE_PRIVATE
                )
                .getString(
                    key(documentUri),
                    null
                )
                ?: return null

        return try {

            Locator.fromJSON(
                JSONObject(jsonString)
            )

        } catch (
            exception: Exception
        ) {

            null
        }
    }

    fun remove(
        context: Context,
        documentUri: String
    ) {

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(
                key(documentUri)
            )
            .apply()
    }
}