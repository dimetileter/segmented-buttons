package com.dimetileter.segmentedbuttonbar

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Hibrit (Karma) Buton Çubuğu (ör. 2 Yatay Buton + 1 Dairesel 32x32dp Buton) birim testleri.
 * Unit tests for Hybrid SegmentedButtonBar (e.g. 2 Horizontal Buttons + 1 Circular 32x32dp Button).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HybridStyleTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        context = ContextThemeWrapper(baseContext, androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
    }

    /**
     * 2 Yatay + 1 Dairesel butonlu hibrit çubuğun doğru boyut ve düzenle oluşturulduğunu test eder.
     * Tests hybrid bar with 2 Horizontal + 1 Circular buttons creates correct dimensions and layouts.
     */
    @Test
    fun verifyHybridTwoHorizontalOneCircularBar() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "3")
            .addAttribute(R.attr.sbButton1Text, "Option 1")
            .addAttribute(R.attr.sbButton2Text, "Option 2")
            .addAttribute(R.attr.sbButton3Style, "circular")
            .addAttribute(R.attr.sbButton3Icon, "@drawable/ic_sb_arrow_next")
            .build()

        val bar = SegmentedButtonBar(context, attrs)

        assertThat(bar.getButtonCount()).isEqualTo(3)

        val btn1 = bar.getButton(0)
        val btn2 = bar.getButton(1)
        val btn3 = bar.getButton(2)

        // 1 ve 2. butonlar yatay düzen öğesi olmalı / Buttons 1 and 2 must have sb_item_text
        assertThat(btn1?.findViewById<android.view.View>(R.id.sb_item_text)).isNotNull()
        assertThat(btn2?.findViewById<android.view.View>(R.id.sb_item_text)).isNotNull()

        // 3. buton dairesel düzen öğesi olmalı / Button 3 must have sb_circular_icon
        assertThat(btn3?.findViewById<android.view.View>(R.id.sb_circular_icon)).isNotNull()

        // 3. butonun boyutu 32x32dp olmalı / Button 3 dimensions must be 32x32dp
        val expectedCircularSize = context.resources.getDimensionPixelSize(R.dimen.sb_circular_button_hybrid_size)
        assertThat(btn3?.layoutParams?.width).isEqualTo(expectedCircularSize)
        assertThat(btn3?.layoutParams?.height).isEqualTo(expectedCircularSize)
    }

    /**
     * Hibrit çubuktaki dairesel butonun tıklama dinleyicisinin bağımsız çalıştığını test eder.
     * Tests circular action button click listener dispatches properly in hybrid bar.
     */
    @Test
    fun verifyHybridCircularButtonClickDispatch() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "3")
            .addAttribute(R.attr.sbButton1Text, "Tab 1")
            .addAttribute(R.attr.sbButton2Text, "Tab 2")
            .addAttribute(R.attr.sbButton3Style, "circular")
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        var tab1Clicked = false
        var circularActionClicked = false

        bar.setOnButton1Click { tab1Clicked = true }
        bar.setOnButton3Click { circularActionClicked = true }

        bar.getButton(0)?.performClick()
        assertThat(tab1Clicked).isTrue()
        assertThat(circularActionClicked).isFalse()

        bar.getButton(2)?.performClick()
        assertThat(circularActionClicked).isTrue()
    }

    /**
     * Hibrit çubuklarda XML'deki per-button pill stilinin gerçekten pill layout ürettiğini doğrular.
     * Verifies per-button pill style creates an actual pill item in hybrid bars.
     */
    @Test
    fun verifyHybridPillButtonStyle() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "3")
            .addAttribute(R.attr.sbButton1Text, "Tab 1")
            .addAttribute(R.attr.sbButton2Text, "Tab 2")
            .addAttribute(R.attr.sbButton3Style, "pill")
            .addAttribute(R.attr.sbButton3Text, "Apply")
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        var pillClicked = false

        bar.setOnButton3Click { pillClicked = true }
        bar.getButton(2)?.performClick()

        assertThat(bar.getButton(2)?.id).isEqualTo(R.id.sb_pill_root)
        assertThat(bar.getButton(2)?.findViewById<android.view.View>(R.id.sb_pill_text)).isNotNull()
        assertThat(pillClicked).isTrue()
    }
}
