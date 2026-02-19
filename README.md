# Inkarr Mihon Extension

A [Mihon](https://github.com/mihonapp/mihon) extension for [Inkarr](https://github.com/your-username/inkarr), the self-hosted manga and comic server.

## Installation

### Add the Repository

1. Open Mihon
2. Go to **Settings** → **Browse** → **Extension repos**
3. Tap **Add** and enter:
   ```
   https://raw.githubusercontent.com/YOUR_USERNAME/inkarr-mihon-extension/repo/index.min.json
   ```

### Install the Extension

1. Go to **Browse** → **Extensions**
2. Find **Inkarr** in the list
3. Tap **Install**

## Configuration

After installing, configure the extension:

1. Go to **Browse** → **Extensions**
2. Tap the gear icon next to **Inkarr**
3. Set your **Server URL** (e.g., `http://192.168.1.100:3000`)
4. Set your **API Key** (from Inkarr Settings → API Keys)

## Features

- Browse your Inkarr manga/comic library
- Search across all series
- View chapter lists and read chapters
- Automatic page ordering
- Secure API key authentication

## Building

```bash
./gradlew :src:all:inkarr:assembleDebug
```

The APK will be in `src/all/inkarr/build/outputs/apk/debug/`.

## License

    Copyright 2015 Javier Tomás

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

## Credits

Build system based on [Keiyoushi/extensions-source](https://github.com/keiyoushi/extensions-source).
