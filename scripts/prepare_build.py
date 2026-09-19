from pathlib import Path
import base64
import os
import re
import binascii

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

    helper = '''    private fun displayNameForUri(uri: Uri): String {
        val fallback = uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() && !it.matches(Regex("\\\\d+")) }
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
    }'''

    if 'private fun displayNameForUri(uri: Uri)' not in text:
        marker = '    private fun openLibraryEntry(uriString: String) = openPdf(Uri.parse(uriString), getSavedPage(uriString))'
        text = text.replace(marker, helper + '\n\n' + marker, 1)

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

    text = re.sub(r'versionCode\s*=\s*\d+', 'versionCode = 2', text)
    text = re.sub(r'versionName\s*=\s*"[^"]+"', 'versionName = "0.2.0"', text)
    GRADLE.write_text(text, encoding='utf-8')


def prepare_keystore() -> bool:
    encoded = os.environ.get('RELEASE_KEYSTORE_BASE64', '').strip()
    password = os.environ.get('RELEASE_STORE_PASSWORD', '').strip()
    alias = os.environ.get('RELEASE_KEY_ALIAS', '').strip()
    key_password = os.environ.get('RELEASE_KEY_PASSWORD', '').strip()

    if not encoded:
        return False
    if not password or not alias or not key_password:
        raise SystemExit('Release signing is incomplete: all four RELEASE_* secrets are required.')

    try:
        data = base64.b64decode(encoded, validate=True)
    except (binascii.Error, ValueError) as exc:
        raise SystemExit(f'RELEASE_KEYSTORE_BASE64 is not valid base64: {exc}') from exc

    key_path = APP / 'release-keystore.jks'
    key_path.write_bytes(data)
    props = ROOT / 'release-signing.properties'
    props.write_text(
        'storeFile=app/release-keystore.jks\n'
        f'storePassword={password}\n'
        f'keyAlias={alias}\n'
        f'keyPassword={key_password}\n',
        encoding='utf-8'
    )
    return True


patch_activity()
patch_gradle()
print('PDF filename preservation patch applied.')
if prepare_keystore():
    print('Release signing configuration prepared.')
else:
    print('No release keystore supplied; release signing will be blocked by the workflow.')
