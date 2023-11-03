package com.example.myapplication.shimmer

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import com.example.myapplication.R
import kotlin.math.cos
import kotlin.math.sin

class ShimmerLayout : FrameLayout {
    private var maskOffsetX = 0
    private var maskAlpha = 255
    private var maskRect: Rect? = null
    private var gradientTexturePaint: Paint? = null
    private var maskAnimator: ValueAnimator? = null
    private var localMaskBitmap: Bitmap? = null
    private var maskBitmap: Bitmap? = null
    private var canvasForShimmerMask: Canvas? = null
    private var isAnimationReversed = false
    private var isAnimationStarted = false
    private var autoStart = false
    private var shimmerAnimationDuration = 0
    private var shimmerColor = 0
    private var shimmerAngle = 0
    private var maskWidth: Float = 0f
    private var gradientCenterColorWidth = 0f
    private var startAnimationPreDrawListener: ViewTreeObserver.OnPreDrawListener? = null

    private var shimmerPulsate = false

    constructor(context: Context) : super(context)

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

    private fun init(context: Context, attrs: AttributeSet) {
        setWillNotDraw(false)
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ShimmerLayout,
            0, 0
        )
        try {
            shimmerAngle = a.getInteger(
                R.styleable.ShimmerLayout_shimmer_angle,
                DEFAULT_ANGLE.toInt()
            )
            shimmerAnimationDuration = a.getInteger(
                R.styleable.ShimmerLayout_shimmer_animationDuration,
                DEFAULT_ANIMATION_DURATION
            )
            shimmerColor = a.getColor(
                R.styleable.ShimmerLayout_shimmer_color,
                getColor(Color.WHITE)
            )
            autoStart = a.getBoolean(R.styleable.ShimmerLayout_shimmer_autostart, false)
            maskWidth = a.getFloat(R.styleable.ShimmerLayout_shimmer_maskWidth, 0.5f)
            gradientCenterColorWidth = a.getFloat(
                R.styleable.ShimmerLayout_shimmer_gradientCenterColorWidth,
                0.1f
            )
            isAnimationReversed =
                a.getBoolean(R.styleable.ShimmerLayout_shimmer_reverseAnimation, false)

            shimmerPulsate = a.getBoolean(R.styleable.ShimmerLayout_shimmer_pulsate, false)

        } finally {
            a.recycle()
        }
        setMaskWidth(maskWidth)
        maskWidth =
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
                .width.toFloat()
        setGradientCenterColorWidth(gradientCenterColorWidth)
        setShimmerAngle(shimmerAngle)
        if (autoStart && visibility == View.VISIBLE) {
            startShimmerAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        resetShimmering()
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (autoStart && visibility == View.VISIBLE) {
            startShimmerAnimation()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (!isAnimationStarted || width <= 0 || height <= 0) {
            super.dispatchDraw(canvas)
        } else {
            dispatchDrawShimmer(canvas)
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == View.VISIBLE) {
            if (autoStart) {
                startShimmerAnimation()
            }
        } else {
            stopShimmerAnimation()
        }
    }

    fun startShimmerAnimation() {
        if (isAnimationStarted) {
            return
        }
        if (width == 0) {
            startAnimationPreDrawListener = object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    viewTreeObserver.removeOnPreDrawListener(this)
                    startShimmerAnimation()
                    return true
                }
            }
            viewTreeObserver.addOnPreDrawListener(startAnimationPreDrawListener)
            return
        }
        val animator = shimmerAnimation
        animator.start()
        isAnimationStarted = true
    }

    fun stopShimmerAnimation() {
        if (startAnimationPreDrawListener != null) {
            viewTreeObserver.removeOnPreDrawListener(startAnimationPreDrawListener)
        }
        resetShimmering()
    }

    fun setShimmerColor(shimmerColor: Int) {
        this.shimmerColor = shimmerColor
        resetIfStarted()
    }

    fun setShimmerAnimationDuration(durationMillis: Int) {
        shimmerAnimationDuration = durationMillis
        resetIfStarted()
    }

    fun setAnimationReversed(animationReversed: Boolean) {
        isAnimationReversed = animationReversed
        resetIfStarted()
    }

    /**
     * Set the angle of the shimmer effect in clockwise direction in degrees.
     * The angle must be between {@value #MIN_ANGLE_VALUE} and {@value #MAX_ANGLE_VALUE}.
     *
     * @param angle The angle to be set
     */
    private fun setShimmerAngle(angle: Int) {
        require(!(angle < MIN_ANGLE_VALUE || MAX_ANGLE_VALUE < angle)) {
            String.format(
                "shimmerAngle value must be between %d and %d",
                MIN_ANGLE_VALUE,
                MAX_ANGLE_VALUE
            )
        }
        shimmerAngle = angle
        resetIfStarted()
    }

    /**
     * Sets the width of the shimmer line to a value higher than 0 to less or equal to 1.
     * 1 means the width of the shimmer line is equal to half of the width of the ShimmerLayout.
     * The default value is 0.5.
     *
     * @param maskWidth The width of the shimmer line.
     */
    fun setMaskWidth(maskWidth: Float) {
        require(!(maskWidth <= MIN_MASK_WIDTH_VALUE || MAX_MASK_WIDTH_VALUE < maskWidth)) {
            String.format(
                "maskWidth value must be higher than %d and less or equal to %d",
                MIN_MASK_WIDTH_VALUE,
                MAX_MASK_WIDTH_VALUE
            )
        }
        this.maskWidth = maskWidth
        resetIfStarted()
    }

    /**
     * Sets the width of the center gradient color to a value higher than 0 to less than 1.
     * 0.99 means that the whole shimmer line will have this color with a little transparent edges.
     * The default value is 0.1.
     *
     * @param gradientCenterColorWidth The width of the center gradient color.
     */
    fun setGradientCenterColorWidth(gradientCenterColorWidth: Float) {
        require(
            !(gradientCenterColorWidth <= MIN_GRADIENT_CENTER_COLOR_WIDTH_VALUE
                || MAX_GRADIENT_CENTER_COLOR_WIDTH_VALUE <= gradientCenterColorWidth)
        ) {
            String.format(
                "gradientCenterColorWidth value must be higher than %d and less than %d",
                MIN_GRADIENT_CENTER_COLOR_WIDTH_VALUE,
                MAX_GRADIENT_CENTER_COLOR_WIDTH_VALUE
            )
        }
        this.gradientCenterColorWidth = gradientCenterColorWidth
        resetIfStarted()
    }

    private fun resetIfStarted() {
        if (isAnimationStarted) {
            resetShimmering()
            startShimmerAnimation()
        }
    }

    private fun dispatchDrawShimmer(canvas: Canvas) {
        super.dispatchDraw(canvas)
        localMaskBitmap = getMaskBitmap()
        if (localMaskBitmap == null) {
            return
        }
        if (canvasForShimmerMask == null) {
            canvasForShimmerMask = Canvas(localMaskBitmap!!)
        }
        canvasForShimmerMask!!.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        canvasForShimmerMask!!.save()
        canvasForShimmerMask!!.translate(-maskOffsetX.toFloat(), 0f)
        canvasForShimmerMask?.let {
            super.dispatchDraw(it)
        }
        canvasForShimmerMask!!.restore()
        drawShimmer(canvas)
        localMaskBitmap = null
    }

    private fun drawShimmer(destinationCanvas: Canvas) {
        createShimmerPaint()
        destinationCanvas.save()
        destinationCanvas.translate(maskOffsetX.toFloat(), 0f)

        if (shimmerPulsate) {
            gradientTexturePaint?.alpha = maskAlpha
        }

        destinationCanvas.drawRect(
            maskRect!!.left.toFloat(),
            0f,
            maskRect!!.width().toFloat(),
            maskRect!!.height().toFloat(),
            gradientTexturePaint!!
        )
        destinationCanvas.restore()
    }

    private fun resetShimmering() {
        if (maskAnimator != null) {
            maskAnimator!!.end()
            maskAnimator!!.removeAllUpdateListeners()
        }
        maskAnimator = null
        gradientTexturePaint = null
        isAnimationStarted = false
        releaseBitMaps()
    }

    private fun releaseBitMaps() {
        canvasForShimmerMask = null
        if (maskBitmap != null) {
            maskBitmap!!.recycle()
            maskBitmap = null
        }
    }

    private fun getMaskBitmap(): Bitmap? {
        if (maskBitmap == null) {
            maskBitmap = createBitmap(maskRect!!.width(), height)
        }
        return maskBitmap
    }

    private fun createShimmerPaint() {
        if (gradientTexturePaint != null) {
            return
        }
        val edgeColor = reduceColorAlphaValueToZero(shimmerColor)
        val shimmerLineWidth = maskWidth
        val yPosition = if (0 <= shimmerAngle) height.toFloat() else 0.toFloat()

        val shaderGradient: Shader = if (shimmerPulsate) {
            LinearGradient(
                0f,
                0f,
                width.toFloat(),
                0f,
                shimmerColor,
                shimmerColor,
                Shader.TileMode.CLAMP
            )
        } else {
            LinearGradient(
                0f,
                yPosition,
                cos(Math.toRadians(shimmerAngle.toDouble()))
                    .toFloat() * shimmerLineWidth,
                yPosition + sin(Math.toRadians(shimmerAngle.toDouble()))
                    .toFloat() * shimmerLineWidth,
                intArrayOf(
                    edgeColor,
                    shimmerColor,
                    shimmerColor,
                    edgeColor
                ),
                gradientColorDistribution,
                Shader.TileMode.CLAMP
            )
        }

        val maskBitmapShader = BitmapShader(localMaskBitmap!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val composeShader =
            ComposeShader(shaderGradient, maskBitmapShader, PorterDuff.Mode.DST_IN)

        gradientTexturePaint = Paint().apply {
            isAntiAlias = true
            isDither = true
            isFilterBitmap = true
            shader = composeShader
        }
    }

    private val shimmerAnimation: Animator
        @SuppressLint("ObjectAnimatorBinding")
        get() {
            if (maskAnimator != null) {
                return maskAnimator!!
            }
            if (maskRect == null) {
                maskRect = calculateBitmapMaskRect()
            }

            if (shimmerPulsate) {
                maskAnimator = ValueAnimator.ofInt(MAX_ALPHA, MIN_ALPHA, MIN_ALPHA, MAX_ALPHA).apply {
                    duration = DEFAULT_ANIMATION_DURATION_FOR_PULSATE.toLong()
                    interpolator = PathInterpolator(0.5f, 0.0f, 0.5f, 1.0f)

                    addUpdateListener { animation ->
                        maskAlpha = animation.animatedValue as Int
                        invalidate()
                    }
                }
            } else {
                val animationToX = width
                val animationFromX: Int = if (width > maskRect!!.width()) {
                    -animationToX
                } else {
                    -maskRect!!.width()
                }

                val shimmerBitmapWidth = maskRect!!.width()
                val shimmerAnimationFullLength = animationToX - animationFromX

                maskAnimator = if (isAnimationReversed) {
                    ValueAnimator.ofInt(
                        shimmerAnimationFullLength,
                        0
                    )
                } else {
                    ValueAnimator.ofInt(
                        0,
                        shimmerAnimationFullLength
                    )
                }

                maskAnimator?.apply {
                    duration = shimmerAnimationDuration.toLong()
                    addUpdateListener { animation ->
                        maskOffsetX = animationFromX + animation.animatedValue as Int
                        if (maskOffsetX + shimmerBitmapWidth >= 0) {
                            invalidate()
                        }
                    }
                }
            }

            return maskAnimator!!
        }

    private fun createBitmap(width: Int, height: Int): Bitmap? {
        return try {
            Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        } catch (e: OutOfMemoryError) {
            @Suppress("ExplicitGarbageCollectionCall")
            System.gc()
            null
        }
    }

    private fun getColor(id: Int): Int {
        return context.getColor(id)
    }

    @Suppress("CustomColorsKotlin")
    private fun reduceColorAlphaValueToZero(actualColor: Int): Int {
        return Color.argb(
            0,
            Color.red(actualColor),
            Color.green(actualColor),
            Color.blue(actualColor)
        )
    }

    private fun calculateBitmapMaskRect(): Rect {
        return Rect(0, 0, calculateMaskWidth(), height)
    }

    private fun calculateMaskWidth(): Int {
        val shimmerLineBottomWidth = maskWidth / cos(
            Math.toRadians(
                Math.abs(shimmerAngle).toDouble()
            )
        )
        val shimmerLineRemainingTopWidth =
            height * Math.tan(Math.toRadians(Math.abs(shimmerAngle).toDouble()))
        return (shimmerLineBottomWidth + shimmerLineRemainingTopWidth).toInt()
    }

    private val gradientColorDistribution: FloatArray
        get() {
            val colorDistribution = FloatArray(4)
            colorDistribution[0] = 0f
            colorDistribution[3] = 1f
            colorDistribution[1] = 0.5f - gradientCenterColorWidth / 2f
            colorDistribution[2] = 0.5f + gradientCenterColorWidth / 2f
            return colorDistribution
        }

    companion object {
        private const val DEFAULT_ANIMATION_DURATION = 1500
        private const val DEFAULT_ANGLE: Byte = 20
        private const val MIN_ANGLE_VALUE: Byte = -45
        private const val MAX_ANGLE_VALUE: Byte = 45
        private const val MIN_MASK_WIDTH_VALUE: Byte = 0
        private const val MAX_MASK_WIDTH_VALUE: Byte = 1
        private const val MIN_GRADIENT_CENTER_COLOR_WIDTH_VALUE: Byte = 0
        private const val MAX_GRADIENT_CENTER_COLOR_WIDTH_VALUE: Byte = 1
        private const val MAX_ALPHA = 255
        private const val MIN_ALPHA = 127
        private const val DEFAULT_ANIMATION_DURATION_FOR_PULSATE = 1200
    }
}
