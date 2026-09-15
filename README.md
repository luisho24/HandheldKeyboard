# Handheld Keyboard

A controller-friendly Android keyboard for handheld gaming devices, Android TV, and touchscreen use. This is a modified version of [LeanKeyboard](https://github.com/yuliskov/LeanKeyboard/tree/6.1.31).

**Modified on 2026-09-15.** This project adds handheld layouts and navigation, a redesigned onboarding flow, adjustable sizing, custom light/dark themes, keyboard surface effects, configurable sound feedback, emoji and textmoji pickers with whole-grapheme backspace, a themed editor-action key integrated into the keyboard row, and an optional controller pointer with remappable actions. The handheld keyboard repurposes its unused on-screen microphone key for emoji access. The inherited source is distributed under GPL-3.0-only; see [LICENSE.md](LICENSE.md).

## In action

| Keyboard in landscape | Quick settings with controller navigation | Theme gallery |
| --- | --- | --- |
| ![Retroid Quartz keyboard preview](docs/images/keyboard-retroid-quartz.png) | ![Quick settings deck](docs/images/quick-settings-deck.png) | ![Theme gallery preview](docs/images/theme-gallery.png) |


## Features

- Navigate and type with a D-pad or supported game controller, including corrected spatial key focus and controller-aware key hints/remapping.
- Show recognizable Xbox, PlayStation, Nintendo Switch, and generic controller icons beside mapped keyboard actions.
- Move the editor cursor with left/right controls while keeping cursor movement inside the focused editor.
- Adapt the keyboard to handheld screen sizes and landscape layouts; adjust its height or use a floating layout.
- Choose system, light, or dark appearance. Edit colors and background images for the custom theme variants.
- Select solid, translucent, or glass-style keyboard surfaces.
- Configure sounds for navigation, typing, deletion, and modifier changes.
- Insert emoji and textmoji from dedicated, controller-navigable keyboard pages.
- Preview themes in onboarding and open the reorganized keyboard settings directly from the keyboard.
- Match the Go, Send, Search, or Done action key to the keyboard theme; the action key sits alongside the cursor controls.
- Use an optional Android Accessibility pointer: toggle it with a configurable two-button chord (M1 + M4 by default), move it with the D-pad anywhere or the right stick while the keyboard is open, then map click, Back, and scrolling to your preferred controller buttons.
- Theme the cursor from the keyboard focus color or with Mint, Violet, and Amber styles; animate activation, movement, and clicks.
- Retain LeanKeyboard's language and layout support.

## Support the project

[![Donate with PayPal](https://img.shields.io/badge/Donate-PayPal-00457C?logo=paypal&logoColor=white)](https://paypal.me/lucabarcas)

Donations help maintain and test Handheld Keyboard on real handheld devices.

## Download and install

Download the latest APK and its SHA-256 checksum from [GitHub Releases](https://github.com/luisho24/HandheldKeyboard/releases/latest). The v0.4.6 APK is built from the minified `handheldRelease` variant. To allow an in-place upgrade from v0.4.0, it is signed with the same Android debug certificate used by that release. **This is not a production signing key**; use this APK for testing/sideloading only. A production release requires a privately managed signing key and cannot replace this build without uninstalling it first.

Install it with Android Debug Bridge:

    adb install -r HandheldKeyboard-v0.4.6-release.apk
    adb shell ime enable com.handheldkeyboard.ime/com.liskovsoft.leankeyboard.ime.LeanbackImeService
    adb shell ime set com.handheldkeyboard.ime/com.liskovsoft.leankeyboard.ime.LeanbackImeService

You can also enable and select the keyboard in Android's Languages & input settings.

### Optional controller pointer

Open **Keyboard settings → Pointer & mouse → Enable controller pointer in Android** and enable **Handheld controller pointer** in Android Accessibility. The service starts with pointer mode off and displays a controller cursor only after its configurable two-button chord is pressed. It does not inspect screen content or typed text. While pointer mode is active, its D-pad controls are captured by the pointer; when it is inactive, the controller keeps its normal keyboard navigation. The right stick moves the pointer while the keyboard is on screen, leaving the keyboard's left-stick navigation untouched.

## Build from source

Requirements: JDK 17 and Android SDK Platform 35. The release APK distributed on GitHub is signed after the Gradle build with the maintainer's local Android debug key so it stays compatible with v0.4.0; the private key is not included in this repository.

    ./gradlew :leankeykeyboard:testHandheldDebugUnitTest
    ./gradlew :leankeykeyboard:assembleHandheldRelease

The unsigned Gradle output is written to `leankeykeyboard/build/outputs/apk/handheld/release/`. The signed release artifact and checksum are published with each GitHub release.

## AI-assisted development

Development of this project used AI coding assistance under human direction and review. That describes how the source was developed; the shipped app contains no AI model, AI SDK, AI API client, or AI service integration.

## Privacy

See [PRIVACY.md](PRIVACY.md) for what the keyboard handles on-device and the limits of Android's optional controller voice-assist path. The app does not send typed text, theme images, or settings to a developer-operated service.

## Attribution and third-party licenses

This project is based on [LeanKeyboard 6.1.31](https://github.com/yuliskov/LeanKeyboard/tree/6.1.31) by Yuriy Liskov and contributors. The fork keeps the upstream source notices and GPL-3.0-only license; it is independently maintained and is not an official LeanKeyboard release.

Some inherited source files are Apache-2.0 licensed, and the voice overlay dependency is MIT licensed. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and the included license texts in LICENSES/.
