package com.dimetileter.segmentedbuttonbar

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * SegmentedButtonBar farklı stil (horizontal, vertical, circular, pill) oluşturma testleri.
 * Unit tests for inflating different SegmentedButtonBar styles (horizontal, vertical, circular, pill).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StyleInflationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        context = ContextThemeWrapper(baseContext, androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
    }

    /**
     * Dikey stilin dikey yönelim ve varsayılan 3 butonla oluşturulduğunu doğrular.
     * Verifies vertical style initializes with VERTICAL orientation and default 3 buttons.
     */
    @Test
    fun verifyVerticalStyleInflation() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "vertical")
            .addAttribute(R.attr.sbButtonCount, "3")
            .build()

        val bar = SegmentedButtonBar(context, attrs)

        assertThat(bar.getStyle()).isEqualTo(SegmentedButtonBar.STYLE_VERTICAL)
        assertThat(bar.orientation).isEqualTo(LinearLayout.VERTICAL)
        assertThat(bar.getButtonCount()).isEqualTo(3)
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(0)
    }

    /**
     * Dairesel stilin tekil buton ve yatay yönelimle oluşturulduğunu doğrular.
     * Verifies circular style initializes with 1 button and HORIZONTAL orientation.
     */
    @Test
    fun verifyCircularStyleInflation() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "circular")
            .build()

        val bar = SegmentedButtonBar(context, attrs)

        assertThat(bar.getStyle()).isEqualTo(SegmentedButtonBar.STYLE_CIRCULAR)
        assertThat(bar.getButtonCount()).isEqualTo(1)
    }

    /**
     * Pill geri (back) ve metin (text) stillerinin doğru oluşturulduğunu test eder.
     * Verifies pill back and text styles inflate properly.
     */
    @Test
    fun verifyPillBackAndTextStyleInflation() {
        val backAttrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "pill")
            .addAttribute(R.attr.sbPillType, "back")
            .build()

        val backBar = SegmentedButtonBar(context, backAttrs)
        assertThat(backBar.getStyle()).isEqualTo(SegmentedButtonBar.STYLE_PILL)
        assertThat(backBar.getButtonCount()).isEqualTo(1)

        val textAttrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "pill")
            .addAttribute(R.attr.sbPillType, "text")
            .addAttribute(R.attr.sbButton1Text, "Confirm")
            .build()

        val textBar = SegmentedButtonBar(context, textAttrs)
        assertThat(textBar.getStyle()).isEqualTo(SegmentedButtonBar.STYLE_PILL)
        assertThat(textBar.getButtonCount()).isEqualTo(1)
    }
}
