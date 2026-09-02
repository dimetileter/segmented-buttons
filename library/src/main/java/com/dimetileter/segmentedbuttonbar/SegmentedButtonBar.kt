package com.dimetileter.segmentedbuttonbar

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat

/**
 * SegmentedButtonBar — Özelleştirilebilir, hibrit stilleri, ikon ve arkaplan renklendirmesini (Icon Tint, Gradient/Color BG),
 * TalkBack erişilebilirliğini, Tooltip (uzun basma) desteğini ve gelişmiş XML durum yönetimini destekleyen Segmented Buton bileşeni.
 *
 * SegmentedButtonBar — Customizable, dynamic Segmented Button component with hybrid styles,
 * icon tinting, custom gradients/colors, TalkBack accessibility, Tooltip (long press) support,
 * and XML-first state management (selected, activated, enabled).
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
        private const val ANIMATION_DURATION_MS = 300L
    }

    private var currentStyle: Int = STYLE_HORIZONTAL
    private var buttonCount: Int = DEFAULT_BUTTON_COUNT
    private var autoSelect: Boolean = true
    private var autoTooltip: Boolean = true
    private var pillType: Int = PILL_NEXT
    private var expandDirection: Int = EXPAND_END
    private var maxWidthPx: Int = -1

    private var globalIconTint: ColorStateList? = null
    private var globalAllActivated: Boolean? = null
    private var globalSelectedBackground: Drawable? = null
    private var globalSelectedColor: Int? = null
    private var showUnselectedBackground: Boolean = false
    private var unselectedButtonColor: Int? = null

    private var isExpanded: Boolean = false
    private var isAnimating: Boolean = false
    private var onExpandChangeListener: ((Boolean) -> Unit)? = null

    private val buttonViews = mutableListOf<View>()
    private val buttonClickListeners = mutableMapOf<Int, () -> Unit>()
    private val buttonLongClickListeners = mutableMapOf<Int, () -> Boolean>()
    private var pillClickListener: (() -> Unit)? = null
    private var selectedIndex: Int = 0

    init {
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
        maxWidthPx = ta.getDimensionPixelSize(R.styleable.SegmentedButtonBar_sbMaxWidth, -1)

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
            STYLE_HORIZONTAL -> setupHorizontal(ta)
            STYLE_VERTICAL -> setupVertical(ta)
            STYLE_CIRCULAR -> setupCircular(ta)
            STYLE_PILL -> setupPill(ta)
            STYLE_EXPANDABLE -> setupExpandable(ta)
            else -> setupHorizontal(ta)
        }
    }

    /**
     * Yatay segmented buton çubuğunu yapılandırır.
     * Standart ve hibrit (ör. 2 yatay + 1 dairesel) butonları destekler.
     */
    private fun setupHorizontal(ta: TypedArray) {
        orientation = HORIZONTAL
        clipToPadding = true

        val barPadding = context.resources.getDimensionPixelSize(R.dimen.sb_bar_padding)
        val buttonGap = context.resources.getDimensionPixelSize(R.dimen.sb_button_gap)
        val buttonGapIconText = context.resources.getDimensionPixelSize(R.dimen.sb_button_gap_icon_text)
        val buttonHeight = context.resources.getDimensionPixelSize(R.dimen.sb_button_height)
        val minWidth = context.resources.getDimensionPixelSize(R.dimen.sb_button_min_width)
        val circularHybridSize = context.resources.getDimensionPixelSize(R.dimen.sb_circular_button_hybrid_size)

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
                perButtonTint?.let { ImageViewCompat.setImageTintList(iconView, it) }

                applyButtonBackground(
                    itemView,
                    isCircular = true,
                    customBackgroundDrawable = customBg,
                    selectedCustomDrawable = selectedBg ?: globalSelectedBackground,
                    selectedCustomColor = selectedCol ?: globalSelectedColor,
                    showUnselectedBg = showUnselectedBackground,
                    unselectedCustomColor = unselectedButtonColor
                )

                val finalCd = customCd ?: textVal ?: getDefaultContentDescription(i)
                itemView.contentDescription = finalCd

                val tooltipText = customTooltip ?: textVal ?: customCd
                if (autoTooltip && !tooltipText.isNullOrEmpty()) {
                    TooltipCompat.setTooltipText(itemView, tooltipText)
                }

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
                // Standart Yatay Buton / Flexible Horizontal Button
                itemView = inflater.inflate(R.layout.sb_button_horizontal_item, this, false)
                val iconView = itemView.findViewById<ImageView>(R.id.sb_item_icon)
                val textView = itemView.findViewById<TextView>(R.id.sb_item_text)

                perButtonTint?.let { ImageViewCompat.setImageTintList(iconView, it) }

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
                        textView.text = getDefaultContentDescription(i)
                        textView.visibility = View.VISIBLE
                        (textView.layoutParams as? MarginLayoutParams)?.marginStart = 0
                        textView.gravity = Gravity.CENTER
                    }
                }

                val finalCd = customCd ?: textVal ?: getDefaultContentDescription(i)
                itemView.contentDescription = finalCd

                val tooltipText = customTooltip ?: textVal ?: customCd
                if (autoTooltip && !tooltipText.isNullOrEmpty()) {
                    TooltipCompat.setTooltipText(itemView, tooltipText)
                }

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

            val perButtonTint = getButtonIconTint(ta, i) ?: globalIconTint
            perButtonTint?.let { ImageViewCompat.setImageTintList(iconView, it) }

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
            if (autoTooltip && !tooltipText.isNullOrEmpty()) {
                TooltipCompat.setTooltipText(itemView, tooltipText)
            }

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

        val perButtonTint = getButtonIconTint(ta, 0) ?: globalIconTint
        perButtonTint?.let { ImageViewCompat.setImageTintList(iconView, it) }

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
        if (autoTooltip && !tooltipText.isNullOrEmpty()) {
            TooltipCompat.setTooltipText(circularView, tooltipText)
        }

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

        val perButtonTint = getButtonIconTint(ta, 0) ?: globalIconTint
        perButtonTint?.let { ImageViewCompat.setImageTintList(iconView, it) }

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
        if (autoTooltip && !tooltipText.isNullOrEmpty()) {
            TooltipCompat.setTooltipText(pillView, tooltipText)
        }

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

        val inflater = LayoutInflater.from(context)

        for (i in 0 until buttonCount) {
            val itemView = inflater.inflate(R.layout.sb_button_circular_item, this, false)
            val iconView = itemView.findViewById<ImageView>(R.id.sb_circular_icon)

            val perButtonTint = getButtonIconTint(ta, i) ?: globalIconTint
            perButtonTint?.let { ImageViewCompat.setImageTintList(iconView, it) }

            val (iconRes, textVal) = getButtonAttributes(ta, i)
            val (explicitSelected, explicitActivated, explicitEnabled) = getButtonStateAttributes(ta, i)
            val (customBg, selectedBg, selectedCol) = getButtonBackgroundAttributes(ta, i)
            val customCd = getButtonContentDescriptionAttr(ta, i)
            val customTooltip = getButtonTooltipAttr(ta, i)

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
            if (autoTooltip && !tooltipText.isNullOrEmpty()) {
                TooltipCompat.setTooltipText(itemView, tooltipText)
            }

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
    // Public API — Expandable Animation
    // ==========================================

    fun expand(animate: Boolean = true) {
        if (currentStyle != STYLE_EXPANDABLE || isExpanded || isAnimating) return

        if (!animate) {
            for (i in 1 until buttonViews.size) {
                val child = buttonViews[i]
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
        for (i in 1 until buttonViews.size) {
            val child = buttonViews[i]
            child.visibility = View.VISIBLE
            child.alpha = 0f
            val startTranslation = if (expandDirection == EXPAND_START) 30f else -30f
            child.translationX = startTranslation

            val delay = ((i - 1) * 30L)
            child.animate()
                .alpha(1f)
                .translationX(0f)
                .setStartDelay(delay)
                .setDuration(ANIMATION_DURATION_MS)
                .setInterpolator(OvershootInterpolator(1.1f))
                .setListener(if (i == buttonViews.size - 1) object : AnimatorListenerAdapter() {
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

        if (!animate) {
            for (i in 1 until buttonViews.size) {
                val child = buttonViews[i]
                child.visibility = View.GONE
                child.alpha = 0f
            }
            isExpanded = false
            onExpandChangeListener?.invoke(false)
            requestLayout()
            return
        }

        isAnimating = true
        val totalChildren = buttonViews.size - 1
        for (i in 1 until buttonViews.size) {
            val child = buttonViews[i]
            val endTranslation = if (expandDirection == EXPAND_START) 20f else -20f

            child.animate()
                .alpha(0f)
                .translationX(endTranslation)
                .setDuration(ANIMATION_DURATION_MS / 2)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        child.visibility = View.GONE
                        if (i == totalChildren) {
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

    // ==========================================
    // Public API — Selection & State Management
    // ==========================================

    /**
     * Belirtilen butonu seçer, diğer tüm butonların seçimini ve aktifliğini kaldırır.
     * Selects the button at index, clears selection and activation from all other buttons.
     */
    fun selectButton(index: Int) {
        if (index !in buttonViews.indices) return
        selectedIndex = index
        buttonViews.forEachIndexed { i, view ->
            val isTarget = (i == index)
            view.isSelected = isTarget
            if (!isTarget && view.isActivated) {
                view.isActivated = false
            }
        }
    }

    /**
     * Tüm butonların seçimini kaldırır.
     */
    fun clearSelection() {
        selectedIndex = -1
        buttonViews.forEach { it.isSelected = false }
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
        TooltipCompat.setTooltipText(button, tooltipText)
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
                globalIconTint?.let { tint -> ImageViewCompat.setImageTintList(it, tint) }
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
