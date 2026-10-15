<div align="center">

# 🎵 VYBE Music Player

### *Next-Gen Ad-Free Music Streaming & Offline Audio Player for Android*

[![Latest Release](https://img.shields.io/github/v/release/nydatamind/vybe?color=00E5FF&style=for-the-badge&logo=github)](https://github.com/nydatamind/vybe/releases/latest)
[![Download APK](https://img.shields.io/badge/Download-APK%20(Direct)-00B7D8?style=for-the-badge&logo=android&logoColor=white)](https://github.com/nydatamind/vybe/releases/latest/download/vybe.apk)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/AndroidX%20Media3-1.5.1-34A853?style=for-the-badge&logo=googleplay&logoColor=white)](https://developer.android.com/media/media3)
[![License](https://img.shields.io/badge/License-MIT-FBBC05?style=for-the-badge)](LICENSE)

<br/>

**[⬇️ Download Latest APK](https://github.com/nydatamind/vybe/releases/latest/download/vybe.apk)** • **[🚀 Releases](https://github.com/nydatamind/vybe/releases)** • **[🐛 Report Bug](https://github.com/nydatamind/vybe/issues)**

---

</div>

## ✨ Highlights & Features

### 🎛️ Supercharged Lock Screen & Notification Shade
* **❤️ 1-Tap Like Button**: Like or unlike tracks directly from the notification shade or lockscreen without unlocking your device.
* **⬇️ 1-Tap Offline Download**: Direct download action on the playback card to save current song in 320kbps HD with one tap.
* **🎤 Live Synced Karaoke Lyrics**: Real-time LRC lyric lines flash in the notification subtext as the song plays.
* **⏭️ Queue Up-Next Peek**: Preview the upcoming song in queue right from your notifications.
* **🔀 Shuffle & 🔁 Repeat**: Quick access toggles for shuffle and multi-state repeat modes.
* **🎨 Dynamic Monet Theming**: Album artwork colors are extracted in real-time to tint controls with a vibrant glowing aura.
* **❌ Clean Stop**: Dismiss the notification and pause playback cleanly without killing the app.

---

### 🎧 Audio Excellence & Sound Engine
* **✨ 320 kbps Studio Quality**: High-bitrate streaming with zero compression artifacts.
* **🌐 Aura 3D Spatial Audio**: Immersive virtualized soundstage for headphones & earphones.
* **🎚️ 5-Band Studio Equalizer**: Bass boost, acoustic virtualizer, and custom audio presets (Hip-Hop, Rock, Pop, Jazz, Vocal).
* **⚡ Ultra-Low Latency Buffering**: Instant playback start (<5ms) with 15s back-buffer for instantaneous scrubbing and replaying.
* **⏩ Auto Skip Intro & Outro**: Intelligently skips silence and empty intros/outros for a continuous DJ-like experience.

---

### 🎤 Synchronized Karaoke Lyrics
* **Full LRC Support**: Synchronized, line-by-line animated lyrics that scroll smoothly with the melody.
* **Curated & Live Sources**: Automatic lyrics discovery with fallback to rhythmic synced timing.

---

### 📥 High-Speed Offline Downloads
* **Permanent Offline Library**: Download tracks in 320kbps MP3 format.
* **Full ID3 Tagging**: Embedded album artwork, artist metadata, title, and year.
* **Custom Storage Access (SAF)**: Save downloads to your custom SD card/folder or internal app storage with fallback protection.
* **Real-time Download Notifications**: Live progress bars with ETA, speed indicators, and 1-tap "Play Now" completion actions.

---

### 🔄 Automatic In-App Updates
* **Zero-Configuration OTA Updates**: Checks GitHub Releases API in the background on startup.
* **Instant Update Modal**: Displays version comparison, release notes, and a direct 1-tap "Update Now" download trigger.
* **Manual Update Checker**: Accessible via `Settings ➔ About & Updates ➔ Check for Updates`.

---

## 📱 Screenshots & Previews

> *Designed with a dark glassmorphic UI, curated neon cyan accents, and fluid 120Hz Compose animations.*

| Home & Mixes | Now Playing & Lyrics | Queue & Lockscreen |
| :---: | :---: | :---: |
| *(Curated Playlists, Trending Mixes & Search)* | *(Karaoke Synced Lyrics & Aura 3D)* | *(Supercharged Notification & Lock Screen)* |

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
* **Coroutines & Flow**: Reactive asynchronous state management across the UI and services

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

# Compile debug Kotlin sources
./gradlew compileDebugKotlin

# Assemble debug APK (Output in app/build/outputs/apk/debug/vybe.apk)
./gradlew assembleDebug
```

---

## 📄 License & Attribution

* Licensed under the **MIT License**.
* Music metadata and streams are powered by public domain, Creative Commons, and open audio APIs.

<div align="center">
  <sub>Built with ❤️ by <a href="https://github.com/nydatamind">Nitin Yadav</a></sub>
</div>
