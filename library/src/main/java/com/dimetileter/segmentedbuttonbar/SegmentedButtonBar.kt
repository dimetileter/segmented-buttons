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
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
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
        const val EXPAND_DOWN = 2
        const val EXPAND_UP = 3
        const val EXPAND_RIGHT = 0
        const val EXPAND_LEFT = 1
        const val EXPAND_BOTTOM = 2
        const val EXPAND_TOP = 3

        private const val MIN_BUTTON_COUNT = 1
        private const val MAX_BUTTON_COUNT = 6
        private const val DEFAULT_BUTTON_COUNT = 2
        private const val DEFAULT_VERTICAL_COUNT = 3
        private const val DEFAULT_EXPANDABLE_COUNT = 3
        private const val EXPAND_ANIMATION_DURATION_MS = 260L
        private const val COLLAPSE_ANIMATION_DURATION_MS = 220L
        private const val DEFAULT_INDICATOR_DURATION_MS = 250L
        private const val MAX_INDICATOR_DURATION_MS = 10_000L
    }

    class TabConfig {
        var text: CharSequence? = null
        var iconRes: Int = 0
        var iconTint: Int? = null
        var textColor: Int? = null
        var textColorStateList: ColorStateList? = null
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
    private var globalTextColor: ColorStateList? = null
    private var globalAllActivated: Boolean? = null
    private var globalSelectedBackground: Drawable? = null
    private var globalSelectedColor: Int? = null
    private var showUnselectedBackground: Boolean = false
    private var unselectedButtonColor: Int? = null
    private var globalRippleColor: Int? = null

    // Kayan Gösterge (Sliding Indicator) Çizim Durumları
    private var indicatorLeft: Float = 0f
    private var indicatorTop: Float = 0f
    private var indicatorRight: Float = 0f
    private var indicatorBottom: Float = 0f
    private var isIndicatorPositionInitialized: Boolean = false
    private var indicatorAnimator: ValueAnimator? = null
    private var indicatorDrawable: Drawable? = null

    private var isExpanded: Boolean = false
    private var isAnimating: Boolean = false
    private var expandContainerAnimator: ValueAnimator? = null
    private var animatingWidth: Int? = null
    private var animatingHeight: Int? = null
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
            .takeIf(::isValidExpandDirection) ?: EXPAND_END
        collapseOnSelect = ta.getBoolean(R.styleable.SegmentedButtonBar_sbCollapseOnSelect, false)
        maxWidthPx = ta.getDimensionPixelSize(R.styleable.SegmentedButtonBar_sbMaxWidth, -1)

        val defaultSlide = (currentStyle == STYLE_TAB)
        slideIndicator = ta.getBoolean(R.styleable.SegmentedButtonBar_sbSlideIndicator, defaultSlide)
        indicatorDurationMs = sanitizeIndicatorDuration(
            ta.getInt(R.styleable.SegmentedButtonBar_sbIndicatorDuration, DEFAULT_INDICATOR_DURATION_MS.toInt()).toLong()
        )

        globalIconTint = ta.getColorStateList(R.styleable.SegmentedButtonBar_sbIconTint)
        globalTextColor = ta.getColorStateList(R.styleable.SegmentedButtonBar_sbTextColor)
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
        val customBarColor = if (ta.hasValue(R.styleable.SegmentedButtonBar_sbBarColor)) {
            ta.getColor(R.styleable.SegmentedButtonBar_sbBarColor, 0)
        } else null

        when {
            customBarColor != null -> {
                setBarColor(customBarColor)
            }
            customBarBg != null -> {
                if (customBarBg is ColorDrawable) {
                    setBarColor(customBarBg.color)
                } else {
                    background = copyDrawable(customBarBg)
                }
            }
            else -> {
                background = ContextCompat.getDrawable(context, R.drawable.bg_segmented_button_bar)
            }
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
        updateIndicatorDrawable()
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
                        unselectedCustomColor = unselectedButtonColor,
                        isPill = false
                    )
                } else {
                    itemView.background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.TRANSPARENT)
                    }
                    applyButtonRipple(itemView, isCircular = true, isPill = false)
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
            } else if (buttonStyle == BUTTON_STYLE_PILL) {
                // Hibrit pill aksiyon butonu / Hybrid pill action button
                itemView = inflater.inflate(R.layout.sb_button_pill_item, this, false)
                val iconView = itemView.findViewById<ImageView>(R.id.sb_pill_icon)
                val textView = itemView.findViewById<TextView>(R.id.sb_pill_text)

                val perButtonTextColor = getButtonTextColor(ta, i)
                perButtonTextColor?.let { textView.setTextColor(it) }
                resolveIconTint(perButtonTint)?.let { ImageViewCompat.setImageTintList(iconView, it) }

                if (!slideIndicator) {
                    applyButtonBackground(
                        itemView,
                        isCircular = false,
                        customBackgroundDrawable = customBg,
                        selectedCustomDrawable = selectedBg ?: globalSelectedBackground,
                        selectedCustomColor = selectedCol ?: globalSelectedColor,
                        showUnselectedBg = showUnselectedBackground,
                        unselectedCustomColor = unselectedButtonColor,
                        isPill = true
                    )
                } else {
                    val cornerRadius = context.resources.getDimension(R.dimen.sb_bar_radius)
                    itemView.background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        this.cornerRadius = cornerRadius
                        setColor(Color.TRANSPARENT)
                    }
                    applyButtonRipple(itemView, isCircular = false, isPill = true)
                }

                val hasCustomIcon = (iconRes != 0)
                val hasText = !textVal.isNullOrEmpty()

                when {
                    hasText -> {
                        iconView.visibility = View.GONE
                        textView.text = textVal
                        textView.visibility = View.VISIBLE
                    }
                    hasCustomIcon -> {
                        iconView.setImageResource(iconRes)
                        iconView.visibility = View.VISIBLE
                        textView.visibility = View.GONE
                    }
                    pillType == PILL_BACK -> {
                        iconView.setImageResource(R.drawable.ic_sb_arrow_back)
                        iconView.visibility = View.VISIBLE
                        textView.visibility = View.GONE
                    }
                    pillType == PILL_TEXT -> {
                        iconView.visibility = View.GONE
                        textView.text = context.getString(R.string.sb_cd_next)
                        textView.visibility = View.VISIBLE
                    }
                    else -> {
                        iconView.setImageResource(R.drawable.ic_sb_arrow_next)
                        iconView.visibility = View.VISIBLE
                        textView.visibility = View.GONE
                    }
                }

                val finalCd = customCd ?: textVal ?: when (pillType) {
                    PILL_BACK -> context.getString(R.string.sb_cd_back)
                    else -> context.getString(R.string.sb_cd_next)
                }
                itemView.contentDescription = finalCd

                val tooltipText = customTooltip ?: textVal ?: customCd
                applyTooltip(itemView, tooltipText)

                val params = LayoutParams(minWidth, buttonHeight).apply {
                    if (i > 0) {
                        marginStart = buttonGap
                    }
                }
                itemView.layoutParams = params

                val isSelectedVal = explicitSelected ?: (barSelectedIndex?.let { it == buttonIndex } ?: false)
                val isActivatedVal = explicitActivated ?: (globalAllActivated ?: true)

                itemView.isSelected = isSelectedVal
                itemView.isActivated = isActivatedVal
                itemView.isEnabled = explicitEnabled

                if (isSelectedVal) {
                    selectedIndex = buttonIndex
                }

                itemView.setOnClickListener {
                    if (!it.isEnabled || !it.isActivated) return@setOnClickListener
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

                val perButtonTextColor = getButtonTextColor(ta, i)
                perButtonTextColor?.let { textView.setTextColor(it) }

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

            val perButtonTextColor = getButtonTextColor(ta, i)
            perButtonTextColor?.let { textView.setTextColor(it) }

            val (iconRes, textVal) = getButtonAttributes(ta, i)
            val (explicitSelected, explicitActivated, explicitEnabled) = getButtonStateAttributes(ta, i)
            val (customBg, selectedBg, selectedCol) = getButtonBackgroundAttributes(ta, i)
            val customCd = getButtonContentDescriptionAttr(ta, i)
            val customTooltip = getButtonTooltipAttr(ta, i)

            if (!slideIndicator) {
                applyButtonBackground(
                    itemView,
                    isCircular = false,
                    customBackgroundDrawable = customBg,
                    selectedCustomDrawable = selectedBg ?: globalSelectedBackground,
                    selectedCustomColor = selectedCol ?: globalSelectedColor,
                    showUnselectedBg = showUnselectedBackground,
                    unselectedCustomColor = unselectedButtonColor,
                    isPill = false
                )
            } else {
                val cornerRadius = context.resources.getDimension(R.dimen.sb_button_radius)
                itemView.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    this.cornerRadius = cornerRadius
                    setColor(Color.TRANSPARENT)
                }
                applyButtonRipple(itemView, isCircular = false, isPill = false)
            }

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
            unselectedCustomColor = unselectedButtonColor,
            isPill = true
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

        val perButtonTextColor = getButtonTextColor(ta, 0)
        perButtonTextColor?.let { textView.setTextColor(it) }

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
            if (!it.isEnabled || !it.isActivated) return@setOnClickListener
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
        val isVertical = (expandDirection == EXPAND_DOWN || expandDirection == EXPAND_UP)
        orientation = if (isVertical) VERTICAL else HORIZONTAL
        clipToPadding = true
        clipChildren = true

        val barPadding = context.resources.getDimensionPixelSize(R.dimen.sb_bar_padding)

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

            itemView.isSelected = explicitSelected ?: false
            itemView.isActivated = explicitActivated ?: (globalAllActivated ?: false)
            itemView.isEnabled = explicitEnabled

            val buttonIndex = i
            if (buttonIndex == 0) {
                itemView.visibility = View.VISIBLE
            } else {
                itemView.visibility = View.GONE
                itemView.alpha = 0f
            }

            itemView.setOnClickListener {
                if (!isExpanded) {
                    expand(animate = true)
                } else {
                    if (autoSelect) {
                        selectButton(buttonIndex)
                    } else {
                        selectedIndex = buttonIndex
                    }
                    buttonClickListeners[buttonIndex]?.invoke()
                    if (collapseOnSelect) {
                        collapse(animate = true)
                    }
                }
            }

            itemView.setOnLongClickListener {
                buttonLongClickListeners[buttonIndex]?.invoke() ?: false
            }

            buttonViews.add(itemView)
        }

        rebuildExpandableLayout()
        updateCollapsedVisual(selectedIndex)
        isExpanded = false
    }

    private fun rebuildExpandableLayout() {
        removeAllViews()
        val isReverse = (expandDirection == EXPAND_START || expandDirection == EXPAND_UP)
        val isVertical = (expandDirection == EXPAND_DOWN || expandDirection == EXPAND_UP)
        val buttonGap = context.resources.getDimensionPixelSize(R.dimen.sb_button_gap)
        val circularSize = context.resources.getDimensionPixelSize(R.dimen.sb_circular_button_size)

        if (isReverse) {
            for (i in 1 until buttonViews.size) {
                val child = buttonViews[i]
                val params = LayoutParams(circularSize, circularSize).apply {
                    if (isVertical) {
                        if (i > 1) topMargin = buttonGap
                    } else {
                        if (i > 1) marginStart = buttonGap
                    }
                }
                child.layoutParams = params
                addView(child)
            }
            if (buttonViews.isNotEmpty()) {
                val anchor = buttonViews[0]
                val params = LayoutParams(circularSize, circularSize).apply {
                    if (buttonViews.size > 1) {
                        if (isVertical) topMargin = buttonGap else marginStart = buttonGap
                    }
                }
                anchor.layoutParams = params
                addView(anchor)
            }
        } else {
            buttonViews.forEachIndexed { i, child ->
                val params = LayoutParams(circularSize, circularSize).apply {
                    if (i > 0) {
                        if (isVertical) topMargin = buttonGap else marginStart = buttonGap
                    }
                }
                child.layoutParams = params
                addView(child)
            }
        }
    }

    private fun applyButtonMetaToView(view: View, meta: ButtonMeta, isSelected: Boolean) {
        val iconView = view.findViewById<ImageView>(R.id.sb_circular_icon)
        val finalIcon = if (meta.iconRes != 0) meta.iconRes else R.drawable.ic_sb_arrow_next
        iconView?.setImageResource(finalIcon)
        resolveIconTint(meta.iconTint)?.let { ImageViewCompat.setImageTintList(iconView, it) }

        val finalCd = meta.contentDescription ?: meta.text ?: getDefaultContentDescription(0)
        view.contentDescription = finalCd

        val tooltipText = meta.tooltip ?: meta.text ?: meta.contentDescription
        applyTooltip(view, tooltipText)

        applyButtonBackground(
            view,
            isCircular = true,
            customBackgroundDrawable = meta.customBg,
            selectedCustomDrawable = meta.selectedBg ?: globalSelectedBackground,
            selectedCustomColor = meta.selectedColor ?: globalSelectedColor,
            showUnselectedBg = showUnselectedBackground,
            unselectedCustomColor = unselectedButtonColor
        )
        view.isSelected = isSelected
    }

    private fun updateCollapsedVisual(index: Int) {
        if (buttonViews.isEmpty() || index !in buttonMetaList.indices) return
        val anchorView = buttonViews[0]
        val meta = buttonMetaList[index]
        applyButtonMetaToView(anchorView, meta, isSelected = true)
    }

    private fun restoreButtonVisual(index: Int) {
        if (index !in buttonViews.indices || index !in buttonMetaList.indices) return
        val view = buttonViews[index]
        val meta = buttonMetaList[index]
        applyButtonMetaToView(view, meta, isSelected = (index == selectedIndex))
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

    private fun sanitizeIndicatorDuration(durationMs: Long): Long {
        return durationMs.coerceIn(0L, MAX_INDICATOR_DURATION_MS)
    }

    private fun isValidExpandDirection(direction: Int): Boolean {
        return direction == EXPAND_END ||
            direction == EXPAND_START ||
            direction == EXPAND_DOWN ||
            direction == EXPAND_UP
    }

    private fun copyDrawable(drawable: Drawable?): Drawable? {
        return drawable?.constantState?.newDrawable()?.mutate() ?: drawable?.mutate()
    }

    private fun createButtonShapeDrawable(@ColorInt color: Int, isCircular: Boolean, isPill: Boolean): Drawable {
        val radiusRes = when {
            isPill -> R.dimen.sb_bar_radius
            isCircular -> R.dimen.sb_circular_button_size
            else -> R.dimen.sb_button_radius
        }
        val cornerRadius = context.resources.getDimension(radiusRes)
        return GradientDrawable().apply {
            shape = if (isCircular) GradientDrawable.OVAL else GradientDrawable.RECTANGLE
            if (!isCircular) {
                this.cornerRadius = cornerRadius
            }
            setColor(color)
        }
    }

    private fun updateIndicatorDrawable() {
        indicatorDrawable = copyDrawable(globalSelectedBackground) ?: createButtonShapeDrawable(
            globalSelectedColor ?: ContextCompat.getColor(context, R.color.sb_button_selected),
            isCircular = false,
            isPill = false
        )
    }

    /**
     * Butonun tıklama, dokunma ve dalgalanma (ripple) alanlarını tam 54dp yuvarlak köşelere göre kırpar.
     * Clips the button touch, focus, and ripple areas to match exact rounded corners.
     */
    private fun applyButtonOutline(view: View, isCircular: Boolean, isPill: Boolean = false) {
        if (isInEditMode) return
        val radiusRes = when {
            isPill -> R.dimen.sb_bar_radius
            isCircular -> R.dimen.sb_circular_button_size
            else -> R.dimen.sb_button_radius
        }
        val radius = context.resources.getDimension(radiusRes)
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
    private fun applyButtonRipple(view: View, isCircular: Boolean, isPill: Boolean = false) {
        applyButtonOutline(view, isCircular, isPill)
        if (isInEditMode) return
        val radiusRes = when {
            isPill -> R.dimen.sb_bar_radius
            isCircular -> R.dimen.sb_circular_button_size
            else -> R.dimen.sb_button_radius
        }
        val radius = context.resources.getDimension(radiusRes)
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
        applyButtonOutline(view, isCircular, isPill)
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
        unselectedCustomColor: Int?,
        isPill: Boolean = false
    ) {
        applyButtonRipple(view, isCircular, isPill)
        if (customBackgroundDrawable != null) {
            view.background = copyDrawable(customBackgroundDrawable)
            return
        }

        // Özel bir renk veya arkaplan atanmamışsa standart state drawable'ını koru
        if (selectedCustomDrawable == null && selectedCustomColor == null && !showUnselectedBg) {
            val standardRes = when {
                isPill -> R.drawable.state_segmented_next_button
                isCircular -> R.drawable.state_segmented_circular_button
                else -> R.drawable.state_segmented_button
            }
            view.background = ContextCompat.getDrawable(context, standardRes)
            return
        }

        val disabledColor = ContextCompat.getColor(context, R.color.sb_button_disabled)
        val defaultSelectedColor = ContextCompat.getColor(context, R.color.sb_button_selected)

        val stateList = StateListDrawable()

        // 1. Devre dışı durumu (Disabled state)
        val disabledDrawable = createButtonShapeDrawable(disabledColor, isCircular, isPill)
        stateList.addState(intArrayOf(-android.R.attr.state_enabled), disabledDrawable)

        if (isPill) {
            stateList.addState(
                intArrayOf(-android.R.attr.state_activated),
                copyDrawable(disabledDrawable) ?: disabledDrawable
            )
        }

        // 2. Seçili ve Aktif durumu (Selected & Activated state)
        val selectedStateDrawable: Drawable = when {
            selectedCustomDrawable != null -> copyDrawable(selectedCustomDrawable) ?: selectedCustomDrawable
            selectedCustomColor != null -> createButtonShapeDrawable(selectedCustomColor, isCircular, isPill)
            else -> createButtonShapeDrawable(defaultSelectedColor, isCircular, isPill)
        }
        stateList.addState(
            intArrayOf(android.R.attr.state_selected),
            copyDrawable(selectedStateDrawable) ?: selectedStateDrawable
        )
        stateList.addState(
            intArrayOf(android.R.attr.state_activated),
            copyDrawable(selectedStateDrawable) ?: selectedStateDrawable
        )

        // 3. Seçilmemiş durumu (Unselected state)
        val unselectedStateDrawable: Drawable = if (showUnselectedBg && unselectedCustomColor != null) {
            createButtonShapeDrawable(unselectedCustomColor, isCircular, isPill)
        } else {
            createButtonShapeDrawable(Color.TRANSPARENT, isCircular, isPill)
        }
        stateList.addState(intArrayOf(), unselectedStateDrawable)

        view.background = stateList
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (slideIndicator && buttonViews.isNotEmpty() && selectedIndex in buttonViews.indices) {
            val target = buttonViews[selectedIndex]
            if (target.width > 0 && target.height > 0 && (!isIndicatorPositionInitialized || changed)) {
                indicatorLeft = target.left.toFloat()
                indicatorTop = target.top.toFloat()
                indicatorRight = target.right.toFloat()
                indicatorBottom = target.bottom.toFloat()
                isIndicatorPositionInitialized = true
                invalidate()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (slideIndicator && buttonViews.isNotEmpty() && selectedIndex in buttonViews.indices) {
            val selectedView = buttonViews[selectedIndex]
            if (!isIndicatorPositionInitialized && selectedView.width > 0 && selectedView.height > 0) {
                indicatorLeft = selectedView.left.toFloat()
                indicatorTop = selectedView.top.toFloat()
                indicatorRight = selectedView.right.toFloat()
                indicatorBottom = selectedView.bottom.toFloat()
                isIndicatorPositionInitialized = true
            }

            if (isIndicatorPositionInitialized && indicatorRight > indicatorLeft && indicatorBottom > indicatorTop) {
                indicatorDrawable?.let { drawable ->
                    drawable.setBounds(
                        indicatorLeft.toInt(),
                        indicatorTop.toInt(),
                        indicatorRight.toInt(),
                        indicatorBottom.toInt()
                    )
                    drawable.draw(canvas)
                }
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

    private fun getButtonTextColor(ta: TypedArray, index: Int): ColorStateList? {
        val specific = when (index) {
            0 -> ta.getColorStateList(R.styleable.SegmentedButtonBar_sbButton1TextColor)
            1 -> ta.getColorStateList(R.styleable.SegmentedButtonBar_sbButton2TextColor)
            2 -> ta.getColorStateList(R.styleable.SegmentedButtonBar_sbButton3TextColor)
            3 -> ta.getColorStateList(R.styleable.SegmentedButtonBar_sbButton4TextColor)
            4 -> ta.getColorStateList(R.styleable.SegmentedButtonBar_sbButton5TextColor)
            5 -> ta.getColorStateList(R.styleable.SegmentedButtonBar_sbButton6TextColor)
            else -> null
        }
        return specific ?: globalTextColor
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

        val targetWidthSpec = when {
            maxWidthPx <= 0 -> widthMeasureSpec
            widthMode == MeasureSpec.UNSPECIFIED -> widthMeasureSpec
            widthSize > maxWidthPx -> MeasureSpec.makeMeasureSpec(maxWidthPx, MeasureSpec.AT_MOST)
            else -> widthMeasureSpec
        }

        super.onMeasure(targetWidthSpec, heightMeasureSpec)

        if (maxWidthPx > 0 && widthMode == MeasureSpec.UNSPECIFIED && measuredWidth > maxWidthPx) {
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(maxWidthPx, MeasureSpec.AT_MOST),
                heightMeasureSpec
            )
        }

        if (currentStyle == STYLE_EXPANDABLE && isAnimating) {
            val animW = animatingWidth
            val animH = animatingHeight
            val finalW = animW ?: measuredWidth
            val finalH = animH ?: measuredHeight
            setMeasuredDimension(finalW, finalH)
        }
    }

    private fun isViewCircular(view: View): Boolean {
        return view.id == R.id.sb_circular_root || currentStyle == STYLE_CIRCULAR
    }

    private fun isViewPill(view: View): Boolean {
        return view.id == R.id.sb_pill_root || currentStyle == STYLE_PILL
    }

    // ==========================================
    // Public API — Sliding Pill Indicator & Tabs
    // ==========================================

    fun setSlideIndicator(enabled: Boolean) {
        slideIndicator = enabled
        isIndicatorPositionInitialized = false
        updateIndicatorDrawable()
        buttonViews.forEach { view ->
            val isCircular = isViewCircular(view)
            val isPill = isViewPill(view)
            if (enabled) {
                val cornerRadius = context.resources.getDimension(
                    if (isPill) R.dimen.sb_bar_radius else R.dimen.sb_button_radius
                )
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
                    unselectedCustomColor = unselectedButtonColor,
                    isPill = isPill
                )
            }
            applyButtonOutline(view, isCircular, isPill)
        }
        invalidate()
    }

    fun isSlideIndicator(): Boolean = slideIndicator

    fun setIndicatorDuration(durationMs: Long) {
        indicatorDurationMs = sanitizeIndicatorDuration(durationMs)
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
        val targetTop = currentView.top + positionOffset * (nextView.top - currentView.top)
        val targetRight = currentView.right + positionOffset * (nextView.right - currentView.right)
        val targetBottom = currentView.bottom + positionOffset * (nextView.bottom - currentView.bottom)

        indicatorLeft = targetLeft
        indicatorTop = targetTop
        indicatorRight = targetRight
        indicatorBottom = targetBottom
        isIndicatorPositionInitialized = true
        invalidate()
    }

    private fun animateIndicator(targetLeft: Float, targetTop: Float, targetRight: Float, targetBottom: Float) {
        indicatorAnimator?.cancel()
        val startLeft = indicatorLeft
        val startTop = indicatorTop
        val startRight = indicatorRight
        val startBottom = indicatorBottom

        indicatorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = indicatorDurationMs
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                indicatorLeft = startLeft + fraction * (targetLeft - startLeft)
                indicatorTop = startTop + fraction * (targetTop - startTop)
                indicatorRight = startRight + fraction * (targetRight - startRight)
                indicatorBottom = startBottom + fraction * (targetBottom - startBottom)
                invalidate()
            }
            start()
        }
    }

    private fun cancelRunningAnimations() {
        indicatorAnimator?.removeAllUpdateListeners()
        indicatorAnimator?.removeAllListeners()
        indicatorAnimator?.cancel()
        indicatorAnimator = null

        expandContainerAnimator?.removeAllUpdateListeners()
        expandContainerAnimator?.removeAllListeners()
        expandContainerAnimator?.cancel()
        expandContainerAnimator = null

        buttonViews.forEach { child ->
            child.animate().cancel()
            child.animate().setStartDelay(0L)
        }

        animatingWidth = null
        animatingHeight = null
        isAnimating = false
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
        unbindViewPager2()
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
                cfg.textColor?.let { setButtonTextColor(i, it) }
                cfg.textColorStateList?.let { setButtonTextColor(i, it) }
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

    fun unbindViewPager2() {
        registeredViewPagerCallback?.let { callback ->
            boundViewPager?.unregisterOnPageChangeCallback(callback)
        }
        registeredViewPagerCallback = null
        boundViewPager = null
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

    fun clearFragmentBinding() {
        internalFragmentTabListener = null
    }

    override fun onDetachedFromWindow() {
        unbindViewPager2()
        clearFragmentBinding()
        cancelRunningAnimations()
        super.onDetachedFromWindow()
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
        expandContainerAnimator?.cancel()

        val isVertical = (expandDirection == EXPAND_DOWN || expandDirection == EXPAND_UP)
        val circularSize = context.resources.getDimensionPixelSize(R.dimen.sb_circular_button_size)
        val barPadding = context.resources.getDimensionPixelSize(R.dimen.sb_bar_padding)
        val buttonGap = context.resources.getDimensionPixelSize(R.dimen.sb_button_gap)

        // 0. butonun görselini orijinal haline (buttonMetaList[0]) geri yükle
        restoreButtonVisual(0)

        // Tüm butonların seçim durumlarını güncelle
        buttonViews.forEachIndexed { i, view ->
            view.isSelected = (i == selectedIndex)
        }

        val totalActiveButtons = buttonViews.size
        val collapsedSize = circularSize + 2 * barPadding
        val expandedSize = 2 * barPadding + totalActiveButtons * circularSize + (totalActiveButtons - 1) * buttonGap

        if (!animate) {
            buttonViews.forEach { child ->
                child.visibility = View.VISIBLE
                child.alpha = 1f
                child.scaleX = 1f
                child.scaleY = 1f
                child.translationX = 0f
                child.translationY = 0f
            }
            isExpanded = true
            onExpandChangeListener?.invoke(true)
            requestLayout()
            return
        }

        isAnimating = true
        isExpanded = true

        // 1. Çubuğun Dış Kapsül Boyutunu Eşzamanlı Genişleten Akıcı Morf Animasyonu
        expandContainerAnimator = ValueAnimator.ofInt(collapsedSize, expandedSize).apply {
            duration = EXPAND_ANIMATION_DURATION_MS
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { animator ->
                val currentSize = animator.animatedValue as Int
                if (isVertical) {
                    animatingHeight = currentSize
                } else {
                    animatingWidth = currentSize
                }
                requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animatingWidth = null
                    animatingHeight = null
                    isAnimating = false
                    requestLayout()
                    onExpandChangeListener?.invoke(true)
                }
            })
            start()
        }

        // 2. İç Butonların Kademeli ve Yumuşak Açılış Animasyonu (1..N-1)
        val expandingChildren = if (buttonViews.size > 1) buttonViews.subList(1, buttonViews.size) else emptyList()
        expandingChildren.forEachIndexed { animIndex, child ->
            child.visibility = View.VISIBLE
            child.alpha = 0f
            child.scaleX = 0.78f
            child.scaleY = 0.78f

            val (transX, transY) = when (expandDirection) {
                EXPAND_START, EXPAND_LEFT -> Pair(20f, 0f)
                EXPAND_END, EXPAND_RIGHT -> Pair(-20f, 0f)
                EXPAND_UP, EXPAND_TOP -> Pair(0f, 20f)
                EXPAND_DOWN, EXPAND_BOTTOM -> Pair(0f, -20f)
                else -> Pair(-20f, 0f)
            }
            child.translationX = transX
            child.translationY = transY

            val delay = (animIndex * 22L)
            child.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationX(0f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(EXPAND_ANIMATION_DURATION_MS)
                .setInterpolator(OvershootInterpolator(1.15f))
                .start()
        }
    }

    fun collapse(animate: Boolean = true) {
        if (currentStyle != STYLE_EXPANDABLE || !isExpanded || isAnimating) return
        expandContainerAnimator?.cancel()

        val isVertical = (expandDirection == EXPAND_DOWN || expandDirection == EXPAND_UP)
        val circularSize = context.resources.getDimensionPixelSize(R.dimen.sb_circular_button_size)
        val barPadding = context.resources.getDimensionPixelSize(R.dimen.sb_bar_padding)
        val buttonGap = context.resources.getDimensionPixelSize(R.dimen.sb_button_gap)

        val closingChildren = if (buttonViews.size > 1) buttonViews.subList(1, buttonViews.size) else emptyList()
        val totalActiveButtons = buttonViews.size
        val collapsedSize = circularSize + 2 * barPadding
        val expandedSize = 2 * barPadding + totalActiveButtons * circularSize + (totalActiveButtons - 1) * buttonGap
        val currentSize = if (isVertical) {
            if (height > 0) height else expandedSize
        } else {
            if (width > 0) width else expandedSize
        }

        if (!animate) {
            closingChildren.forEach { view ->
                view.visibility = View.GONE
                view.alpha = 0f
                view.scaleX = 1f
                view.scaleY = 1f
                view.translationX = 0f
                view.translationY = 0f
            }
            updateCollapsedVisual(selectedIndex)
            isExpanded = false
            onExpandChangeListener?.invoke(false)
            requestLayout()
            return
        }

        isAnimating = true

        // 1. Çubuğun Dış Kapsülünü Butonlarla Eşzamanlı Daraltan Morf Animasyonu
        expandContainerAnimator = ValueAnimator.ofInt(currentSize, collapsedSize).apply {
            duration = COLLAPSE_ANIMATION_DURATION_MS
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { animator ->
                val size = animator.animatedValue as Int
                if (isVertical) {
                    animatingHeight = size
                } else {
                    animatingWidth = size
                }
                requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animatingWidth = null
                    animatingHeight = null
                    closingChildren.forEach { child ->
                        child.visibility = View.GONE
                        child.scaleX = 1f
                        child.scaleY = 1f
                        child.translationX = 0f
                        child.translationY = 0f
                    }
                    updateCollapsedVisual(selectedIndex)
                    isAnimating = false
                    isExpanded = false
                    requestLayout()
                    onExpandChangeListener?.invoke(false)
                }
            })
            start()
        }

        // 2. İç Butonların Çubuğun İçine Doğru Süzülerek Kaybolması (Ters Kademeli Dalga)
        val totalCount = closingChildren.size
        closingChildren.forEachIndexed { animIndex, child ->
            val (endTransX, endTransY) = when (expandDirection) {
                EXPAND_START, EXPAND_LEFT -> Pair(16f, 0f)
                EXPAND_END, EXPAND_RIGHT -> Pair(-16f, 0f)
                EXPAND_UP, EXPAND_TOP -> Pair(0f, 16f)
                EXPAND_DOWN, EXPAND_BOTTOM -> Pair(0f, -16f)
                else -> Pair(-16f, 0f)
            }
            val delay = ((totalCount - 1 - animIndex) * 10L)

            child.animate()
                .alpha(0f)
                .scaleX(0.75f)
                .scaleY(0.75f)
                .translationX(endTransX)
                .translationY(endTransY)
                .setStartDelay(delay)
                .setDuration(COLLAPSE_ANIMATION_DURATION_MS)
                .setInterpolator(FastOutSlowInInterpolator())
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

    fun setExpandDirection(direction: Int) {
        if (!isValidExpandDirection(direction)) return
        if (expandDirection == direction) return
        expandDirection = direction
        if (currentStyle == STYLE_EXPANDABLE) {
            val isVertical = (direction == EXPAND_DOWN || direction == EXPAND_UP)
            orientation = if (isVertical) VERTICAL else HORIZONTAL
            rebuildExpandableLayout()
        }
    }

    fun getExpandDirection(): Int = expandDirection

    // ==========================================
    // Public API — Selection & State Management
    // ==========================================

    /**
     * Belirtilen butonu seçer ve diğer tüm butonların seçimini kaldırır.
     * Selects the button at index and clears selection from all other buttons.
     */
    @JvmOverloads
    fun selectButton(index: Int, animate: Boolean = true) {
        if (index !in buttonViews.indices) return
        val oldIndex = selectedIndex
        selectedIndex = index
        buttonViews.forEachIndexed { i, view ->
            view.isSelected = (i == index)
        }
        if (currentStyle == STYLE_EXPANDABLE && !isExpanded && !isAnimating) {
            updateCollapsedVisual(index)
        }

        if (slideIndicator && buttonViews.isNotEmpty()) {
            val targetView = buttonViews[index]
            if (targetView.width > 0 && targetView.height > 0) {
                val targetLeft = targetView.left.toFloat()
                val targetTop = targetView.top.toFloat()
                val targetRight = targetView.right.toFloat()
                val targetBottom = targetView.bottom.toFloat()
                if (animate && isIndicatorPositionInitialized) {
                    animateIndicator(targetLeft, targetTop, targetRight, targetBottom)
                } else {
                    indicatorLeft = targetLeft
                    indicatorTop = targetTop
                    indicatorRight = targetRight
                    indicatorBottom = targetBottom
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
            indicatorLeft = 0f
            indicatorTop = 0f
            indicatorRight = 0f
            indicatorBottom = 0f
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

    /**
     * Seçili butonun 0 tabanlı indeksini döndürür. Hiçbir buton seçili değilse -1 döner.
     * Returns the 0-based index of the currently selected button, or -1 if none is selected.
     */
    fun getSelectedButtonIndex(): Int = selectedIndex

    /**
     * Belirtilen indisteki butonun seçim durumunu ayarlar.
     * [selected] true ise tekil radyo-buton seçim semantiği korunur ve diğer butonlar deselect edilir.
     * Geçersiz indeksler güvenli bir şekilde yok sayılır.
     *
     * Sets selection state for the button at [index].
     * If [selected] is true, single radio-selection semantics are enforced, deselecting other items.
     * Out-of-bounds indices are safely ignored.
     */
    fun setButtonSelected(index: Int, selected: Boolean) {
        if (selected) {
            selectButton(index, animate = false)
        } else if (selectedIndex == index) {
            buttonViews.getOrNull(index)?.isSelected = false
            selectedIndex = -1
        } else {
            buttonViews.getOrNull(index)?.isSelected = false
        }
    }

    /**
     * Belirtilen indisteki butonun seçili olup olmadığını döndürür. İndeks geçersizse false döner.
     * Returns whether the button at [index] is currently selected. Returns false if index is out of bounds.
     */
    fun isButtonSelected(index: Int): Boolean = buttonViews.getOrNull(index)?.isSelected ?: false

    /**
     * Belirtilen indisteki butonun etkinleştirme (activation / lock gating) durumunu ayarlar.
     * Radyo buton seçiminden bağımsızdır. Geçersiz indeksler güvenli bir şekilde yok sayılır.
     *
     * Sets activation/gate state for the button at [index] independently of selection state.
     * Out-of-bounds indices are safely ignored.
     */
    fun setButtonActivated(index: Int, activated: Boolean) {
        buttonViews.getOrNull(index)?.isActivated = activated
    }

    /**
     * Belirtilen indisteki butonun etkinleştirilmiş (activated) olup olmadığını döndürür.
     * Returns whether the button at [index] is activated.
     */
    fun isButtonActivated(index: Int): Boolean = buttonViews.getOrNull(index)?.isActivated ?: false

    /**
     * Belirtilen indisteki butonu etkinleştirir veya devre dışı bırakır.
     * Devre dışı butonlar tıklanamaz ve görsel olarak disabled görünüm alır.
     *
     * Enables or disables the button at [index]. Disabled buttons cannot be clicked.
     */
    fun setButtonEnabled(index: Int, enabled: Boolean) {
        buttonViews.getOrNull(index)?.isEnabled = enabled
    }

    /**
     * Belirtilen indisteki butonun etkin (enabled) olup olmadığını döndürür.
     * Returns whether the button at [index] is enabled.
     */
    fun isButtonEnabled(index: Int): Boolean = buttonViews.getOrNull(index)?.isEnabled ?: false

    /**
     * SegmentedButtonBar'ın geçerli stil türünü döndürür (ör. [STYLE_HORIZONTAL], [STYLE_TAB] vb.).
     * Returns the active style mode of the SegmentedButtonBar.
     */
    fun getStyle(): Int = currentStyle

    /**
     * Pill aksiyon butonunun kilit (activation) durumunu ayarlar.
     * false olduğunda buton kilitlenir, tıklamalar engellenir ve devre dışı görünüm uygulanır.
     *
     * Sets the lock/activation state of the primary pill action button.
     * When false, clicks are blocked and disabled visual styling is displayed.
     */
    fun setPillActivated(active: Boolean) {
        setButtonActivated(0, active)
    }

    /**
     * Pill aksiyon butonunun aktif olup olmadığını döndürür.
     * Returns whether the pill action button is activated.
     */
    fun isPillActivated(): Boolean = isButtonActivated(0)

    // ==========================================
    // Public API — Custom Backgrounds & Colors
    // ==========================================

    fun setSelectedBackground(drawable: Drawable?) {
        globalSelectedBackground = drawable
        updateIndicatorDrawable()
        if (slideIndicator) {
            invalidate()
            return
        }
        buttonViews.forEach { view ->
            val isCircular = isViewCircular(view)
            val isPill = isViewPill(view)
            applyButtonBackground(
                view,
                isCircular = isCircular,
                customBackgroundDrawable = null,
                selectedCustomDrawable = drawable,
                selectedCustomColor = globalSelectedColor,
                showUnselectedBg = showUnselectedBackground,
                unselectedCustomColor = unselectedButtonColor,
                isPill = isPill
            )
        }
    }

    fun setSelectedColor(@ColorInt color: Int) {
        globalSelectedColor = color
        updateIndicatorDrawable()
        if (slideIndicator) {
            invalidate()
            return
        }
        buttonViews.forEach { view ->
            val isCircular = isViewCircular(view)
            val isPill = isViewPill(view)
            applyButtonBackground(
                view,
                isCircular = isCircular,
                customBackgroundDrawable = null,
                selectedCustomDrawable = null,
                selectedCustomColor = color,
                showUnselectedBg = showUnselectedBackground,
                unselectedCustomColor = unselectedButtonColor,
                isPill = isPill
            )
        }
    }

    fun setButtonSelectedBackground(index: Int, drawable: Drawable?) {
        val view = buttonViews.getOrNull(index) ?: return
        if (index in buttonMetaList.indices) {
            val old = buttonMetaList[index]
            buttonMetaList[index] = old.copy(selectedBg = drawable)
        }
        applyButtonBackground(
            view,
            isCircular = isViewCircular(view),
            customBackgroundDrawable = null,
            selectedCustomDrawable = drawable,
            selectedCustomColor = null,
            showUnselectedBg = showUnselectedBackground,
            unselectedCustomColor = unselectedButtonColor,
            isPill = isViewPill(view)
        )
    }

    fun setButtonSelectedColor(index: Int, @ColorInt color: Int) {
        val view = buttonViews.getOrNull(index) ?: return
        if (index in buttonMetaList.indices) {
            val old = buttonMetaList[index]
            buttonMetaList[index] = old.copy(selectedColor = color)
        }
        applyButtonBackground(
            view,
            isCircular = isViewCircular(view),
            customBackgroundDrawable = null,
            selectedCustomDrawable = null,
            selectedCustomColor = color,
            showUnselectedBg = showUnselectedBackground,
            unselectedCustomColor = unselectedButtonColor,
            isPill = isViewPill(view)
        )
    }

    fun setButtonBackground(index: Int, drawable: Drawable?) {
        if (index in buttonMetaList.indices) {
            val old = buttonMetaList[index]
            buttonMetaList[index] = old.copy(customBg = drawable)
        }
        buttonViews.getOrNull(index)?.background = copyDrawable(drawable)
    }

    /**
     * Çubuğun dış sarmalayıcı kapsül arka plan rengini (60dp yuvarlak köşeleri koruyarak) dinamik olarak ayarlar.
     * Sets the bar container background color dynamically while preserving the 60dp rounded capsule shape.
     */
    fun setBarColor(@ColorInt color: Int) {
        val cornerRadius = context.resources.getDimension(R.dimen.sb_bar_radius)
        val shape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            this.cornerRadius = cornerRadius
            setColor(color)
        }
        background = shape
    }

    /**
     * Çubuğun arka plan rengini ayarlar (setBarColor ile aynı şekilde 60dp yuvarlak köşeleri korur).
     */
    fun setBarBackgroundColor(@ColorInt color: Int) {
        setBarColor(color)
    }

    fun setBarBackground(drawable: Drawable?) {
        if (drawable is ColorDrawable) {
            setBarColor(drawable.color)
        } else if (drawable != null) {
            background = copyDrawable(drawable)
        }
    }

    fun setBarBackground(@ColorInt color: Int) {
        setBarColor(color)
    }

    fun setBarBackgroundResource(@DrawableRes resId: Int) {
        val drawable = ContextCompat.getDrawable(context, resId)
        if (drawable is ColorDrawable) {
            setBarColor(drawable.color)
        } else {
            background = copyDrawable(drawable)
        }
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
        if (index in buttonMetaList.indices) {
            val old = buttonMetaList[index]
            buttonMetaList[index] = old.copy(iconTint = tintList)
        }
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

    // ==========================================
    // Public API — Text Color Management
    // ==========================================

    /**
     * Tüm butonların metin rengini (ColorStateList) dinamik olarak ayarlar.
     * Sets the text color (ColorStateList) dynamically across all buttons.
     */
    fun setTextColor(colorStateList: ColorStateList?) {
        globalTextColor = colorStateList
        buttonViews.forEach { button ->
            val textView = button.findViewById<TextView>(R.id.sb_item_text)
                ?: button.findViewById<TextView>(R.id.sb_vertical_text)
                ?: button.findViewById<TextView>(R.id.sb_pill_text)
            textView?.let {
                if (colorStateList != null) {
                    it.setTextColor(colorStateList)
                }
            }
        }
    }

    /**
     * Tüm butonların metin rengini (ColorInt) dinamik olarak ayarlar.
     * Sets the solid text color dynamically across all buttons.
     */
    fun setTextColor(@ColorInt color: Int) {
        setTextColor(ColorStateList.valueOf(color))
    }

    /**
     * Belirli bir butonun metin rengini (ColorStateList) ayarlar.
     * Sets the text color (ColorStateList) for a specific button.
     */
    fun setButtonTextColor(index: Int, colorStateList: ColorStateList?) {
        val button = buttonViews.getOrNull(index) ?: return
        val textView = button.findViewById<TextView>(R.id.sb_item_text)
            ?: button.findViewById<TextView>(R.id.sb_vertical_text)
            ?: button.findViewById<TextView>(R.id.sb_pill_text)
        textView?.let {
            if (colorStateList != null) {
                it.setTextColor(colorStateList)
            }
        }
    }

    /**
     * Belirli bir butonun metin rengini (ColorInt) ayarlar.
     * Sets the solid text color for a specific button.
     */
    fun setButtonTextColor(index: Int, @ColorInt color: Int) {
        setButtonTextColor(index, ColorStateList.valueOf(color))
    }

    /**
     * Belirli bir butonun metin rengini (ColorStateList) döndürür.
     * Returns the ColorStateList text colors of a specific button.
     */
    fun getButtonTextColor(index: Int): ColorStateList? {
        val button = buttonViews.getOrNull(index) ?: return null
        val textView = button.findViewById<TextView>(R.id.sb_item_text)
            ?: button.findViewById<TextView>(R.id.sb_vertical_text)
            ?: button.findViewById<TextView>(R.id.sb_pill_text)
        return textView?.textColors
    }

    /**
     * Butonların dokunma ve dalgalanma (ripple) rengini dinamik olarak ayarlar.
     * Sets the touch highlight/ripple color dynamically across all buttons.
     */
    fun setRippleColor(@ColorInt color: Int) {
        globalRippleColor = color
        buttonViews.forEach { view ->
            val isCircular = isViewCircular(view)
            applyButtonRipple(view, isCircular, isViewPill(view))
        }
    }

    fun getRippleColor(): Int? = globalRippleColor

    // ==========================================
    // Public API — Tooltip & Accessibility
    // ==========================================

    fun setButtonContentDescription(index: Int, contentDescription: CharSequence?) {
        if (index in buttonMetaList.indices) {
            val old = buttonMetaList[index]
            buttonMetaList[index] = old.copy(contentDescription = contentDescription?.toString())
        }
        buttonViews.getOrNull(index)?.contentDescription = contentDescription
    }

    fun setButtonContentDescription(index: Int, @StringRes resId: Int) {
        setButtonContentDescription(index, context.getString(resId))
    }

    fun getButtonContentDescription(index: Int): CharSequence? {
        return buttonViews.getOrNull(index)?.contentDescription
    }

    fun setButtonTooltip(index: Int, tooltipText: CharSequence?) {
        if (index in buttonMetaList.indices) {
            val old = buttonMetaList[index]
            buttonMetaList[index] = old.copy(tooltip = tooltipText?.toString())
        }
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

    /**
     * Belirtilen indisteki buton için tıklama dinleyicisi atar.
     * Geçersiz indeksler için callback tetiklenmez.
     *
     * Sets a click listener callback for the button at [index].
     * Callbacks will not fire for out-of-bounds indices.
     */
    fun setOnButtonClick(index: Int, listener: () -> Unit) {
        buttonClickListeners[index] = listener
    }

    /** 1. buton (indeks 0) için tıklama dinleyicisi / Click listener for button 1 (index 0). */
    fun setOnButton1Click(listener: () -> Unit) = setOnButtonClick(0, listener)
    /** 2. buton (indeks 1) için tıklama dinleyicisi / Click listener for button 2 (index 1). */
    fun setOnButton2Click(listener: () -> Unit) = setOnButtonClick(1, listener)
    /** 3. buton (indeks 2) için tıklama dinleyicisi / Click listener for button 3 (index 2). */
    fun setOnButton3Click(listener: () -> Unit) = setOnButtonClick(2, listener)
    /** 4. buton (indeks 3) için tıklama dinleyicisi / Click listener for button 4 (index 3). */
    fun setOnButton4Click(listener: () -> Unit) = setOnButtonClick(3, listener)
    /** 5. buton (indeks 4) için tıklama dinleyicisi / Click listener for button 5 (index 4). */
    fun setOnButton5Click(listener: () -> Unit) = setOnButtonClick(4, listener)
    /** 6. buton (indeks 5) için tıklama dinleyicisi / Click listener for button 6 (index 5). */
    fun setOnButton6Click(listener: () -> Unit) = setOnButtonClick(5, listener)

    /** 1. buton (indeks 0) için uzun basma dinleyicisi / Long-click listener for button 1 (index 0). */
    fun setOnButton1LongClick(listener: () -> Boolean) = setOnButtonLongClick(0, listener)
    /** 2. buton (indeks 1) için uzun basma dinleyicisi / Long-click listener for button 2 (index 1). */
    fun setOnButton2LongClick(listener: () -> Boolean) = setOnButtonLongClick(1, listener)
    /** 3. buton (indeks 2) için uzun basma dinleyicisi / Long-click listener for button 3 (index 2). */
    fun setOnButton3LongClick(listener: () -> Boolean) = setOnButtonLongClick(2, listener)
    /** 4. buton (indeks 3) için uzun basma dinleyicisi / Long-click listener for button 4 (index 3). */
    fun setOnButton4LongClick(listener: () -> Boolean) = setOnButtonLongClick(3, listener)
    /** 5. buton (indeks 4) için uzun basma dinleyicisi / Long-click listener for button 5 (index 4). */
    fun setOnButton5LongClick(listener: () -> Boolean) = setOnButtonLongClick(4, listener)
    /** 6. buton (indeks 5) için uzun basma dinleyicisi / Long-click listener for button 6 (index 5). */
    fun setOnButton6LongClick(listener: () -> Boolean) = setOnButtonLongClick(5, listener)

    /**
     * Pill aksiyon butonu için tıklama dinleyicisi atar.
     * Buton kilitli (`isActivated == false`) veya devre dışı (`isEnabled == false`) ise tetiklenmez.
     *
     * Sets a click listener callback for the pill action button.
     * Will not fire if the button is locked (`isActivated == false`) or disabled (`isEnabled == false`).
     */
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

    /**
     * Belirtilen indisteki buton View nesnesini döndürür. İndeks geçersizse null döner.
     * Returns the button View at [index], or null if index is out of bounds.
     */
    fun getButton(index: Int): View? = buttonViews.getOrNull(index)

    /**
     * Bar içindeki toplam buton sayısını döndürür.
     * Returns the total count of buttons currently managed in the bar.
     */
    fun getButtonCount(): Int = buttonViews.size

    /**
     * Belirtilen indisteki butonun metnini dinamik olarak günceller.
     * Metin boş veya null ise TextView gizlenir ve ikon ortalanır.
     * Expandable stilinde daraltılmış durum önbelleği de otomatik olarak senkronize edilir.
     *
     * Dynamically updates the text label of the button at [index].
     * If text is null or empty, the TextView is hidden and icon is centered.
     * Automatically synchronizes metadata cache for expandable styles.
     */
    fun setButtonText(index: Int, text: CharSequence?) {
        if (index in buttonMetaList.indices) {
            val old = buttonMetaList[index]
            buttonMetaList[index] = old.copy(text = text?.toString())
        }
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
                val isIconVisible = (iconView?.isVisible == true)
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

    /**
     * Belirtilen indisteki butonun ikonunu dinamik olarak günceller.
     * [iconRes] 0 ise ikon gizlenir ve metin ortalanır.
     * Varsa butona özel renk tonu (tint) korunur.
     * Expandable stilinde daraltılmış durum önbelleği de otomatik olarak senkronize edilir.
     *
     * Dynamically updates the icon drawable resource of the button at [index].
     * If [iconRes] is 0, the ImageView is hidden and text is centered.
     * Preserves per-button icon tint if configured.
     * Automatically synchronizes metadata cache for expandable styles.
     */
    fun setButtonIcon(index: Int, @DrawableRes iconRes: Int) {
        if (index in buttonMetaList.indices) {
            val old = buttonMetaList[index]
            buttonMetaList[index] = old.copy(iconRes = iconRes)
        }
        val button = buttonViews.getOrNull(index) ?: return
        val iconView = button.findViewById<ImageView>(R.id.sb_item_icon)
            ?: button.findViewById<ImageView>(R.id.sb_vertical_icon)
            ?: button.findViewById<ImageView>(R.id.sb_circular_icon)
            ?: button.findViewById<ImageView>(R.id.sb_pill_icon)
        val textView = button.findViewById<TextView>(R.id.sb_item_text)
            ?: button.findViewById<TextView>(R.id.sb_vertical_text)
            ?: button.findViewById<TextView>(R.id.sb_pill_text)

        val buttonGapIconText = context.resources.getDimensionPixelSize(R.dimen.sb_button_gap_icon_text)

        iconView?.let {
            if (iconRes != 0) {
                it.setImageResource(iconRes)
                it.visibility = View.VISIBLE
                val perButtonTint = buttonMetaList.getOrNull(index)?.iconTint ?: globalIconTint
                resolveIconTint(perButtonTint)?.let { tint -> ImageViewCompat.setImageTintList(it, tint) }
                textView?.let { tv ->
                    if (tv.isVisible) {
                        (tv.layoutParams as? MarginLayoutParams)?.marginStart = buttonGapIconText
                        tv.gravity = Gravity.CENTER_VERTICAL or Gravity.START
                    }
                }
            } else {
                it.visibility = View.GONE
                textView?.let { tv ->
                    if (tv.isVisible) {
                        (tv.layoutParams as? MarginLayoutParams)?.marginStart = 0
                        tv.gravity = Gravity.CENTER
                    }
                }
            }
        }
    }
}
