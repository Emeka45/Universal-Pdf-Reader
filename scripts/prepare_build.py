from pathlib import Path
import base64
import os
import re

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / 'app'
ACTIVITY = APP / 'src/main/java/com/coeric/universalpdfreader/MainActivity.kt'
GRADLE = APP / 'build.gradle.kts'


def patch_activity() -> None:
    text = ACTIVITY.read_text(encoding='utf-8')

    if 'android.provider.OpenableColumns' not in text:
        text = text.replace(
            'import android.os.ParcelFileDescriptor\n',
            'import android.os.ParcelFileDescriptor\nimport android.provider.OpenableColumns\n'
        )

    helper = r'''
    private fun displayNameForUri(uri: Uri): String {
        val fallback = uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() && !it.matches(Regex("\\d+")) }
            ?: "Untitled PDF"
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) {
                    cursor.getString(index)?.trim()?.takeIf { it.isNotBlank() } ?: fallback
                } else fallback
            } ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }
'''.strip('\n')

    if 'private fun displayNameForUri(uri: Uri)' not in text:
        marker = '    private fun openLibraryEntry(uriString: String) = openPdf(Uri.parse(uriString), getSavedPage(uriString))'
        text = text.replace(marker, helper + '\n\n' + marker)

    text = text.replace(
        'val name = uri.lastPathSegment?.substringAfterLast(\'/\') ?: "Untitled PDF"',
        'val name = displayNameForUri(uri)'
    )
    text = text.replace(
        'titleLabel.text = uri.lastPathSegment?.substringAfterLast(\'/\') ?: "Universal PDF Reader"',
        'titleLabel.text = displayNameForUri(uri)'
    )
    text = text.replace(
        'private fun openLibraryEntry(uriString: String) = openPdf(Uri.parse(uriString), getSavedPage(uriString))',
        'private fun openLibraryEntry(uriString: String) {\n        val uri = Uri.parse(uriString)\n        rememberInLibrary(uri, displayNameForUri(uri))\n        openPdf(uri, getSavedPage(uriString))\n    }'
    )

    ACTIVITY.write_text(text, encoding='utf-8')


def patch_gradle() -> None:
    text = GRADLE.read_text(encoding='utf-8')

    if 'signingConfigs {' not in text:
        marker = '    defaultConfig {'
        signing = '''    signingConfigs {\n        create("release") {\n            val propsFile = rootProject.file("release-signing.properties")\n            if (propsFile.exists()) {\n                val props = java.util.Properties().apply { propsFile.inputStream().use { load(it) } }\n                storeFile = rootProject.file(props.getProperty("storeFile"))\n                storePassword = props.getProperty("storePassword")\n                keyAlias = props.getProperty("keyAlias")\n                keyPassword = props.getProperty("keyPassword")\n            }\n        }\n    }\n'''
        text = text.replace(marker, signing + marker, 1)

    if 'signingConfig = signingConfigs.getByName("release")' not in text:
        marker = '    compileOptions {'
        release = '''    buildTypes {\n        getByName("release") {\n            isMinifyEnabled = false\n            isDebuggable = false\n            val propsFile = rootProject.file("release-signing.properties")\n            if (propsFile.exists()) {\n                signingConfig = signingConfigs.getByName("release")\n            }\n        }\n    }\n'''
        text = text.replace(marker, release + marker, 1)

    # Make the next store submission an actual version increment.
    text = re.sub(r'versionCode\\s*=\\s*\\d+', 'versionCode = 2', text)
    text = re.sub(r'versionName\\s*=\\s*"[^"]+"', 'versionName = "0.2.0"', text)
    GRADLE.write_text(text, encoding='utf-8')


def prepare_keystore() -> bool:
    encoded = os.environ.get('RELEASE_KEYSTORE_BASE64', '').strip()
    if not encoded:
        return False
    data = base64.b64decode(encoded)
    key_path = APP / 'release-keystore.jks'
    key_path.write_bytes(data)
    props = ROOT / 'release-signing.properties'
    props.write_text(
        'storeFile=app/release-keystore.jks\n'
        f'storePassword={os.environ.get("RELEASE_STORE_PASSWORD", "")}\n'
        f'keyAlias={os.environ.get("RELEASE_KEY_ALIAS", "") }\n'
        f'keyPassword={os.environ.get("RELEASE_KEY_PASSWORD", "")}\n',
        encoding='utf-8'
    )
    return True


patch_activity()
patch_gradle()
print('PDF filename preservation patch applied.')
if prepare_keystore():
    print('Release signing keystore loaded from GitHub Actions secret.')
else:
    print('No release keystore secret supplied; debug build can run, store release will be skipped.')
