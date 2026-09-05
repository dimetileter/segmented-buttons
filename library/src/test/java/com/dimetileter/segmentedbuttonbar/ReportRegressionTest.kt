package com.dimetileter.segmentedbuttonbar

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import androidx.test.core.app.ApplicationProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * REPORTS.md bulgularını kapatan kritik regresyon testleri.
 * Critical regression tests for issues documented in REPORTS.md.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReportRegressionTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        context = ContextThemeWrapper(baseContext, androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
    }

    @Test
    fun lockedPillDoesNotDispatchClickCallbacks() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "pill")
            .addAttribute(R.attr.sbPillActivated, "false")
            .build()
        val bar = SegmentedButtonBar(context, attrs)
        var pillClicked = false
        var buttonClicked = false

        bar.setOnPillClick { pillClicked = true }
        bar.setOnButton1Click { buttonClicked = true }

        bar.getButton(0)?.performClick()
        assertThat(pillClicked).isFalse()
        assertThat(buttonClicked).isFalse()

        bar.setPillActivated(true)
        bar.getButton(0)?.performClick()
        assertThat(pillClicked).isTrue()
        assertThat(buttonClicked).isTrue()
    }

    @Test
    fun selectingButtonDoesNotClearActivationState() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbButtonCount, "3")
            .build()
        val bar = SegmentedButtonBar(context, attrs)

        bar.setAllActivated(true)
        bar.selectButton(2, animate = false)

        assertThat(bar.getSelectedButtonIndex()).isEqualTo(2)
        assertThat(bar.isButtonActivated(0)).isTrue()
        assertThat(bar.isButtonActivated(1)).isTrue()
        assertThat(bar.isButtonActivated(2)).isTrue()
    }

    @Test
    fun setButtonSelectedKeepsSingleSelection() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbButtonCount, "3")
            .build()
        val bar = SegmentedButtonBar(context, attrs)

        bar.setButtonSelected(1, true)

        assertThat(bar.getSelectedButtonIndex()).isEqualTo(1)
        assertThat(bar.isButtonSelected(0)).isFalse()
        assertThat(bar.isButtonSelected(1)).isTrue()
        assertThat(bar.isButtonSelected(2)).isFalse()
    }

    @Test
    fun invalidIndicatorDurationValuesAreClamped() {
        val bar = SegmentedButtonBar(context)

        bar.setIndicatorDuration(-25L)
        assertThat(bar.getIndicatorDuration()).isEqualTo(0L)

        bar.setIndicatorDuration(25_000L)
        assertThat(bar.getIndicatorDuration()).isEqualTo(10_000L)
    }

    @Test
    fun invalidExpandDirectionIsIgnored() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "expandable")
            .addAttribute(R.attr.sbExpandDirection, "end")
            .build()
        val bar = SegmentedButtonBar(context, attrs)

        bar.setExpandDirection(99)

        assertThat(bar.getExpandDirection()).isEqualTo(SegmentedButtonBar.EXPAND_END)
    }

    @Test
    fun unbindViewPagerClearsStoredReferences() {
        val bar = SegmentedButtonBar(context)
        val viewPager = ViewPager2(context)

        bar.setupWithViewPager2(viewPager)
        assertThat(privateField(bar, "registeredViewPagerCallback")).isNotNull()
        assertThat(privateField(bar, "boundViewPager")).isSameInstanceAs(viewPager)

        bar.unbindViewPager2()

        assertThat(privateField(bar, "registeredViewPagerCallback")).isNull()
        assertThat(privateField(bar, "boundViewPager")).isNull()
    }

    @Test
    fun maxWidthAppliesWhenWidthSpecIsUnspecified() {
        val maxWidthPx = dp(120)
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbButtonCount, "6")
            .addAttribute(R.attr.sbMaxWidth, "120dp")
            .addAttribute(R.attr.sbButton1Text, "One")
            .addAttribute(R.attr.sbButton2Text, "Two")
            .addAttribute(R.attr.sbButton3Text, "Three")
            .addAttribute(R.attr.sbButton4Text, "Four")
            .addAttribute(R.attr.sbButton5Text, "Five")
            .addAttribute(R.attr.sbButton6Text, "Six")
            .build()
        val bar = SegmentedButtonBar(context, attrs)

        bar.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        assertThat(bar.measuredWidth).isAtMost(maxWidthPx)
    }

    @Test
    fun runtimeExpandableTextUpdateIsUsedByCollapsedAnchor() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "expandable")
            .addAttribute(R.attr.sbButtonCount, "3")
            .addAttribute(R.attr.sbButton1Text, "One")
            .addAttribute(R.attr.sbButton2Text, "Two")
            .addAttribute(R.attr.sbButton3Text, "Three")
            .build()
        val bar = SegmentedButtonBar(context, attrs)

        bar.setButtonText(1, "Updated")
        bar.selectButton(1, animate = false)

        assertThat(bar.getButton(0)?.contentDescription.toString()).isEqualTo("Updated")
    }

    @Test
    fun setButtonIconKeepsPerButtonTint() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "expandable")
            .addAttribute(R.attr.sbButtonCount, "2")
            .build()
        val bar = SegmentedButtonBar(context, attrs)
        val redTint = ColorStateList.valueOf(Color.RED)

        bar.setButtonIconTint(1, redTint)
        bar.setButtonIcon(1, R.drawable.ic_sb_arrow_back)

        val iconView = bar.getButton(1)?.findViewById<ImageView>(R.id.sb_circular_icon)
        assertThat(iconView).isNotNull()
        assertThat(ImageViewCompat.getImageTintList(iconView!!)).isEqualTo(redTint)
    }

    @Test
    fun onDetachedFromWindowClearsViewPagerCallbacksAndCancelsAnimations() {
        val bar = SegmentedButtonBar(context)
        val viewPager = ViewPager2(context)

        bar.setupWithViewPager2(viewPager)
        assertThat(privateField(bar, "registeredViewPagerCallback")).isNotNull()
        assertThat(privateField(bar, "boundViewPager")).isSameInstanceAs(viewPager)

        // Invoke protected onDetachedFromWindow
        val detachMethod = View::class.java.getDeclaredMethod("onDetachedFromWindow")
        detachMethod.isAccessible = true
        detachMethod.invoke(bar)

        assertThat(privateField(bar, "registeredViewPagerCallback")).isNull()
        assertThat(privateField(bar, "boundViewPager")).isNull()
        assertThat(privateField(bar, "internalFragmentTabListener")).isNull()
        assertThat(privateField(bar, "isAnimating")).isEqualTo(false)
    }

    @Test
    fun lockedPillAppliesDisabledVisualState() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "pill")
            .addAttribute(R.attr.sbPillActivated, "false")
            .build()
        val bar = SegmentedButtonBar(context, attrs)
        val pill = bar.getButton(0)

        assertThat(pill).isNotNull()
        assertThat(bar.isPillActivated()).isFalse()
        assertThat(pill!!.isActivated).isFalse()
        assertThat(pill.background).isNotNull()

        bar.setPillActivated(true)
        assertThat(bar.isPillActivated()).isTrue()
        assertThat(pill.isActivated).isTrue()
    }

    private fun privateField(target: Any, fieldName: String): Any? {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(target)
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}
