# Privacy

This note describes the Handheld Keyboard source and v0.4.4 handheld build. It is not a statement about Android, device manufacturers, the apps where you type, or third-party recognition providers.

## What the keyboard does with text

An input method must receive key events and send entered characters to the currently focused app. This keyboard also uses Android's input-method APIs for cursor navigation and may temporarily hold suggestions supplied by the focused app. It does not save typed text in its settings or a keyboard database.

The app's handheld manifest does not request the Android INTERNET permission. The source has no app-controlled network client, analytics, crash-reporting service, or developer-operated upload endpoint. No typed text, theme image, or keyboard setting is uploaded by this project's code.

Keyboard settings, emoji/textmoji selections, and imported theme images are handled on the device. Emoji and textmoji are bundled keyboard labels inserted directly into the focused app. Theme images are copied into the app's private storage; the app does not sync them to a server.

## Optional voice input

The handheld keyboard no longer displays an on-screen microphone key. Voice input may still be invoked through a supported system/controller voice-assist key; when used, it requires microphone access and delegates recognition to Android's installed speech-recognition provider. That provider may process audio off-device under its own privacy policy and network behavior; the keyboard does not control those services. If you require voice input to remain entirely on-device, use a device/provider configured for offline recognition or do not use voice assist.

## AI

AI was used to assist development. The app itself has no bundled AI model, AI SDK, AI API client, or AI-powered runtime feature.

## Scope

This is a source-level review of the app code and handheld manifest in this repository. Android and device services, speech-recognition providers, third-party apps, and future modifications may have their own behavior and policies.
