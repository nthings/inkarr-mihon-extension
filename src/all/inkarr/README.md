# Inkarr Extension for Mihon

A Mihon/Tachiyomi extension that connects to your self-hosted [Inkarr](https://github.com/your-repo/inkarr) server, allowing you to browse and read your manga/comic collection directly from the Mihon app.

## Features

- Browse your Inkarr library directly from Mihon
- Read CBZ/CBR files streamed from your server
- Support for series metadata (author, artist, genres, status)
- API key authentication for secure access
- Works on local network or remotely (port forwarding/VPN required)

## Requirements

- A running Inkarr server
- Mihon app (v0.5.0+) or compatible fork
- Network connectivity to your Inkarr server

## Installation

### Option 1: Add Repository (Recommended)

1. Open Mihon
2. Go to **Settings** → **Browse** → **Extension repos**
3. Tap **Add** and enter the repository URL:
   ```
   https://raw.githubusercontent.com/your-repo/inkarr-extension/repo/index.min.json
   ```
4. Go to **Browse** → **Extensions** tab
5. Find "Inkarr" and tap **Install**
6. Tap **Trust** when prompted

### Option 2: Manual APK Installation

1. Download the latest APK from the releases page
2. Install the APK on your Android device
3. Open Mihon and trust the extension

## Configuration

After installing the extension:

1. Go to **Browse** → **Extensions**
2. Long press on "Inkarr" and tap **Settings**
3. Configure the following:

| Setting | Description |
|---------|-------------|
| **Server Address** | Your Inkarr server URL (e.g., `http://192.168.1.100:3000`) |
| **API Key** | Your Inkarr API key (optional, for authenticated access) |

> **Note:** The server address must NOT end with a trailing slash.

## Building from Source

### Prerequisites

- JDK 17+
- Android SDK
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
