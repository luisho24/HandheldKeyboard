# Handheld Keyboard

A controller-friendly Android keyboard for handheld gaming devices, Android TV, and touchscreen use. This is a modified version of [LeanKeyboard](https://github.com/yuliskov/LeanKeyboard/tree/6.1.31).

**Modified on 2026-09-14.** This project adds handheld layouts and navigation, a redesigned onboarding flow, adjustable sizing, custom light/dark themes, keyboard surface effects, and configurable sound feedback. The inherited source is distributed under GPL-3.0-only; see [LICENSE.md](LICENSE.md).

## Features

- Navigate and type with a D-pad or supported game controller, with controller-aware key hints and remapping.
- Adapt the keyboard to handheld screen sizes and landscape layouts; adjust its height or use a floating layout.
- Choose system, light, or dark appearance. Edit colors and background images for the custom theme variants.
- Select solid, translucent, or glass-style keyboard surfaces.
- Configure sounds for navigation, typing, deletion, and modifier changes.
- Preview themes in onboarding and open keyboard settings directly from the keyboard.
- Retain LeanKeyboard's language and layout support.

## Download and install

Download the latest APK from [GitHub Releases](https://github.com/luisho24/HandheldKeyboard/releases/latest). The published v0.4.0 APK is a **debug-signed sideload build**, suitable for testing; it is not signed with a production release key.

Install it with Android Debug Bridge:

    adb install -r HandheldKeyboard-v0.4.0-debug.apk
    adb shell ime enable com.handheldkeyboard.ime/com.liskovsoft.leankeyboard.ime.LeanbackImeService
    adb shell ime set com.handheldkeyboard.ime/com.liskovsoft.leankeyboard.ime.LeanbackImeService

You can also enable and select the keyboard in Android's Languages & input settings.

## Build from source

Requirements: JDK 17 and Android SDK Platform 35.

    ./gradlew :leankeykeyboard:assembleHandheldDebug
    ./gradlew :leankeykeyboard:testHandheldDebugUnitTest

The APK is written to leankeykeyboard/build/outputs/apk/handheld/debug/.

## AI-assisted development

Development of this project used AI coding assistance under human direction and review. That describes how the source was developed; the shipped app contains no AI model, AI SDK, AI API client, or AI service integration.

## Privacy

See [PRIVACY.md](PRIVACY.md) for what the keyboard handles on-device and the limits of the optional Android voice-recognition feature. The app does not send typed text, theme images, or settings to a developer-operated service.

## Attribution and third-party licenses

This project is based on [LeanKeyboard 6.1.31](https://github.com/yuliskov/LeanKeyboard/tree/6.1.31) by Yuriy Liskov and contributors. The fork keeps the upstream source notices and GPL-3.0-only license; it is independently maintained and is not an official LeanKeyboard release.

Some inherited source files are Apache-2.0 licensed, and the voice overlay dependency is MIT licensed. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and the included license texts in LICENSES/.
