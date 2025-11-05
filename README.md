# 🌟 WellSync - Your Personal Wellness Companion

![WellSync Logo](https://img.shields.io/badge/WellSync-Wellness%20App-green?style=for-the-badge&logo=android)
![API Level](https://img.shields.io/badge/API-24%2B-brightgreen?style=flat-square)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.10-purple?style=flat-square&logo=kotlin)
![Android Studio](https://img.shields.io/badge/Android%20Studio-2024.1+-blue?style=flat-square&logo=android-studio)

WellSync is a comprehensive Android wellness application designed to help users track and maintain their physical and mental health through habit tracking, mood monitoring, step counting, and hydration reminders. Built with modern Android development practices using Kotlin and Jetpack components.

## ✨ Features

### 🏠 Dashboard
- **Personalized greeting** based on time of day
- **Quick overview** of daily progress
- **Interactive charts** showing mood trends and activity
- **Real-time updates** of habits, steps, and hydration

### 🔄 Habit Tracking
- Create and manage **custom daily habits**
- **Visual progress tracking** with completion percentages
- **Streak counters** to maintain motivation
- **Reset functionality** for new beginnings

### 😊 Mood Journal
- **Daily mood tracking** with intuitive interface
- **7-day mood trend visualization** using charts
- **Shake-to-add** quick mood entry feature
- **Historical mood data** with detailed analytics

### 👟 Step Counter
- **Real-time step tracking** using device sensors
- **Distance and calorie calculation** based on steps
- **Daily goal progress** (default: 10,000 steps)
- **Share progress** with friends and family

### 💧 Hydration Tracking
- **Water intake monitoring** with customizable goals
- **Smart reminders** at configurable intervals
- **Progress visualization** throughout the day
- **Widget integration** for quick access

### 🎨 Customization
- **Multiple theme colors**: Emerald Green, Ocean Blue, Sunset Orange, Royal Purple, Rose Pink
- **Dark/Light mode** support
- **Personalized notification settings**
- **Custom reminder times** for morning and evening

### 🔔 Smart Notifications
- **Morning motivation** messages
- **Evening reflection** prompts
- **Hydration reminders** at custom intervals
- **Habit completion** notifications

### 📱 Widget Support
- **Home screen widget** showing key metrics
- **Quick access** to pending habits
- **Real-time updates** of water intake and steps

## 🛠️ Technical Specifications

### Architecture
- **MVVM Pattern** with ViewBinding
- **Navigation Component** for seamless fragment transitions
- **Kotlin Coroutines** for asynchronous operations
- **SharedPreferences** for local data persistence

### Dependencies
- **AndroidX Libraries**: Core KTX, Lifecycle, AppCompat, Material Design
- **Navigation Components**: Fragment and UI Navigation
- **MPAndroidChart**: Beautiful chart visualizations
- **Work Manager**: Background task scheduling
- **Gson**: JSON serialization/deserialization

### Permissions
- `ACTIVITY_RECOGNITION`: Step counting functionality
- `POST_NOTIFICATIONS`: Push notifications
- `SCHEDULE_EXACT_ALARM`: Precise reminder scheduling
- `WAKE_LOCK`: Background processing
- `RECEIVE_BOOT_COMPLETED`: Restart services after reboot

## 📋 Requirements

- **Android 7.0 (API level 24)** or higher
- **4GB RAM** recommended
- **100MB** storage space
- **Internet connection** for initial setup (optional)
- **Step counter sensor** for step tracking features

## 🚀 Installation & Setup

### Prerequisites
1. **Android Studio Hedgehog** (2023.1.1) or later
2. **JDK 8** or higher
3. **Android SDK** with API level 24+
4. **Gradle 8.5** or compatible version

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/wellsync.git
   cd wellsync
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory and select it

3. **Sync project with Gradle**
   - Android Studio will automatically prompt to sync
   - Wait for Gradle sync to complete
   - Resolve any dependency issues if prompted

4. **Configure build variants**
   - Select **Build > Select Build Variant**
   - Choose **debug** for development or **release** for production

5. **Run the application**
   - Connect an Android device or start an emulator
   - Click the **Run** button or press `Shift + F10`

### Building APK

#### Debug Build
```bash
./gradlew assembleDebug
```

#### Release Build
```bash
./gradlew assembleRelease
```

The generated APK will be located in `app/build/outputs/apk/`

## 🎯 Usage Guide

### First Launch
1. **Registration**: Create a new account with username, email, and password
2. **Profile Setup**: Add your name and customize your preferences
3. **Permissions**: Grant necessary permissions for full functionality
4. **Theme Selection**: Choose your preferred color theme and mode

### Daily Workflow
1. **Morning**: Check dashboard for daily overview and set intentions
2. **Habit Tracking**: Mark habits as completed throughout the day
3. **Mood Entry**: Log your mood multiple times or use shake feature
4. **Step Monitoring**: Let the app automatically track your steps
5. **Hydration**: Add water intake and respond to reminders
6. **Evening**: Review progress and reflect on the day

### Widget Setup
1. Long press on home screen
2. Select **Widgets**
3. Find **WellSync** widget
4. Drag to desired location
5. Widget updates automatically with your progress

## 🏗️ Project Structure

```
WellSync/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/wellsync/
│   │   │   │   ├── activities/          # Activity classes
│   │   │   │   ├── fragments/           # Fragment classes
│   │   │   │   ├── models/              # Data models
│   │   │   │   ├── utils/               # Utility classes
│   │   │   │   └── widgets/             # Widget components
│   │   │   ├── res/                     # Resources
│   │   │   │   ├── layout/              # XML layouts
│   │   │   │   ├── values/              # Strings, colors, styles
│   │   │   │   ├── drawable/            # Images and icons
│   │   │   │   └── xml/                 # Widget configurations
│   │   │   └── AndroidManifest.xml      # App configuration
│   │   ├── androidTest/                 # Instrumented tests
│   │   └── test/                        # Unit tests
│   ├── build.gradle.kts                 # Module build script
│   └── proguard-rules.pro              # ProGuard configuration
├── gradle/
│   └── libs.versions.toml              # Version catalog
├── build.gradle.kts                    # Project build script
├── settings.gradle.kts                 # Project settings
└── README.md                           # This file
```

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Code Coverage
```bash
./gradlew jacocoTestReport
```

## 🔧 Configuration

### Custom Settings
- **Notification Times**: Modify in Profile > Notification Settings
- **Step Goals**: Adjustable in Steps fragment
- **Water Goals**: Configurable in Water Limit settings
- **Theme Colors**: Available in Profile > Theme Settings

### Advanced Configuration
- **Reminder Intervals**: Can be customized in `NotificationUtils.kt`
- **Chart Configurations**: Modify in respective fragment files
- **Widget Update Frequency**: Adjustable in `WellSyncWidgetProvider.kt`

## 🐛 Troubleshooting

### Common Issues

#### App Crashes on Launch
- Ensure all permissions are granted
- Clear app data and restart
- Check device compatibility (API 24+)

#### Step Counter Not Working
- Verify `ACTIVITY_RECOGNITION` permission
- Check if device has step counter sensor
- Restart the app or device

#### Notifications Not Appearing
- Enable notifications in device settings
- Check `POST_NOTIFICATIONS` permission
- Verify notification settings in app

#### Widget Not Updating
- Remove and re-add widget
- Check background app refresh settings
- Ensure app has battery optimization exceptions

### Debug Mode
Enable debug logging by adding this to your `local.properties`:
```properties
debug.logging=true
```

## 🤝 Contributing

We welcome contributions to WellSync! Please follow these guidelines:

### Development Setup
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes following our coding standards
4. Add tests for new functionality
5. Commit your changes: `git commit -m 'Add amazing feature'`
6. Push to the branch: `git push origin feature/amazing-feature`
7. Open a Pull Request

### Coding Standards
- Follow **Kotlin coding conventions**
- Use **meaningful variable and function names**
- Add **comments for complex logic**
- Maintain **consistent formatting**
- Write **unit tests** for new features

### Pull Request Process
1. Update documentation for any new features
2. Ensure all tests pass
3. Update version numbers if needed
4. Get approval from maintainers

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2024 WellSync Team

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 👥 Authors & Contributors

- **IT23752504** - *Initial development* - [Kajapirathap](https://github.com/kajapirathap)

## 🙏 Acknowledgments

- **MPAndroidChart** library for beautiful chart visualizations
- **Material Design** guidelines for UI/UX inspiration
- **Android Jetpack** components for modern architecture
- **Kotlin** community for excellent language support

## 📞 Support

For support, feature requests, or bug reports:

- 📧 Email: support@wellsync.app
- 🐛 Issues: [GitHub Issues](https://github.com/yourusername/wellsync/issues)
- 💬 Discussions: [GitHub Discussions](https://github.com/yourusername/wellsync/discussions)

## 🚀 Roadmap

### Upcoming Features
- [ ] **Cloud synchronization** across devices
- [ ] **Social features** and community challenges
- [ ] **Advanced analytics** and insights
- [ ] **Export data** functionality
- [ ] **Integration** with fitness trackers
- [ ] **Meditation and mindfulness** features
- [ ] **Nutrition tracking** capabilities

### Version History
- **v1.0.0** - Initial release with core features
- **v1.1.0** - Widget support and theme customization
- **v1.2.0** - Enhanced notifications and data recovery

---

<div align="center">

**Made with ❤️ for a healthier tomorrow**

[![Follow](https://img.shields.io/github/followers/kajapirathap?style=social)](https://github.com/kajapirathap)
[![Star](https://img.shields.io/github/stars/kajapirathap/wellsync?style=social)](https://github.com/kajapirathap/wellsync)

</div>