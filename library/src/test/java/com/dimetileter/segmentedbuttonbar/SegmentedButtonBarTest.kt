package com.dimetileter.segmentedbuttonbar

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * SegmentedButtonBar durum, seçim ve etkileşim birim testleri.
 * Unit tests for SegmentedButtonBar state, selection, and interactions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SegmentedButtonBarTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        context = ContextThemeWrapper(baseContext, androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
    }

    /**
     * Varsayılan yatay buton çubuğunun 2 butonla ve ilk buton seçili olarak başlatıldığını doğrular.
     * Verifies default horizontal bar initializes with 2 buttons and first button selected.
     */
    @Test
    fun verifyDefaultHorizontalInitialization() {
        val bar = SegmentedButtonBar(context)

        assertThat(bar.getButtonCount()).isEqualTo(2)
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(0)
        assertThat(bar.getButton(0)?.isSelected).isTrue()
        assertThat(bar.getButton(1)?.isSelected).isFalse()
    }

    /**
     * Buton seçimi değiştirildiğinde radyo butonu mantığının (tek seçim) çalıştığını test eder.
     * Tests mutually-exclusive single selection behavior when switching button selection.
     */
    @Test
    fun verifySingleSelectionBehavior() {
        val bar = SegmentedButtonBar(context)

        bar.selectButton(1)
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(1)
        assertThat(bar.getButton(0)?.isSelected).isFalse()
        assertThat(bar.getButton(1)?.isSelected).isTrue()

        bar.selectButton(0)
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(0)
        assertThat(bar.getButton(0)?.isSelected).isTrue()
        assertThat(bar.getButton(1)?.isSelected).isFalse()
    }

    /**
     * Geçersiz indeks girildiğinde çökme yaşanmadığını doğrular.
     * Verifies out-of-bounds selection index does not crash.
     */
    @Test
    fun verifyOutOfBoundsSelectionIsSafe() {
        val bar = SegmentedButtonBar(context)
        bar.selectButton(99)
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(0)

        bar.selectButton(-1)
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(0)
    }

    /**
     * Buton tıklama dinleyicilerinin doğru şekilde tetiklendiğini test eder.
     * Tests that click listeners are properly dispatched when buttons are tapped.
     */
    @Test
    fun verifyButtonClickListeners() {
        val bar = SegmentedButtonBar(context)
        var button1Clicked = false
        var button2Clicked = false

        bar.setOnButton1Click { button1Clicked = true }
        bar.setOnButton2Click { button2Clicked = true }

        bar.getButton(0)?.performClick()
        assertThat(button1Clicked).isTrue()
        assertThat(button2Clicked).isFalse()

        bar.getButton(1)?.performClick()
        assertThat(button2Clicked).isTrue()
    }

    /**
     * Pill butonunun isActivated kilit modelini test eder (isSelected'dan bağımsız).
     * Tests pill button isActivated lock model (independent from isSelected).
     */
    @Test
    fun verifyPillActivationState() {
        val bar = SegmentedButtonBar(context)

        assertThat(bar.isPillActivated()).isFalse()

        bar.setPillActivated(true)
        assertThat(bar.isPillActivated()).isTrue()

        bar.setPillActivated(false)
        assertThat(bar.isPillActivated()).isFalse()
    }

    /**
     * Dinamik metin güncellemesinin çalıştığını doğrular.
     * Verifies dynamic text updates on button items.
     */
    @Test
    fun verifyDynamicTextUpdate() {
        val bar = SegmentedButtonBar(context)
        bar.setButtonText(0, "Updated Text")

        val button = bar.getButton(0)
        val textView = button?.findViewById<android.widget.TextView>(R.id.sb_item_text)
        assertThat(textView?.text.toString()).isEqualTo("Updated Text")
    }
}
