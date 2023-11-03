package com.example.myapplication.shimmer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.example.myapplication.R

class ShimmerFrameLayout : FrameLayout {
    private val mContentPaint = Paint()
    private val mShimmerDrawable = ShimmerDrawable()

    val isShimmerStarted: Boolean
        get() = mShimmerDrawable.isShimmerStarted

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        setWillNotDraw(false)
        mShimmerDrawable.callback = this

        if (attrs == null) {
            setShimmer(Shimmer.AlphaHighlightBuilder().build())
            return
        }

        val a = context.obtainStyledAttributes(attrs, R.styleable.ShimmerFrameLayout, 0, 0)
        try {
            val shimmerBuilder = if (
                a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_colored) &&
                a.getBoolean(R.styleable.ShimmerFrameLayout_shimmer_colored, false)
            ) {
                Shimmer.ColorHighlightBuilder()
            } else {
                Shimmer.AlphaHighlightBuilder()
            }
            setShimmer(shimmerBuilder.consumeAttributes(a).build())
        } finally {
            a.recycle()
        }
    }

    fun setShimmer(shimmer: Shimmer?): ShimmerFrameLayout {
        mShimmerDrawable.setShimmer(shimmer)
        if (shimmer != null && shimmer.clipToChildren) {
            setLayerType(View.LAYER_TYPE_HARDWARE, mContentPaint)
        } else {
            setLayerType(View.LAYER_TYPE_NONE, null)
        }

        return this
    }

    fun startShimmer() {
        mShimmerDrawable.startShimmer()
    }

    fun stopShimmer() {
        mShimmerDrawable.stopShimmer()
    }

    public override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val width = width
        val height = height
        mShimmerDrawable.setBounds(0, 0, width, height)
    }

    public override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mShimmerDrawable.maybeStartShimmer()
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopShimmer()
    }

    public override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        mShimmerDrawable.draw(canvas)
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return super.verifyDrawable(who) || who === mShimmerDrawable
    }
}
