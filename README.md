# AssistKit

![AssistKit icon](/Users/kacs/development/dev/assist_kit/app/src/main/res/mipmap-xxxhdpi/ic_launcher_black.png)

AssistKit is an Android accessibility-powered floating control kit built for moments when reaching the right system action should feel instant.

Instead of opening menus, switching apps, or tapping through settings, AssistKit stays available as a compact floating overlay that can:

- auto-scroll content with adjustable speed
- raise or lower device volume
- lock the screen on demand
- remain accessible through a persistent foreground notification

## Why It Exists

Some interactions are repetitive.
Some controls are buried.
Some moments call for fewer taps, less friction, and a more direct path to the action you already know you want.

AssistKit turns those repeated actions into a lightweight on-screen utility:

- small when you do not need it
- fast when you do
- explicit and always user-triggered

## Core Experience

### Floating Overlay

The app is centered around a draggable floating overlay that:

- expands into a vertical action rail
- automatically dims when left idle
- wakes back up on touch
- keeps controls visible above other apps

### Accessibility-Controlled Scrolling

AssistKit uses an `AccessibilityService` to perform controlled downward gestures for user-initiated auto-scroll. It can:

- start, pause, and stop scrolling
- detect repeated no-effect scroll attempts
- stop automatically when content appears to be exhausted
- apply a flexible speed level instead of only fixed presets

### Device Utility Actions

Beyond scrolling, AssistKit also includes:

- volume up
- volume down
- lock screen

These actions are exposed directly from the same overlay so the app feels like a focused assistive toolkit rather than a single-purpose scroller.

## Design Direction

AssistKit uses a dark minimal interface with:

- a black and grey palette
- icon-led controls
- compact overlay surfaces
- low-friction setup and operation

The goal is utility first: quiet visuals, fast recognition, and minimal interruption.

## Technical Overview

Built with:

- Kotlin
- Android 12+ (`minSdk 31`)
- Jetpack Compose for the main app UI
- `AccessibilityService` for gesture dispatch and lock-screen global action
- Foreground `Service` + `WindowManager` overlay for persistent controls
- DataStore for lightweight settings persistence

Key responsibilities:

- `MainActivity`: onboarding and control UI
- `OverlayService`: foreground runtime owner
- `OverlayManager`: floating overlay presentation and interaction
- `AutoScrollAccessibilityService`: gesture-based scroll execution
- `ScrollController`: shared command/state coordination
- `AndroidSettingsRepository`: persisted overlay position and scroll speed

## Permissions

AssistKit requires:

- Accessibility access
- Draw over other apps

These are used to support the explicit, user-triggered actions available in the overlay. The app does not begin scrolling on its own.

## Current Capabilities

- adjustable auto-scroll speed
- start / pause / stop scroll actions
- end-of-content auto-stop
- floating overlay dim-on-idle behavior
- volume controls
- lock screen action
- persistent notification controls

## Future Possibilities

Potential directions for AssistKit:

- per-app behavior tuning
- richer device shortcuts
- better gesture customization
- alternate overlay layouts
- quick profiles for different reading or browsing contexts

## Development

Run unit tests with:

```bash
./gradlew testDebugUnitTest
```

Build a debug APK with:

```bash
./gradlew assembleDebug
```

## Status

AssistKit is currently an MVP focused on floating accessibility-driven control, with scrolling as the anchor feature and device utility actions layered in around it.
