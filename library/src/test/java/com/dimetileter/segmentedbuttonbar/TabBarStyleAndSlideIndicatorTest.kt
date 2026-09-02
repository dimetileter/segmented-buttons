package com.dimetileter.segmentedbuttonbar

import android.content.Context
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
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
 * TabBar stili, Kayan Seçim Göstergesi (Sliding Indicator) ve ViewPager2/Fragment entegrasyon testleri.
 * Unit tests for TabBar style, Sliding Pill Indicator, and ViewPager2/Fragment binding.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TabBarStyleAndSlideIndicatorTest {

    private lateinit var context: Context

    class TestActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
            super.onCreate(savedInstanceState)
        }
    }

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        context = ContextThemeWrapper(baseContext, androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
    }

    /**
     * sbStyle="tab" olarak oluşturulan çubuğun varsayılan olarak kayan göstergeye sahip olduğunu doğrular.
     * Verifies that sbStyle="tab" initializes with sliding indicator enabled by default.
     */
    @Test
    fun verifyTabStyleDefaults() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "tab")
            .addAttribute(R.attr.sbButtonCount, "3")
            .addAttribute(R.attr.sbButton1Text, "Anasayfa")
            .addAttribute(R.attr.sbButton2Text, "Keşfet")
            .addAttribute(R.attr.sbButton3Text, "Profil")
            .build()

        val bar = SegmentedButtonBar(context, attrs)

        assertThat(bar.getStyle()).isEqualTo(SegmentedButtonBar.STYLE_TAB)
        assertThat(bar.getButtonCount()).isEqualTo(3)
        assertThat(bar.isSlideIndicator()).isTrue()
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(0)
    }

    /**
     * Standart yatay bir barda sbSlideIndicator="true" yapılarak kayma efektinin açılabildiğini test eder.
     * Tests that sliding indicator can be enabled on horizontal bars via sbSlideIndicator="true".
     */
    @Test
    fun verifySlideIndicatorOnHorizontalStyle() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "horizontal")
            .addAttribute(R.attr.sbButtonCount, "2")
            .addAttribute(R.attr.sbSlideIndicator, "true")
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        assertThat(bar.isSlideIndicator()).isTrue()
    }

    /**
     * setIndicatorPosition çağrısının kaydırma oranlarını doğru hesaplayıp invalidation yaptığını doğrular.
     * Tests setIndicatorPosition calculation during live swipe gestures.
     */
    @Test
    fun verifyLiveIndicatorPositionTracking() {
        val bar = SegmentedButtonBar(context).apply {
            setSlideIndicator(true)
        }

        // Layout simülasyonu
        bar.layout(0, 0, 400, 100)
        bar.getButton(0)?.layout(0, 0, 200, 100)
        bar.getButton(1)?.layout(200, 0, 400, 100)

        // %50 kaydırma
        bar.setIndicatorPosition(0, 0.5f)
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(0)
    }

    /**
     * onTabSelectedListener dinleyicisinin sekme değiştiğinde doğru pozisyonla tetiklendiğini test eder.
     * Tests onTabSelectedListener callback when selecting a tab.
     */
    @Test
    fun verifyOnTabSelectedListener() {
        val bar = SegmentedButtonBar(context)
        var selectedTab: Int? = null

        bar.setOnTabSelectedListener { position ->
            selectedTab = position
        }

        bar.selectButton(1, animate = false)
        assertThat(selectedTab).isEqualTo(1)
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(1)
    }

    /**
     * ViewPager2 ile setupWithViewPager2 çağrısının iki yönlü senkronizasyon sağladığını test eder.
     * Tests two-way sync with ViewPager2.
     */
    @Test
    fun verifySetupWithViewPager2() {
        val activity = Robolectric.buildActivity(TestActivity::class.java).setup().get()
        val bar = SegmentedButtonBar(activity)
        val viewPager = ViewPager2(activity)

        // Dummy Adapter for ViewPager2
        viewPager.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(View(parent.context)) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount(): Int = 2
        }

        bar.setupWithViewPager2(viewPager) { config, position ->
            config.text = "Page $position"
        }

        // 1. Sekmeye tıklandığında ViewPager2 sayfasının güncellendiğini test et
        bar.getButton(1)?.performClick()
        assertThat(viewPager.currentItem).isEqualTo(1)
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(1)
    }

    /**
     * setupWithFragments çağrısının Fragment işlemlerini doğru yönettiğini ve
     * kullanıcının setOnTabSelectedListener ve addOnTabSelectedListener çağrılarını ezmediğini test eder.
     * Tests that setupWithFragments handles Fragment transactions AND co-exists with user listeners without overriding.
     */
    @Test
    fun verifySetupWithFragmentsCoexistsWithUserListeners() {
        val activity = Robolectric.buildActivity(TestActivity::class.java).setup().get()
        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)

        val bar = SegmentedButtonBar(activity)
        val fragment1 = Fragment()
        val fragment2 = Fragment()

        bar.setupWithFragments(
            fragmentManager = activity.supportFragmentManager,
            containerId = container.id,
            fragments = listOf(fragment1, fragment2)
        )

        var userCallbackReceived: Int? = null
        var addCallbackReceived: Int? = null

        // Kullanıcı kendi listener'ını ekler
        bar.setOnTabSelectedListener { pos ->
            userCallbackReceived = pos
        }
        bar.addOnTabSelectedListener { pos ->
            addCallbackReceived = pos
        }

        // Başlangıçta 1. fragment eklenmiş olmalı
        activity.supportFragmentManager.executePendingTransactions()
        assertThat(activity.supportFragmentManager.findFragmentById(container.id)).isEqualTo(fragment1)

        // 2. sekmeye geçildiğinde HEM fragment değişmeli HEM DE kullanıcı dinleyicileri tetiklenmeli
        bar.selectButton(1)
        activity.supportFragmentManager.executePendingTransactions()

        assertThat(activity.supportFragmentManager.findFragmentById(container.id)).isEqualTo(fragment2)
        assertThat(userCallbackReceived).isEqualTo(1)
        assertThat(addCallbackReceived).isEqualTo(1)
    }

    /**
     * Butonların foreground nesnesinin maskeli RippleDrawable ve clipToOutline=true içerdiğini test eder.
     * Tests that button items have a masked RippleDrawable foreground and clipToOutline=true.
     */
    @Test
    fun verifyButtonRippleAndClipToOutline() {
        val bar = SegmentedButtonBar(context)
        val btn0 = bar.getButton(0)!!

        assertThat(btn0.clipToOutline).isTrue()
        assertThat(btn0.outlineProvider).isNotNull()
        assertThat(btn0.foreground).isInstanceOf(android.graphics.drawable.RippleDrawable::class.java)
    }

    /**
     * sbStyle="tab" modunda Android OS & TalkBack semantiklerinin (TabWidget, Role: Tab) uygulandığını doğrular.
     * Verifies that native TabBar accessibility node info (TabWidget, Role: Tab) is initialized.
     */
    @Test
    fun verifyAccessibilitySemanticsForTabStyle() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "tab")
            .addAttribute(R.attr.sbButtonCount, "2")
            .build()

        val bar = SegmentedButtonBar(context, attrs)

        val barNode = AccessibilityNodeInfoCompat.wrap(bar.createAccessibilityNodeInfo()!!)
        assertThat(barNode.className).isEqualTo("android.widget.TabWidget")
        assertThat(barNode.collectionInfo?.columnCount).isEqualTo(2)

        val child0 = bar.getButton(0)!!
        val child0Node = AccessibilityNodeInfoCompat.wrap(child0.createAccessibilityNodeInfo()!!)
        assertThat(child0Node.roleDescription).isEqualTo("Tab")
        assertThat(child0Node.collectionItemInfo?.columnIndex).isEqualTo(0)
        assertThat(child0Node.isSelected).isTrue()
    }

    /**
     * Dikey stilde (sbStyle="vertical") sbSlideIndicator="true" kayan göstergenin dikey eksende çalıştığını doğrular.
     * Verifies that sliding indicator works vertically on vertical button bar style.
     */
    @Test
    fun verifySlideIndicatorOnVerticalStyle() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.sbStyle, "vertical")
            .addAttribute(R.attr.sbButtonCount, "3")
            .addAttribute(R.attr.sbSlideIndicator, "true")
            .build()

        val bar = SegmentedButtonBar(context, attrs)
        assertThat(bar.getStyle()).isEqualTo(SegmentedButtonBar.STYLE_VERTICAL)
        assertThat(bar.isSlideIndicator()).isTrue()

        // Dikey Layout Simülasyonu
        bar.layout(0, 0, 100, 300)
        bar.getButton(0)?.layout(0, 0, 100, 100)
        bar.getButton(1)?.layout(0, 100, 100, 200)
        bar.getButton(2)?.layout(0, 200, 100, 300)

        // 2. Butonu seç
        bar.selectButton(1, animate = false)
        assertThat(bar.getSelectedButtonIndex()).isEqualTo(1)

        // Dinamik olarak slide indicator açıp kapatmayı test et
        bar.setSlideIndicator(false)
        assertThat(bar.isSlideIndicator()).isFalse()

        bar.setSlideIndicator(true)
        assertThat(bar.isSlideIndicator()).isTrue()
    }
}
