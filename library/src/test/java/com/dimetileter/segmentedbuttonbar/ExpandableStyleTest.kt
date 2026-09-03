package com.dimetileter.segmentedbuttonbar

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Expandable (animasyonlu genişleyen) stil birim testleri.
 * Unit tests for Expandable (animated) button bar style.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExpandableStyleTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        context = ContextThemeWrapper(baseContext, androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
    }

    /**
     * Expandable çubuğun kapalı durumda (anchor görünür, diğerleri gizli) başladığını doğrular.
     * Verifies expandable bar starts in collapsed state (anchor visible, children gone).
     */
    @Test
    fun verifyExpandableInitialState() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "expandable")
            .addAttribute(R.attr.sbButtonCount, "3")
            .build()

        val bar = SegmentedButtonBar(context, attrs)

        assertThat(bar.getStyle()).isEqualTo(SegmentedButtonBar.STYLE_EXPANDABLE)
        assertThat(bar.getButtonCount()).isEqualTo(3)
        assertThat(bar.isExpanded()).isFalse()

        // 0. buton (Anchor) görünür olmalı / Button 0 (Anchor) must be VISIBLE
        assertThat(bar.getButton(0)?.visibility).isEqualTo(View.VISIBLE)
        // 1 ve 2. butonlar kapalıyken GONE olmalı / Buttons 1 and 2 must be GONE when collapsed
        assertThat(bar.getButton(1)?.visibility).isEqualTo(View.GONE)
        assertThat(bar.getButton(2)?.visibility).isEqualTo(View.GONE)
    }

    /**
     * expand() çağrısının çocuk butonları görünür yaptığını ve dinleyiciyi tetiklediğini test eder.
     * Tests expand() reveals child buttons and triggers the expansion listener.
     */
    @Test
    fun verifyExpandFunctionality() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "expandable")
            .addAttribute(R.attr.sbButtonCount, "3")
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        var expandStateReceived: Boolean? = null
        bar.setOnExpandChangeListener { isExpanded ->
            expandStateReceived = isExpanded
        }

        bar.expand(animate = false)

        assertThat(bar.isExpanded()).isTrue()
        assertThat(expandStateReceived).isTrue()
        assertThat(bar.getButton(1)?.visibility).isEqualTo(View.VISIBLE)
        assertThat(bar.getButton(2)?.visibility).isEqualTo(View.VISIBLE)
    }

    /**
     * collapse() çağrısının çocuk butonları gizlediğini ve dinleyiciyi tetiklediğini test eder.
     * Tests collapse() hides child buttons and triggers the expansion listener.
     */
    @Test
    fun verifyCollapseFunctionality() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "expandable")
            .addAttribute(R.attr.sbButtonCount, "3")
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        bar.expand(animate = false)
        assertThat(bar.isExpanded()).isTrue()

        var collapseStateReceived: Boolean? = null
        bar.setOnExpandChangeListener { isExpanded ->
            collapseStateReceived = isExpanded
        }

        bar.collapse(animate = false)

        assertThat(bar.isExpanded()).isFalse()
        assertThat(collapseStateReceived).isFalse()
        assertThat(bar.getButton(1)?.visibility).isEqualTo(View.GONE)
        assertThat(bar.getButton(2)?.visibility).isEqualTo(View.GONE)
    }

    /**
     * toggleExpand() çağrısının durumu tersine çevirdiğini test eder.
     * Tests toggleExpand() flips the expansion state.
     */
    @Test
    fun verifyToggleExpand() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "expandable")
            .addAttribute(R.attr.sbButtonCount, "2")
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        assertThat(bar.isExpanded()).isFalse()

        bar.toggleExpand(animate = false)
        assertThat(bar.isExpanded()).isTrue()

        bar.toggleExpand(animate = false)
        assertThat(bar.isExpanded()).isFalse()
    }

    /**
     * sbCollapseOnSelect=true olduğunda, genişleme sonrası seçilen butonun ikonu ile otomatik daraldığını test eder.
     * Tests auto-collapse on selection where the selected button icon becomes visible on the collapsed anchor.
     */
    @Test
    fun verifyCollapseOnSelectUpdatesAnchorAndCollapses() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "expandable")
            .addAttribute(R.attr.sbButtonCount, "3")
            .addAttribute(R.attr.sbCollapseOnSelect, "true")
            .addAttribute(R.attr.sbButton1Icon, "@drawable/ic_sb_arrow_next")
            .addAttribute(R.attr.sbButton2Icon, "@drawable/ic_sb_arrow_back")
            .addAttribute(R.attr.sbButton3Icon, "@drawable/ic_sb_arrow_next")
            .addAttribute(R.attr.sbButton3ContentDescription, "Dollar Action")
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        assertThat(bar.isCollapseOnSelect()).isTrue()

        // 1. Menüyü genişlet
        bar.expand(animate = false)
        assertThat(bar.isExpanded()).isTrue()

        // 2. 3. butona (index 2) tıkla
        bar.getButton(2)?.performClick()
        ShadowLooper.idleMainLooper()

        // 3. Otomatik daralma gerçekleşmeli
        assertThat(bar.isExpanded()).isFalse()
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(2)

        // 4. Görünür kalan ana (anchor) buton artık 3. butonun özelliklerini taşımalı
        val anchorView = bar.getButton(0)
        assertThat(anchorView?.contentDescription?.toString()).isEqualTo("Dollar Action")
        assertThat(anchorView?.isSelected).isTrue()

        // 5. Tekrar açıldığında (expand), 2. buton (Anchor üzerinde seçili olan) açılır menüde GONE olmalı, mükerrer buton oluşmamalı!
        bar.expand(animate = false)
        assertThat(bar.isExpanded()).isTrue()
        assertThat(bar.getButton(0)?.visibility).isEqualTo(View.VISIBLE)
        assertThat(bar.getButton(1)?.visibility).isEqualTo(View.VISIBLE)
        assertThat(bar.getButton(2)?.visibility).isEqualTo(View.GONE) // Mükerrer olmaması için gizlendi
    }

    /**
     * 4 farklı genişleme yönünün (end/start/down/up) ve dinamik yön değişiminin doğru çalıştığını test eder.
     * Tests all 4 expansion directions (end, start, down, up) and dynamic direction updates.
     */
    @Test
    fun verifyExpandDirectionsAndDynamicChanges() {
        // 1. Dikey Aşağıya Doğru Genişleme (Down)
        val downAttrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "expandable")
            .addAttribute(R.attr.sbButtonCount, "3")
            .addAttribute(R.attr.sbExpandDirection, "down")
            .build()
        val downBar = SegmentedButtonBar(context, downAttrs)
        assertThat(downBar.getExpandDirection()).isEqualTo(SegmentedButtonBar.EXPAND_DOWN)
        assertThat(downBar.orientation).isEqualTo(android.widget.LinearLayout.VERTICAL)

        // 2. Dikey Yukarıya Doğru Genişleme (Up)
        val upAttrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "expandable")
            .addAttribute(R.attr.sbButtonCount, "3")
            .addAttribute(R.attr.sbExpandDirection, "up")
            .build()
        val upBar = SegmentedButtonBar(context, upAttrs)
        assertThat(upBar.getExpandDirection()).isEqualTo(SegmentedButtonBar.EXPAND_UP)
        assertThat(upBar.orientation).isEqualTo(android.widget.LinearLayout.VERTICAL)
        // Reverse layout kontrolü: Anchor buton en altta olmalı
        assertThat(upBar.getChildAt(upBar.childCount - 1)).isEqualTo(upBar.getButton(0))

        // 3. Sola Doğru Genişleme (Start / Left)
        val startAttrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "expandable")
            .addAttribute(R.attr.sbButtonCount, "3")
            .addAttribute(R.attr.sbExpandDirection, "start")
            .build()
        val startBar = SegmentedButtonBar(context, startAttrs)
        assertThat(startBar.getExpandDirection()).isEqualTo(SegmentedButtonBar.EXPAND_START)
        assertThat(startBar.orientation).isEqualTo(android.widget.LinearLayout.HORIZONTAL)
        // Reverse layout kontrolü: Anchor buton en sağda olmalı
        assertThat(startBar.getChildAt(startBar.childCount - 1)).isEqualTo(startBar.getButton(0))

        // 4. Dinamik setExpandDirection
        startBar.setExpandDirection(SegmentedButtonBar.EXPAND_UP)
        assertThat(startBar.getExpandDirection()).isEqualTo(SegmentedButtonBar.EXPAND_UP)
        assertThat(startBar.orientation).isEqualTo(android.widget.LinearLayout.VERTICAL)
    }
}
