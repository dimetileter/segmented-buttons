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
 * XML durum öznitelikleri (Selected, Activated, Enabled) ve runtime API birim testleri.
 * Unit tests for XML state attributes (Selected, Activated, Enabled) and runtime state APIs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StateAttributesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        context = ContextThemeWrapper(baseContext, androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
    }

    /**
     * XML'den sbSelectedIndex=-1 verildiğinde hiçbir butonun seçili olmadığını doğrular.
     * Verifies setting sbSelectedIndex=-1 starts with all buttons unselected.
     */
    @Test
    fun verifyNoInitialSelectionViaXml() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "2")
            .addAttribute(R.attr.sbSelectedIndex, "-1")
            .build()

        val bar = SegmentedButtonBar(context, attrs)

        assertThat(bar.getSelectedButtonIndex()).isEqualTo(-1)
        assertThat(bar.getButton(0)?.isSelected).isFalse()
        assertThat(bar.getButton(1)?.isSelected).isFalse()
    }

    /**
     * Buton bazlı sbButton1Selected/sbButton2Selected XML özniteliklerini doğrular.
     * Verifies per-button sbButton1Selected/sbButton2Selected XML attributes.
     */
    @Test
    fun verifyPerButtonSelectionViaXml() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "2")
            .addAttribute(R.attr.sbButton1Selected, "false")
            .addAttribute(R.attr.sbButton2Selected, "true")
            .build()

        val bar = SegmentedButtonBar(context, attrs)

        assertThat(bar.getSelectedButtonIndex()).isEqualTo(1)
        assertThat(bar.getButton(0)?.isSelected).isFalse()
        assertThat(bar.getButton(1)?.isSelected).isTrue()
    }

    /**
     * Buton bazlı sbButton1Activated ve sbButton2Enabled XML özniteliklerini doğrular.
     * Verifies per-button sbButton1Activated and sbButton2Enabled XML attributes.
     */
    @Test
    fun verifyPerButtonActivationAndEnabledViaXml() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "2")
            .addAttribute(R.attr.sbButton1Activated, "true")
            .addAttribute(R.attr.sbButton2Enabled, "false")
            .build()

        val bar = SegmentedButtonBar(context, attrs)

        assertThat(bar.isButtonActivated(0)).isTrue()
        assertThat(bar.isButtonActivated(1)).isFalse()

        assertThat(bar.isButtonEnabled(0)).isTrue()
        assertThat(bar.isButtonEnabled(1)).isFalse()
    }

    /**
     * Pill çubuğunun sbPillActivated=true ile doğrudan aktif başlatılabildiğini test eder.
     * Verifies pill bar initializes activated when sbPillActivated=true is set in XML.
     */
    @Test
    fun verifyPillActivatedViaXml() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "pill")
            .addAttribute(R.attr.sbPillActivated, "true")
            .build()

        val bar = SegmentedButtonBar(context, attrs)

        assertThat(bar.isPillActivated()).isTrue()
    }

    /**
     * clearSelection(), setButtonSelected(), setButtonActivated() çalışma zamanı API'lerini test eder.
     * Tests clearSelection(), setButtonSelected(), and setButtonActivated() runtime APIs.
     */
    @Test
    fun verifyRuntimeStateApis() {
        val bar = SegmentedButtonBar(context)

        bar.clearSelection()
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(-1)
        assertThat(bar.getButton(0)?.isSelected).isFalse()

        bar.setButtonSelected(1, true)
        assertThat(bar.isButtonSelected(1)).isTrue()

        bar.setButtonActivated(0, true)
        assertThat(bar.isButtonActivated(0)).isTrue()

        bar.setButtonEnabled(0, false)
        assertThat(bar.isButtonEnabled(0)).isFalse()
    }
}
