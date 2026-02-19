# Inkarr Extension for Mihon

[![Build & Release](https://github.com/your-username/inkarr-mihon-extension/actions/workflows/release-inkarr.yml/badge.svg)](https://github.com/your-username/inkarr-mihon-extension/actions/workflows/release-inkarr.yml)

A Mihon/Tachiyomi extension that connects to your self-hosted [Inkarr](https://github.com/your-username/inkarr) server, allowing you to browse and read your manga/comic collection directly from the Mihon app.

## Features

- 📚 Browse your Inkarr library directly from Mihon
- 📖 Read CBZ/CBR files streamed from your server
- 🏷️ Support for series metadata (author, artist, genres, status)
- 🔐 API key authentication for secure access
- 🌐 Works on local network or remotely (port forwarding/VPN)

## Requirements

- A running Inkarr server (v1.0+)
- Mihon app (v0.5.0+) or compatible fork (Tachiyomi, TachiJ2K, etc.)
- Network connectivity to your Inkarr server

---

## 📲 Installation

### Option 1: Add Extension Repository (Recommended)

This is the easiest method and enables automatic updates.

1. **Open Mihon** on your Android device
2. Navigate to: **Settings** → **Browse** → **Extension repos**
3. Tap **➕ Add** and enter:
   ```
   https://raw.githubusercontent.com/your-username/inkarr-mihon-extension/repo/index.min.json
   ```
4. Go to **Browse** → **Extensions** tab
5. Search for **"Inkarr"** and tap **Install**
6. When prompted, tap **Trust** to enable the extension

> **⚠️ Security Note:** Mihon requires trusting third-party extensions as a security measure. This allows the extension to make network requests to your Inkarr server.

### Option 2: Direct APK Installation

1. Download the latest APK from [Releases](https://github.com/your-username/inkarr-mihon-extension/releases)
2. Install the APK on your device (enable "Install from unknown sources" if needed)
3. Open Mihon → **Browse** → **Extensions** → Trust the extension

---

## ⚙️ Configuration

After installation, configure the extension to connect to your server:

1. Go to **Browse** → **Extensions**
2. Long press on **"Inkarr"** and tap **Settings**
3. Configure:

| Setting | Description | Example |
|---------|-------------|---------|
| **Server Address** | Your Inkarr server URL | `http://192.168.1.100:3000` |
| **API Key** | Authentication key (optional) | `abc123...` |

### Important Notes

- ❌ Server address must **NOT** end with `/`
- ✅ Use `http://` for local networks, `https://` for remote access
- 🔄 Restart Mihon after changing settings

---

## 🏗️ Building from Source

### Prerequisites

- JDK 17+
- Android SDK with build-tools
- Git

### Clone the Repository

```bash
# Clone with sparse checkout for efficiency
git clone --filter=blob:none --sparse https://github.com/your-repo/inkarr-mihon-extension.git
cd inkarr-mihon-extension

# Enable sparse checkout
git sparse-checkout set buildSrc core gradle lib src/all/inkarr
```

### Build the APK

```bash
# Build debug APK
./gradlew :src:all:inkarr:assembleDebug

# Build release APK (requires signing configuration)
./gradlew :src:all:inkarr:assembleRelease
```

The APK will be generated at:
```
src/all/inkarr/build/outputs/apk/debug/tachiyomi-all.inkarr-v1.4.1.apk
```

## Inkarr API Requirements

For this extension to work, your Inkarr server must expose the following endpoints:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/series` | GET | List all series |
| `/api/v1/series/{id}` | GET | Get series details |
| `/api/v1/chapter/{id}` | GET | Get chapter details |
| `/api/v1/mediafile/{id}` | GET | Get media file details |
| `/api/v1/mediafile/{id}/page/{num}` | GET | Get page image from CBZ/CBR |

### Authentication

If authentication is enabled on your Inkarr server, include your API key in the extension settings. The extension will send it as an `X-API-Key` header with every request.

## Troubleshooting

### "No entries found"
- Verify your server address is correct
- Check that your Inkarr server is running
- Ensure your device can reach the server (same network or port forwarding)

### "Unauthorized" errors
- Verify your API key is correct
- Check that the API key hasn't expired

### Images not loading
- Ensure your Inkarr server implements the `/api/v1/mediafile/{id}/page/{num}` endpoint
- Check server logs for errors

### Local IP not working
- The extension disables DNS over HTTPS to support local IPs
- Try using the hostname if IP doesn't work

## Development

### Project Structure

```
src/all/inkarr/
├── build.gradle           # Extension configuration
├── README.md              # This file
├── res/
│   └── mipmap-*/         # Extension icons
└── src/eu/kanade/tachiyomi/extension/all/inkarr/
    ├── Inkarr.kt          # Main source implementation
    └── dto/
        └── Dto.kt         # Data transfer objects
```

### Key Classes

- `Inkarr.kt`: Main extension class implementing `HttpSource` and `ConfigurableSource`
- `SeriesDto`: Maps Inkarr series to Mihon's `SManga`
- `ChapterDto`: Maps Inkarr chapters to Mihon's `SChapter`
- `MediaFileDto`: Represents CBZ/CBR files and generates page URLs

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test with your Inkarr server
5. Submit a pull request

## License

```
Copyright 2024

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
