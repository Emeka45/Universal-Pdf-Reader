@@
-import androidx.compose.ui.platform.LocalContext
-import androidx.compose.ui.res.painterResource
-import androidx.compose.ui.unit.dp
+import androidx.compose.ui.platform.LocalContext
+import androidx.compose.ui.res.painterResource
+import androidx.compose.ui.unit.dp
+import androidx.compose.ui.graphics.vector.ImageVector
@@
-@Composable
-private fun QuickAccessCard(
-    title: String,
-    icon: androidx.compose.material.icons.Icons,
-    modifier: Modifier = Modifier,
-    onClick: () -> Unit
-) {
-
-    Card(
-        modifier =
-            modifier
-    ) {
-
-        Column(
-            modifier =
-                Modifier
-                    .fillMaxWidth()
-                    .padding(16.dp),
-            horizontalAlignment =
-                Alignment.CenterHorizontally,
-            verticalArrangement =
-                Arrangement.Center
-        ) {
-
-            Icon(
-                imageVector =
-                    icon,
-                contentDescription =
-                    title,
-                modifier =
-                    Modifier.padding(
-                        8.dp
-                    )
-            )
-
-            Spacer(
-                modifier =
-                    Modifier.height(8.dp)
-            )
-
-            Text(
-                text = title,
-                style =
-                    MaterialTheme
-                        .typography
-                        .titleSmall
-            )
-        }
-    }
-}
+@Composable
+private fun QuickAccessCard(
+    title: String,
+    icon: ImageVector,
+    modifier: Modifier = Modifier,
+    onClick: () -> Unit
+) {
+
+    Card(
+        modifier = modifier
+    ) {
+
+        Column(
+            modifier = Modifier
+                .fillMaxWidth()
+                .padding(16.dp),
+            horizontalAlignment = Alignment.CenterHorizontally,
+            verticalArrangement = Arrangement.Center
+        ) {
+
+            Icon(
+                imageVector = icon,
+                contentDescription = title,
+                modifier = Modifier.padding(8.dp)
+            )
+
+            Spacer(modifier = Modifier.height(8.dp))
+
+            Text(
+                text = title,
+                style = MaterialTheme.typography.titleSmall
+            )
+        }
+    }
+}
