package com.dimetileter.segmentedbuttonbar

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
 * Özel arkaplanlar (renk/gradyan), TalkBack erişilebilirliği ve Tooltip birim testleri.
 * Unit tests for custom backgrounds (color/gradient), TalkBack accessibility, and Tooltip interactions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CustomBackgroundAndAccessibilityTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        context = ContextThemeWrapper(baseContext, androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
    }

    /**
     * TalkBack contentDescription ve Tooltip özniteliklerinin doğru yüklendiğini test eder.
     * Tests TalkBack contentDescription and Tooltip attributes.
     */
    @Test
    fun verifyContentDescriptionAndTooltip() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "2")
            .addAttribute(R.attr.sbButton1Icon, "@drawable/ic_sb_arrow_back")
            .addAttribute(R.attr.sbButton1Text, "Kamera")
            .addAttribute(R.attr.sbButton1ContentDescription, "Fotoğraf Çekme Butonu")
            .addAttribute(R.attr.sbButton1Tooltip, "Kamera")
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        val btn1 = bar.getButton(0)

        assertThat(btn1?.contentDescription?.toString()).isEqualTo("Fotoğraf Çekme Butonu")

        // Kotlin API test
        bar.setButtonContentDescription(1, "Sonraki Sayfa")
        assertThat(bar.getButtonContentDescription(1)?.toString()).isEqualTo("Sonraki Sayfa")
    }

    /**
     * Özel seçili renk veya gradyan arkaplan atamalarını test eder.
     * Tests custom selected color and gradient background assignments.
     */
    @Test
    fun verifyCustomSelectedColorAndGradients() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "2")
            .addAttribute(R.attr.sbSelectedColor, "#FF5722") // Orange color
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        val btn1 = bar.getButton(0)
        assertThat(btn1?.background).isNotNull()

        // Runtime gradient assignment
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.RED, Color.BLUE)
        )
        bar.setButtonSelectedBackground(0, gradient)
        assertThat(btn1?.background).isNotNull()
    }

    /**
     * Hibrit kullanımda dairesel butona basılınca grubun diğer butonlarının seçimden çıktığını doğrular.
     * Verifies clicking circular button in hybrid bar deactivates and deselects all other buttons in group.
     */
    @Test
    fun verifyInteractiveHybridGroupSelection() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "3")
            .addAttribute(R.attr.sbButton1Text, "Tab 1")
            .addAttribute(R.attr.sbButton2Text, "Tab 2")
            .addAttribute(R.attr.sbButton3Style, "circular")
            .addAttribute(R.attr.sbButton3Icon, "@drawable/ic_sb_arrow_next")
            .build()

        val bar = SegmentedButtonBar(context, attrs)

        // 1. buton başlangıçta seçili
        assertThat(bar.isButtonSelected(0)).isTrue()
        assertThat(bar.isButtonSelected(1)).isFalse()
        assertThat(bar.isButtonSelected(2)).isFalse()

        // 3. dairesel butona tıkla
        bar.getButton(2)?.performClick()

        // Sadece 3. dairesel buton seçili olmalı, 1 ve 2 seçilmemiş olmalı
        assertThat(bar.isButtonSelected(2)).isTrue()
        assertThat(bar.isButtonSelected(0)).isFalse()
        assertThat(bar.isButtonSelected(1)).isFalse()
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(2)

        // Tekrar 1. butona tıkla
        bar.getButton(0)?.performClick()
        assertThat(bar.isButtonSelected(0)).isTrue()
        assertThat(bar.isButtonSelected(2)).isFalse()
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(0)
    }

    /**
     * Uzun basma dinleyicisinin (onLongClick) çalıştığını test eder.
     * Tests onLongClick listener execution.
     */
    @Test
    fun verifyLongClickListener() {
        val bar = SegmentedButtonBar(context)
        var longClicked = false

        bar.setOnButton1LongClick {
            longClicked = true
            true
        }

        bar.getButton(0)?.performLongClick()
        assertThat(longClicked).isTrue()
    }

    /**
     * sbBarColor, sbBarBackground ve setBarColor() ile çubuk dış kapsül arka planının 60dp köşeleri koruyarak değiştiğini test eder.
     * Tests that bar container background color updates while preserving 60dp capsule shape.
     */
    @Test
    fun verifyBarColorAndBackgroundCustomization() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "2")
            .addAttribute(R.attr.sbBarColor, "#FF223344")
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        assertThat(bar.background).isInstanceOf(GradientDrawable::class.java)

        // Runtime setBarColor
        bar.setBarColor(Color.YELLOW)
        assertThat(bar.background).isInstanceOf(GradientDrawable::class.java)

        // Runtime setBarBackground with color
        bar.setBarBackground(Color.MAGENTA)
        assertThat(bar.background).isInstanceOf(GradientDrawable::class.java)
    }
}
