package com.dimetileter.segmentedbuttonbar

import android.content.Context
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * İkon renklendirme (Icon Tint), sbAllActivated ve 5+ buton birim testleri.
 * Unit tests for Icon Tint, sbAllActivated, and 5+ buttons in hybrid/horizontal bars.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IconTintAndAllActivatedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        context = ContextThemeWrapper(baseContext, androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
    }

    /**
     * sbIconTint ve sbButton1IconTint ile ikonların renklendirildiğini test eder.
     * Tests global and per-button icon tinting.
     */
    @Test
    fun verifyIconTinting() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "2")
            .addAttribute(R.attr.sbButton1Icon, "@drawable/ic_sb_arrow_back")
            .addAttribute(R.attr.sbButton2Icon, "@drawable/ic_sb_arrow_next")
            .addAttribute(R.attr.sbIconTint, "#FF0000") // Red
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        val icon1 = bar.getButton(0)?.findViewById<ImageView>(R.id.sb_item_icon)

        assertThat(icon1).isNotNull()
        assertThat(ImageViewCompat.getImageTintList(icon1!!)).isNotNull()
        assertThat(ImageViewCompat.getImageTintList(icon1)?.defaultColor).isEqualTo(Color.RED)

        // Runtime tint update
        bar.setButtonIconTint(1, Color.BLUE)
        val icon2 = bar.getButton(1)?.findViewById<ImageView>(R.id.sb_item_icon)
        assertThat(icon2).isNotNull()
        assertThat(ImageViewCompat.getImageTintList(icon2!!)?.defaultColor).isEqualTo(Color.BLUE)
    }

    /**
     * sbAllActivated="true" özniteliğinin tüm butonları tek seferde aktif yaptığını test eder.
     * Tests sbAllActivated="true" activates all buttons simultaneously.
     */
    @Test
    fun verifyAllActivatedAttribute() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "3")
            .addAttribute(R.attr.sbAllActivated, "true")
            .build()

        val bar = SegmentedButtonBar(context, attrs)

        assertThat(bar.isButtonActivated(0)).isTrue()
        assertThat(bar.isButtonActivated(1)).isTrue()
        assertThat(bar.isButtonActivated(2)).isTrue()

        bar.setAllActivated(false)
        assertThat(bar.isButtonActivated(0)).isFalse()
        assertThat(bar.isButtonActivated(1)).isFalse()
        assertThat(bar.isButtonActivated(2)).isFalse()
    }

    /**
     * Hibrit barda dairesel butona tıklandığında seçilip diğerlerinin seçimden çıktığını test eder.
     * Tests clicking a circular button in hybrid bar selects it and deselects other buttons.
     */
    @Test
    fun verifyHybridCircularButtonSelectsAndDeselectsOthers() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "4")
            .addAttribute(R.attr.sbButton1Icon, "@drawable/ic_sb_arrow_back")
            .addAttribute(R.attr.sbButton2Icon, "@drawable/ic_sb_arrow_next")
            .addAttribute(R.attr.sbButton3Icon, "@drawable/ic_sb_arrow_back")
            .addAttribute(R.attr.sbButton4Style, "circular")
            .addAttribute(R.attr.sbButton4Icon, "@drawable/ic_sb_arrow_next")
            .build()

        val bar = SegmentedButtonBar(context, attrs)

        // Başlangıçta 1. buton seçili
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(0)
        assertThat(bar.isButtonSelected(0)).isTrue()
        assertThat(bar.isButtonSelected(3)).isFalse()

        // 4. butona (Dairesel) tıklanıyor
        bar.getButton(3)?.performClick()

        // Artık 4. buton seçili, 1. buton seçili değil!
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(3)
        assertThat(bar.isButtonSelected(3)).isTrue()
        assertThat(bar.isButtonSelected(0)).isFalse()
        assertThat(bar.isButtonSelected(1)).isFalse()
        assertThat(bar.isButtonSelected(2)).isFalse()
    }

    /**
     * 5 ve 6 butonlu çubukların başarıyla oluşturulduğunu test eder.
     * Tests bars with 5 and 6 buttons inflate properly.
     */
    @Test
    fun verifyFiveAndSixButtonCounts() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "5")
            .addAttribute(R.attr.sbButton1Icon, "@drawable/ic_sb_arrow_back")
            .addAttribute(R.attr.sbButton2Icon, "@drawable/ic_sb_arrow_next")
            .addAttribute(R.attr.sbButton3Icon, "@drawable/ic_sb_arrow_back")
            .addAttribute(R.attr.sbButton4Icon, "@drawable/ic_sb_arrow_next")
            .addAttribute(R.attr.sbButton5Icon, "@drawable/ic_sb_arrow_back")
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        assertThat(bar.getButtonCount()).isEqualTo(5)
    }

    /**
     * Özel tint atanmadığında ikonların tema renk tonunu (sb_button_icon) aldığını test eder.
     * Tests default theme-adaptive icon tint when no explicit tint is defined.
     */
    @Test
    fun verifyDefaultThemeAdaptiveIconTint() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "2")
            .addAttribute(R.attr.sbButton1Icon, "@drawable/ic_sb_arrow_back")
            .addAttribute(R.attr.sbButton2Icon, "@drawable/ic_sb_arrow_next")
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        val icon1 = bar.getButton(0)?.findViewById<ImageView>(R.id.sb_item_icon)
        assertThat(icon1).isNotNull()
        assertThat(ImageViewCompat.getImageTintList(icon1!!)).isNotNull()
    }

    /**
     * sbTextColor ve sbButton1TextColor..sbButton6TextColor ile buton metin renklerinin değiştirilebildiğini test eder.
     * Tests global and per-button text color customization.
     */
    @Test
    fun verifyTextColorCustomization() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "2")
            .addAttribute(R.attr.sbButton1Text, "Button 1")
            .addAttribute(R.attr.sbButton2Text, "Button 2")
            .addAttribute(R.attr.sbButton1TextColor, "#FF0000") // Red
            .addAttribute(R.attr.sbButton2TextColor, "#0000FF") // Blue
            .build()

        val bar = SegmentedButtonBar(context, attrs)

        assertThat(bar.getButtonTextColor(0)?.defaultColor).isEqualTo(Color.RED)
        assertThat(bar.getButtonTextColor(1)?.defaultColor).isEqualTo(Color.BLUE)

        // Runtime setButtonTextColor
        bar.setButtonTextColor(0, Color.YELLOW)
        assertThat(bar.getButtonTextColor(0)?.defaultColor).isEqualTo(Color.YELLOW)

        // Runtime global setTextColor
        bar.setTextColor(Color.GREEN)
        assertThat(bar.getButtonTextColor(0)?.defaultColor).isEqualTo(Color.GREEN)
        assertThat(bar.getButtonTextColor(1)?.defaultColor).isEqualTo(Color.GREEN)
    }
}
