# SNOREGUARD

![SNOREGUARD Banner](https://img.shields.io/badge/Android-Kotlin-blue?logo=android)
**Smart Snoring Data Analyze App**

---

## 📱 Introduction

**SNOREGUARD** is an Android application for smart snoring detection, analysis, and intervention using SNORE-GUARD pillow. The app provides detailed reports, intuitive charts, and supports WiFi configuration for the device.

---

## 🚀 Features

- **IoT Device Connection:** Configure WiFi and send network info to the device.
- **Snoring Monitoring:** Display snoring data by day, intensity, and duration.
- **Visual Charts:** Waveform, bar chart, and improvement line chart.
- **Sleep Quality Report:** Calculate AHI, sleep quality, and snoring rate.
- **Date Selection:** Beautiful Material DatePicker for report browsing.
- **English UI:** Friendly, modern, and easy to use.
- **Notifications & Alerts:** Snoring alerts and device status.

---

## 🏗️ Architecture & Technologies

- **Language:** Kotlin
- **Architecture:** MVVM (ViewModel, LiveData, Repository)
- **UI:** Material Design, ViewBinding, Custom Views (Waveform, CircularProgress)
- **Networking:** Retrofit + OkHttp
- **Charts:** MPAndroidChart
- **Data Handling:** Coroutine, LiveData
- **WiFi Management:** WifiManager, SmartConfig (ESP32)

---



## ⚙️ Installation & Usage

### 1. Clone the project
```bash
git clone https://github.com/yourusername/SNOREGUARD.git
cd SNOREGUARD
```

### 2. Open with Android Studio
- File > Open > Select the project folder

### 3. Add API Key (if needed)
- Configure API endpoint in `ApiClient.kt` if changed.

### 4. Build & Run
- Select device/emulator > Click **Run** (Shift+F10)

---

## 📝 Project Structure

```
app/
 ├── src/
 │   ├── main/
 │   │   ├── java/com/example/snoreguard/
 │   │   │   ├── data/         # Model, Repository, Remote API
 │   │   │   ├── ui/           # Fragment, ViewModel, Custom View
 │   │   │   ├── ...           # Others
 │   │   ├── res/              # Layout, Drawable, Values
 │   │   ├── AndroidManifest.xml
 │   ├── build.gradle
 ├── build.gradle
 └── README.md
```

---

## 🛠️ Useful Commands

- **Clean project:**  
  `./gradlew clean`
- **Build APK:**  
  `./gradlew assembleDebug`
- **Install on device:**  
  Use Android Studio or `adb install`

---

## 💡 Contribution & Contact

- **Issues & PRs:** Welcome! Please open an issue or pull request.
- **Contact:** [https://www.facebook.com/coderbug404]
- **Author:** [Coderbug]-[Chau Van Kim Cuong] - only me :))

---

> © 2025 SNOREGUARD. All rights reserved. 
