# SegmentedButtonBar

[![](https://jitpack.io/v/dimetileter/segmented-buttons.svg)](https://jitpack.io/#dimetileter/segmented-buttons)
[![MinSdk](https://img.shields.io/badge/minSdk-26-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A polished, lightweight, highly customizable Android Segmented Button Bar library built entirely with modern Android standard Views, supporting **6 distinct styles**, XML-first configuration, sliding pill indicators, physics-based spring animations, dark mode, ViewPager2/Fragment integration, and TalkBack accessibility.

---

## ✨ Features

- 🎯 **6 Display Styles**: `horizontal`, `vertical`, `circular`, `pill`, `expandable`, and `tab` (Sliding Pill Indicator & ViewPager2 / Fragment integration)
- 🛝 **Sliding Pill Indicator**: Smooth, hardware-accelerated indicator sliding across tabs with real-time ViewPager2 drag tracking.
- 🎨 **Design System Fidelity**: Exact concentric corner radii ($R_{\text{button}} = R_{\text{bar}} - \text{padding}$), pixel-perfect spacing, bounded and unbounded ripples.
- 🌓 **Full Dark Mode**: Automatic theme-aware color tokens and contrast adherence.
- 🚀 **Physics-Based Spring Animations**: Fluid expand/collapse animations with natural overshoot dynamics.
- ⚡ **Zero External Heavy Dependencies**: Pure Android Views, no ViewBinding overhead in library, minSdk 26+.
- ♿ **Accessibility First**: WCAG-compliant touch targets, dynamic content descriptions, and native TabWidget / Tab role TalkBack semantics.

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
    implementation("com.github.dimetileter:segmented-buttons:v1.0.6")
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

An animated bar that starts collapsed (44×44dp) and smoothly expands on tap. Supports auto-collapsing to the selected action (`sbCollapseOnSelect="true"`):

```xml
<com.dimetileter.segmentedbuttonbar.SegmentedButtonBar
    android:id="@+id/expandableBar"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:sbStyle="expandable"
    app:sbButtonCount="3"
    app:sbCollapseOnSelect="true"
    app:sbButton1Icon="@drawable/ic_add"
    app:sbButton2Icon="@drawable/ic_camera"
    app:sbButton3Icon="@drawable/ic_gallery"
    app:sbExpandDirection="end" />
```

### 6. Modern TabBar Style (`tab`)

A premium TabBar with a **smoothly sliding selection pill indicator** that animates beneath tabs. Fully compatible with `ViewPager2` (including live swipe gesture tracking) and Fragment navigation:

```xml
<com.dimetileter.segmentedbuttonbar.SegmentedButtonBar
    android:id="@+id/tabBar"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:sbStyle="tab"
    app:sbButtonCount="3"
    app:sbButton1Text="Home"
    app:sbButton2Text="Explore"
    app:sbButton3Text="Profile" />
```

---

## 💻 Kotlin API Reference

```kotlin
val segmentedBar = findViewById<SegmentedButtonBar>(R.id.segmentedBar)

// ViewPager2 Two-Way Synchronization (with live swipe tracking)
segmentedBar.setupWithViewPager2(viewPager2) { config, position ->
    config.text = "Page $position"
    // config.iconRes = R.drawable.ic_tab
}

// FragmentContainerView Integration
segmentedBar.setupWithFragments(
    fragmentManager = supportFragmentManager,
    containerId = R.id.fragmentContainer,
    fragments = listOf(HomeFragment(), ExploreFragment(), ProfileFragment())
)

// Tab Selection Callback
segmentedBar.setOnTabSelectedListener { position ->
    // Handle tab change
}

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

// Expandable Controls & Auto-Collapse on Select
segmentedBar.expand(animate = true)
segmentedBar.collapse(animate = true)
segmentedBar.toggleExpand(animate = true)
segmentedBar.setCollapseOnSelect(true)
segmentedBar.setOnExpandChangeListener { isExpanded ->
    // Track expand/collapse state
}
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

// Tooltip & TalkBack Accessibility
segmentedBar.setButtonContentDescription(0, "Capture Photo")
segmentedBar.setButtonTooltip(0, "Camera")
segmentedBar.setOnButton1LongClick {
    // Custom long press action
    true
}

// Custom Colors & Gradient Backgrounds
segmentedBar.setSelectedColor(Color.parseColor("#FF5722"))
segmentedBar.setButtonSelectedBackground(0, myGradientDrawable)
segmentedBar.setBarBackgroundColor(Color.parseColor("#222222"))
```

---

## 💡 Recommended Practices & Tips

- **Handling Many Buttons (4+ or 5+)**: When using many buttons in a single bar (e.g. 4, 5 or 6 items), it is strongly recommended to prefer **circular** (icon-only) button styles (`app:sbButton*Style="circular"`) and omit text labels. Text labels on 4+ items may cause cramped horizontal spacing on smaller screens, whereas icon-only circular buttons maintain clean touch targets, optical harmony, and compact responsive layouts.
- **Outline vs. Filled Icon States**: For optimal visual feedback, provide a `StateListDrawable` (or dynamic state drawable) for button icons so that when a button becomes selected (`android:state_selected="true"`), its icon automatically switches to a **filled** variant (e.g. `ic_bookmark_filled`), and when deselected, it reverts back to an **outlined** variant (e.g. `ic_bookmark_outlined`):
  ```xml
  <!-- res/drawable/ic_state_bookmark.xml -->
  <selector xmlns:android="http://schemas.android.com/apk/res/android">
      <item android:state_selected="true" android:drawable="@drawable/ic_bookmark_filled" />
      <item android:state_activated="true" android:drawable="@drawable/ic_bookmark_filled" />
      <item android:drawable="@drawable/ic_bookmark_outline" />
  </selector>
  ```
- **Accessibility & Tooltips**: Always provide meaningful `app:sbButton*ContentDescription` or `app:sbButton*Tooltip` for icon-only buttons so TalkBack screen readers can announce the action and users can long-press to reveal the action tooltip.
- **Custom Gradients & Colors**: You can completely customize the active button background with brand gradients or solid colors using `app:sbSelectedBackground` or `app:sbSelectedColor`.

---

## 📋 XML Attributes Reference

| Attribute | Format | Default | Description |
|---|---|---|---|
| `app:sbStyle` | `enum` | `horizontal` | Button bar layout style (`horizontal`, `vertical`, `circular`, `pill`, `expandable`, `tab`) |
| `app:sbButtonCount` | `integer` | `2` (or `3`) | Total number of buttons (`1` to `6`) |
| `app:sbSelectedIndex` | `integer` | `0` | Initial selected button index (`-1` for none) |
| `app:sbAllActivated` | `boolean` | `false` | Set `isActivated` for all buttons at once |
| `app:sbPillActivated` | `boolean` | `false` | Initial locked/active state for pill buttons |
| `app:sbIconTint` | `color` | `@null` | Global tint color or ColorStateList applied to all button icons |
| `app:sbSelectedBackground` | `reference` | `@null` | Custom drawable or gradient for selected buttons |
| `app:sbSelectedColor` | `color` | `@null` | Custom solid color for selected buttons |
| `app:sbBarBackground` | `reference\|color` | `@null` | Override bar container background |
| `app:sbRippleColor` | `color` | `#14000000` | Custom touch highlight and ripple feedback color |
| `app:sbAutoTooltip` | `boolean` | `true` | Automatically show tooltips on button long-press |
| `app:sbSlideIndicator` | `boolean` | `false` (`true` on `tab`) | Smooth sliding pill indicator animation across tabs |
| `app:sbIndicatorDuration` | `integer` | `250` (ms) | Duration of the sliding pill animation in milliseconds |
| `app:sbButton1Style` .. `app:sbButton6Style` | `enum` | `horizontal` | Individual button style for hybrid bars (`horizontal`, `circular`, `pill`) |
| `app:sbButton1Icon` .. `app:sbButton6Icon` | `reference` | `@null` | Icon resource drawable for the corresponding button |
| `app:sbButton1Text` .. `app:sbButton6Text` | `string` | `@null` | Text label for the corresponding button |
| `app:sbButton1IconTint` .. `app:sbButton6IconTint` | `color` | `@null` | Individual icon tint per button |
| `app:sbButton1Tooltip` .. `app:sbButton6Tooltip` | `string` | `@null` | Tooltip text displayed on long press |
| `app:sbButton1ContentDescription` .. `app:sbButton6ContentDescription` | `string` | `@null` | TalkBack accessibility content description |
| `app:sbButton1SelectedBackground` .. `app:sbButton6SelectedBackground` | `reference` | `@null` | Custom selected background drawable/gradient per button |
| `app:sbButton1SelectedColor` .. `app:sbButton6SelectedColor` | `color` | `@null` | Custom selected color per button |
| `app:sbButton1Selected` .. `app:sbButton6Selected` | `boolean` | `false` (btn 1: `true`) | Initial selection state (`isSelected`) per button |
| `app:sbButton1Activated` .. `app:sbButton6Activated` | `boolean` | `false` | Initial activation state (`isActivated`) per button |
| `app:sbButton1Enabled` .. `app:sbButton6Enabled` | `boolean` | `true` | Enable/disable state (`isEnabled`) per button |
| `app:sbAutoSelect` | `boolean` | `true` | Automatically toggle `isSelected` on tap |
| `app:sbMaxWidth` | `dimension` | `match_parent` | Optional maximum width cap for responsive layouts |
| `app:sbPillType` | `enum` | `next` | Action type for pill style (`next`, `back`, `text`) |
| `app:sbExpandDirection` | `enum` | `end` | Direction for expandable expansion (`end`, `start`) |
| `app:sbCollapseOnSelect` | `boolean` | `false` | Automatically collapse expandable bar to selected item upon click |
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
