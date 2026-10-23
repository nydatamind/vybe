<div align="center">

# 🎵 VYBE Music Player

### *Next-Gen Ad-Free Music Streaming, Live Karaoke Lyrics & Offline Audio Player for Android*

[![Latest Release](https://img.shields.io/github/v/release/nydatamind/vybe?color=00E5FF&style=for-the-badge&logo=github)](https://github.com/nydatamind/vybe/releases/latest)
[![Download APK](https://img.shields.io/badge/Download-APK%20(Direct)-00FF88?style=for-the-badge&logo=android&logoColor=white)](https://github.com/nydatamind/vybe/releases/latest/download/vybe.apk)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/AndroidX%20Media3-1.5.1-34A853?style=for-the-badge&logo=googleplay&logoColor=white)](https://developer.android.com/media/media3)
[![License](https://img.shields.io/badge/License-MIT-FBBC05?style=for-the-badge)](LICENSE)

<br/>

**[⬇️ Download Latest APK](https://github.com/nydatamind/vybe/releases/latest/download/vybe.apk)** • **[🚀 Releases](https://github.com/nydatamind/vybe/releases)** • **[🐛 Report Bug](https://github.com/nydatamind/vybe/issues)**

---

</div>

## ✨ Key Highlights & Features

### 🎨 3 Ultra-Premium Visual Themes & 2 Live Lyrics Modes
Switch between **5 curated player themes** seamlessly using the top dual-arrow switch button with real-time floating indicator toasts:

1. **✨ Velvet Aura Prism**:
   - Multi-layer chromatic rotating halo, breathing glass poster card, and ambient sound diffusion.
2. **🎛️ Cyber Spectrum Stage**:
   - Dynamic 3D audio-reactive equalizer pillars dancing to frequencies, stage spotlight floor, and volumetric lighting.
3. **🌌 Celestial Galaxy Vortex**:
   - 36 spiral galaxy stardust particles traveling on dynamic Keplerian spirals with shooting star trails.
4. **🎵 Line-by-Line Synced Lyrics**:
   - Spotify / Apple Music style synchronized auto-scrolling lyrics (`LazyColumn`).
   - Active line highlighted with glowing emerald border, play indicator icon, and tap-to-seek interaction.
5. **🎤 Word-by-Word Live Karaoke Stage**:
   - Real-time progressive word-by-word karaoke singing highlight (`#00FF88`).
   - Live line singing progress bar indicator.
   - Previous line preview (dimmed) and next line forecast pill (`NEXT ❯ ...`).
   - Fast previous/next line jump buttons for practicing and singing along.

---

### 🎙️ Unified Voice & Lyrics Search
- **Single Universal Entry Point**: Speak a song name, an artist, or even sing/speak lyrics directly into the search bar.
- **Multilingual Recognition**: Built-in speech recognition for Hindi (`hi-IN`), English (`en-IN`, `en-US`), Punjabi (`pa-IN`), Telugu (`te-IN`), Tamil (`ta-IN`), and more.
- Instant search results with zero latency.

---

### 🎛️ Supercharged Notification Shade & Lockscreen
* **❤️ 1-Tap Like Button**: Like or unlike tracks directly from the notification shade or lockscreen without unlocking your phone.
* **⬇️ 1-Tap Offline Download**: Direct download trigger on the playback card to save tracks in 320kbps HD.
* **🎤 Live Lyrics Subtext**: Real-time LRC lyric lines flash in the notification subtitle as the song plays.
* **⏭️ Queue Up-Next Peek**: Preview the upcoming song in queue right from your notifications.
* **🔀 Shuffle & 🔁 Multi-State Repeat**: Quick access toggles for seamless queue management.
* **🎨 Dynamic Monet Theming**: Real-time artwork color extraction for controls and glowing accents.
* **❌ Clean Stop**: Dismiss the notification and pause playback cleanly without killing the background service.

---

### 🎧 Audio Excellence & Sound Engine
* **✨ 320 kbps Studio Quality**: High-bitrate streaming with zero compression artifacts.
* **🌐 Aura 3D Spatial Audio**: Immersive virtualized soundstage for headphones & earphones.
* **🎚️ 5-Band Studio Equalizer**: Custom bass boost, acoustic virtualizer, and custom audio presets (Hip-Hop, Rock, Pop, Jazz, Vocal).
* **⚡ Ultra-Low Latency Buffering**: Instant playback start (<5ms) with 15s back-buffer for instantaneous scrubbing and replaying.
* **⏩ Auto Skip Intro & Outro**: Intelligently skips silence and empty intros/outros for a continuous DJ-like experience.

---

### 📥 High-Speed Offline Downloads & File Management
* **Permanent Offline Library**: Download tracks in 320kbps MP3 format.
* **Full ID3 Tagging**: Embedded album artwork, artist metadata, title, album, and year.
* **Custom Storage Access (SAF)**: Save downloads to custom SD cards or internal folders with fallback protection.
* **Real-time Download Notifications**: Live progress bars with download speed, ETA, and a **1-tap "Cancel Download"** action.
* **1-Tap "Play Now"**: Start listening immediately once a download finishes.

---

### 🧹 Storage & Cache Optimizer
* **Automated Chunk Cleaner**: Periodic cleanup of temporary streaming audio cache and expired images.
* **Storage Manager Settings**: One-tap cache size calculation and manual cache purge.

---

### 🔄 Automatic In-App Updates
* **Zero-Configuration OTA Updates**: Checks GitHub Releases API in the background on startup.
* **Instant Update Modal**: Displays version comparison, release notes, and a direct 1-tap "Update Now" download trigger.
* **Manual Update Checker**: Accessible via `Settings ➔ About & Updates ➔ Check for Updates`.

---

## 🛠️ Tech Stack & Architecture

VYBE is engineered with modern Android development standards:

* **Language**: [Kotlin 2.1](https://kotlinlang.org/)
* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 design system
* **Audio Engine**: [AndroidX Media3](https://developer.android.com/media/media3) (`ExoPlayer`, `MediaSessionService`, `ForwardingPlayer`)
* **Local Database**: [Room Database](https://developer.android.com/training/data-storage/room) with SQLite
* **Key-Value Store**: [Jetpack DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore)
* **Networking**: [OkHttp 4](https://square.github.io/okhttp/) & [KotlinX Serialization](https://github.com/Kotlin/kotlinx.serialization)
* **Image Loading**: [Coil Compose](https://coil-kt.github.io/coil/) with hardware bitmap decoders
* **Coroutines & Flow**: Reactive asynchronous state management across the UI and background playback services
* **Code Shrinking & Optimization**: ProGuard / R8 full-mode optimization and resource shrinking (~5.28 MB APK)

---

## 📥 Installation

1. Download the latest `vybe.apk` directly from GitHub:
   👉 **[Download vybe.apk](https://github.com/nydatamind/vybe/releases/latest/download/vybe.apk)**
2. Tap the downloaded APK in your device's notification bar or File Manager.
3. If prompted, allow *"Install unknown apps"* for your browser.
4. Enjoy unlimited, uninterrupted, ad-free music! 🎶

---

## 💻 Building from Source

### Prerequisites:
* Android Studio Ladybug / Meerkat or newer
* JDK 17+
* Android SDK (API 35 target, API 24 minimum)

### Build Steps:
```bash
# Clone the repository
git clone https://github.com/nydatamind/vybe.git
cd vybe

# Clean and compile Kotlin sources
./gradlew clean compileDebugKotlin

# Assemble release APK (produces optimized, signed ~5.28 MB APK)
./gradlew assembleRelease
```

---

## 📄 License & Attribution

* Licensed under the **MIT License**.
* Music metadata and streams are powered by public domain, Creative Commons, and open audio APIs.

<div align="center">
  <sub>Built with ❤️ by <a href="https://github.com/nydatamind">Nitin Yadav</a></sub>
</div>
