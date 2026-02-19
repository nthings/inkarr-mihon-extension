# Inkarr Extension Repository Setup

This document explains how to configure the automated build and release system for the Inkarr Mihon extension.

## Repository Structure

```
inkarr-mihon-extension/
├── .github/
│   └── workflows/
│       └── release-inkarr.yml    # Automated build & publish workflow
├── scripts/
│   └── create-repo.py            # Repository index generator
├── src/all/inkarr/               # Extension source code
│   ├── build.gradle
│   ├── README.md
│   └── src/
└── repo/                         # Generated repository files
    ├── apk/                      # Built APK files
    ├── icon/                     # Extension icons
    ├── index.json                # Full index
    ├── index.min.json            # Minified index (used by Mihon)
    └── repo.json                 # Repository metadata
```

## GitHub Actions Setup

### Required Secrets

Configure these secrets in your repository settings (Settings → Secrets and variables → Actions):

| Secret | Description | Required |
|--------|-------------|----------|
| `SIGNING_KEY` | Base64-encoded keystore file | Yes (for signed releases) |
| `ALIAS` | Keystore alias | Yes (for signed releases) |
| `KEY_STORE_PASSWORD` | Keystore password | Yes (for signed releases) |
| `KEY_PASSWORD` | Key password | Yes (for signed releases) |

### Creating a Signing Key

1. **Generate a new keystore:**
   ```bash
   keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias inkarr
   ```

2. **Encode it to base64:**
   ```bash
   base64 -w 0 release-key.jks > release-key.txt
   ```

3. **Add to GitHub Secrets:**
   - Copy the contents of `release-key.txt` to `SIGNING_KEY`
   - Set `ALIAS` to `inkarr`
   - Set `KEY_STORE_PASSWORD` and `KEY_PASSWORD` to your passwords

### Workflow Triggers

The workflow runs automatically when:
- Push to `main` branch (changes to `src/all/inkarr/**`)
- A new Release is published
- Manual trigger via workflow_dispatch

## Repository Index Format

The `index.json` file follows Mihon's extension repository schema:

```json
[
  {
    "name": "Inkarr",
    "pkg": "eu.kanade.tachiyomi.extension.all.inkarr",
    "apk": "tachiyomi-inkarr-v1.4.1.apk",
    "lang": "all",
    "code": 1,
    "version": "1.4.1",
    "nsfw": 0,
    "hasIcon": 1,
    "sources": [{
      "name": "Inkarr",
      "lang": "all",
      "id": 1234567890,
      "baseUrl": ""
    }]
  }
]
```

### Field Reference

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Display name in Mihon |
| `pkg` | string | Android package name |
| `apk` | string | APK filename in repo |
| `lang` | string | Language code ("all" for multi-language) |
| `code` | int | Version code (incremental) |
| `version` | string | Human-readable version |
| `nsfw` | int | 0 = SFW, 1 = NSFW |
| `hasIcon` | int | 1 if icon exists |
| `sources` | array | Source definitions |

## Manual Repository Generation

If you need to generate the repository manually:

```bash
# Install dependencies (Python 3.8+)
cd inkarr-mihon-extension

# Build the APK
./gradlew :src:all:inkarr:assembleRelease

# Generate repository index
python scripts/create-repo.py \
  --apk-dir src/all/inkarr/build/outputs/apk/release \
  --output-dir repo \
  --repo-url https://raw.githubusercontent.com/YOUR_USER/inkarr-mihon-extension/repo
```

## Adding Repository to Mihon

Users add your repository by:

1. Opening Mihon
2. Going to **Settings** → **Browse** → **Extension repos**
3. Tapping **Add**
4. Entering the repository URL:
   ```
   https://raw.githubusercontent.com/YOUR_USER/inkarr-mihon-extension/repo/index.min.json
   ```

## Hosting Options

### GitHub Pages (Recommended)

Enable GitHub Pages on the `repo` branch:
1. Go to repository **Settings** → **Pages**
2. Set Source to "Deploy from a branch"
3. Select `repo` branch, `/ (root)` folder
4. Repository URL becomes: `https://YOUR_USER.github.io/inkarr-mihon-extension/index.min.json`

### Raw GitHub Content

Use the raw content URL (no setup required):
```
https://raw.githubusercontent.com/YOUR_USER/inkarr-mihon-extension/repo/index.min.json
```

### Self-Hosted

Copy the `repo/` folder to your web server and configure CORS headers:
```nginx
location /extensions/ {
    add_header Access-Control-Allow-Origin *;
    add_header Cache-Control "public, max-age=3600";
}
```

## Version Updates

To release a new version:

1. **Update version code** in `src/all/inkarr/build.gradle`:
   ```groovy
   ext {
       extVersionCode = 2  // Increment this
   }
   ```

2. **Commit and push** to `main` branch

3. **Create a release** (optional, for asset download):
   - Go to GitHub → Releases → Draft new release
   - Tag: `v1.4.2`
   - The workflow will attach the APK automatically

## Troubleshooting

### "Extension not showing in Mihon"
- Verify the repository URL is correct
- Check that `index.min.json` exists and is valid JSON
- Try removing and re-adding the repository

### "Trust button doesn't appear"
- Make sure the extension is installed first
- Check Mihon version (requires v0.5.0+)

### "Build fails in GitHub Actions"
- Verify all secrets are configured
- Check workflow logs for specific errors
- Ensure signing key is valid base64
