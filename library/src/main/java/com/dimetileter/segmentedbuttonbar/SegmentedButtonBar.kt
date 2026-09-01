package com.dimetileter.segmentedbuttonbar

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
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
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat

/**
 * SegmentedButtonBar — Özelleştirilebilir, hibrit stilleri, ikon renklendirmeyi (Icon Tint)
 * ve gelişmiş XML durum yönetimini destekleyen Segmented Buton bileşeni.
 *
 * SegmentedButtonBar — Customizable, dynamic Segmented Button component with hybrid styles,
 * icon tinting, and XML-first state management (selected, activated, enabled).
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
    private var pillType: Int = PILL_NEXT
    private var expandDirection: Int = EXPAND_END
    private var maxWidthPx: Int = -1

    private var globalIconTint: ColorStateList? = null
    private var globalAllActivated: Boolean? = null

    private var isExpanded: Boolean = false
    private var isAnimating: Boolean = false
    private var onExpandChangeListener: ((Boolean) -> Unit)? = null

    private val buttonViews = mutableListOf<View>()
    private val buttonClickListeners = mutableMapOf<Int, () -> Unit>()
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
        pillType = ta.getInt(R.styleable.SegmentedButtonBar_sbPillType, PILL_NEXT)
        expandDirection = ta.getInt(R.styleable.SegmentedButtonBar_sbExpandDirection, EXPAND_END)
        maxWidthPx = ta.getDimensionPixelSize(R.styleable.SegmentedButtonBar_sbMaxWidth, -1)

        globalIconTint = ta.getColorStateList(R.styleable.SegmentedButtonBar_sbIconTint)
        if (ta.hasValue(R.styleable.SegmentedButtonBar_sbAllActivated)) {
            globalAllActivated = ta.getBoolean(R.styleable.SegmentedButtonBar_sbAllActivated, false)
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
     * Configures the horizontal segmented button bar.
     */
    private fun setupHorizontal(ta: TypedArray) {
        orientation = HORIZONTAL
        clipToPadding = true
        background = ContextCompat.getDrawable(context, R.drawable.bg_segmented_button_bar)

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

                itemView.contentDescription = textVal ?: getDefaultContentDescription(i)

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
            } else {
                // Standart Yatay Buton / Flexible Horizontal Button
                itemView = inflater.inflate(R.layout.sb_button_horizontal_item, this, false)
                val iconView = itemView.findViewById<ImageView>(R.id.sb_item_icon)
                val textView = itemView.findViewById<TextView>(R.id.sb_item_text)

                perButtonTint?.let { ImageViewCompat.setImageTintList(iconView, it) }

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

                itemView.contentDescription = textVal ?: getDefaultContentDescription(i)

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
     * Configures the vertical segmented button bar.
     */
    private fun setupVertical(ta: TypedArray) {
        orientation = VERTICAL
        clipToPadding = true
        background = ContextCompat.getDrawable(context, R.drawable.bg_segmented_button_bar)

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

            itemView.contentDescription = textVal ?: getDefaultContentDescription(i)

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

            buttonViews.add(itemView)
            addView(itemView)
        }

        if (buttonViews.none { it.isSelected }) {
            selectedIndex = -1
        }
    }

    /**
     * Dairesel tekil buton stilini yapılandırır.
     * Configures the circular single button style.
     */
    private fun setupCircular(ta: TypedArray) {
        orientation = HORIZONTAL
        clipToPadding = true
        background = ContextCompat.getDrawable(context, R.drawable.bg_segmented_button_bar)

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

        val customIcon = ta.getResourceId(R.styleable.SegmentedButtonBar_sbButton1Icon, 0)
        val customText = ta.getString(R.styleable.SegmentedButtonBar_sbButton1Text)
        val (explicitSelected, explicitActivated, explicitEnabled) = getButtonStateAttributes(ta, 0)

        val finalIcon = if (customIcon != 0) customIcon else R.drawable.ic_sb_arrow_next
        iconView.setImageResource(finalIcon)
        circularView.contentDescription = customText ?: context.getString(R.string.sb_cd_circular)

        circularView.layoutParams = LayoutParams(circularSize, circularSize)
        circularView.isSelected = explicitSelected ?: false
        circularView.isActivated = explicitActivated ?: (globalAllActivated ?: false)
        circularView.isEnabled = explicitEnabled

        circularView.setOnClickListener {
            buttonClickListeners[0]?.invoke()
        }

        buttonViews.add(circularView)
        addView(circularView)
    }

    /**
     * Pill (Next / Back / Text) buton stilini yapılandırır.
     * Configures the Pill (Next / Back / Text) button style.
     */
    private fun setupPill(ta: TypedArray) {
        orientation = HORIZONTAL
        clipToPadding = true
        background = ContextCompat.getDrawable(context, R.drawable.bg_segmented_button_bar)

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

        val customIcon = ta.getResourceId(R.styleable.SegmentedButtonBar_sbButton1Icon, 0)
        val customText = ta.getString(R.styleable.SegmentedButtonBar_sbButton1Text)

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
                pillView.contentDescription = customText ?: context.getString(R.string.sb_cd_next)
            }
            PILL_BACK -> {
                val iconRes = if (customIcon != 0) customIcon else R.drawable.ic_sb_arrow_back
                iconView.setImageResource(iconRes)
                iconView.visibility = View.VISIBLE
                textView.visibility = View.GONE
                pillView.contentDescription = customText ?: context.getString(R.string.sb_cd_back)
            }
            PILL_TEXT -> {
                iconView.visibility = View.GONE
                textView.text = customText ?: context.getString(R.string.sb_cd_next)
                textView.visibility = View.VISIBLE
                pillView.contentDescription = customText ?: context.getString(R.string.sb_cd_next)
            }
        }

        pillView.layoutParams = LayoutParams(minWidth, buttonHeight)
        pillView.isSelected = explicitSelected ?: false
        pillView.isActivated = initialPillActivated
        pillView.isEnabled = explicitEnabled

        pillView.setOnClickListener {
            pillClickListener?.invoke()
            buttonClickListeners[0]?.invoke()
        }

        buttonViews.add(pillView)
        addView(pillView)
    }

    /**
     * Animasyonlu genişleyen (Expandable) buton stilini yapılandırır.
     * Configures the animated Expandable button style.
     */
    private fun setupExpandable(ta: TypedArray) {
        orientation = HORIZONTAL
        clipToPadding = true
        background = ContextCompat.getDrawable(context, R.drawable.bg_segmented_button_bar)

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
            val finalIcon = if (iconRes != 0) iconRes else R.drawable.ic_sb_arrow_next
            iconView.setImageResource(finalIcon)

            itemView.contentDescription = textVal ?: getDefaultContentDescription(i)

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

            buttonViews.add(itemView)
            addView(itemView)
        }

        isExpanded = false
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

    /**
     * Expandable çubuğu genişletir.
     * Expands the expandable button bar.
     */
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

    /**
     * Expandable çubuğu kapatır (daraltır).
     * Collapses the expandable button bar.
     */
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

    /**
     * Expandable çubuğun genişleme durumunu tersine çevirir.
     * Toggles expansion state of the expandable bar.
     */
    fun toggleExpand(animate: Boolean = true) {
        if (isExpanded) collapse(animate) else expand(animate)
    }

    /**
     * Expandable çubuğun açık olup olmadığını döner.
     * Returns whether the expandable bar is expanded.
     */
    fun isExpanded(): Boolean = isExpanded

    /**
     * Expandable çubuğun açılma/kapanma durum değişikliği dinleyicisini atar.
     * Sets expansion change listener.
     */
    fun setOnExpandChangeListener(listener: (Boolean) -> Unit) {
        onExpandChangeListener = listener
    }

    // ==========================================
    // Public API — Selection & State Management
    // ==========================================

    /**
     * Belirtilen indisteki butonu tekil olarak seçili yapar (0 tabanlı).
     * Selects the button at the specified index (0-based) exclusively.
     */
    fun selectButton(index: Int) {
        if (index !in buttonViews.indices) return
        selectedIndex = index
        buttonViews.forEachIndexed { i, view ->
            view.isSelected = (i == index)
        }
    }

    /**
     * Tüm butonların seçimini kaldırır.
     * Clears selection from all buttons.
     */
    fun clearSelection() {
        selectedIndex = -1
        buttonViews.forEach { it.isSelected = false }
    }

    /**
     * Tüm butonların aktiflik durumunu tek bir çağrıyla ayarlar.
     * Sets the activation state of all buttons in a single call.
     */
    fun setAllActivated(activated: Boolean) {
        buttonViews.forEach { it.isActivated = activated }
    }

    /**
     * Belirtilen butonu tekil olarak aktif (isActivated) yapar, diğerlerini pasife çeker.
     * Activates the specified button exclusively and deactivates the rest.
     */
    fun activateButton(index: Int) {
        buttonViews.forEachIndexed { i, view ->
            view.isActivated = (i == index)
        }
    }

    /**
     * Seçili butonun indisini döner (hiçbiri seçili değilse -1).
     * Returns the index of the currently selected button (-1 if none).
     */
    fun getSelectedButtonIndex(): Int = selectedIndex

    /**
     * Belirtilen butonun seçim durumunu ayarlar.
     * Sets the selection state of a specific button.
     */
    fun setButtonSelected(index: Int, selected: Boolean) {
        buttonViews.getOrNull(index)?.isSelected = selected
        if (selected) {
            selectedIndex = index
        } else if (selectedIndex == index) {
            selectedIndex = -1
        }
    }

    /**
     * Belirtilen butonun seçili olup olmadığını döner.
     * Returns whether the button at the given index is selected.
     */
    fun isButtonSelected(index: Int): Boolean = buttonViews.getOrNull(index)?.isSelected ?: false

    /**
     * Belirtilen butonun kilit/aktiflik (isActivated) durumunu ayarlar.
     * Sets the activation (isActivated) state of a specific button.
     */
    fun setButtonActivated(index: Int, activated: Boolean) {
        buttonViews.getOrNull(index)?.isActivated = activated
    }

    /**
     * Belirtilen butonun aktiflik durumunu döner.
     * Returns whether the button at the given index is activated.
     */
    fun isButtonActivated(index: Int): Boolean = buttonViews.getOrNull(index)?.isActivated ?: false

    /**
     * Belirtilen butonun etkin/devre dışı (isEnabled) durumunu ayarlar.
     * Sets the enabled state of a specific button.
     */
    fun setButtonEnabled(index: Int, enabled: Boolean) {
        buttonViews.getOrNull(index)?.isEnabled = enabled
    }

    /**
     * Belirtilen butonun etkin olup olmadığını döner.
     * Returns whether the button at the given index is enabled.
     */
    fun isButtonEnabled(index: Int): Boolean = buttonViews.getOrNull(index)?.isEnabled ?: false

    /**
     * Mevcut stili döner.
     * Returns the current button bar style.
     */
    fun getStyle(): Int = currentStyle

    /**
     * Pill butonunun kilit (aktiflik) durumunu ayarlar.
     * Sets the activation (lock/unlock) state of the pill button.
     */
    fun setPillActivated(active: Boolean) {
        setButtonActivated(0, active)
    }

    /**
     * Pill butonunun aktiflik durumunu döner.
     * Returns whether the pill button is activated.
     */
    fun isPillActivated(): Boolean {
        return isButtonActivated(0)
    }

    // ==========================================
    // Public API — Icon Tint Management
    // ==========================================

    /**
     * Çubuktaki tüm ikonların renk tonunu genel olarak ayarlar.
     * Sets icon tint globally for all buttons in the bar.
     */
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

    /**
     * Çubuktaki tüm ikonların renk tonunu ColorInt değeriyle ayarlar.
     * Sets icon tint globally for all buttons using a ColorInt.
     */
    fun setIconTint(@ColorInt color: Int) {
        setIconTint(ColorStateList.valueOf(color))
    }

    /**
     * Belirli bir butonun ikon renk tonunu ayarlar.
     * Sets icon tint for a specific button.
     */
    fun setButtonIconTint(index: Int, tintList: ColorStateList?) {
        val button = buttonViews.getOrNull(index) ?: return
        val iconView = button.findViewById<ImageView>(R.id.sb_item_icon)
            ?: button.findViewById<ImageView>(R.id.sb_vertical_icon)
            ?: button.findViewById<ImageView>(R.id.sb_circular_icon)
            ?: button.findViewById<ImageView>(R.id.sb_pill_icon)
        iconView?.let { ImageViewCompat.setImageTintList(it, tintList) }
    }

    /**
     * Belirli bir butonun ikon renk tonunu ColorInt değeriyle ayarlar.
     * Sets icon tint for a specific button using a ColorInt.
     */
    fun setButtonIconTint(index: Int, @ColorInt color: Int) {
        setButtonIconTint(index, ColorStateList.valueOf(color))
    }

    // ==========================================
    // Public API — Click Listeners & Helpers
    // ==========================================

    /**
     * Belirtilen buton için tıklama dinleyicisi atar.
     * Sets a click listener for the specified button index.
     */
    fun setOnButtonClick(index: Int, listener: () -> Unit) {
        buttonClickListeners[index] = listener
    }

    fun setOnButton1Click(listener: () -> Unit) = setOnButtonClick(0, listener)
    fun setOnButton2Click(listener: () -> Unit) = setOnButtonClick(1, listener)
    fun setOnButton3Click(listener: () -> Unit) = setOnButtonClick(2, listener)
    fun setOnButton4Click(listener: () -> Unit) = setOnButtonClick(3, listener)
    fun setOnButton5Click(listener: () -> Unit) = setOnButtonClick(4, listener)
    fun setOnButton6Click(listener: () -> Unit) = setOnButtonClick(5, listener)

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

    /**
     * Çubuk içindeki buton görünümünü döner.
     * Returns the button view at the given index.
     */
    fun getButton(index: Int): View? = buttonViews.getOrNull(index)

    /**
     * Çubuktaki toplam buton sayısını döner.
     * Returns total button count in the bar.
     */
    fun getButtonCount(): Int = buttonViews.size

    /**
     * Buton metnini dinamik olarak günceller ve ortalama/hizalamayı yeniden düzenler.
     * Updates button text dynamically and reconfigures centering/alignment.
     */
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

    /**
     * Buton ikonunu dinamik olarak günceller ve ikonsuzluk durumunda metni otomatik ortalar.
     * Updates button icon dynamically and automatically centers text when icon is absent (iconRes = 0).
     */
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
