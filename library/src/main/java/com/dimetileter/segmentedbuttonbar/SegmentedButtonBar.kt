package com.dimetileter.segmentedbuttonbar

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

/**
 * SegmentedButtonBar — Özelleştirilebilir ve XML tabanlı Segmented Buton bileşeni.
 * SegmentedButtonBar — Customizable and XML-configurable Segmented Button component.
 */
class SegmentedButtonBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        // Stil Sabitleri / Style Constants
        const val STYLE_HORIZONTAL = 0
        const val STYLE_VERTICAL = 1
        const val STYLE_CIRCULAR = 2
        const val STYLE_PILL = 3
        const val STYLE_EXPANDABLE = 4

        // Pill Türü Sabitleri / Pill Type Constants
        const val PILL_NEXT = 0
        const val PILL_BACK = 1
        const val PILL_TEXT = 2

        // Genişleme Yönü Sabitleri / Expand Direction Constants
        const val EXPAND_END = 0
        const val EXPAND_START = 1

        private const val MIN_BUTTON_COUNT = 1
        private const val MAX_BUTTON_COUNT = 4
        private const val DEFAULT_BUTTON_COUNT = 2
        private const val DEFAULT_VERTICAL_COUNT = 3
    }

    private var currentStyle: Int = STYLE_HORIZONTAL
    private var buttonCount: Int = DEFAULT_BUTTON_COUNT
    private var autoSelect: Boolean = true
    private var pillType: Int = PILL_NEXT
    private var maxWidthPx: Int = -1

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
        val defaultCount = if (currentStyle == STYLE_VERTICAL) DEFAULT_VERTICAL_COUNT else DEFAULT_BUTTON_COUNT
        val rawCount = ta.getInt(R.styleable.SegmentedButtonBar_sbButtonCount, defaultCount)
        buttonCount = rawCount.coerceIn(MIN_BUTTON_COUNT, MAX_BUTTON_COUNT)
        autoSelect = ta.getBoolean(R.styleable.SegmentedButtonBar_sbAutoSelect, true)
        pillType = ta.getInt(R.styleable.SegmentedButtonBar_sbPillType, PILL_NEXT)
        maxWidthPx = ta.getDimensionPixelSize(R.styleable.SegmentedButtonBar_sbMaxWidth, -1)

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
            else -> setupHorizontal(ta)
        }
    }

    /**
     * Yatay segmented buton çubuğunu yapılandırır.
     * Configures the horizontal segmented button bar.
     */
    private fun setupHorizontal(ta: TypedArray) {
        orientation = HORIZONTAL
        clipToPadding = true
        background = ContextCompat.getDrawable(context, R.drawable.bg_segmented_button_bar)

        val barPadding = context.resources.getDimensionPixelSize(R.dimen.sb_bar_padding)
        val buttonGap = context.resources.getDimensionPixelSize(R.dimen.sb_button_gap)
        val buttonHeight = context.resources.getDimensionPixelSize(R.dimen.sb_button_height)
        val minWidth = context.resources.getDimensionPixelSize(R.dimen.sb_button_min_width)

        setPadding(barPadding, barPadding, barPadding, barPadding)
        removeAllViews()
        buttonViews.clear()

        val inflater = LayoutInflater.from(context)

        for (i in 0 until buttonCount) {
            val itemView = inflater.inflate(R.layout.sb_button_horizontal_item, this, false)
            val iconView = itemView.findViewById<ImageView>(R.id.sb_item_icon)
            val textView = itemView.findViewById<TextView>(R.id.sb_item_text)

            val (iconRes, textVal) = getButtonAttributes(ta, i)

            if (iconRes != 0) {
                iconView.setImageResource(iconRes)
                iconView.visibility = View.VISIBLE
            } else {
                iconView.visibility = View.GONE
            }

            if (!textVal.isNullOrEmpty()) {
                textView.text = textVal
                textView.visibility = View.VISIBLE
            } else {
                textView.visibility = View.GONE
            }

            itemView.contentDescription = textVal ?: getDefaultContentDescription(i)

            val params = LayoutParams(0, buttonHeight, 1f).apply {
                if (i > 0) {
                    marginStart = buttonGap
                }
            }
            itemView.minimumWidth = minWidth
            itemView.layoutParams = params

            val buttonIndex = i
            itemView.isSelected = (buttonIndex == 0)
            itemView.setOnClickListener {
                if (autoSelect) {
                    selectButton(buttonIndex)
                }
                buttonClickListeners[buttonIndex]?.invoke()
            }

            buttonViews.add(itemView)
            addView(itemView)
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
        val buttonWidth = context.resources.getDimensionPixelSize(R.dimen.sb_vertical_button_width)
        val buttonHeight = context.resources.getDimensionPixelSize(R.dimen.sb_vertical_button_height)

        setPadding(barPadding, barPadding, barPadding, barPadding)
        removeAllViews()
        buttonViews.clear()

        val inflater = LayoutInflater.from(context)

        for (i in 0 until buttonCount) {
            val itemView = inflater.inflate(R.layout.sb_button_vertical_item, this, false)
            val iconView = itemView.findViewById<ImageView>(R.id.sb_vertical_icon)

            val (iconRes, textVal) = getButtonAttributes(ta, i)
            val finalIcon = if (iconRes != 0) iconRes else R.drawable.ic_sb_arrow_next
            iconView.setImageResource(finalIcon)

            itemView.contentDescription = textVal ?: getDefaultContentDescription(i)

            val params = LayoutParams(buttonWidth, buttonHeight).apply {
                if (i > 0) {
                    topMargin = buttonGap
                }
            }
            itemView.layoutParams = params

            val buttonIndex = i
            itemView.isSelected = (buttonIndex == 0)
            itemView.setOnClickListener {
                if (autoSelect) {
                    selectButton(buttonIndex)
                }
                buttonClickListeners[buttonIndex]?.invoke()
            }

            buttonViews.add(itemView)
            addView(itemView)
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

        val customIcon = ta.getResourceId(R.styleable.SegmentedButtonBar_sbButton1Icon, 0)
        val customText = ta.getString(R.styleable.SegmentedButtonBar_sbButton1Text)

        val finalIcon = if (customIcon != 0) customIcon else R.drawable.ic_sb_arrow_next
        iconView.setImageResource(finalIcon)
        circularView.contentDescription = customText ?: context.getString(R.string.sb_cd_circular)

        circularView.layoutParams = LayoutParams(circularSize, circularSize)

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

        val customIcon = ta.getResourceId(R.styleable.SegmentedButtonBar_sbButton1Icon, 0)
        val customText = ta.getString(R.styleable.SegmentedButtonBar_sbButton1Text)

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
        pillView.isActivated = false

        pillView.setOnClickListener {
            pillClickListener?.invoke()
            buttonClickListeners[0]?.invoke()
        }

        buttonViews.add(pillView)
        addView(pillView)
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
            else -> Pair(0, null)
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
    // Public API — Selection & Click Handlers
    // ==========================================

    /**
     * Belirtilen indisteki butonu seçili yapar (0 tabanlı).
     * Selects the button at the specified index (0-based).
     */
    fun selectButton(index: Int) {
        if (index !in buttonViews.indices) return
        selectedIndex = index
        buttonViews.forEachIndexed { i, view ->
            view.isSelected = (i == index)
        }
    }

    /**
     * Seçili butonun indisini döner.
     * Returns the index of the currently selected button.
     */
    fun getSelectedButtonIndex(): Int = selectedIndex

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
        if (buttonViews.isNotEmpty()) {
            buttonViews[0].isActivated = active
        }
    }

    /**
     * Pill butonunun aktiflik durumunu döner.
     * Returns whether the pill button is activated.
     */
    fun isPillActivated(): Boolean {
        return buttonViews.firstOrNull()?.isActivated ?: false
    }

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
    fun setOnPillClick(listener: () -> Unit) {
        pillClickListener = listener
    }

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
     * Buton metnini dinamik olarak günceller.
     * Updates button text dynamically.
     */
    fun setButtonText(index: Int, text: CharSequence?) {
        val button = buttonViews.getOrNull(index) ?: return
        val textView = button.findViewById<TextView>(R.id.sb_item_text)
            ?: button.findViewById<TextView>(R.id.sb_pill_text)
        textView?.let {
            it.text = text
            it.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }

    /**
     * Buton ikonunu dinamik olarak günceller.
     * Updates button icon dynamically.
     */
    fun setButtonIcon(index: Int, @DrawableRes iconRes: Int) {
        val button = buttonViews.getOrNull(index) ?: return
        val iconView = button.findViewById<ImageView>(R.id.sb_item_icon)
            ?: button.findViewById<ImageView>(R.id.sb_vertical_icon)
            ?: button.findViewById<ImageView>(R.id.sb_circular_icon)
            ?: button.findViewById<ImageView>(R.id.sb_pill_icon)
        iconView?.let {
            if (iconRes != 0) {
                it.setImageResource(iconRes)
                it.visibility = View.VISIBLE
            } else {
                it.visibility = View.GONE
            }
        }
    }
}
