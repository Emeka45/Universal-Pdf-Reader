# Universal PDF Reader — Release Signing

The store release must be signed with a permanent release keystore. Do not use a debug certificate.

## GitHub Actions secrets

The release workflow expects these four repository Actions secrets:

- `RELEASE_KEYSTORE_BASE64` — Base64-encoded `release-keystore.jks`
- `RELEASE_STORE_PASSWORD` — keystore password
- `RELEASE_KEY_ALIAS` — alias of the release key
- `RELEASE_KEY_PASSWORD` — password of the release key

GitHub encrypts repository Actions secrets and makes them available to workflows without exposing their values in the repository. See the GitHub Actions secrets documentation.

## Create the release keystore

Create the keystore on a trusted computer with Java/keytool. Use a strong password and keep the resulting `release-keystore.jks` somewhere safe.

Example:

```bash
keytool -genkeypair -v \
  -keystore release-keystore.jks \
  -alias universal-pdf-reader \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Record the keystore password, alias, and key password securely. They are required for every future update of this app.

## Encode the keystore

On Linux:

```bash
base64 -w 0 release-keystore.jks > release-keystore.base64
```

On macOS:

```bash
base64 < release-keystore.jks | tr -d '\\n' > release-keystore.base64
```

Do not commit either the keystore or the Base64 file.

## Add the secrets

In GitHub:

**Universal-Pdf-Reader → Settings → Secrets and variables → Actions → New repository secret**

Add the four names above and their corresponding values.

After the secrets are present, a push to `main` or a manual workflow run will build the signed release.

## Release verification

The workflow now:

1. Confirms all four signing secrets exist.
2. Checks that the keystore/password/alias combination is valid before building.
3. Builds the release with the release signing configuration.
4. Runs `apksigner verify --verbose --print-certs`.
5. Uploads the verified release APK as `universal-pdf-reader-release`.

Pull requests intentionally build only the debug APK so forked PRs do not require access to release secrets.

## Important

Never replace the release keystore casually. The same signing identity must be retained for future updates to the same Android application.
