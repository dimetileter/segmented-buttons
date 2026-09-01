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
}
