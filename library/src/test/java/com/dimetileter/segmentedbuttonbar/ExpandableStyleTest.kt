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
import org.robolectric.shadows.ShadowLooper

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

    /**
     * sbCollapseOnSelect=true olduğunda, genişleme sonrası seçilen butonun ikonu ile otomatik daraldığını test eder.
     * Tests auto-collapse on selection where the selected button icon becomes visible on the collapsed anchor.
     */
    @Test
    fun verifyCollapseOnSelectUpdatesAnchorAndCollapses() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "expandable")
            .addAttribute(R.attr.sbButtonCount, "3")
            .addAttribute(R.attr.sbCollapseOnSelect, "true")
            .addAttribute(R.attr.sbButton1Icon, "@drawable/ic_sb_arrow_next")
            .addAttribute(R.attr.sbButton2Icon, "@drawable/ic_sb_arrow_back")
            .addAttribute(R.attr.sbButton3Icon, "@drawable/ic_sb_arrow_next")
            .addAttribute(R.attr.sbButton3ContentDescription, "Dollar Action")
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        assertThat(bar.isCollapseOnSelect()).isTrue()

        // 1. Menüyü genişlet
        bar.expand(animate = false)
        assertThat(bar.isExpanded()).isTrue()

        // 2. 3. butona (index 2) tıkla
        bar.getButton(2)?.performClick()
        ShadowLooper.idleMainLooper()

        // 3. Otomatik daralma gerçekleşmeli
        assertThat(bar.isExpanded()).isFalse()
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(2)

        // 4. Görünür kalan ana (anchor) buton artık 3. butonun özelliklerini taşımalı
        val anchorView = bar.getButton(0)
        assertThat(anchorView?.contentDescription?.toString()).isEqualTo("Dollar Action")
        assertThat(anchorView?.isSelected).isTrue()

        // 5. Tekrar açıldığında (expand), tüm butonlar görünür olmalı ve 1. buton orijinaline dönmeli!
        bar.expand(animate = false)
        assertThat(bar.isExpanded()).isTrue()
        assertThat(bar.getButton(0)?.visibility).isEqualTo(View.VISIBLE)
        assertThat(bar.getButton(1)?.visibility).isEqualTo(View.VISIBLE)
        assertThat(bar.getButton(2)?.visibility).isEqualTo(View.VISIBLE)
        assertThat(bar.getButton(2)?.contentDescription?.toString()).isEqualTo("Dollar Action")
        assertThat(bar.getButton(2)?.isSelected).isTrue()
    }

    /**
     * 5 butonlu menüde (Örn: Dur, Güneş, Yaprak, Çiçek, Kar) başka bir buton seçilip daraldıktan
     * sonra tekrar açıldığında 1. butonun ("Dur") kaybolmadığını ve tekrar seçilebildiğini test eder.
     * Verifies that all buttons remain present, visible, and selectable across multiple expand/collapse cycles.
     */
    @Test
    fun verifyFiveButtonSeasonPickerPreservesFirstButton() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "expandable")
            .addAttribute(R.attr.sbButtonCount, "5")
            .addAttribute(R.attr.sbCollapseOnSelect, "true")
            .addAttribute(R.attr.sbButton1Icon, "@drawable/ic_sb_arrow_next")
            .addAttribute(R.attr.sbButton1ContentDescription, "Dur")
            .addAttribute(R.attr.sbButton2Icon, "@drawable/ic_sb_arrow_back")
            .addAttribute(R.attr.sbButton2ContentDescription, "Gunes")
            .addAttribute(R.attr.sbButton3Icon, "@drawable/ic_sb_arrow_next")
            .addAttribute(R.attr.sbButton3ContentDescription, "Yaprak")
            .addAttribute(R.attr.sbButton4Icon, "@drawable/ic_sb_arrow_back")
            .addAttribute(R.attr.sbButton4ContentDescription, "Cicek")
            .addAttribute(R.attr.sbButton5Icon, "@drawable/ic_sb_arrow_next")
            .addAttribute(R.attr.sbButton5ContentDescription, "Kar")
            .build()

        val bar = SegmentedButtonBar(context, attrs)

        // Başlangıç: Bar kapalı, 0. buton (Dur) seçili ve görünüyor
        assertThat(bar.isExpanded()).isFalse()
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(0)
        assertThat(bar.getButton(0)?.contentDescription?.toString()).isEqualTo("Dur")

        // 1. Kullanıcı tıklar -> Bar açılır
        bar.getButton(0)?.performClick()
        ShadowLooper.idleMainLooper()
        assertThat(bar.isExpanded()).isTrue()
        // Tüm 5 buton açık ve görünür olmalı
        for (i in 0 until 5) {
            assertThat(bar.getButton(i)?.visibility).isEqualTo(View.VISIBLE)
        }
        assertThat(bar.getButton(0)?.contentDescription?.toString()).isEqualTo("Dur")
        assertThat(bar.getButton(4)?.contentDescription?.toString()).isEqualTo("Kar")

        // 2. Kullanıcı "Kar" butonunu (index 4) seçer -> Bar daralır
        bar.getButton(4)?.performClick()
        ShadowLooper.idleMainLooper()
        assertThat(bar.isExpanded()).isFalse()
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(4)
        // Daralmış barda görünen buton artık "Kar" görselini taşır
        assertThat(bar.getButton(0)?.contentDescription?.toString()).isEqualTo("Kar")

        // 3. Kullanıcı tekrar tıklar -> Bar tekrar açılır
        bar.getButton(0)?.performClick()
        ShadowLooper.idleMainLooper()
        assertThat(bar.isExpanded()).isTrue()

        // 4. KRİTİK KONTROL: 1. buton ("Dur") kaybolmamış olmalı, orijinal görseliyle yerinde durmalı!
        assertThat(bar.getButton(0)?.visibility).isEqualTo(View.VISIBLE)
        assertThat(bar.getButton(0)?.contentDescription?.toString()).isEqualTo("Dur")
        assertThat(bar.getButton(4)?.visibility).isEqualTo(View.VISIBLE)
        assertThat(bar.getButton(4)?.contentDescription?.toString()).isEqualTo("Kar")
        assertThat(bar.getButton(4)?.isSelected).isTrue()

        // 5. Kullanıcı tekrar "Dur" butonunu (index 0) seçebilir!
        bar.getButton(0)?.performClick()
        ShadowLooper.idleMainLooper()
        assertThat(bar.isExpanded()).isFalse()
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(0)
        assertThat(bar.getButton(0)?.contentDescription?.toString()).isEqualTo("Dur")
    }

    /**
     * Genişletme esnasında (menü kapalıyken tıklama yapıldığında) kayıtlı buton tıklama dinleyicilerinin
     * (click listener) tetiklenmediğini ve LiveData/ViewModel observer döngüsü olsa dahi
     * 1. butonun görselinin seçili buton görseliyle ezilmediğini test eder.
     */
    @Test
    fun verifyExpandDoesNotTriggerButtonClickListenersOrOverwriteFirstButton() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "expandable")
            .addAttribute(R.attr.sbButtonCount, "4")
            .addAttribute(R.attr.sbCollapseOnSelect, "true")
            .addAttribute(R.attr.sbButton1Icon, "@drawable/ic_sb_arrow_next")
            .addAttribute(R.attr.sbButton1ContentDescription, "Gunes")
            .addAttribute(R.attr.sbButton2Icon, "@drawable/ic_sb_arrow_back")
            .addAttribute(R.attr.sbButton2ContentDescription, "Yaprak")
            .addAttribute(R.attr.sbButton3Icon, "@drawable/ic_sb_arrow_next")
            .addAttribute(R.attr.sbButton3ContentDescription, "Kar")
            .addAttribute(R.attr.sbButton4Icon, "@drawable/ic_sb_arrow_back")
            .addAttribute(R.attr.sbButton4ContentDescription, "Cicek")
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        var winterListenerCalled = false

        bar.setOnButtonClick(2) {
            winterListenerCalled = true
            // Gerçek MVVM senaryosu: ViewModel güncellenir ve geri selectButton çağrılır
            bar.selectButton(2)
        }

        // Başlangıçta hava tahminiyle Kar (index 2) seçilsin
        bar.selectButton(2)
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(2)
        // Kapalı durumda anchor buton (0) Kar görselini gösterir
        assertThat(bar.getButton(0)?.contentDescription?.toString()).isEqualTo("Kar")

        // Kullanıcı menüyü açmak için tıklar -> Bar açılır
        bar.getButton(0)?.performClick()
        ShadowLooper.idleMainLooper()

        // KRİTİK: Menü açılırken Kar dinleyicisi tetiklenmemelidir!
        assertThat(winterListenerCalled).isFalse()
        assertThat(bar.isExpanded()).isTrue()

        // KRİTİK: Açıldığında 1. buton (index 0) Gunes olmalı, Kar olarak kalmamalıdır!
        assertThat(bar.getButton(0)?.contentDescription?.toString()).isEqualTo("Gunes")
        assertThat(bar.getButton(1)?.contentDescription?.toString()).isEqualTo("Yaprak")
        assertThat(bar.getButton(2)?.contentDescription?.toString()).isEqualTo("Kar")
        assertThat(bar.getButton(2)?.isSelected).isTrue()
        assertThat(bar.getButton(3)?.contentDescription?.toString()).isEqualTo("Cicek")
    }

    /**
     * 4 farklı genişleme yönünün (end/start/down/up) ve dinamik yön değişiminin doğru çalıştığını test eder.
     * Tests all 4 expansion directions (end, start, down, up) and dynamic direction updates.
     */
    @Test
    fun verifyExpandDirectionsAndDynamicChanges() {
        // 1. Dikey Aşağıya Doğru Genişleme (Down)
        val downAttrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "expandable")
            .addAttribute(R.attr.sbButtonCount, "3")
            .addAttribute(R.attr.sbExpandDirection, "down")
            .build()
        val downBar = SegmentedButtonBar(context, downAttrs)
        assertThat(downBar.getExpandDirection()).isEqualTo(SegmentedButtonBar.EXPAND_DOWN)
        assertThat(downBar.orientation).isEqualTo(android.widget.LinearLayout.VERTICAL)

        // 2. Dikey Yukarıya Doğru Genişleme (Up)
        val upAttrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "expandable")
            .addAttribute(R.attr.sbButtonCount, "3")
            .addAttribute(R.attr.sbExpandDirection, "up")
            .build()
        val upBar = SegmentedButtonBar(context, upAttrs)
        assertThat(upBar.getExpandDirection()).isEqualTo(SegmentedButtonBar.EXPAND_UP)
        assertThat(upBar.orientation).isEqualTo(android.widget.LinearLayout.VERTICAL)
        // Reverse layout kontrolü: Anchor buton en altta olmalı
        assertThat(upBar.getChildAt(upBar.childCount - 1)).isEqualTo(upBar.getButton(0))

        // 3. Sola Doğru Genişleme (Start / Left)
        val startAttrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "expandable")
            .addAttribute(R.attr.sbButtonCount, "3")
            .addAttribute(R.attr.sbExpandDirection, "start")
            .build()
        val startBar = SegmentedButtonBar(context, startAttrs)
        assertThat(startBar.getExpandDirection()).isEqualTo(SegmentedButtonBar.EXPAND_START)
        assertThat(startBar.orientation).isEqualTo(android.widget.LinearLayout.HORIZONTAL)
        // Reverse layout kontrolü: Anchor buton en sağda olmalı
        assertThat(startBar.getChildAt(startBar.childCount - 1)).isEqualTo(startBar.getButton(0))

        // 4. Dinamik setExpandDirection
        startBar.setExpandDirection(SegmentedButtonBar.EXPAND_UP)
        assertThat(startBar.getExpandDirection()).isEqualTo(SegmentedButtonBar.EXPAND_UP)
        assertThat(startBar.orientation).isEqualTo(android.widget.LinearLayout.VERTICAL)
    }
}
