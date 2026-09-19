# Universal PDF Reader — Release Signing

The store release must be signed with a persistent release keystore. Do not commit the keystore or signing properties to Git.

## GitHub Actions secrets

Configure these four repository Actions secrets:

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

The workflow decodes the keystore only on the GitHub Actions runner, signs the release APK, verifies the APK with `apksigner`, uploads the release artifact, and removes the temporary signing material.

## Important

Keep a secure backup of the keystore and its passwords. The same signing identity must be retained for future updates to the app.

Never put the keystore, passwords, or base64-encoded keystore into source files, issues, commits, or chat messages.
