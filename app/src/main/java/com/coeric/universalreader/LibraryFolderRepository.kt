package com.coeric.universalreader

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object LibraryFolderRepository {

    private const val PREFS =
        "universal_reader_folders"

    private const val FOLDERS =
        "folders"

    private const val BOOK_FOLDERS =
        "book_folders"

    fun getFolders(
        context: Context
    ): List<LibraryFolder> {

        val json =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            ).getString(
                FOLDERS,
                null
            )
                ?: return emptyList()

        return try {

            val array =
                JSONArray(json)

            val result =
                mutableListOf<LibraryFolder>()

            for (
                index in 0 until array.length()
            ) {

                val item =
                    array.getJSONObject(index)

                result.add(
                    LibraryFolder(
                        id =
                            item.optString(
                                "id"
                            ),
                        name =
                            item.optString(
                                "name",
                                "Folder"
                            ),
                        createdAt =
                            item.optLong(
                                "createdAt",
                                0L
                            )
                    )
                )
            }

            result

        } catch (
            exception: Exception
        ) {

            emptyList()
        }
    }

    fun createFolder(
        context: Context,
        name: String
    ): LibraryFolder? {

        val cleanName =
            name.trim()

        if (cleanName.isBlank()) {
            return null
        }

        val folders =
            getFolders(context)
                .toMutableList()

        if (
            folders.any {
                it.name.equals(
                    cleanName,
                    ignoreCase = true
                )
            }
        ) {
            return null
        }

        val folder =
            LibraryFolder(
                id =
                    UUID.randomUUID()
                        .toString(),
                name =
                    cleanName
            )

        folders.add(folder)

        saveFolders(
            context,
            folders
        )

        return folder
    }

    fun renameFolder(
        context: Context,
        folderId: String,
        newName: String
    ) {

        val cleanName =
            newName.trim()

        if (cleanName.isBlank()) {
            return
        }

        val folders =
            getFolders(context)
                .map { folder ->

                    if (
                        folder.id ==
                        folderId
                    ) {

                        folder.copy(
                            name =
                                cleanName
                        )

                    } else {
                        folder
                    }
                }

        saveFolders(
            context,
            folders
        )
    }

    fun deleteFolder(
        context: Context,
        folderId: String
    ) {

        val folders =
            getFolders(context)
                .filter {
                    it.id != folderId
                }

        saveFolders(
            context,
            folders
        )

        val assignments =
            getBookFolders(context)
                .filterValues {
                    it != folderId
                }

        saveBookFolders(
            context,
            assignments
        )
    }

    fun setBookFolder(
        context: Context,
        bookUri: String,
        folderId: String?
    ) {

        val assignments =
            getBookFolders(context)
                .toMutableMap()

        if (folderId == null) {

            assignments.remove(
                bookUri
            )

        } else {

            assignments[bookUri] =
                folderId
        }

        saveBookFolders(
            context,
            assignments
        )
    }

    fun getBookFolder(
        context: Context,
        bookUri: String
    ): LibraryFolder? {

        val folderId =
            getBookFolders(context)[bookUri]
                ?: return null

        return getFolders(context)
            .firstOrNull {
                it.id == folderId
            }
    }

    fun getBooksInFolder(
        context: Context,
        folderId: String
    ): List<LibraryBook> {

        val bookUris =
            getBookFolders(context)
                .filterValues {
                    it == folderId
                }
                .keys

        return LibraryRepository
            .getBooks(context)
            .filter {
                it.uri in bookUris
            }
    }

    private fun saveFolders(
        context: Context,
        folders: List<LibraryFolder>
    ) {

        val array =
            JSONArray()

        for (
            folder in folders
        ) {

            val item =
                JSONObject()

            item.put(
                "id",
                folder.id
            )

            item.put(
                "name",
                folder.name
            )

            item.put(
                "createdAt",
                folder.createdAt
            )

            array.put(
                item
            )
        }

        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                FOLDERS,
                array.toString()
            )
            .apply()
    }

    private fun getBookFolders(
        context: Context
    ): Map<String, String> {

        val json =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            ).getString(
                BOOK_FOLDERS,
                null
            )
                ?: return emptyMap()

        return try {

            val objectData =
                JSONObject(json)

            val result =
                mutableMapOf<String, String>()

            val keys =
                objectData.keys()

            while (keys.hasNext()) {

                val key =
                    keys.next()

                result[key] =
                    objectData.optString(
                        key
                    )
            }

            result

        } catch (
            exception: Exception
        ) {

            emptyMap()
        }
    }

    private fun saveBookFolders(
        context: Context,
        assignments: Map<String, String>
    ) {

        val objectData =
            JSONObject()

        assignments.forEach {
            (uri, folderId) ->

            objectData.put(
                uri,
                folderId
            )
        }

        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                BOOK_FOLDERS,
                objectData.toString()
            )
            .apply()
    }
}