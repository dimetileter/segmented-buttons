package com.dimetileter.segmentedbuttonbar

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewOutlineProvider
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.viewpager2.widget.ViewPager2

/**
 * SegmentedButtonBar — Özelleştirilebilir, hibrit stilleri, ikon ve arkaplan renklendirmesini (Icon Tint, Gradient/Color BG),
 * TalkBack erişilebilirliğini, Tooltip (uzun basma) desteğini, animasyonlu genişleyen/daralan (Expandable) menü yönetimini,
 * modern kayan göstergeli TabBar (`sbStyle="tab"`) ve ViewPager2 / Fragment çift yönlü entegrasyonunu destekleyen Segmented Buton bileşeni.
 *
 * SegmentedButtonBar — Customizable, dynamic Segmented Button component with hybrid styles,
 * icon tinting, custom gradients/colors, TalkBack accessibility, Tooltip support,
 * animated Expandable menu, modern sliding pill TabBar (`sbStyle="tab"`), and seamless ViewPager2 / Fragment two-way integration.
 */
class SegmentedButtonBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        // Çubuk Stil Sabitleri / Bar Style Constants
        const val STYLE_HORIZONTAL = 0
        const val STYLE_VERTICAL = 1
        const val STYLE_CIRCULAR = 2
        const val STYLE_PILL = 3
        const val STYLE_EXPANDABLE = 4
        const val STYLE_TAB = 5

        // Buton Bazlı Stil Sabitleri / Per-Button Style Constants
        const val BUTTON_STYLE_HORIZONTAL = 0
        const val BUTTON_STYLE_CIRCULAR = 1
        const val BUTTON_STYLE_PILL = 2

        // Pill Türü Sabitleri / Pill Type Constants
        const val PILL_NEXT = 0
        const val PILL_BACK = 1
        const val PILL_TEXT = 2

        // Genişleme Yönü Sabitleri / Expand Direction Constants
        const val EXPAND_END = 0
        const val EXPAND_START = 1

        private const val MIN_BUTTON_COUNT = 1
        private const val MAX_BUTTON_COUNT = 6
        private const val DEFAULT_BUTTON_COUNT = 2
        private const val DEFAULT_VERTICAL_COUNT = 3
        private const val DEFAULT_EXPANDABLE_COUNT = 3
        private const val EXPAND_ANIMATION_DURATION_MS = 200L
        private const val COLLAPSE_ANIMATION_DURATION_MS = 100L
        private const val DEFAULT_INDICATOR_DURATION_MS = 250L
    }

    class TabConfig {
        var text: CharSequence? = null
        var iconRes: Int = 0
        var iconTint: Int? = null
        var tooltip: CharSequence? = null
        var contentDescription: CharSequence? = null
    }

    private data class ButtonMeta(
        val iconRes: Int,
        val text: String?,
        val iconTint: ColorStateList?,
        val contentDescription: String?,
        val tooltip: String?,
        val customBg: Drawable?,
        val selectedBg: Drawable?,
        val selectedColor: Int?
    )

    private var currentStyle: Int = STYLE_HORIZONTAL
    private var buttonCount: Int = DEFAULT_BUTTON_COUNT
    private var autoSelect: Boolean = true
    private var autoTooltip: Boolean = true
    private var pillType: Int = PILL_NEXT
    private var expandDirection: Int = EXPAND_END
    private var collapseOnSelect: Boolean = false
    private var slideIndicator: Boolean = false
    private var indicatorDurationMs: Long = DEFAULT_INDICATOR_DURATION_MS
    private var maxWidthPx: Int = -1

    private var globalIconTint: ColorStateList? = null
    private var globalAllActivated: Boolean? = null
    private var globalSelectedBackground: Drawable? = null
    private var globalSelectedColor: Int? = null
    private var showUnselectedBackground: Boolean = false
    private var unselectedButtonColor: Int? = null
    private var globalRippleColor: Int? = null

    // Kayan Gösterge (Sliding Indicator) Çizim Durumları
    private var indicatorLeft: Float = 0f
    private var indicatorRight: Float = 0f
    private var isIndicatorPositionInitialized: Boolean = false
    private var indicatorAnimator: ValueAnimator? = null
    private var indicatorDrawable: Drawable? = null

    private var isExpanded: Boolean = false
    private var isAnimating: Boolean = false
    private var onExpandChangeListener: ((Boolean) -> Unit)? = null
    private var internalFragmentTabListener: ((Int) -> Unit)? = null
    private var userTabSelectedListener: ((Int) -> Unit)? = null
    private val customTabSelectedListeners = mutableListOf<(Int) -> Unit>()

    private val buttonViews = mutableListOf<View>()
    private val buttonMetaList = mutableListOf<ButtonMeta>()
    private val buttonClickListeners = mutableMapOf<Int, () -> Unit>()
    private val buttonLongClickListeners = mutableMapOf<Int, () -> Boolean>()
    private var pillClickListener: (() -> Unit)? = null
    private var selectedIndex: Int = 0

    // ViewPager2 Dinleyicisi Referansı
    private var registeredViewPagerCallback: ViewPager2.OnPageChangeCallback? = null
    private var boundViewPager: ViewPager2? = null

    init {
        setWillNotDraw(false)
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SegmentedButtonBar, defStyleAttr, 0)
        try {
            readAttributesAndSetup(ta)
        } finally {
            ta.recycle()
        }
    }

    /**
     * XML özniteliklerini okur ve uygun stil düzenini hazırlar.
     * Reads XML attributes and sets up the appropriate style layout.
     */
    private fun readAttributesAndSetup(ta: TypedArray) {
        currentStyle = ta.getInt(R.styleable.SegmentedButtonBar_sbStyle, STYLE_HORIZONTAL)
        val defaultCount = when (currentStyle) {
            STYLE_VERTICAL -> DEFAULT_VERTICAL_COUNT
            STYLE_EXPANDABLE -> DEFAULT_EXPANDABLE_COUNT
            else -> DEFAULT_BUTTON_COUNT
        }
        val rawCount = ta.getInt(R.styleable.SegmentedButtonBar_sbButtonCount, defaultCount)
        buttonCount = rawCount.coerceIn(MIN_BUTTON_COUNT, MAX_BUTTON_COUNT)
        autoSelect = ta.getBoolean(R.styleable.SegmentedButtonBar_sbAutoSelect, true)
        autoTooltip = ta.getBoolean(R.styleable.SegmentedButtonBar_sbAutoTooltip, true)
        pillType = ta.getInt(R.styleable.SegmentedButtonBar_sbPillType, PILL_NEXT)
        expandDirection = ta.getInt(R.styleable.SegmentedButtonBar_sbExpandDirection, EXPAND_END)
        collapseOnSelect = ta.getBoolean(R.styleable.SegmentedButtonBar_sbCollapseOnSelect, false)
        maxWidthPx = ta.getDimensionPixelSize(R.styleable.SegmentedButtonBar_sbMaxWidth, -1)

        val defaultSlide = (currentStyle == STYLE_TAB)
        slideIndicator = ta.getBoolean(R.styleable.SegmentedButtonBar_sbSlideIndicator, defaultSlide)
        indicatorDurationMs = ta.getInt(R.styleable.SegmentedButtonBar_sbIndicatorDuration, DEFAULT_INDICATOR_DURATION_MS.toInt()).toLong()

        globalIconTint = ta.getColorStateList(R.styleable.SegmentedButtonBar_sbIconTint)
        if (ta.hasValue(R.styleable.SegmentedButtonBar_sbAllActivated)) {
            globalAllActivated = ta.getBoolean(R.styleable.SegmentedButtonBar_sbAllActivated, false)
        }

        globalSelectedBackground = ta.getDrawable(R.styleable.SegmentedButtonBar_sbSelectedBackground)
        if (ta.hasValue(R.styleable.SegmentedButtonBar_sbSelectedColor)) {
            globalSelectedColor = ta.getColor(R.styleable.SegmentedButtonBar_sbSelectedColor, Color.WHITE)
        }

        showUnselectedBackground = ta.getBoolean(R.styleable.SegmentedButtonBar_sbShowUnselectedBackground, false)
        if (ta.hasValue(R.styleable.SegmentedButtonBar_sbUnselectedButtonColor)) {
            unselectedButtonColor = ta.getColor(R.styleable.SegmentedButtonBar_sbUnselectedButtonColor, Color.TRANSPARENT)
        }

        if (ta.hasValue(R.styleable.SegmentedButtonBar_sbRippleColor)) {
            globalRippleColor = ta.getColor(R.styleable.SegmentedButtonBar_sbRippleColor, 0)
        }

        val customBarBg = ta.getDrawable(R.styleable.SegmentedButtonBar_sbBarBackground)
        if (customBarBg != null) {
            background = customBarBg
        } else {
            background = ContextCompat.getDrawable(context, R.drawable.bg_segmented_button_bar)
        }

        val elevationVal = ta.getDimension(R.styleable.SegmentedButtonBar_sbElevation, 0f)
        if (elevationVal > 0f) {
            elevation = elevationVal
            translationZ = elevationVal
        }

        when (currentStyle) {
            STYLE_HORIZONTAL, STYLE_TAB -> setupHorizontal(ta)
            STYLE_VERTICAL -> setupVertical(ta)
            STYLE_CIRCULAR -> setupCircular(ta)
            STYLE_PILL -> setupPill(ta)
            STYLE_EXPANDABLE -> setupExpandable(ta)
            else -> setupHorizontal(ta)
        }

        setupAccessibilitySemantics()
    }

    /**
     * Android OS ve TalkBack ekran okuyucusu için TabBar semantiklerini yapılandırır.
     * Configures native TabBar semantics for Android OS and TalkBack accessibility services.
     */
    private fun setupAccessibilitySemantics() {
        if (isInEditMode || currentStyle != STYLE_TAB) return
        ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = "android.widget.TabWidget"
                    info.setCollectionInfo(
                        AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(
                            /* rowCount = */ 1,
                            /* columnCount = */ buttonViews.size,
                            /* isHierarchical = */ false,
                            /* selectionMode = */ AccessibilityNodeInfoCompat.CollectionInfoCompat.SELECTION_MODE_SINGLE
                        )
                    )
                }
            })

            val roleTabString = context.getString(R.string.sb_role_tab)
            buttonViews.forEachIndexed { i, buttonView ->
                ViewCompat.setAccessibilityDelegate(buttonView, object : AccessibilityDelegateCompat() {
                    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        info.roleDescription = roleTabString
                        info.className = "android.app.ActionBar\$Tab"
                        val isSel = (i == selectedIndex)
                        info.isSelected = isSel
                        info.setCollectionItemInfo(
                            AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(
                                /* rowIndex = */ 0,
                                /* rowSpan = */ 1,
                                /* columnIndex = */ i,
                                /* columnSpan = */ 1,
                                /* heading = */ false,
                                /* selected = */ isSel
                            )
                        )
                    }
                })
            }
    }

    /**
     * Yatay ve TabBar segmented buton çubuğunu yapılandırır.
     * Standart, hibrit ve kayan göstergeli TabBar butonlarını destekler.
     */
    private fun setupHorizontal(ta: TypedArray) {
        orientation = HORIZONTAL
        clipToPadding = true

        val barPadding = context.resources.getDimensionPixelSize(R.dimen.sb_bar_padding)
        val buttonGap = context.resources.getDimensionPixelSize(R.dimen.sb_button_gap)
        val buttonGapIconText = context.resources.getDimensionPixelSize(R.dimen.sb_button_gap_icon_text)
        val buttonHeight = context.resources.getDimensionPixelSize(R.dimen.sb_button_height)
        val minWidth = if (currentStyle == STYLE_TAB) 0 else context.resources.getDimensionPixelSize(R.dimen.sb_button_min_width)
        val circularHybridSize = context.resources.getDimensionPixelSize(R.dimen.sb_circular_button_size)

        val barSelectedIndex = if (ta.hasValue(R.styleable.SegmentedButtonBar_sbSelectedIndex)) {
            ta.getInt(R.styleable.SegmentedButtonBar_sbSelectedIndex, 0)
        } else {
            null
        }

        setPadding(barPadding, barPadding, barPadding, barPadding)
        removeAllViews()
        buttonViews.clear()
        isIndicatorPositionInitialized = false

        val inflater = LayoutInflater.from(context)

        for (i in 0 until buttonCount) {
            val (iconRes, textVal) = getButtonAttributes(ta, i)
            val (explicitSelected, explicitActivated, explicitEnabled) = getButtonStateAttributes(ta, i)
            val perButtonTint = getButtonIconTint(ta, i) ?: globalIconTint
            val (customBg, selectedBg, selectedCol) = getButtonBackgroundAttributes(ta, i)
            val customCd = getButtonContentDescriptionAttr(ta, i)
            val customTooltip = getButtonTooltipAttr(ta, i)
            val buttonStyle = getButtonStyle(ta, i)

            val buttonIndex = i
            val itemView: View

            if (buttonStyle == BUTTON_STYLE_CIRCULAR) {
                // Hibrit Dairesel Buton (32x32dp) / Hybrid Circular Button (32x32dp)
                itemView = inflater.inflate(R.layout.sb_button_circular_item, this, false)
                val iconView = itemView.findViewById<ImageView>(R.id.sb_circular_icon)
                val finalIcon = if (iconRes != 0) iconRes else R.drawable.ic_sb_arrow_next
                iconView.setImageResource(finalIcon)
                resolveIconTint(perButtonTint)?.let { ImageViewCompat.setImageTintList(iconView, it) }

                if (!slideIndicator) {
                    applyButtonBackground(
                        itemView,
                        isCircular = true,
                        customBackgroundDrawable = customBg,
                        selectedCustomDrawable = selectedBg ?: globalSelectedBackground,
                        selectedCustomColor = selectedCol ?: globalSelectedColor,
                        showUnselectedBg = showUnselectedBackground,
                        unselectedCustomColor = unselectedButtonColor
                    )
                } else {
                    itemView.background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.TRANSPARENT)
                    }
                    applyButtonRipple(itemView, isCircular = true)
                }

                val finalCd = customCd ?: textVal ?: getDefaultContentDescription(i)
                itemView.contentDescription = finalCd

                val tooltipText = customTooltip ?: textVal ?: customCd
                applyTooltip(itemView, tooltipText)

                val params = LayoutParams(circularHybridSize, circularHybridSize).apply {
                    if (i > 0) {
                        marginStart = buttonGap
                    }
                }
                itemView.layoutParams = params

                val isSelectedVal = explicitSelected ?: (barSelectedIndex?.let { it == buttonIndex } ?: false)
                val isActivatedVal = explicitActivated ?: (globalAllActivated ?: false)

                itemView.isSelected = isSelectedVal
                itemView.isActivated = isActivatedVal
                itemView.isEnabled = explicitEnabled

                if (isSelectedVal) {
                    selectedIndex = buttonIndex
                }

                itemView.setOnClickListener {
                    if (autoSelect) {
                        selectButton(buttonIndex)
                    }
                    buttonClickListeners[buttonIndex]?.invoke()
                }

                itemView.setOnLongClickListener {
                    buttonLongClickListeners[buttonIndex]?.invoke() ?: false
                }
            } else {
                // Standart Yatay Buton / TabBar Butonu
                itemView = inflater.inflate(R.layout.sb_button_horizontal_item, this, false)
                val iconView = itemView.findViewById<ImageView>(R.id.sb_item_icon)
                val textView = itemView.findViewById<TextView>(R.id.sb_item_text)

                resolveIconTint(perButtonTint)?.let { ImageViewCompat.setImageTintList(iconView, it) }

                if (!slideIndicator) {
                    applyButtonBackground(
                        itemView,
                        isCircular = false,
                        customBackgroundDrawable = customBg,
                        selectedCustomDrawable = selectedBg ?: globalSelectedBackground,
                        selectedCustomColor = selectedCol ?: globalSelectedColor,
                        showUnselectedBg = showUnselectedBackground,
                        unselectedCustomColor = unselectedButtonColor
                    )
                } else {
                    val cornerRadius = context.resources.getDimension(R.dimen.sb_button_radius)
                    itemView.background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        this.cornerRadius = cornerRadius
                        setColor(Color.TRANSPARENT)
                    }
                    applyButtonRipple(itemView, isCircular = false)
                }

                val hasIcon = (iconRes != 0)
                val hasText = !textVal.isNullOrEmpty()

                when {
                    hasIcon && hasText -> {
                        iconView.setImageResource(iconRes)
                        iconView.visibility = View.VISIBLE
                        textView.text = textVal
                        textView.visibility = View.VISIBLE
                        (textView.layoutParams as? MarginLayoutParams)?.marginStart = buttonGapIconText
                        textView.gravity = Gravity.CENTER_VERTICAL or Gravity.START
                    }
                    !hasIcon && hasText -> {
                        iconView.visibility = View.GONE
                        textView.text = textVal
                        textView.visibility = View.VISIBLE
                        (textView.layoutParams as? MarginLayoutParams)?.marginStart = 0
                        textView.gravity = Gravity.CENTER
                    }
                    hasIcon && !hasText -> {
                        iconView.setImageResource(iconRes)
                        iconView.visibility = View.VISIBLE
                        textView.visibility = View.GONE
                        (textView.layoutParams as? MarginLayoutParams)?.marginStart = 0
                    }
                    else -> {
                        iconView.visibility = View.GONE
                        textView.text = if (currentStyle == STYLE_TAB) "Tab ${i + 1}" else getDefaultContentDescription(i)
                        textView.visibility = View.VISIBLE
                        (textView.layoutParams as? MarginLayoutParams)?.marginStart = 0
                        textView.gravity = Gravity.CENTER
                    }
                }

                val finalCd = customCd ?: textVal ?: getDefaultContentDescription(i)
                itemView.contentDescription = finalCd

                val tooltipText = customTooltip ?: textVal ?: customCd
                applyTooltip(itemView, tooltipText)

                val params = LayoutParams(0, buttonHeight, 1f).apply {
                    if (i > 0) {
                        marginStart = buttonGap
                    }
                }
                itemView.minimumWidth = minWidth
                itemView.layoutParams = params

                val isSelectedVal = explicitSelected ?: (barSelectedIndex?.let { it == buttonIndex } ?: (buttonIndex == 0))
                val isActivatedVal = explicitActivated ?: (globalAllActivated ?: false)

                itemView.isSelected = isSelectedVal
                itemView.isActivated = isActivatedVal
                itemView.isEnabled = explicitEnabled

                if (isSelectedVal) {
                    selectedIndex = buttonIndex
                }

                itemView.setOnClickListener {
                    if (autoSelect) {
                        selectButton(buttonIndex)
                    }
                    buttonClickListeners[buttonIndex]?.invoke()
                }

                itemView.setOnLongClickListener {
                    buttonLongClickListeners[buttonIndex]?.invoke() ?: false
                }
            }

            buttonViews.add(itemView)
            addView(itemView)
        }

        if (buttonViews.none { it.isSelected }) {
            selectedIndex = -1
        }
    }

    /**
     * Dikey segmented buton çubuğunu yapılandırır.
     */
    private fun setupVertical(ta: TypedArray) {
        orientation = VERTICAL
        clipToPadding = true

        val barPadding = context.resources.getDimensionPixelSize(R.dimen.sb_bar_padding)
        val buttonGap = context.resources.getDimensionPixelSize(R.dimen.sb_button_gap)
        val buttonGapIconText = context.resources.getDimensionPixelSize(R.dimen.sb_button_gap_icon_text)
        val buttonWidth = context.resources.getDimensionPixelSize(R.dimen.sb_vertical_button_width)
        val buttonHeight = context.resources.getDimensionPixelSize(R.dimen.sb_vertical_button_height)

        val barSelectedIndex = if (ta.hasValue(R.styleable.SegmentedButtonBar_sbSelectedIndex)) {
            ta.getInt(R.styleable.SegmentedButtonBar_sbSelectedIndex, 0)
        } else {
            null
        }

        setPadding(barPadding, barPadding, barPadding, barPadding)
        removeAllViews()
        buttonViews.clear()

        val inflater = LayoutInflater.from(context)

        for (i in 0 until buttonCount) {
            val itemView = inflater.inflate(R.layout.sb_button_vertical_item, this, false)
            val iconView = itemView.findViewById<ImageView>(R.id.sb_vertical_icon)
            val textView = itemView.findViewById<TextView>(R.id.sb_vertical_text)

            val perButtonTint = getButtonIconTint(ta, i)
            resolveIconTint(perButtonTint)?.let { ImageViewCompat.setImageTintList(iconView, it) }

            val (iconRes, textVal) = getButtonAttributes(ta, i)
            val (explicitSelected, explicitActivated, explicitEnabled) = getButtonStateAttributes(ta, i)
            val (customBg, selectedBg, selectedCol) = getButtonBackgroundAttributes(ta, i)
            val customCd = getButtonContentDescriptionAttr(ta, i)
            val customTooltip = getButtonTooltipAttr(ta, i)

            applyButtonBackground(
                itemView,
                isCircular = false,
                customBackgroundDrawable = customBg,
                selectedCustomDrawable = selectedBg ?: globalSelectedBackground,
                selectedCustomColor = selectedCol ?: globalSelectedColor,
                showUnselectedBg = showUnselectedBackground,
                unselectedCustomColor = unselectedButtonColor
            )

            val hasIcon = (iconRes != 0)
            val hasText = !textVal.isNullOrEmpty()

            when {
                hasIcon && hasText -> {
                    iconView.setImageResource(iconRes)
                    iconView.visibility = View.VISIBLE
                    textView.text = textVal
                    textView.visibility = View.VISIBLE
                    (textView.layoutParams as? MarginLayoutParams)?.topMargin = buttonGapIconText
                }
                !hasIcon && hasText -> {
                    iconView.visibility = View.GONE
                    textView.text = textVal
                    textView.visibility = View.VISIBLE
                    (textView.layoutParams as? MarginLayoutParams)?.topMargin = 0
                    textView.gravity = Gravity.CENTER
                }
                hasIcon && !hasText -> {
                    iconView.setImageResource(iconRes)
                    iconView.visibility = View.VISIBLE
                    textView.visibility = View.GONE
                    (textView.layoutParams as? MarginLayoutParams)?.topMargin = 0
                }
                else -> {
                    iconView.setImageResource(R.drawable.ic_sb_arrow_next)
                    iconView.visibility = View.VISIBLE
                    textView.visibility = View.GONE
                    (textView.layoutParams as? MarginLayoutParams)?.topMargin = 0
                }
            }

            val finalCd = customCd ?: textVal ?: getDefaultContentDescription(i)
            itemView.contentDescription = finalCd

            val tooltipText = customTooltip ?: textVal ?: customCd
            applyTooltip(itemView, tooltipText)

            val params = LayoutParams(buttonWidth, buttonHeight).apply {
                if (i > 0) {
                    topMargin = buttonGap
                }
            }
            itemView.layoutParams = params

            val buttonIndex = i
            val isSelectedVal = explicitSelected ?: (barSelectedIndex?.let { it == buttonIndex } ?: (buttonIndex == 0))
            val isActivatedVal = explicitActivated ?: (globalAllActivated ?: false)

            itemView.isSelected = isSelectedVal
            itemView.isActivated = isActivatedVal
            itemView.isEnabled = explicitEnabled

            if (isSelectedVal) {
                selectedIndex = buttonIndex
            }

            itemView.setOnClickListener {
                if (autoSelect) {
                    selectButton(buttonIndex)
                }
                buttonClickListeners[buttonIndex]?.invoke()
            }

            itemView.setOnLongClickListener {
                buttonLongClickListeners[buttonIndex]?.invoke() ?: false
            }

            buttonViews.add(itemView)
            addView(itemView)
        }

        if (buttonViews.none { it.isSelected }) {
            selectedIndex = -1
        }
    }

    /**
     * Dairesel tekil buton stilini yapılandırır.
     */
    private fun setupCircular(ta: TypedArray) {
        orientation = HORIZONTAL
        clipToPadding = true

        val barPadding = context.resources.getDimensionPixelSize(R.dimen.sb_bar_padding)
        val circularSize = context.resources.getDimensionPixelSize(R.dimen.sb_circular_button_size)

        setPadding(barPadding, barPadding, barPadding, barPadding)
        removeAllViews()
        buttonViews.clear()

        val inflater = LayoutInflater.from(context)
        val circularView = inflater.inflate(R.layout.sb_button_circular_item, this, false)
        val iconView = circularView.findViewById<ImageView>(R.id.sb_circular_icon)

        val perButtonTint = getButtonIconTint(ta, 0)
        resolveIconTint(perButtonTint)?.let { ImageViewCompat.setImageTintList(iconView, it) }

        val (customBg, selectedBg, selectedCol) = getButtonBackgroundAttributes(ta, 0)
        applyButtonBackground(
            circularView,
            isCircular = true,
            customBackgroundDrawable = customBg,
            selectedCustomDrawable = selectedBg ?: globalSelectedBackground,
            selectedCustomColor = selectedCol ?: globalSelectedColor,
            showUnselectedBg = showUnselectedBackground,
            unselectedCustomColor = unselectedButtonColor
        )

        val customIcon = ta.getResourceId(R.styleable.SegmentedButtonBar_sbButton1Icon, 0)
        val customText = ta.getString(R.styleable.SegmentedButtonBar_sbButton1Text)
        val customCd = getButtonContentDescriptionAttr(ta, 0)
        val customTooltip = getButtonTooltipAttr(ta, 0)
        val (explicitSelected, explicitActivated, explicitEnabled) = getButtonStateAttributes(ta, 0)

        val finalIcon = if (customIcon != 0) customIcon else R.drawable.ic_sb_arrow_next
        iconView.setImageResource(finalIcon)

        val finalCd = customCd ?: customText ?: context.getString(R.string.sb_cd_circular)
        circularView.contentDescription = finalCd

        val tooltipText = customTooltip ?: customText ?: customCd
        applyTooltip(circularView, tooltipText)

        circularView.layoutParams = LayoutParams(circularSize, circularSize)
        circularView.isSelected = explicitSelected ?: false
        circularView.isActivated = explicitActivated ?: (globalAllActivated ?: false)
        circularView.isEnabled = explicitEnabled

        circularView.setOnClickListener {
            buttonClickListeners[0]?.invoke()
        }

        circularView.setOnLongClickListener {
            buttonLongClickListeners[0]?.invoke() ?: false
        }

        buttonViews.add(circularView)
        addView(circularView)
    }

    /**
     * Pill (Next / Back / Text) buton stilini yapılandırır.
     */
    private fun setupPill(ta: TypedArray) {
        orientation = HORIZONTAL
        clipToPadding = true

        val barPadding = context.resources.getDimensionPixelSize(R.dimen.sb_bar_padding)
        val buttonHeight = context.resources.getDimensionPixelSize(R.dimen.sb_button_height)
        val minWidth = context.resources.getDimensionPixelSize(R.dimen.sb_button_min_width)

        setPadding(barPadding, barPadding, barPadding, barPadding)
        removeAllViews()
        buttonViews.clear()

        val inflater = LayoutInflater.from(context)
        val pillView = inflater.inflate(R.layout.sb_button_pill_item, this, false)
        val iconView = pillView.findViewById<ImageView>(R.id.sb_pill_icon)
        val textView = pillView.findViewById<TextView>(R.id.sb_pill_text)

        val perButtonTint = getButtonIconTint(ta, 0)
        resolveIconTint(perButtonTint)?.let { ImageViewCompat.setImageTintList(iconView, it) }

        val (customBg, selectedBg, selectedCol) = getButtonBackgroundAttributes(ta, 0)
        applyButtonBackground(
            pillView,
            isCircular = false,
            customBackgroundDrawable = customBg,
            selectedCustomDrawable = selectedBg ?: globalSelectedBackground,
            selectedCustomColor = selectedCol ?: globalSelectedColor,
            showUnselectedBg = showUnselectedBackground,
            unselectedCustomColor = unselectedButtonColor
        )

        val customIcon = ta.getResourceId(R.styleable.SegmentedButtonBar_sbButton1Icon, 0)
        val customText = ta.getString(R.styleable.SegmentedButtonBar_sbButton1Text)
        val customCd = getButtonContentDescriptionAttr(ta, 0)
        val customTooltip = getButtonTooltipAttr(ta, 0)

        val (explicitSelected, explicitActivated, explicitEnabled) = getButtonStateAttributes(ta, 0)
        val initialPillActivated = if (ta.hasValue(R.styleable.SegmentedButtonBar_sbPillActivated)) {
            ta.getBoolean(R.styleable.SegmentedButtonBar_sbPillActivated, false)
        } else {
            explicitActivated ?: (globalAllActivated ?: false)
        }

        when (pillType) {
            PILL_NEXT -> {
                val iconRes = if (customIcon != 0) customIcon else R.drawable.ic_sb_arrow_next
                iconView.setImageResource(iconRes)
                iconView.visibility = View.VISIBLE
                textView.visibility = View.GONE
                pillView.contentDescription = customCd ?: customText ?: context.getString(R.string.sb_cd_next)
            }
            PILL_BACK -> {
                val iconRes = if (customIcon != 0) customIcon else R.drawable.ic_sb_arrow_back
                iconView.setImageResource(iconRes)
                iconView.visibility = View.VISIBLE
                textView.visibility = View.GONE
                pillView.contentDescription = customCd ?: customText ?: context.getString(R.string.sb_cd_back)
            }
            PILL_TEXT -> {
                iconView.visibility = View.GONE
                textView.text = customText ?: context.getString(R.string.sb_cd_next)
                textView.visibility = View.VISIBLE
                pillView.contentDescription = customCd ?: customText ?: context.getString(R.string.sb_cd_next)
            }
        }

        val tooltipText = customTooltip ?: customText ?: customCd
        applyTooltip(pillView, tooltipText)

        pillView.layoutParams = LayoutParams(minWidth, buttonHeight)
        pillView.isSelected = explicitSelected ?: false
        pillView.isActivated = initialPillActivated
        pillView.isEnabled = explicitEnabled

        pillView.setOnClickListener {
            pillClickListener?.invoke()
            buttonClickListeners[0]?.invoke()
        }

        pillView.setOnLongClickListener {
            buttonLongClickListeners[0]?.invoke() ?: false
        }

        buttonViews.add(pillView)
        addView(pillView)
    }

    /**
     * Animasyonlu genişleyen (Expandable) buton stilini yapılandırır.
     */
    private fun setupExpandable(ta: TypedArray) {
        orientation = HORIZONTAL
        clipToPadding = true

        val barPadding = context.resources.getDimensionPixelSize(R.dimen.sb_bar_padding)
        val buttonGap = context.resources.getDimensionPixelSize(R.dimen.sb_button_gap)
        val circularSize = context.resources.getDimensionPixelSize(R.dimen.sb_circular_button_size)

        setPadding(barPadding, barPadding, barPadding, barPadding)
        removeAllViews()
        buttonViews.clear()
        buttonMetaList.clear()

        val inflater = LayoutInflater.from(context)

        for (i in 0 until buttonCount) {
            val itemView = inflater.inflate(R.layout.sb_button_circular_item, this, false)
            val iconView = itemView.findViewById<ImageView>(R.id.sb_circular_icon)

            val perButtonTint = getButtonIconTint(ta, i)
            resolveIconTint(perButtonTint)?.let { ImageViewCompat.setImageTintList(iconView, it) }

            val (iconRes, textVal) = getButtonAttributes(ta, i)
            val (explicitSelected, explicitActivated, explicitEnabled) = getButtonStateAttributes(ta, i)
            val (customBg, selectedBg, selectedCol) = getButtonBackgroundAttributes(ta, i)
            val customCd = getButtonContentDescriptionAttr(ta, i)
            val customTooltip = getButtonTooltipAttr(ta, i)

            val meta = ButtonMeta(
                iconRes = iconRes,
                text = textVal,
                iconTint = perButtonTint,
                contentDescription = customCd,
                tooltip = customTooltip,
                customBg = customBg,
                selectedBg = selectedBg,
                selectedColor = selectedCol
            )
            buttonMetaList.add(meta)

            applyButtonBackground(
                itemView,
                isCircular = true,
                customBackgroundDrawable = customBg,
                selectedCustomDrawable = selectedBg ?: globalSelectedBackground,
                selectedCustomColor = selectedCol ?: globalSelectedColor,
                showUnselectedBg = showUnselectedBackground,
                unselectedCustomColor = unselectedButtonColor
            )

            val finalIcon = if (iconRes != 0) iconRes else R.drawable.ic_sb_arrow_next
            iconView.setImageResource(finalIcon)

            val finalCd = customCd ?: textVal ?: getDefaultContentDescription(i)
            itemView.contentDescription = finalCd

            val tooltipText = customTooltip ?: textVal ?: customCd
            applyTooltip(itemView, tooltipText)

            val params = LayoutParams(circularSize, circularSize).apply {
                if (i > 0) {
                    marginStart = buttonGap
                }
            }
            itemView.layoutParams = params
            itemView.isSelected = explicitSelected ?: false
            itemView.isActivated = explicitActivated ?: (globalAllActivated ?: false)
            itemView.isEnabled = explicitEnabled

            val buttonIndex = i
            if (buttonIndex == 0) {
                itemView.visibility = View.VISIBLE
                itemView.setOnClickListener {
                    toggleExpand()
                    buttonClickListeners[0]?.invoke()
                }
            } else {
                itemView.visibility = View.GONE
                itemView.alpha = 0f
                itemView.setOnClickListener {
                    if (autoSelect) {
                        selectButton(buttonIndex)
                    }
                    if (collapseOnSelect) {
                        updateAnchorVisual(buttonIndex)
                        collapse(animate = true)
                    }
                    buttonClickListeners[buttonIndex]?.invoke()
                }
            }

            itemView.setOnLongClickListener {
                buttonLongClickListeners[buttonIndex]?.invoke() ?: false
            }

            buttonViews.add(itemView)
            addView(itemView)
        }

        isExpanded = false
    }

    /**
     * Expandable stilde seçilen butonun ikon ve stilini çubuğun ana (anchor) butonuna aktarır.
     * Updates anchor button's visual representation (icon, tint, cd, tooltip) to reflect selected button.
     */
    private fun updateAnchorVisual(index: Int) {
        if (buttonViews.isEmpty() || index !in buttonMetaList.indices) return
        val anchorView = buttonViews[0]
        val iconView = anchorView.findViewById<ImageView>(R.id.sb_circular_icon)
        val meta = buttonMetaList[index]

        val finalIcon = if (meta.iconRes != 0) meta.iconRes else R.drawable.ic_sb_arrow_next
        iconView?.setImageResource(finalIcon)
        resolveIconTint(meta.iconTint)?.let { ImageViewCompat.setImageTintList(iconView, it) }

        val finalCd = meta.contentDescription ?: meta.text ?: getDefaultContentDescription(index)
        anchorView.contentDescription = finalCd

        val tooltipText = meta.tooltip ?: meta.text ?: meta.contentDescription
        applyTooltip(anchorView, tooltipText)

        applyButtonBackground(
            anchorView,
            isCircular = true,
            customBackgroundDrawable = meta.customBg,
            selectedCustomDrawable = meta.selectedBg ?: globalSelectedBackground,
            selectedCustomColor = meta.selectedColor ?: globalSelectedColor,
            showUnselectedBg = showUnselectedBackground,
            unselectedCustomColor = unselectedButtonColor
        )
        anchorView.isSelected = true
    }

    /**
     * Buton ipucu (Tooltip) metnini güvenli bir şekilde atar. Android Studio önizlemesinde çökmeyi engeller.
     * Applies tooltip text safely, preventing any Layoutlib preview crashes in Android Studio.
     */
    private fun applyTooltip(view: View, tooltipText: CharSequence?) {
        if (isInEditMode || !autoTooltip || tooltipText.isNullOrEmpty()) return
        TooltipCompat.setTooltipText(view, tooltipText)
    }

    /**
     * İkon renk tonunu belirler. Özel tint atanmamışsa gündüz/gece temasına uyumlu @color/sb_button_icon döndürür.
     * Resolves icon tint, falling back to day/night adaptive @color/sb_button_icon.
     */
    private fun resolveIconTint(perButtonTint: ColorStateList?): ColorStateList? {
        return perButtonTint
            ?: globalIconTint
            ?: ContextCompat.getColorStateList(context, R.color.sb_button_icon)
    }

    /**
     * Butonun tıklama, dokunma ve dalgalanma (ripple) alanlarını tam 54dp yuvarlak köşelere göre kırpar.
     * Clips the button touch, focus, and ripple areas to match exact rounded corners.
     */
    private fun applyButtonOutline(view: View, isCircular: Boolean) {
        if (isInEditMode) return
        val radius = context.resources.getDimension(
            if (isCircular) R.dimen.sb_circular_button_size else R.dimen.sb_button_radius
        )
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: Outline) {
                if (isCircular) {
                    outline.setOval(0, 0, v.width, v.height)
                } else {
                    outline.setRoundRect(0, 0, v.width, v.height, radius)
                }
            }
        }
        view.clipToOutline = true
    }

    /**
     * Butona maskeli ve yuvarlatılmış RippleDrawable atar. Böylece tıklamalarda hiçbir kare taşma oluşmaz.
     * Applies a masked rounded RippleDrawable to prevent any rectangular highlight bleed on tap.
     */
    private fun applyButtonRipple(view: View, isCircular: Boolean) {
        applyButtonOutline(view, isCircular)
        if (isInEditMode) return
        val radius = context.resources.getDimension(
            if (isCircular) R.dimen.sb_circular_button_size else R.dimen.sb_button_radius
        )
        val maskDrawable = GradientDrawable().apply {
            shape = if (isCircular) GradientDrawable.OVAL else GradientDrawable.RECTANGLE
            if (!isCircular) {
                cornerRadius = radius
            }
            setColor(Color.WHITE)
        }

        val defaultRipple = ContextCompat.getColor(context, R.color.sb_button_ripple_color)
        val rippleColor = globalRippleColor ?: defaultRipple

        val ripple = RippleDrawable(
            ColorStateList.valueOf(rippleColor),
            null,
            maskDrawable
        )
        view.foreground = ripple
        applyButtonOutline(view, isCircular)
    }

    /**
     * Buton arkaplan StateListDrawable nesnesini oluşturur ve görünüme uygular.
     * Generates and applies the custom StateListDrawable background to the button view.
     */
    private fun applyButtonBackground(
        view: View,
        isCircular: Boolean,
        customBackgroundDrawable: Drawable?,
        selectedCustomDrawable: Drawable?,
        selectedCustomColor: Int?,
        showUnselectedBg: Boolean,
        unselectedCustomColor: Int?
    ) {
        applyButtonRipple(view, isCircular)
        if (customBackgroundDrawable != null) {
            view.background = customBackgroundDrawable
            return
        }

        // Özel bir renk veya arkaplan atanmamışsa standart state drawable'ını koru
        if (selectedCustomDrawable == null && selectedCustomColor == null && !showUnselectedBg) {
            val standardRes = if (isCircular) R.drawable.state_segmented_circular_button else R.drawable.state_segmented_button
            view.background = ContextCompat.getDrawable(context, standardRes)
            return
        }

        val cornerRadius = context.resources.getDimension(
            if (isCircular) R.dimen.sb_circular_button_size else R.dimen.sb_button_radius
        )
        val disabledColor = ContextCompat.getColor(context, R.color.sb_button_disabled)
        val defaultSelectedColor = ContextCompat.getColor(context, R.color.sb_button_selected)

        val stateList = StateListDrawable()

        // 1. Devre dışı durumu (Disabled state)
        val disabledDrawable = GradientDrawable().apply {
            if (isCircular) shape = GradientDrawable.OVAL else {
                shape = GradientDrawable.RECTANGLE
                this.cornerRadius = cornerRadius
            }
            setColor(disabledColor)
        }
        stateList.addState(intArrayOf(-android.R.attr.state_enabled), disabledDrawable)

        // 2. Seçili ve Aktif durumu (Selected & Activated state)
        val selectedStateDrawable: Drawable = when {
            selectedCustomDrawable != null -> selectedCustomDrawable.constantState?.newDrawable()?.mutate() ?: selectedCustomDrawable
            selectedCustomColor != null -> GradientDrawable().apply {
                if (isCircular) shape = GradientDrawable.OVAL else {
                    shape = GradientDrawable.RECTANGLE
                    this.cornerRadius = cornerRadius
                }
                setColor(selectedCustomColor)
            }
            else -> GradientDrawable().apply {
                if (isCircular) shape = GradientDrawable.OVAL else {
                    shape = GradientDrawable.RECTANGLE
                    this.cornerRadius = cornerRadius
                }
                setColor(defaultSelectedColor)
            }
        }
        stateList.addState(intArrayOf(android.R.attr.state_selected), selectedStateDrawable)
        stateList.addState(intArrayOf(android.R.attr.state_activated), selectedStateDrawable.constantState?.newDrawable()?.mutate() ?: selectedStateDrawable)

        // 3. Seçilmemiş durumu (Unselected state)
        val unselectedStateDrawable: Drawable = if (showUnselectedBg && unselectedCustomColor != null) {
            GradientDrawable().apply {
                if (isCircular) shape = GradientDrawable.OVAL else {
                    shape = GradientDrawable.RECTANGLE
                    this.cornerRadius = cornerRadius
                }
                setColor(unselectedCustomColor)
            }
        } else {
            ColorDrawable(Color.TRANSPARENT)
        }
        stateList.addState(intArrayOf(), unselectedStateDrawable)

        view.background = stateList
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (slideIndicator && buttonViews.isNotEmpty() && selectedIndex in buttonViews.indices) {
            val target = buttonViews[selectedIndex]
            if (target.width > 0 && (!isIndicatorPositionInitialized || changed)) {
                indicatorLeft = target.left.toFloat()
                indicatorRight = target.right.toFloat()
                isIndicatorPositionInitialized = true
                invalidate()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (slideIndicator && buttonViews.isNotEmpty() && selectedIndex in buttonViews.indices) {
            val selectedView = buttonViews[selectedIndex]
            if (!isIndicatorPositionInitialized && selectedView.width > 0) {
                indicatorLeft = selectedView.left.toFloat()
                indicatorRight = selectedView.right.toFloat()
                isIndicatorPositionInitialized = true
            }

            if (isIndicatorPositionInitialized && indicatorRight > indicatorLeft) {
                val barPaddingTop = paddingTop.toFloat()
                val barPaddingBottom = (height - paddingBottom).toFloat()
                val cornerRadius = context.resources.getDimension(R.dimen.sb_button_radius)

                val drawable = globalSelectedBackground ?: indicatorDrawable ?: GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    this.cornerRadius = cornerRadius
                    val col = globalSelectedColor ?: ContextCompat.getColor(context, R.color.sb_button_selected)
                    setColor(col)
                }

                drawable.setBounds(
                    indicatorLeft.toInt(),
                    barPaddingTop.toInt(),
                    indicatorRight.toInt(),
                    barPaddingBottom.toInt()
                )
                drawable.draw(canvas)
            }
        }
        super.onDraw(canvas)
    }

    private fun getButtonStyle(ta: TypedArray, index: Int): Int {
        return when (index) {
            0 -> ta.getInt(R.styleable.SegmentedButtonBar_sbButton1Style, BUTTON_STYLE_HORIZONTAL)
            1 -> ta.getInt(R.styleable.SegmentedButtonBar_sbButton2Style, BUTTON_STYLE_HORIZONTAL)
            2 -> ta.getInt(R.styleable.SegmentedButtonBar_sbButton3Style, BUTTON_STYLE_HORIZONTAL)
            3 -> ta.getInt(R.styleable.SegmentedButtonBar_sbButton4Style, BUTTON_STYLE_HORIZONTAL)
            4 -> ta.getInt(R.styleable.SegmentedButtonBar_sbButton5Style, BUTTON_STYLE_HORIZONTAL)
            5 -> ta.getInt(R.styleable.SegmentedButtonBar_sbButton6Style, BUTTON_STYLE_HORIZONTAL)
            else -> BUTTON_STYLE_HORIZONTAL
        }
    }

    private fun getButtonIconTint(ta: TypedArray, index: Int): ColorStateList? {
        return when (index) {
            0 -> ta.getColorStateList(R.styleable.SegmentedButtonBar_sbButton1IconTint)
            1 -> ta.getColorStateList(R.styleable.SegmentedButtonBar_sbButton2IconTint)
            2 -> ta.getColorStateList(R.styleable.SegmentedButtonBar_sbButton3IconTint)
            3 -> ta.getColorStateList(R.styleable.SegmentedButtonBar_sbButton4IconTint)
            4 -> ta.getColorStateList(R.styleable.SegmentedButtonBar_sbButton5IconTint)
            5 -> ta.getColorStateList(R.styleable.SegmentedButtonBar_sbButton6IconTint)
            else -> null
        }
    }

    private fun getButtonBackgroundAttributes(ta: TypedArray, index: Int): Triple<Drawable?, Drawable?, Int?> {
        return when (index) {
            0 -> Triple(
                ta.getDrawable(R.styleable.SegmentedButtonBar_sbButton1Background),
                ta.getDrawable(R.styleable.SegmentedButtonBar_sbButton1SelectedBackground),
                if (ta.hasValue(R.styleable.SegmentedButtonBar_sbButton1SelectedColor)) ta.getColor(R.styleable.SegmentedButtonBar_sbButton1SelectedColor, Color.WHITE) else null
            )
            1 -> Triple(
                ta.getDrawable(R.styleable.SegmentedButtonBar_sbButton2Background),
                ta.getDrawable(R.styleable.SegmentedButtonBar_sbButton2SelectedBackground),
                if (ta.hasValue(R.styleable.SegmentedButtonBar_sbButton2SelectedColor)) ta.getColor(R.styleable.SegmentedButtonBar_sbButton2SelectedColor, Color.WHITE) else null
            )
            2 -> Triple(
                ta.getDrawable(R.styleable.SegmentedButtonBar_sbButton3Background),
                ta.getDrawable(R.styleable.SegmentedButtonBar_sbButton3SelectedBackground),
                if (ta.hasValue(R.styleable.SegmentedButtonBar_sbButton3SelectedColor)) ta.getColor(R.styleable.SegmentedButtonBar_sbButton3SelectedColor, Color.WHITE) else null
            )
            3 -> Triple(
                ta.getDrawable(R.styleable.SegmentedButtonBar_sbButton4Background),
                ta.getDrawable(R.styleable.SegmentedButtonBar_sbButton4SelectedBackground),
                if (ta.hasValue(R.styleable.SegmentedButtonBar_sbButton4SelectedColor)) ta.getColor(R.styleable.SegmentedButtonBar_sbButton4SelectedColor, Color.WHITE) else null
            )
            4 -> Triple(
                ta.getDrawable(R.styleable.SegmentedButtonBar_sbButton5Background),
                ta.getDrawable(R.styleable.SegmentedButtonBar_sbButton5SelectedBackground),
                if (ta.hasValue(R.styleable.SegmentedButtonBar_sbButton5SelectedColor)) ta.getColor(R.styleable.SegmentedButtonBar_sbButton5SelectedColor, Color.WHITE) else null
            )
            5 -> Triple(
                ta.getDrawable(R.styleable.SegmentedButtonBar_sbButton6Background),
                ta.getDrawable(R.styleable.SegmentedButtonBar_sbButton6SelectedBackground),
                if (ta.hasValue(R.styleable.SegmentedButtonBar_sbButton6SelectedColor)) ta.getColor(R.styleable.SegmentedButtonBar_sbButton6SelectedColor, Color.WHITE) else null
            )
            else -> Triple(null, null, null)
        }
    }

    private fun getButtonContentDescriptionAttr(ta: TypedArray, index: Int): String? {
        return when (index) {
            0 -> ta.getString(R.styleable.SegmentedButtonBar_sbButton1ContentDescription)
            1 -> ta.getString(R.styleable.SegmentedButtonBar_sbButton2ContentDescription)
            2 -> ta.getString(R.styleable.SegmentedButtonBar_sbButton3ContentDescription)
            3 -> ta.getString(R.styleable.SegmentedButtonBar_sbButton4ContentDescription)
            4 -> ta.getString(R.styleable.SegmentedButtonBar_sbButton5ContentDescription)
            5 -> ta.getString(R.styleable.SegmentedButtonBar_sbButton6ContentDescription)
            else -> null
        }
    }

    private fun getButtonTooltipAttr(ta: TypedArray, index: Int): String? {
        return when (index) {
            0 -> ta.getString(R.styleable.SegmentedButtonBar_sbButton1Tooltip)
            1 -> ta.getString(R.styleable.SegmentedButtonBar_sbButton2Tooltip)
            2 -> ta.getString(R.styleable.SegmentedButtonBar_sbButton3Tooltip)
            3 -> ta.getString(R.styleable.SegmentedButtonBar_sbButton4Tooltip)
            4 -> ta.getString(R.styleable.SegmentedButtonBar_sbButton5Tooltip)
            5 -> ta.getString(R.styleable.SegmentedButtonBar_sbButton6Tooltip)
            else -> null
        }
    }

    private fun getButtonAttributes(ta: TypedArray, index: Int): Pair<Int, String?> {
        return when (index) {
            0 -> Pair(
                ta.getResourceId(R.styleable.SegmentedButtonBar_sbButton1Icon, 0),
                ta.getString(R.styleable.SegmentedButtonBar_sbButton1Text)
            )
            1 -> Pair(
                ta.getResourceId(R.styleable.SegmentedButtonBar_sbButton2Icon, 0),
                ta.getString(R.styleable.SegmentedButtonBar_sbButton2Text)
            )
            2 -> Pair(
                ta.getResourceId(R.styleable.SegmentedButtonBar_sbButton3Icon, 0),
                ta.getString(R.styleable.SegmentedButtonBar_sbButton3Text)
            )
            3 -> Pair(
                ta.getResourceId(R.styleable.SegmentedButtonBar_sbButton4Icon, 0),
                ta.getString(R.styleable.SegmentedButtonBar_sbButton4Text)
            )
            4 -> Pair(
                ta.getResourceId(R.styleable.SegmentedButtonBar_sbButton5Icon, 0),
                ta.getString(R.styleable.SegmentedButtonBar_sbButton5Text)
            )
            5 -> Pair(
                ta.getResourceId(R.styleable.SegmentedButtonBar_sbButton6Icon, 0),
                ta.getString(R.styleable.SegmentedButtonBar_sbButton6Text)
            )
            else -> Pair(0, null)
        }
    }

    private fun getButtonStateAttributes(ta: TypedArray, index: Int): Triple<Boolean?, Boolean?, Boolean> {
        return when (index) {
            0 -> Triple(
                if (ta.hasValue(R.styleable.SegmentedButtonBar_sbButton1Selected)) ta.getBoolean(R.styleable.SegmentedButtonBar_sbButton1Selected, false) else null,
                if (ta.hasValue(R.styleable.SegmentedButtonBar_sbButton1Activated)) ta.getBoolean(R.styleable.SegmentedButtonBar_sbButton1Activated, false) else null,
                ta.getBoolean(R.styleable.SegmentedButtonBar_sbButton1Enabled, true)
            )
            1 -> Triple(
                if (ta.hasValue(R.styleable.SegmentedButtonBar_sbButton2Selected)) ta.getBoolean(R.styleable.SegmentedButtonBar_sbButton2Selected, false) else null,
                if (ta.hasValue(R.styleable.SegmentedButtonBar_sbButton2Activated)) ta.getBoolean(R.styleable.SegmentedButtonBar_sbButton2Activated, false) else null,
                ta.getBoolean(R.styleable.SegmentedButtonBar_sbButton2Enabled, true)
            )
            2 -> Triple(
                if (ta.hasValue(R.styleable.SegmentedButtonBar_sbButton3Selected)) ta.getBoolean(R.styleable.SegmentedButtonBar_sbButton3Selected, false) else null,
                if (ta.hasValue(R.styleable.SegmentedButtonBar_sbButton3Activated)) ta.getBoolean(R.styleable.SegmentedButtonBar_sbButton3Activated, false) else null,
                ta.getBoolean(R.styleable.SegmentedButtonBar_sbButton3Enabled, true)
            )
            3 -> Triple(
                if (ta.hasValue(R.styleable.SegmentedButtonBar_sbButton4Selected)) ta.getBoolean(R.styleable.SegmentedButtonBar_sbButton4Selected, false) else null,
                if (ta.hasValue(R.styleable.SegmentedButtonBar_sbButton4Activated)) ta.getBoolean(R.styleable.SegmentedButtonBar_sbButton4Activated, false) else null,
                ta.getBoolean(R.styleable.SegmentedButtonBar_sbButton4Enabled, true)
            )
            4 -> Triple(
                if (ta.hasValue(R.styleable.SegmentedButtonBar_sbButton5Selected)) ta.getBoolean(R.styleable.SegmentedButtonBar_sbButton5Selected, false) else null,
                if (ta.hasValue(R.styleable.SegmentedButtonBar_sbButton5Activated)) ta.getBoolean(R.styleable.SegmentedButtonBar_sbButton5Activated, false) else null,
                ta.getBoolean(R.styleable.SegmentedButtonBar_sbButton5Enabled, true)
            )
            5 -> Triple(
                if (ta.hasValue(R.styleable.SegmentedButtonBar_sbButton6Selected)) ta.getBoolean(R.styleable.SegmentedButtonBar_sbButton6Selected, false) else null,
                if (ta.hasValue(R.styleable.SegmentedButtonBar_sbButton6Activated)) ta.getBoolean(R.styleable.SegmentedButtonBar_sbButton6Activated, false) else null,
                ta.getBoolean(R.styleable.SegmentedButtonBar_sbButton6Enabled, true)
            )
            else -> Triple(null, null, true)
        }
    }

    private fun getDefaultContentDescription(index: Int): String {
        return when (index) {
            0 -> context.getString(R.string.sb_cd_button_1)
            1 -> context.getString(R.string.sb_cd_button_2)
            2 -> context.getString(R.string.sb_cd_button_3)
            3 -> context.getString(R.string.sb_cd_button_4)
            else -> "Button ${index + 1}"
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val targetWidthSpec = if (maxWidthPx in 1 until widthSize && widthMode != MeasureSpec.UNSPECIFIED) {
            MeasureSpec.makeMeasureSpec(maxWidthPx, MeasureSpec.AT_MOST)
        } else {
            widthMeasureSpec
        }

        super.onMeasure(targetWidthSpec, heightMeasureSpec)
    }

    // ==========================================
    // Public API — Sliding Pill Indicator & Tabs
    // ==========================================

    fun setSlideIndicator(enabled: Boolean) {
        slideIndicator = enabled
        isIndicatorPositionInitialized = false
        buttonViews.forEachIndexed { i, view ->
            val isCircular = (getButtonStyle(context.obtainStyledAttributes(intArrayOf()), i) == BUTTON_STYLE_CIRCULAR)
            if (enabled) {
                val cornerRadius = context.resources.getDimension(R.dimen.sb_button_radius)
                view.background = GradientDrawable().apply {
                    shape = if (isCircular) GradientDrawable.OVAL else GradientDrawable.RECTANGLE
                    if (!isCircular) {
                        this.cornerRadius = cornerRadius
                    }
                    setColor(Color.TRANSPARENT)
                }
            } else {
                applyButtonBackground(
                    view,
                    isCircular = isCircular,
                    customBackgroundDrawable = null,
                    selectedCustomDrawable = globalSelectedBackground,
                    selectedCustomColor = globalSelectedColor,
                    showUnselectedBg = showUnselectedBackground,
                    unselectedCustomColor = unselectedButtonColor
                )
            }
            applyButtonOutline(view, isCircular)
        }
        invalidate()
    }

    fun isSlideIndicator(): Boolean = slideIndicator

    fun setIndicatorDuration(durationMs: Long) {
        indicatorDurationMs = durationMs
    }

    fun getIndicatorDuration(): Long = indicatorDurationMs

    /**
     * ViewPager2 kaydırma işlemi sırasında gösterge pozisyonunu milimetrik olarak günceller.
     * Updates the sliding indicator position during live ViewPager2 drag gestures.
     */
    fun setIndicatorPosition(position: Int, positionOffset: Float) {
        if (!slideIndicator || position !in buttonViews.indices) return
        indicatorAnimator?.cancel()
        val currentView = buttonViews[position]
        val nextView = buttonViews.getOrNull(position + 1) ?: currentView

        val targetLeft = currentView.left + positionOffset * (nextView.left - currentView.left)
        val targetRight = currentView.right + positionOffset * (nextView.right - currentView.right)

        indicatorLeft = targetLeft
        indicatorRight = targetRight
        isIndicatorPositionInitialized = true
        invalidate()
    }

    private fun animateIndicator(targetLeft: Float, targetRight: Float) {
        indicatorAnimator?.cancel()
        val startLeft = indicatorLeft
        val startRight = indicatorRight

        indicatorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = indicatorDurationMs
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                indicatorLeft = startLeft + fraction * (targetLeft - startLeft)
                indicatorRight = startRight + fraction * (targetRight - startRight)
                invalidate()
            }
            start()
        }
    }

    // ==========================================
    // Public API — ViewPager2 & Fragment Binding
    // ==========================================

    /**
     * SegmentedButtonBar'ı ViewPager2 ile çift yönlü otomatik senkronizasyona bağlar.
     * Binds SegmentedButtonBar with ViewPager2 for automatic two-way synchronization.
     */
    @JvmOverloads
    fun setupWithViewPager2(
        viewPager: ViewPager2,
        smoothScroll: Boolean = true,
        tabConfig: ((TabConfig, Int) -> Unit)? = null
    ) {
        // Önceki dinleyiciyi temizle
        registeredViewPagerCallback?.let { boundViewPager?.unregisterOnPageChangeCallback(it) }
        boundViewPager = viewPager

        // Dinamik tab başlıkları / ikonları yapılandırması
        tabConfig?.let { configCallback ->
            for (i in 0 until buttonViews.size) {
                val cfg = TabConfig()
                configCallback(cfg, i)
                cfg.text?.let { setButtonText(i, it) }
                if (cfg.iconRes != 0) {
                    setButtonIcon(i, cfg.iconRes)
                }
                cfg.iconTint?.let { setButtonIconTint(i, it) }
                cfg.tooltip?.let { setButtonTooltip(i, it) }
                cfg.contentDescription?.let { setButtonContentDescription(i, it) }
            }
        }

        // Taba tıklandığında ViewPager2 sayfasını kaydır
        buttonViews.forEachIndexed { index, view ->
            view.setOnClickListener {
                selectButton(index, animate = true)
                viewPager.setCurrentItem(index, smoothScroll)
                buttonClickListeners[index]?.invoke()
            }
        }

        // ViewPager2 sayfa kaymalarını dinle
        val callback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                if (slideIndicator) {
                    setIndicatorPosition(position, positionOffset)
                }
            }

            override fun onPageSelected(position: Int) {
                selectButton(position, animate = true)
            }
        }

        registeredViewPagerCallback = callback
        viewPager.registerOnPageChangeCallback(callback)

        // Başlangıç sayfasını senkronize et
        val initialPage = viewPager.currentItem
        if (initialPage in buttonViews.indices) {
            selectButton(initialPage, animate = false)
        }
    }

    /**
     * SegmentedButtonBar sekmelerini FragmentContainerView içindeki Fragment'lar ile bağlar.
     * Connects tab selections with Fragment transactions.
     */
    fun setupWithFragments(
        fragmentManager: FragmentManager,
        @IdRes containerId: Int,
        fragments: List<Fragment>
    ) {
        require(fragments.size <= buttonViews.size) {
            "Fragment sayısı buton sayısından fazla olamaz / Fragment count cannot exceed button count"
        }

        // Başlangıç fragment'ını yükle
        val initialIndex = selectedIndex.coerceAtLeast(0)
        if (initialIndex in fragments.indices) {
            fragmentManager.beginTransaction()
                .replace(containerId, fragments[initialIndex])
                .commit()
        }

        // Dahili fragment geçişini internalFragmentTabListener'a ata (Kullanıcı dinleyicisini ezmez!)
        internalFragmentTabListener = { position ->
            if (position in fragments.indices) {
                fragmentManager.beginTransaction()
                    .replace(containerId, fragments[position])
                    .commit()
            }
        }
    }

    /**
     * Sekme seçimini dinleyen ana kullanıcı geri çağrısını atar.
     */
    fun setOnTabSelectedListener(listener: ((position: Int) -> Unit)?) {
        userTabSelectedListener = listener
    }

    /**
     * Sekme seçimini dinleyen ek bir geri çağrı ekler.
     */
    fun addOnTabSelectedListener(listener: (position: Int) -> Unit) {
        customTabSelectedListeners.add(listener)
    }

    /**
     * Belirtilen sekme dinleyicisini kaldırır.
     */
    fun removeOnTabSelectedListener(listener: (position: Int) -> Unit) {
        customTabSelectedListeners.remove(listener)
    }

    /**
     * Tüm ek sekme dinleyicilerini ve kullanıcı dinleyicisini temizler.
     */
    fun clearOnTabSelectedListeners() {
        customTabSelectedListeners.clear()
        userTabSelectedListener = null
    }

    private fun notifyTabSelected(index: Int) {
        internalFragmentTabListener?.invoke(index)
        userTabSelectedListener?.invoke(index)
        customTabSelectedListeners.forEach { it.invoke(index) }
    }

    // ==========================================
    // Public API — Expandable Animation
    // ==========================================

    fun expand(animate: Boolean = true) {
        if (currentStyle != STYLE_EXPANDABLE || isExpanded || isAnimating) return

        // Seçili olan buton ana buton (0) üzerinde zaten gösterilmektedir.
        // Bu nedenle açılır menü içerisinde tekrar gösterilip mükerrer buton oluşması engellenir.
        val targetChildren = mutableListOf<View>()
        for (i in 1 until buttonViews.size) {
            val child = buttonViews[i]
            if (collapseOnSelect && selectedIndex > 0 && i == selectedIndex) {
                child.visibility = View.GONE
            } else {
                targetChildren.add(child)
            }
        }

        if (targetChildren.isEmpty()) {
            isExpanded = true
            onExpandChangeListener?.invoke(true)
            return
        }

        if (!animate) {
            targetChildren.forEach { child ->
                child.visibility = View.VISIBLE
                child.alpha = 1f
                child.translationX = 0f
            }
            isExpanded = true
            onExpandChangeListener?.invoke(true)
            requestLayout()
            return
        }

        isAnimating = true
        targetChildren.forEachIndexed { animIndex, child ->
            child.visibility = View.VISIBLE
            child.alpha = 0f
            val startTranslation = if (expandDirection == EXPAND_START) 24f else -24f
            child.translationX = startTranslation

            val delay = (animIndex * 20L)
            child.animate()
                .alpha(1f)
                .translationX(0f)
                .setStartDelay(delay)
                .setDuration(EXPAND_ANIMATION_DURATION_MS)
                .setInterpolator(OvershootInterpolator(1.1f))
                .setListener(if (animIndex == targetChildren.size - 1) object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        isAnimating = false
                        isExpanded = true
                        onExpandChangeListener?.invoke(true)
                    }
                } else null)
                .start()
        }
    }

    fun collapse(animate: Boolean = true) {
        if (currentStyle != STYLE_EXPANDABLE || !isExpanded || isAnimating) return

        val activeVisibleChildren = buttonViews.filterIndexed { index, view ->
            index > 0 && view.visibility == View.VISIBLE
        }

        if (activeVisibleChildren.isEmpty()) {
            isExpanded = false
            onExpandChangeListener?.invoke(false)
            return
        }

        if (!animate) {
            buttonViews.forEachIndexed { index, view ->
                if (index > 0) {
                    view.visibility = View.GONE
                    view.alpha = 0f
                }
            }
            isExpanded = false
            onExpandChangeListener?.invoke(false)
            requestLayout()
            return
        }

        isAnimating = true
        activeVisibleChildren.forEachIndexed { animIndex, child ->
            val endTranslation = if (expandDirection == EXPAND_START) 12f else -12f
            child.animate()
                .alpha(0f)
                .translationX(endTranslation)
                .setStartDelay(0L)
                .setDuration(COLLAPSE_ANIMATION_DURATION_MS)
                .setInterpolator(FastOutLinearInInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        child.visibility = View.GONE
                        if (animIndex == activeVisibleChildren.size - 1) {
                            isAnimating = false
                            isExpanded = false
                            onExpandChangeListener?.invoke(false)
                        }
                    }
                })
                .start()
        }
    }

    fun toggleExpand(animate: Boolean = true) {
        if (isExpanded) collapse(animate) else expand(animate)
    }

    fun isExpanded(): Boolean = isExpanded

    fun setOnExpandChangeListener(listener: (Boolean) -> Unit) {
        onExpandChangeListener = listener
    }

    fun setCollapseOnSelect(collapse: Boolean) {
        collapseOnSelect = collapse
    }

    fun isCollapseOnSelect(): Boolean = collapseOnSelect

    // ==========================================
    // Public API — Selection & State Management
    // ==========================================

    /**
     * Belirtilen butonu seçer, diğer tüm butonların seçimini ve aktifliğini kaldırır.
     * Selects the button at index, clears selection and activation from all other buttons.
     */
    @JvmOverloads
    fun selectButton(index: Int, animate: Boolean = true) {
        if (index !in buttonViews.indices) return
        val oldIndex = selectedIndex
        selectedIndex = index
        buttonViews.forEachIndexed { i, view ->
            val isTarget = (i == index)
            view.isSelected = isTarget
            if (!isTarget && view.isActivated) {
                view.isActivated = false
            }
        }
        if (currentStyle == STYLE_EXPANDABLE && collapseOnSelect) {
            updateAnchorVisual(index)
        }

        if (slideIndicator && buttonViews.isNotEmpty()) {
            val targetView = buttonViews[index]
            if (targetView.width > 0) {
                val targetLeft = targetView.left.toFloat()
                val targetRight = targetView.right.toFloat()
                if (animate && isIndicatorPositionInitialized) {
                    animateIndicator(targetLeft, targetRight)
                } else {
                    indicatorLeft = targetLeft
                    indicatorRight = targetRight
                    isIndicatorPositionInitialized = true
                    invalidate()
                }
            }
        }

        if (oldIndex != index) {
            notifyTabSelected(index)
        }
    }

    /**
     * Tüm butonların seçimini kaldırır.
     */
    fun clearSelection() {
        selectedIndex = -1
        buttonViews.forEach { it.isSelected = false }
        if (slideIndicator) {
            isIndicatorPositionInitialized = false
            invalidate()
        }
    }

    /**
     * Tüm butonların aktiflik durumunu tek bir çağrıyla ayarlar.
     */
    fun setAllActivated(activated: Boolean) {
        buttonViews.forEach { it.isActivated = activated }
    }

    /**
     * Belirtilen butonu tekil olarak aktif (isActivated) yapar, diğerlerini pasife çeker.
     */
    fun activateButton(index: Int) {
        buttonViews.forEachIndexed { i, view ->
            view.isActivated = (i == index)
        }
    }

    fun getSelectedButtonIndex(): Int = selectedIndex

    fun setButtonSelected(index: Int, selected: Boolean) {
        buttonViews.getOrNull(index)?.isSelected = selected
        if (selected) {
            selectedIndex = index
        } else if (selectedIndex == index) {
            selectedIndex = -1
        }
    }

    fun isButtonSelected(index: Int): Boolean = buttonViews.getOrNull(index)?.isSelected ?: false

    fun setButtonActivated(index: Int, activated: Boolean) {
        buttonViews.getOrNull(index)?.isActivated = activated
    }

    fun isButtonActivated(index: Int): Boolean = buttonViews.getOrNull(index)?.isActivated ?: false

    fun setButtonEnabled(index: Int, enabled: Boolean) {
        buttonViews.getOrNull(index)?.isEnabled = enabled
    }

    fun isButtonEnabled(index: Int): Boolean = buttonViews.getOrNull(index)?.isEnabled ?: false

    fun getStyle(): Int = currentStyle

    fun setPillActivated(active: Boolean) {
        setButtonActivated(0, active)
    }

    fun isPillActivated(): Boolean = isButtonActivated(0)

    // ==========================================
    // Public API — Custom Backgrounds & Colors
    // ==========================================

    fun setSelectedBackground(drawable: Drawable?) {
        globalSelectedBackground = drawable
        if (slideIndicator) {
            invalidate()
            return
        }
        buttonViews.forEachIndexed { i, view ->
            val isCircular = (getButtonStyle(context.obtainStyledAttributes(intArrayOf()), i) == BUTTON_STYLE_CIRCULAR)
            applyButtonBackground(
                view,
                isCircular = isCircular,
                customBackgroundDrawable = null,
                selectedCustomDrawable = drawable,
                selectedCustomColor = globalSelectedColor,
                showUnselectedBg = showUnselectedBackground,
                unselectedCustomColor = unselectedButtonColor
            )
        }
    }

    fun setSelectedColor(@ColorInt color: Int) {
        globalSelectedColor = color
        if (slideIndicator) {
            invalidate()
            return
        }
        buttonViews.forEachIndexed { i, view ->
            val isCircular = (currentStyle == STYLE_CIRCULAR)
            applyButtonBackground(
                view,
                isCircular = isCircular,
                customBackgroundDrawable = null,
                selectedCustomDrawable = null,
                selectedCustomColor = color,
                showUnselectedBg = showUnselectedBackground,
                unselectedCustomColor = unselectedButtonColor
            )
        }
    }

    fun setButtonSelectedBackground(index: Int, drawable: Drawable?) {
        val view = buttonViews.getOrNull(index) ?: return
        applyButtonBackground(
            view,
            isCircular = (currentStyle == STYLE_CIRCULAR),
            customBackgroundDrawable = null,
            selectedCustomDrawable = drawable,
            selectedCustomColor = null,
            showUnselectedBg = showUnselectedBackground,
            unselectedCustomColor = unselectedButtonColor
        )
    }

    fun setButtonSelectedColor(index: Int, @ColorInt color: Int) {
        val view = buttonViews.getOrNull(index) ?: return
        applyButtonBackground(
            view,
            isCircular = (currentStyle == STYLE_CIRCULAR),
            customBackgroundDrawable = null,
            selectedCustomDrawable = null,
            selectedCustomColor = color,
            showUnselectedBg = showUnselectedBackground,
            unselectedCustomColor = unselectedButtonColor
        )
    }

    fun setButtonBackground(index: Int, drawable: Drawable?) {
        buttonViews.getOrNull(index)?.background = drawable
    }

    fun setBarBackground(drawable: Drawable?) {
        background = drawable
    }

    fun setBarBackgroundColor(@ColorInt color: Int) {
        val cornerRadius = context.resources.getDimension(R.dimen.sb_bar_radius)
        val shape = GradientDrawable().apply {
            this.cornerRadius = cornerRadius
            setColor(color)
        }
        background = shape
    }

    // ==========================================
    // Public API — Icon Tint Management
    // ==========================================

    fun setIconTint(tintList: ColorStateList?) {
        globalIconTint = tintList
        buttonViews.forEach { button ->
            val iconView = button.findViewById<ImageView>(R.id.sb_item_icon)
                ?: button.findViewById<ImageView>(R.id.sb_vertical_icon)
                ?: button.findViewById<ImageView>(R.id.sb_circular_icon)
                ?: button.findViewById<ImageView>(R.id.sb_pill_icon)
            iconView?.let { ImageViewCompat.setImageTintList(it, tintList) }
        }
    }

    fun setIconTint(@ColorInt color: Int) {
        setIconTint(ColorStateList.valueOf(color))
    }

    fun setButtonIconTint(index: Int, tintList: ColorStateList?) {
        val button = buttonViews.getOrNull(index) ?: return
        val iconView = button.findViewById<ImageView>(R.id.sb_item_icon)
            ?: button.findViewById<ImageView>(R.id.sb_vertical_icon)
            ?: button.findViewById<ImageView>(R.id.sb_circular_icon)
            ?: button.findViewById<ImageView>(R.id.sb_pill_icon)
        iconView?.let { ImageViewCompat.setImageTintList(it, tintList) }
    }

    fun setButtonIconTint(index: Int, @ColorInt color: Int) {
        setButtonIconTint(index, ColorStateList.valueOf(color))
    }

    /**
     * Butonların dokunma ve dalgalanma (ripple) rengini dinamik olarak ayarlar.
     * Sets the touch highlight/ripple color dynamically across all buttons.
     */
    fun setRippleColor(@ColorInt color: Int) {
        globalRippleColor = color
        buttonViews.forEachIndexed { i, view ->
            val isCircular = (getButtonStyle(context.obtainStyledAttributes(intArrayOf()), i) == BUTTON_STYLE_CIRCULAR)
            applyButtonRipple(view, isCircular)
        }
    }

    fun getRippleColor(): Int? = globalRippleColor

    // ==========================================
    // Public API — Tooltip & Accessibility
    // ==========================================

    fun setButtonContentDescription(index: Int, contentDescription: CharSequence?) {
        buttonViews.getOrNull(index)?.contentDescription = contentDescription
    }

    fun setButtonContentDescription(index: Int, @StringRes resId: Int) {
        setButtonContentDescription(index, context.getString(resId))
    }

    fun getButtonContentDescription(index: Int): CharSequence? {
        return buttonViews.getOrNull(index)?.contentDescription
    }

    fun setButtonTooltip(index: Int, tooltipText: CharSequence?) {
        val button = buttonViews.getOrNull(index) ?: return
        applyTooltip(button, tooltipText)
    }

    fun setButtonTooltip(index: Int, @StringRes resId: Int) {
        setButtonTooltip(index, context.getString(resId))
    }

    fun setOnButtonLongClick(index: Int, listener: () -> Boolean) {
        buttonLongClickListeners[index] = listener
    }

    // ==========================================
    // Public API — Click Listeners & Helpers
    // ==========================================

    fun setOnButtonClick(index: Int, listener: () -> Unit) {
        buttonClickListeners[index] = listener
    }

    fun setOnButton1Click(listener: () -> Unit) = setOnButtonClick(0, listener)
    fun setOnButton2Click(listener: () -> Unit) = setOnButtonClick(1, listener)
    fun setOnButton3Click(listener: () -> Unit) = setOnButtonClick(2, listener)
    fun setOnButton4Click(listener: () -> Unit) = setOnButtonClick(3, listener)
    fun setOnButton5Click(listener: () -> Unit) = setOnButtonClick(4, listener)
    fun setOnButton6Click(listener: () -> Unit) = setOnButtonClick(5, listener)

    fun setOnButton1LongClick(listener: () -> Boolean) = setOnButtonLongClick(0, listener)
    fun setOnButton2LongClick(listener: () -> Boolean) = setOnButtonLongClick(1, listener)
    fun setOnButton3LongClick(listener: () -> Boolean) = setOnButtonLongClick(2, listener)
    fun setOnButton4LongClick(listener: () -> Boolean) = setOnButtonLongClick(3, listener)
    fun setOnButton5LongClick(listener: () -> Boolean) = setOnButtonLongClick(4, listener)
    fun setOnButton6LongClick(listener: () -> Boolean) = setOnButtonLongClick(5, listener)

    fun setOnPillClick(listener: () -> Unit) {
        pillClickListener = listener
    }

    fun setButton1Text(text: CharSequence?) = setButtonText(0, text)
    fun setButton1Text(@StringRes resId: Int) = setButtonText(0, context.getString(resId))
    fun setButton2Text(text: CharSequence?) = setButtonText(1, text)
    fun setButton2Text(@StringRes resId: Int) = setButtonText(1, context.getString(resId))
    fun setButton3Text(text: CharSequence?) = setButtonText(2, text)
    fun setButton3Text(@StringRes resId: Int) = setButtonText(2, context.getString(resId))
    fun setButton4Text(text: CharSequence?) = setButtonText(3, text)
    fun setButton4Text(@StringRes resId: Int) = setButtonText(3, context.getString(resId))
    fun setButton5Text(text: CharSequence?) = setButtonText(4, text)
    fun setButton5Text(@StringRes resId: Int) = setButtonText(4, context.getString(resId))
    fun setButton6Text(text: CharSequence?) = setButtonText(5, text)
    fun setButton6Text(@StringRes resId: Int) = setButtonText(5, context.getString(resId))

    fun setButton1Icon(@DrawableRes iconRes: Int) = setButtonIcon(0, iconRes)
    fun setButton2Icon(@DrawableRes iconRes: Int) = setButtonIcon(1, iconRes)
    fun setButton3Icon(@DrawableRes iconRes: Int) = setButtonIcon(2, iconRes)
    fun setButton4Icon(@DrawableRes iconRes: Int) = setButtonIcon(3, iconRes)
    fun setButton5Icon(@DrawableRes iconRes: Int) = setButtonIcon(4, iconRes)
    fun setButton6Icon(@DrawableRes iconRes: Int) = setButtonIcon(5, iconRes)

    fun setButton1IconTint(@ColorInt color: Int) = setButtonIconTint(0, color)
    fun setButton2IconTint(@ColorInt color: Int) = setButtonIconTint(1, color)
    fun setButton3IconTint(@ColorInt color: Int) = setButtonIconTint(2, color)
    fun setButton4IconTint(@ColorInt color: Int) = setButtonIconTint(3, color)
    fun setButton5IconTint(@ColorInt color: Int) = setButtonIconTint(4, color)
    fun setButton6IconTint(@ColorInt color: Int) = setButtonIconTint(5, color)

    fun setButton1Tooltip(text: CharSequence?) = setButtonTooltip(0, text)
    fun setButton2Tooltip(text: CharSequence?) = setButtonTooltip(1, text)
    fun setButton3Tooltip(text: CharSequence?) = setButtonTooltip(2, text)
    fun setButton4Tooltip(text: CharSequence?) = setButtonTooltip(3, text)
    fun setButton5Tooltip(text: CharSequence?) = setButtonTooltip(4, text)
    fun setButton6Tooltip(text: CharSequence?) = setButtonTooltip(5, text)

    fun setButton1ContentDescription(text: CharSequence?) = setButtonContentDescription(0, text)
    fun setButton2ContentDescription(text: CharSequence?) = setButtonContentDescription(1, text)
    fun setButton3ContentDescription(text: CharSequence?) = setButtonContentDescription(2, text)
    fun setButton4ContentDescription(text: CharSequence?) = setButtonContentDescription(3, text)
    fun setButton5ContentDescription(text: CharSequence?) = setButtonContentDescription(4, text)
    fun setButton6ContentDescription(text: CharSequence?) = setButtonContentDescription(5, text)

    fun getButton(index: Int): View? = buttonViews.getOrNull(index)

    fun getButtonCount(): Int = buttonViews.size

    fun setButtonText(index: Int, text: CharSequence?) {
        val button = buttonViews.getOrNull(index) ?: return
        val textView = button.findViewById<TextView>(R.id.sb_item_text)
            ?: button.findViewById<TextView>(R.id.sb_vertical_text)
            ?: button.findViewById<TextView>(R.id.sb_pill_text)
        val iconView = button.findViewById<ImageView>(R.id.sb_item_icon)
            ?: button.findViewById<ImageView>(R.id.sb_vertical_icon)
            ?: button.findViewById<ImageView>(R.id.sb_pill_icon)

        val buttonGapIconText = context.resources.getDimensionPixelSize(R.dimen.sb_button_gap_icon_text)

        textView?.let {
            it.text = text
            if (text.isNullOrEmpty()) {
                it.visibility = View.GONE
                (it.layoutParams as? MarginLayoutParams)?.marginStart = 0
            } else {
                it.visibility = View.VISIBLE
                val isIconVisible = (iconView?.visibility == View.VISIBLE)
                if (isIconVisible) {
                    (it.layoutParams as? MarginLayoutParams)?.marginStart = buttonGapIconText
                    it.gravity = Gravity.CENTER_VERTICAL or Gravity.START
                } else {
                    (it.layoutParams as? MarginLayoutParams)?.marginStart = 0
                    it.gravity = Gravity.CENTER
                }
            }
        }
    }

    fun setButtonIcon(index: Int, @DrawableRes iconRes: Int) {
        val button = buttonViews.getOrNull(index) ?: return
        val iconView = button.findViewById<ImageView>(R.id.sb_item_icon)
            ?: button.findViewById<ImageView>(R.id.sb_vertical_icon)
            ?: button.findViewById<ImageView>(R.id.sb_circular_icon)
            ?: button.findViewById<ImageView>(R.id.sb_pill_icon)
        val textView = button.findViewById<TextView>(R.id.sb_item_text)
            ?: button.findViewById<TextView>(R.id.sb_vertical_text)

        val buttonGapIconText = context.resources.getDimensionPixelSize(R.dimen.sb_button_gap_icon_text)

        iconView?.let {
            if (iconRes != 0) {
                it.setImageResource(iconRes)
                it.visibility = View.VISIBLE
                resolveIconTint(globalIconTint)?.let { tint -> ImageViewCompat.setImageTintList(it, tint) }
                textView?.let { tv ->
                    if (tv.visibility == View.VISIBLE) {
                        (tv.layoutParams as? MarginLayoutParams)?.marginStart = buttonGapIconText
                        tv.gravity = Gravity.CENTER_VERTICAL or Gravity.START
                    }
                }
            } else {
                it.visibility = View.GONE
                textView?.let { tv ->
                    if (tv.visibility == View.VISIBLE) {
                        (tv.layoutParams as? MarginLayoutParams)?.marginStart = 0
                        tv.gravity = Gravity.CENTER
                    }
                }
            }
        }
    }
}
