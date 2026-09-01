package com.dimetileter.segmentedbuttonbar

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Metin ve İkon odaklı buton düzeni ve ortalama/hizalama birim testleri.
 * Unit tests for text-only, icon-only, and icon+text layouts and alignment behaviors.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TextAndIconLayoutTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        context = ContextThemeWrapper(baseContext, androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
    }

    /**
     * Yalnızca metin içeren butonun ikonsuz ve tam ortalanmış olduğunu doğrular.
     * Verifies text-only button has GONE icon and centered text with 0 marginStart.
     */
    @Test
    fun verifyTextOnlyButtonIsCentered() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "2")
            .addAttribute(R.attr.sbButton1Text, "Text Only")
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        val button = bar.getButton(0)

        val iconView = button?.findViewById<ImageView>(R.id.sb_item_icon)
        val textView = button?.findViewById<TextView>(R.id.sb_item_text)

        assertThat(iconView?.visibility).isEqualTo(View.GONE)
        assertThat(textView?.visibility).isEqualTo(View.VISIBLE)
        assertThat(textView?.text.toString()).isEqualTo("Text Only")
        assertThat(textView?.gravity).isEqualTo(Gravity.CENTER)
        assertThat((textView?.layoutParams as? MarginLayoutParams)?.marginStart).isEqualTo(0)
    }

    /**
     * Yalnızca ikon içeren butonun metinsiz ve görünür ikonlu olduğunu doğrular.
     * Verifies icon-only button has GONE text and VISIBLE icon.
     */
    @Test
    fun verifyIconOnlyButton() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "2")
            .addAttribute(R.attr.sbButton1Icon, "@drawable/ic_sb_arrow_next")
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        val button = bar.getButton(0)

        val iconView = button?.findViewById<ImageView>(R.id.sb_item_icon)
        val textView = button?.findViewById<TextView>(R.id.sb_item_text)

        assertThat(iconView?.visibility).isEqualTo(View.VISIBLE)
        assertThat(textView?.visibility).isEqualTo(View.GONE)
    }

    /**
     * İkon ve metin birlikte verildiğinde ikonun solda, metnin sağında ve aralarında boşluk olduğunu test eder.
     * Tests that icon + text pairs place icon on start and text with marginStart gap.
     */
    @Test
    fun verifyIconAndTextPair() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "1")
            .addAttribute(R.attr.sbButton1Icon, "@drawable/ic_sb_arrow_next")
            .addAttribute(R.attr.sbButton1Text, "Camera")
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        val button = bar.getButton(0)

        val iconView = button?.findViewById<ImageView>(R.id.sb_item_icon)
        val textView = button?.findViewById<TextView>(R.id.sb_item_text)

        assertThat(iconView?.visibility).isEqualTo(View.VISIBLE)
        assertThat(textView?.visibility).isEqualTo(View.VISIBLE)
        val marginStart = (textView?.layoutParams as? MarginLayoutParams)?.marginStart ?: 0
        assertThat(marginStart).isGreaterThan(0)
    }

    /**
     * İkon dinamik olarak kaldırıldığında metnin otomatik olarak ortalandığını test eder.
     * Tests that removing icon dynamically automatically centers the text.
     */
    @Test
    fun verifyDynamicIconRemovalCentersText() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "1")
            .addAttribute(R.attr.sbButton1Icon, "@drawable/ic_sb_arrow_next")
            .addAttribute(R.attr.sbButton1Text, "Camera")
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        val button = bar.getButton(0)
        val textView = button?.findViewById<TextView>(R.id.sb_item_text)

        // İkonu kaldır / Remove icon (iconRes = 0)
        bar.setButtonIcon(0, 0)

        val iconView = button?.findViewById<ImageView>(R.id.sb_item_icon)
        assertThat(iconView?.visibility).isEqualTo(View.GONE)
        assertThat(textView?.gravity).isEqualTo(Gravity.CENTER)
        assertThat((textView?.layoutParams as? MarginLayoutParams)?.marginStart).isEqualTo(0)
    }
}
