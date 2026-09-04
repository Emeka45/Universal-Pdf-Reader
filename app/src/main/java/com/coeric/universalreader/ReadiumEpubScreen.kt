@@
     IconButton(
         enabled =
             publicationReady &&
                 currentLocator != null,
 
         onClick = {
 
             val locator =
                 currentLocator
                     ?: return@IconButton
 
             if (isBookmarked) {
 
                 ReadiumBookmarkRepository
                     .remove(
                         context =
                             activity,
 
-                        bookmarkId =
-                            locator.href ?: UUID.randomUUID().toString()
+                        // Ensure href is used as a String
+                        bookmarkId =
+                            (locator.href as? String)
+                                ?: locator.href?.toString()
+                                ?: UUID.randomUUID().toString()
                     )
 
                 isBookmarked =
                     false
 
             } else {
@@
-                    ReadiumBookmarkRepository
-                        .add(
-                            context =
-                                activity,
-
-                            bookmark =
-                                ReadiumBookmark(
-                                    id = UUID.randomUUID().toString(),
-                                    documentUri = uri.toString(),
-                                    locator = locator,
-                                    title = "Bookmark"
-                                )
-                        )
+                    ReadiumBookmarkRepository
+                        .add(
+                            context = activity,
+                            bookmark = ReadiumBookmark(
+                                id = UUID.randomUUID().toString(),
+                                documentUri = uri.toString(),
+                                locator = locator,
+                                title = "Bookmark"
+                            )
+                        )
