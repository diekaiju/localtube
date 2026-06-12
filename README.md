# LocalTube

[![CI Status](https://github.com/TeamNewPipe/NewPipeExtractor/actions/workflows/ci.yml/badge.svg?branch=dev&event=schedule)](https://github.com/TeamNewPipe/NewPipeExtractor/actions/workflows/ci.yml)
[![JitPack Release](https://jitpack.io/v/teamnewpipe/NewPipeExtractor.svg)](https://jitpack.io/#teamnewpipe/NewPipeExtractor)
[![API Reference](https://img.shields.io/badge/docs-JDoc-blue)](https://teamnewpipe.github.io/NewPipeExtractor/javadoc/)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

**LocalTube** is an enhanced, self-hosted streaming server solution built on top of a specialized fork of [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor). By bundling a lightweight and concurrent Java HTTP server inside an Android application (`localServerApp`), LocalTube enables you to browse, search, and stream content from major streaming platforms directly from any device in your local network using a standard web browser.

---

## 📺 LocalTube Application (`localServerApp`)

LocalTube transforms the stateless library core of NewPipe Extractor into a private, self-hosted streaming web server.

### 🌟 Key Features
- **Decentralized Local Server:** Runs a lightweight, concurrent Java HTTP server directly on your Android device (default port `8080`), serving a modern, responsive web interface.
- **Cross-Device Playback:** Connect seamlessly to the server from any device on your local network (PC, laptop, smart TV, tablet) by visiting your device's local IP (e.g., `http://192.168.1.100:8080`).
- **Theme Customization:** Toggle between modern Dark and clean Light themes instantly, with preferences preserved locally in browser storage.
- **Private Watch History:** Tracks your watched videos locally on the host device using a secure SQLite database (`HistoryDbHelper`), maintaining privacy with zero external telemetry or tracking.
- **HTML5 Player & Stream Proxying:** Proxies stream traffic through the local server to bypass client-side signature and throttling restrictions, fully supporting Range HTTP headers (seeking/fast-forwarding) within native browser elements.
- **Multi-Service Ready:** Ready to stream content across YouTube, SoundCloud, PeerTube, Bandcamp, and media.ccc.de.

---

## 📸 Interface Preview

| Android App Interface | Web Interface Home (Light) | Media Streaming & Details |
| :---: | :---: | :---: |
| ![Android App Interface](screenshots/app_interface.jpg) | ![Web Interface Home](screenshots/web_interface_home.jpg) | ![Streaming Interface](screenshots/web_interface_watch.jpg) |

---

## 🛠️ Build and Installation

To compile and launch the local server application:

1. **Build and install** the application on your Android device or emulator:
   ```bash
   ./gradlew :localServerApp:installDebug
   ```
   *Alternatively, open this repository in Android Studio and run the `:localServerApp` run configuration.*

2. **Run the Server:**
   - Open the **LocalTube** app on your device.
   - Tap **Start Server** to activate the foreground service. A persistent notification will display your active local network URL.
   - Access `http://localhost:8080` (or `http://<your-device-ip>:8080`) from any browser on the same network.

---

## 📦 Extractor Library Integration

The underlying extractor core can be used independently in other Gradle projects via JitPack.

### 1. Repository Configuration
Add the JitPack repository to your `settings.gradle` or root `build.gradle` file:
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
```

### 2. Dependency Declaration
Add the dependency to your application's `build.gradle`:
```groovy
dependencies {
    implementation 'com.github.teamnewpipe:NewPipeExtractor:RELEASE_VERSION'
}
```

> [!NOTE]  
> If target SDK compatibility requires a `minSdk` below 33, configure [Core Library Desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring) utilizing the `desugar_jdk_libs_nio` artifact.

---

## 🧪 Advanced Library Testing

For development and debugging, you can build and test the extractor library locally.

### Option A: Gradle Composite Build (Recommended)
Add the following configuration block to your client project's `settings.gradle` to substitute the published library with your local codebase:
```groovy
includeBuild('../NewPipeExtractor') {
    dependencySubstitution {
        substitute module('com.github.teamnewpipe:NewPipeExtractor') with project(':extractor')
    }
}
```

### Option B: Local Maven Repository Publish
1. Add `mavenLocal()` to your project's repository list (usually as the first entry to prioritize local builds).
2. Run the build wrapper's install task to deploy to your local `.m2` repository:
   ```bash
   ./gradlew install
   ```
3. Reference your local version in your project dependencies (e.g., `com.github.teamnewpipe:NewPipeExtractor:LOCAL-SNAPSHOT`).

---

## 🌐 Supported Sites
The extractor core natively fetches streaming data from:
- YouTube
- SoundCloud
- PeerTube (Non-P2P playback)
- Bandcamp
- media.ccc.de

---

## 📄 License
This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for details.

[![GNU GPLv3](https://www.gnu.org/graphics/gplv3-127x51.png)](https://www.gnu.org/licenses/gpl-3.0.en.html)

## Buy me a coffee
if you want to Buy me a coffee : [buymeacoffee](https://tinyurl.com/utbunyw8)
