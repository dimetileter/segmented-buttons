# SegmentedButtonBar

[![](https://jitpack.io/v/dimetileter/segmented-buttons.svg)](https://jitpack.io/#dimetileter/segmented-buttons)
[![MinSdk](https://img.shields.io/badge/minSdk-26-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

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

A multi-button horizontal selector with radio-group single selection semantics:

```xml
<com.dimetileter.segmentedbuttonbar.SegmentedButtonBar
    android:id="@+id/horizontalBar"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:sbStyle="horizontal"
    app:sbButtonCount="3"
    app:sbButton1Text="All"
    app:sbButton2Text="Favorites"
    app:sbButton3Text="Archived"
    app:sbAutoSelect="true" />
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

// Selection & Dynamic Updates
segmentedBar.selectButton(index = 1)
val selectedIndex = segmentedBar.getSelectedButtonIndex()
segmentedBar.setButtonText(0, "New Title")
segmentedBar.setButtonIcon(0, R.drawable.ic_new_icon)
```

---

## 📋 XML Attributes Reference

| Attribute | Format | Default | Description |
|---|---|---|---|
| `app:sbStyle` | `enum` | `horizontal` | Button bar layout style (`horizontal`, `vertical`, `circular`, `pill`, `expandable`) |
| `app:sbButtonCount` | `integer` | `2` (or `3`) | Total number of buttons (`1` to `4`) |
| `app:sbButton1Icon` .. `app:sbButton4Icon` | `reference` | `@null` | Icon resource drawable for the corresponding button |
| `app:sbButton1Text` .. `app:sbButton4Text` | `string` | `@null` | Text label for the corresponding button |
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
Copyright 2026 Ali Osman

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
