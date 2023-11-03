package com.example.myapplication.shimmer

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable

class ShimmerDrawable : Drawable() {

    private val mUpdateListener = ValueAnimator.AnimatorUpdateListener {
        invalidateSelf()
    }

    private val mShimmerPaint = Paint()
    private val mDrawRect = Rect()
    private val mShaderMatrix = Matrix()

    private var mValueAnimator: ValueAnimator? = null

    private var mShimmer: Shimmer? = null

    val isShimmerStarted: Boolean
        get() {
            val valueAnimator = mValueAnimator
            return valueAnimator != null && valueAnimator.isStarted
        }

    init {
        mShimmerPaint.isAntiAlias = true
    }

    fun setShimmer(newShimmer: Shimmer?) {
        if (newShimmer != null) {
            mShimmerPaint.xfermode = PorterDuffXfermode(
                if (newShimmer.alphaShimmer) PorterDuff.Mode.DST_IN else PorterDuff.Mode.SRC_IN
            )
        }
        mShimmer = newShimmer
        updateShader()
        updateValueAnimator()
        invalidateSelf()
    }

    fun startShimmer() {
        val valueAnimator = mValueAnimator
        if (valueAnimator != null
            && !isShimmerStarted
            && callback != null
        ) {
            valueAnimator.start()
        }
    }

    fun stopShimmer() {
        val valueAnimator = mValueAnimator
        if (valueAnimator != null && isShimmerStarted) {
            valueAnimator.cancel()
        }
    }

    public override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val width = bounds.width()
        val height = bounds.height()
        mDrawRect.set(0, 0, width, height)
        updateShader()
        maybeStartShimmer()
    }

    override fun draw(canvas: Canvas) {
        val shimmer = mShimmer
        if (shimmer == null || mShimmerPaint.shader == null) {
            return
        }

        val tiltTan = Math.tan(Math.toRadians(shimmer.tilt.toDouble())).toFloat()
        val translateHeight = mDrawRect.height() + tiltTan * mDrawRect.width()
        val translateWidth = mDrawRect.width() + tiltTan * mDrawRect.height()
        val dx: Float
        val dy: Float
        val valueAnimator = mValueAnimator
        val animatedValue = if (valueAnimator != null) valueAnimator.animatedFraction else 0f
        when (shimmer.direction) {
            Shimmer.Direction.LEFT_TO_RIGHT -> {
                dx = offset(-translateWidth, translateWidth, animatedValue)
                dy = 0f
            }
            Shimmer.Direction.RIGHT_TO_LEFT -> {
                dx = offset(translateWidth, -translateWidth, animatedValue)
                dy = 0f
            }
            Shimmer.Direction.TOP_TO_BOTTOM -> {
                dx = 0f
                dy = offset(-translateHeight, translateHeight, animatedValue)
            }
            Shimmer.Direction.BOTTOM_TO_TOP -> {
                dx = 0f
                dy = offset(translateHeight, -translateHeight, animatedValue)
            }
            else -> {
                dx = offset(-translateWidth, translateWidth, animatedValue)
                dy = 0f
            }
        }

        mShaderMatrix.reset()
        mShaderMatrix.setRotate(shimmer.tilt, mDrawRect.width() / 2f, mDrawRect.height() / 2f)
        mShaderMatrix.postTranslate(dx, dy)
        mShimmerPaint.shader.setLocalMatrix(mShaderMatrix)
        canvas.drawRect(mDrawRect, mShimmerPaint)
    }

    override fun setAlpha(alpha: Int) {
        // No-op, modify the Shimmer object you pass in instead
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        // No-op, modify the Shimmer object you pass in instead
    }

    override fun getOpacity(): Int {
        val shimmer = mShimmer
        return if (shimmer != null && (shimmer.clipToChildren || shimmer.alphaShimmer))
            PixelFormat.TRANSLUCENT
        else
            PixelFormat.OPAQUE
    }

    private fun offset(start: Float, end: Float, percent: Float): Float {
        return start + (end - start) * percent
    }

    private fun updateValueAnimator() {
        val shimmer = mShimmer ?: return

        val started: Boolean
        val oldValueAnimator = mValueAnimator
        if (oldValueAnimator != null) {
            started = oldValueAnimator.isStarted
            oldValueAnimator.cancel()
            oldValueAnimator.removeAllUpdateListeners()
        } else {
            started = false
        }

        val valueAnimator =
            ValueAnimator.ofFloat(0f, 1f + (shimmer.repeatDelay / shimmer.animationDuration).toFloat())
        mValueAnimator = valueAnimator
        valueAnimator.repeatMode = shimmer.repeatMode
        valueAnimator.duration = shimmer.animationDuration + shimmer.repeatDelay
        valueAnimator.addUpdateListener(mUpdateListener)
        if (started) {
            valueAnimator.start()
        }
    }

    internal fun maybeStartShimmer() {
        val valueAnimator = mValueAnimator
        val shimmer = mShimmer
        if (valueAnimator != null
            && !valueAnimator.isStarted
            && shimmer != null
            && shimmer.autoStart
            && callback != null
        ) {
            valueAnimator.start()
        }
    }

    private fun updateShader() {
        val bounds = bounds
        val boundsWidth = bounds.width()
        val boundsHeight = bounds.height()
        val shimmer = mShimmer
        if (boundsWidth == 0 || boundsHeight == 0 || shimmer == null) {
            return
        }
        val width = shimmer.width(boundsWidth)
        val height = shimmer.height(boundsHeight)

        val shader: Shader
        when (shimmer.shape) {
            Shimmer.Shape.LINEAR -> {
                val vertical = shimmer.direction == Shimmer.Direction.TOP_TO_BOTTOM
                    || shimmer.direction == Shimmer.Direction.BOTTOM_TO_TOP
                val endX = if (vertical) 0 else width
                val endY = if (vertical) height else 0
                shader = LinearGradient(
                    0f,
                    0f,
                    endX.toFloat(),
                    endY.toFloat(),
                    shimmer.colors,
                    shimmer.positions,
                    Shader.TileMode.CLAMP
                )
            }
            Shimmer.Shape.RADIAL -> shader = RadialGradient(
                width / 2f,
                height / 2f,
                (Math.max(width, height) / Math.sqrt(2.0)).toFloat(),
                shimmer.colors,
                shimmer.positions,
                Shader.TileMode.CLAMP
            )
            else -> {
                val vertical =
                    shimmer.direction == Shimmer.Direction.TOP_TO_BOTTOM
                        || shimmer.direction == Shimmer.Direction.BOTTOM_TO_TOP
                val endX = if (vertical) 0 else width
                val endY = if (vertical) height else 0
                shader = LinearGradient(
                    0f,
                    0f,
                    endX.toFloat(),
                    endY.toFloat(),
                    shimmer.colors,
                    shimmer.positions,
                    Shader.TileMode.CLAMP
                )
            }
        }

        mShimmerPaint.shader = shader
    }
}

