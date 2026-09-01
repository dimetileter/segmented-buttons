package com.dimetileter.segmentedbuttonbar

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tasarım belirteçlerini (boyutlar, renkler, oranlar ve kaynak isimlendirmeleri) doğrulayan birim test sınıfı.
 * Unit tests for validating design tokens (dimensions, colors, ratios, and naming conventions).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TokenValidationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    /**
     * Eşmerkezli köşe yarıçapı formülünü test eder: button_radius = bar_radius - bar_padding (54dp = 60dp - 6dp).
     * Tests concentric corner radius formula: button_radius = bar_radius - bar_padding (54dp = 60dp - 6dp).
     */
    @Test
    fun verifyConcentricCornerRadiusFormula() {
        val barRadius = context.resources.getDimension(R.dimen.sb_bar_radius)
        val buttonRadius = context.resources.getDimension(R.dimen.sb_button_radius)
        val barPadding = context.resources.getDimension(R.dimen.sb_bar_padding)

        assertThat(buttonRadius).isEqualTo(barRadius - barPadding)
    }

    /**
     * Bar padding ile butonlar arası boşluk (gap) arasındaki 2:1 oranını test eder (3dp = 6dp / 2).
     * Tests the 2:1 ratio between bar padding and button gap (3dp = 6dp / 2).
     */
    @Test
    fun verifyPaddingToGapRatio() {
        val barPadding = context.resources.getDimension(R.dimen.sb_bar_padding)
        val buttonGap = context.resources.getDimension(R.dimen.sb_button_gap)

        assertThat(buttonGap).isEqualTo(barPadding / 2f)
    }

    /**
     * Pill çifti butonlar arası boşluğun bar padding'e eşit olduğunu test eder (6dp == 6dp).
     * Tests that the gap between pill button pair equals bar padding (6dp == 6dp).
     */
    @Test
    fun verifyPillGapEqualsBarPadding() {
        val barPadding = context.resources.getDimension(R.dimen.sb_bar_padding)
        val pillGap = context.resources.getDimension(R.dimen.sb_button_gap_pill)

        assertThat(pillGap).isEqualTo(barPadding)
    }

    /**
     * Tüm boyut belirteçlerinin tanımlı ve geçerli olduğunu doğrular.
     * Verifies that all dimension tokens are properly defined and valid.
     */
    @Test
    fun verifyAllDimensionTokens() {
        val buttonHeight = context.resources.getDimension(R.dimen.sb_button_height)
        val minWidth = context.resources.getDimension(R.dimen.sb_button_min_width)
        val iconSize = context.resources.getDimension(R.dimen.sb_button_icon_size)
        val buttonPadding = context.resources.getDimension(R.dimen.sb_button_padding)
        val iconTextGap = context.resources.getDimension(R.dimen.sb_button_gap_icon_text)
        val verticalWidth = context.resources.getDimension(R.dimen.sb_vertical_button_width)
        val verticalHeight = context.resources.getDimension(R.dimen.sb_vertical_button_height)
        val circularSize = context.resources.getDimension(R.dimen.sb_circular_button_size)

        assertThat(buttonHeight).isGreaterThan(0f)
        assertThat(minWidth).isGreaterThan(0f)
        assertThat(iconSize).isGreaterThan(0f)
        assertThat(buttonPadding).isGreaterThan(0f)
        assertThat(iconTextGap).isGreaterThan(0f)
        assertThat(verticalWidth).isGreaterThan(0f)
        assertThat(verticalHeight).isGreaterThan(0f)
        assertThat(circularSize).isGreaterThan(0f)
    }

    /**
     * Açık tema renk belirteçlerinin doğruluğunu kontrol eder.
     * Verifies light theme color token values.
     */
    @Test
    fun verifyLightModeColors() {
        val selectedColor = context.getColor(R.color.sb_button_selected)
        val unselectedColor = context.getColor(R.color.sb_button_unselected)
        val barBgColor = context.getColor(R.color.sb_bar_background)
        val textColor = context.getColor(R.color.sb_button_text)

        assertThat(selectedColor).isEqualTo(Color.WHITE)
        assertThat(unselectedColor).isEqualTo(Color.TRANSPARENT)
        assertThat(Color.alpha(barBgColor)).isEqualTo(0x3C)
        assertThat(textColor).isNotEqualTo(0)
    }

    /**
     * Kütüphane kaynaklarının 'sb_' veya 'sb' ön eki standartlarına uyduğunu doğrular (Çakışma Önleme Testi - OPTIMIZATION.md §6).
     * Verifies that library resources conform to 'sb_' or 'sb' prefix standards (Collision Prevention - OPTIMIZATION.md §6).
     */
    @Test
    fun verifyResourceNamingConventions() {
        R.dimen::class.java.fields.forEach { field ->
            assertThat(field.name).startsWith("sb_")
        }

        R.color::class.java.fields.forEach { field ->
            assertThat(field.name).startsWith("sb_")
        }

        R.string::class.java.fields.forEach { field ->
            assertThat(field.name).startsWith("sb_")
        }

        R.styleable::class.java.fields
            .filter { it.name.startsWith("SegmentedButtonBar_") }
            .forEach { field ->
                val attrName = field.name.removePrefix("SegmentedButtonBar_")
                assertThat(attrName).startsWith("sb")
            }
    }
}
