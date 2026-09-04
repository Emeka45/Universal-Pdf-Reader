@@
-                    isBookmarked =
-                        bookmarks.any { bookmark ->
-                            bookmark.locator.href == locator.href
-                        }
+                    isBookmarked =
+                        bookmarks.any { bookmark ->
+                            (bookmark.locator.href as? String)
+                                ?: bookmark.locator.href?.toString()
+                                ?: "" == (locator.href as? String)
+                                ?: locator.href?.toString()
+                                ?: ""
+                        }
