# SegmentedButtonBar

[![](https://jitpack.io/v/dimetileter/segmented-buttons.svg)](https://jitpack.io/#dimetileter/segmented-buttons)
[![MinSdk](https://img.shields.io/badge/minSdk-26-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A polished, lightweight, highly customizable Android Segmented Button Bar library built entirely with modern Android standard Views, supporting **5 distinct styles**, XML-first configuration, physics-based spring animations, dark mode, and accessibility.

---

## ✨ Features

- 🎯 **5 Display Styles**: `horizontal`, `vertical`, `circular`, `pill`, and `expandable`
- 🎨 **Design System Fidelity**: Exact concentric corner radii ($R_{\text{button}} = R_{\text{bar}} - \text{padding}$), pixel-perfect spacing, bounded and unbounded ripples.
- 🌓 **Full Dark Mode**: Automatic theme-aware color tokens and contrast adherence.
- 🚀 **Physics-Based Spring Animations**: Fluid expand/collapse animations with natural overshoot dynamics.
- ⚡ **Zero External Heavy Dependencies**: Pure Android Views, no ViewBinding overhead in library, minSdk 26+.
- ♿ **Accessibility First**: WCAG-compliant touch targets, dynamic content descriptions, and TalkBack optimization.

---

## 📦 Installation

### 1. Add JitPack repository

In your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add the dependency

In your module's `build.gradle.kts` (e.g. `app/build.gradle.kts`):

```kotlin
dependencies {
    implementation("com.github.dimetileter:segmented-buttons:v1.0.0")
}
```

---

## 🛠 Usage & Examples

### 1. Horizontal Style (`horizontal`)

Supports **Text-Only** (automatically centered), **Icon-Only**, or **Icon + Text** with single radio-group selection:

```xml
<!-- Text-Only (Texts are perfectly centered) -->
<com.dimetileter.segmentedbuttonbar.SegmentedButtonBar
    android:id="@+id/horizontalBar"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:sbStyle="horizontal"
    app:sbButtonCount="3"
    app:sbButton1Text="Day"
    app:sbButton2Text="Week"
    app:sbButton3Text="Month"
    app:sbAutoSelect="true" />

<!-- Icon + Text Pair -->
<com.dimetileter.segmentedbuttonbar.SegmentedButtonBar
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:sbStyle="horizontal"
    app:sbButtonCount="2"
    app:sbButton1Icon="@drawable/ic_back"
    app:sbButton1Text="Previous"
    app:sbButton2Icon="@drawable/ic_next"
    app:sbButton2Text="Next" />
<!-- Hybrid Style: 2 Horizontal + 1 Circular 32x32dp Action Button -->
<com.dimetileter.segmentedbuttonbar.SegmentedButtonBar
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:sbStyle="horizontal"
    app:sbButtonCount="3"
    app:sbButton1Text="Active"
    app:sbButton2Text="Completed"
    app:sbButton3Style="circular"
    app:sbButton3Icon="@drawable/ic_add" />
```

### 2. Vertical Style (`vertical`)

A vertical stacked icon-only button bar (32×48dp items, 3dp gap):

```xml
<com.dimetileter.segmentedbuttonbar.SegmentedButtonBar
    android:id="@+id/verticalBar"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:sbStyle="vertical"
    app:sbButtonCount="3"
    app:sbButton1Icon="@drawable/ic_custom_filter"
    app:sbButton2Icon="@drawable/ic_custom_sort"
    app:sbButton3Icon="@drawable/ic_custom_view" />
```

### 3. Circular Style (`circular`)

A single circular 24×24dp button inside a 36×36dp bar container:

```xml
<com.dimetileter.segmentedbuttonbar.SegmentedButtonBar
    android:id="@+id/circularBar"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:sbStyle="circular"
    app:sbButton1Icon="@drawable/ic_custom_add" />
```

### 4. Pill Style (`pill`)

An 80×32dp action pill button supporting `next`, `back`, and `text` actions with lock/unlock activation states:

```xml
<com.dimetileter.segmentedbuttonbar.SegmentedButtonBar
    android:id="@+id/pillBar"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:sbStyle="pill"
    app:sbPillType="next" />
```

### 5. Expandable Style (`expandable`)

An animated bar that starts collapsed (36×36dp) and smoothly expands on tap:

```xml
<com.dimetileter.segmentedbuttonbar.SegmentedButtonBar
    android:id="@+id/expandableBar"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:sbStyle="expandable"
    app:sbButtonCount="3"
    app:sbButton1Icon="@drawable/ic_sb_arrow_next"
    app:sbButton2Icon="@drawable/ic_camera"
    app:sbButton3Icon="@drawable/ic_gallery"
    app:sbExpandDirection="end" />
```

---

## 💻 Kotlin API Reference

```kotlin
val segmentedBar = findViewById<SegmentedButtonBar>(R.id.segmentedBar)

// Button Click Listeners
segmentedBar.setOnButton1Click { /* Handle Button 1 click */ }
segmentedBar.setOnButton2Click { /* Handle Button 2 click */ }
segmentedBar.setOnButton3Click { /* Handle Button 3 click */ }
segmentedBar.setOnButtonClick(index = 0) { /* By index */ }

// Pill Actions
segmentedBar.setOnPillClick {
    val isActivated = !segmentedBar.isPillActivated()
    segmentedBar.setPillActivated(isActivated)
}

// Expandable Controls
segmentedBar.expand(animate = true)
segmentedBar.collapse(animate = true)
segmentedBar.toggleExpand(animate = true)
segmentedBar.setOnExpandChangeListener { isExpanded ->
    // Responding to expand/collapse animation state
}

// Selection & State Management
segmentedBar.selectButton(index = 1)
segmentedBar.clearSelection() // Deselects all buttons
segmentedBar.setAllActivated(true) // Activates all buttons in one call
segmentedBar.activateButton(index = 0) // Activates button 0, deactivates others
segmentedBar.setButtonSelected(index = 0, selected = true)
segmentedBar.setButtonActivated(index = 0, activated = true)
segmentedBar.setButtonEnabled(index = 0, enabled = false)
val selectedIndex = segmentedBar.getSelectedButtonIndex()

// Icon Tint Management
segmentedBar.setIconTint(Color.RED) // Global tint
segmentedBar.setButtonIconTint(index = 1, Color.BLUE) // Per-button tint

// Dynamic Text & Icon Updates
segmentedBar.setButtonText(0, "New Title")
segmentedBar.setButtonIcon(0, R.drawable.ic_new_icon)
```

---

## 📋 XML Attributes Reference

| Attribute | Format | Default | Description |
|---|---|---|---|
| `app:sbStyle` | `enum` | `horizontal` | Button bar layout style (`horizontal`, `vertical`, `circular`, `pill`, `expandable`) |
| `app:sbButtonCount` | `integer` | `2` (or `3`) | Total number of buttons (`1` to `6`) |
| `app:sbSelectedIndex` | `integer` | `0` | Initial selected button index (`-1` for none) |
| `app:sbAllActivated` | `boolean` | `false` | Set `isActivated` for all buttons at once |
| `app:sbPillActivated` | `boolean` | `false` | Initial locked/active state for pill buttons |
| `app:sbIconTint` | `color` | `@null` | Global tint color or ColorStateList applied to all button icons |
| `app:sbButton1Style` .. `app:sbButton6Style` | `enum` | `horizontal` | Individual button style for hybrid bars (`horizontal`, `circular`, `pill`) |
| `app:sbButton1Icon` .. `app:sbButton6Icon` | `reference` | `@null` | Icon resource drawable for the corresponding button |
| `app:sbButton1Text` .. `app:sbButton6Text` | `string` | `@null` | Text label for the corresponding button |
| `app:sbButton1IconTint` .. `app:sbButton6IconTint` | `color` | `@null` | Individual icon tint per button |
| `app:sbButton1Selected` .. `app:sbButton6Selected` | `boolean` | `false` (btn 1: `true`) | Initial selection state (`isSelected`) per button |
| `app:sbButton1Activated` .. `app:sbButton6Activated` | `boolean` | `false` | Initial activation state (`isActivated`) per button |
| `app:sbButton1Enabled` .. `app:sbButton6Enabled` | `boolean` | `true` | Enable/disable state (`isEnabled`) per button |
| `app:sbAutoSelect` | `boolean` | `true` | Automatically toggle `isSelected` on tap |
| `app:sbMaxWidth` | `dimension` | `match_parent` | Optional maximum width cap for responsive layouts |
| `app:sbPillType` | `enum` | `next` | Action type for pill style (`next`, `back`, `text`) |
| `app:sbExpandDirection` | `enum` | `end` | Direction for expandable expansion (`end`, `start`) |
| `app:sbElevation` | `dimension` | `0dp` | Bar elevation and shadow depth |

---

## 📐 Design Tokens & Formula

SegmentedButtonBar strictly enforces the concentric geometry formula for flawless optical nested corners:

$$\text{Radius}_{\text{button}} = \text{Radius}_{\text{bar}} - \text{Padding}_{\text{bar}} \implies 54\text{dp} = 60\text{dp} - 6\text{dp}$$

- **Bar Radius**: `60dp`
- **Button Radius**: `54dp`
- **Bar Padding**: `6dp`
- **Button Gap**: `3dp`
- **Button Height**: `32dp`
- **Minimum Button Width**: `80dp`

---

## 📄 License

```
MIT License

Copyright (c) 2026 Ali Osman Karaoğul

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
