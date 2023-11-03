package com.example.myapplication.shimmer

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.RectF
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.Px
import com.example.myapplication.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Suppress("CustomColorsKotlin")
class Shimmer internal constructor() {

    internal val positions = FloatArray(COMPONENT_COUNT)
    internal val colors = IntArray(COMPONENT_COUNT)
    internal val bounds = RectF()

    @Direction
    internal var direction = Direction.LEFT_TO_RIGHT

    @ColorInt
    internal var highlightColor = Color.WHITE

    @ColorInt
    internal var baseColor = 0x4cffffff

    @Shape
    internal var shape = Shape.LINEAR

    internal var fixedWidth = 0
    internal var fixedHeight = 0

    internal var widthRatio = 1f
    internal var heightRatio = 1f
    internal var intensity = 0f
    internal var dropoff = 0.5f
    internal var tilt = 20f

    internal var clipToChildren = true
    internal var autoStart = true
    internal var alphaShimmer = true

    internal var repeatCount = ValueAnimator.INFINITE
    internal var repeatMode = ValueAnimator.RESTART
    internal var animationDuration = 1000L
    internal var repeatDelay: Long = 0

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(Shape.LINEAR, Shape.RADIAL)
    annotation class Shape {
        companion object {
            const val LINEAR = 0
            const val RADIAL = 1
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        Direction.LEFT_TO_RIGHT,
        Direction.TOP_TO_BOTTOM,
        Direction.RIGHT_TO_LEFT,
        Direction.BOTTOM_TO_TOP
    )
    annotation class Direction {
        companion object {
            const val LEFT_TO_RIGHT = 0
            const val TOP_TO_BOTTOM = 1
            const val RIGHT_TO_LEFT = 2
            const val BOTTOM_TO_TOP = 3
        }
    }

    internal fun width(width: Int): Int {
        return if (fixedWidth > 0) fixedWidth
        else Math.round(widthRatio * width)
    }

    internal fun height(height: Int): Int {
        return if (fixedHeight > 0) fixedHeight
        else (heightRatio * height).roundToInt()
    }

    internal fun updateColors() {
        when (shape) {
            Shape.LINEAR -> {
                colors[0] = baseColor
                colors[1] = highlightColor
                colors[2] = highlightColor
                colors[3] = baseColor
            }
            Shape.RADIAL -> {
                colors[0] = highlightColor
                colors[1] = highlightColor
                colors[2] = baseColor
                colors[3] = baseColor
            }
            else -> {
                colors[0] = baseColor
                colors[1] = highlightColor
                colors[2] = highlightColor
                colors[3] = baseColor
            }
        }
    }

    internal fun updatePositions() {
        when (shape) {
            Shape.LINEAR -> {
                positions[0] = Math.max((1f - intensity - dropoff) / 2f, 0f)
                positions[1] = Math.max((1f - intensity - 0.001f) / 2f, 0f)
                positions[2] = Math.min((1f + intensity + 0.001f) / 2f, 1f)
                positions[3] = Math.min((1f + intensity + dropoff) / 2f, 1f)
            }
            Shape.RADIAL -> {
                positions[0] = 0f
                positions[1] = Math.min(intensity, 1f)
                positions[2] = Math.min(intensity + dropoff, 1f)
                positions[3] = 1f
            }
            else -> {
                positions[0] = Math.max((1f - intensity - dropoff) / 2f, 0f)
                positions[1] = Math.max((1f - intensity - 0.001f) / 2f, 0f)
                positions[2] = Math.min((1f + intensity + 0.001f) / 2f, 1f)
                positions[3] = Math.min((1f + intensity + dropoff) / 2f, 1f)
            }
        }
    }

    fun updateBounds(viewWidth: Int, viewHeight: Int) {
        val magnitude = Math.max(viewWidth, viewHeight)
        val rad = Math.PI / 2f - Math.toRadians((tilt % 90f).toDouble())
        val hyp = magnitude / Math.sin(rad)
        val padding = 3 * Math.round((hyp - magnitude).toFloat() / 2f)
        bounds.set(
            (-padding).toFloat(),
            (-padding).toFloat(),
            (width(viewWidth) + padding).toFloat(),
            (height(viewHeight) + padding).toFloat()
        )
    }

    abstract class Builder<T : Builder<T>> {
        internal val mShimmer = Shimmer()

        protected abstract val `this`: T

        fun consumeAttributes(context: Context, attrs: AttributeSet): T {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ShimmerFrameLayout, 0, 0)
            return consumeAttributes(a)
        }

        internal open fun consumeAttributes(a: TypedArray): T {
            if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_clip_to_children)) {
                setClipToChildren(
                    a.getBoolean(
                        R.styleable.ShimmerFrameLayout_shimmer_clip_to_children, mShimmer.clipToChildren
                    )
                )
            }
            if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_auto_start)) {
                setAutoStart(
                    a.getBoolean(R.styleable.ShimmerFrameLayout_shimmer_auto_start, mShimmer.autoStart)
                )
            }
            if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_base_alpha)) {
                setBaseAlpha(a.getFloat(R.styleable.ShimmerFrameLayout_shimmer_base_alpha, 0.3f))
            }
            if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_highlight_alpha)) {
                setHighlightAlpha(a.getFloat(R.styleable.ShimmerFrameLayout_shimmer_highlight_alpha, 1f))
            }
            if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_duration)) {
                setDuration(
                    a.getInt(
                        R.styleable.ShimmerFrameLayout_shimmer_duration, mShimmer.animationDuration.toInt()
                    ).toLong()
                )
            }
            if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_repeat_count)) {
                setRepeatCount(
                    a.getInt(R.styleable.ShimmerFrameLayout_shimmer_repeat_count, mShimmer.repeatCount)
                )
            }
            if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_repeat_delay)) {
                setRepeatDelay(
                    a.getInt(
                        R.styleable.ShimmerFrameLayout_shimmer_repeat_delay, mShimmer.repeatDelay.toInt()
                    ).toLong()
                )
            }
            if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_repeat_mode)) {
                setRepeatMode(
                    a.getInt(R.styleable.ShimmerFrameLayout_shimmer_repeat_mode, mShimmer.repeatMode)
                )
            }

            if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_direction)) {
                val direction = a.getInt(R.styleable.ShimmerFrameLayout_shimmer_direction, mShimmer.direction)
                when (direction) {
                    Direction.LEFT_TO_RIGHT -> setDirection(Direction.LEFT_TO_RIGHT)
                    Direction.TOP_TO_BOTTOM -> setDirection(Direction.TOP_TO_BOTTOM)
                    Direction.RIGHT_TO_LEFT -> setDirection(Direction.RIGHT_TO_LEFT)
                    Direction.BOTTOM_TO_TOP -> setDirection(Direction.BOTTOM_TO_TOP)
                    else -> setDirection(Direction.LEFT_TO_RIGHT)
                }
            }

            if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_shape)) {
                val shape = a.getInt(R.styleable.ShimmerFrameLayout_shimmer_shape, mShimmer.shape)
                when (shape) {
                    Shape.LINEAR -> setShape(Shape.LINEAR)
                    Shape.RADIAL -> setShape(Shape.RADIAL)
                    else -> setShape(Shape.LINEAR)
                }
            }

            if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_dropoff)) {
                setDropoff(a.getFloat(R.styleable.ShimmerFrameLayout_shimmer_dropoff, mShimmer.dropoff))
            }
            if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_fixed_width)) {
                setFixedWidth(
                    a.getDimensionPixelSize(
                        R.styleable.ShimmerFrameLayout_shimmer_fixed_width, mShimmer.fixedWidth
                    )
                )
            }
            if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_fixed_height)) {
                setFixedHeight(
                    a.getDimensionPixelSize(
                        R.styleable.ShimmerFrameLayout_shimmer_fixed_height, mShimmer.fixedHeight
                    )
                )
            }
            if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_intensity)) {
                setIntensity(
                    a.getFloat(R.styleable.ShimmerFrameLayout_shimmer_intensity, mShimmer.intensity)
                )
            }
            if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_width_ratio)) {
                setWidthRatio(
                    a.getFloat(R.styleable.ShimmerFrameLayout_shimmer_width_ratio, mShimmer.widthRatio)
                )
            }
            if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_height_ratio)) {
                setHeightRatio(
                    a.getFloat(R.styleable.ShimmerFrameLayout_shimmer_height_ratio, mShimmer.heightRatio)
                )
            }
            if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_tilt)) {
                setTilt(a.getFloat(R.styleable.ShimmerFrameLayout_shimmer_tilt, mShimmer.tilt))
            }
            return `this`
        }

        fun copyFrom(other: Shimmer): T {
            setDirection(other.direction)
            setShape(other.shape)
            setFixedWidth(other.fixedWidth)
            setFixedHeight(other.fixedHeight)
            setWidthRatio(other.widthRatio)
            setHeightRatio(other.heightRatio)
            setIntensity(other.intensity)
            setDropoff(other.dropoff)
            setTilt(other.tilt)
            setClipToChildren(other.clipToChildren)
            setAutoStart(other.autoStart)
            setRepeatCount(other.repeatCount)
            setRepeatMode(other.repeatMode)
            setRepeatDelay(other.repeatDelay)
            setDuration(other.animationDuration)
            mShimmer.baseColor = other.baseColor
            mShimmer.highlightColor = other.highlightColor
            return `this`
        }

        private fun setDirection(@Direction direction: Int): T {
            mShimmer.direction = direction
            return `this`
        }

        private fun setShape(@Shape shape: Int): T {
            mShimmer.shape = shape
            return `this`
        }

        private fun setFixedWidth(@Px fixedWidth: Int): T {
            if (fixedWidth < 0) {
                throw IllegalArgumentException("Given invalid width: $fixedWidth")
            }
            mShimmer.fixedWidth = fixedWidth
            return `this`
        }

        private fun setFixedHeight(@Px fixedHeight: Int): T {
            if (fixedHeight < 0) {
                throw IllegalArgumentException("Given invalid height: $fixedHeight")
            }
            mShimmer.fixedHeight = fixedHeight
            return `this`
        }

        fun setWidthRatio(widthRatio: Float): T {
            if (widthRatio < 0f) {
                throw IllegalArgumentException("Given invalid width ratio: $widthRatio")
            }
            mShimmer.widthRatio = widthRatio
            return `this`
        }

        private fun setHeightRatio(heightRatio: Float): T {
            if (heightRatio < 0f) {
                throw IllegalArgumentException("Given invalid height ratio: $heightRatio")
            }
            mShimmer.heightRatio = heightRatio
            return `this`
        }

        private fun setIntensity(intensity: Float): T {
            if (intensity < 0f) {
                throw IllegalArgumentException("Given invalid intensity value: $intensity")
            }
            mShimmer.intensity = intensity
            return `this`
        }

        private fun setDropoff(dropoff: Float): T {
            if (dropoff < 0f) {
                throw IllegalArgumentException("Given invalid dropoff value: $dropoff")
            }
            mShimmer.dropoff = dropoff
            return `this`
        }

        private fun setTilt(tilt: Float): T {
            mShimmer.tilt = tilt
            return `this`
        }

        private fun setBaseAlpha(@FloatRange(from = 0.0, to = 1.0) alpha: Float): T {
            val intAlpha = (clamp(0f, 1f, alpha) * 255f).toInt()
            mShimmer.baseColor = (intAlpha shl 24) or (mShimmer.baseColor and 0x00FFFFFF)
            return `this`
        }

        fun setHighlightAlpha(@FloatRange(from = 0.0, to = 1.0) alpha: Float): T {
            val intAlpha = (clamp(0f, 1f, alpha) * 255f).toInt()
            mShimmer.highlightColor = (intAlpha shl 24) or (mShimmer.highlightColor and 0x00FFFFFF)
            return `this`
        }

        private fun setClipToChildren(status: Boolean): T {
            mShimmer.clipToChildren = status
            return `this`
        }

        private fun setAutoStart(status: Boolean): T {
            mShimmer.autoStart = status
            return `this`
        }

        private fun setRepeatCount(repeatCount: Int): T {
            mShimmer.repeatCount = repeatCount
            return `this`
        }

        private fun setRepeatMode(mode: Int): T {
            mShimmer.repeatMode = mode
            return `this`
        }

        private fun setRepeatDelay(millis: Long): T {
            if (millis < 0) {
                throw IllegalArgumentException("Given a negative repeat delay: $millis")
            }
            mShimmer.repeatDelay = millis
            return `this`
        }

        fun setDuration(millis: Long): T {
            if (millis < 0) {
                throw IllegalArgumentException("Given a negative duration: $millis")
            }
            mShimmer.animationDuration = millis
            return `this`
        }

        fun build(): Shimmer {
            mShimmer.updateColors()
            mShimmer.updatePositions()
            return mShimmer
        }

        private fun clamp(min: Float, max: Float, value: Float): Float {
            return min(max, max(min, value))
        }
    }

    class AlphaHighlightBuilder : Builder<AlphaHighlightBuilder>() {

        override val `this`: AlphaHighlightBuilder
            get() {
                return this
            }

        init {
            mShimmer.alphaShimmer = true
        }
    }

    class ColorHighlightBuilder : Builder<ColorHighlightBuilder>() {

        override val `this`: ColorHighlightBuilder
            get() {
                return this
            }

        init {
            mShimmer.alphaShimmer = false
        }

        fun setHighlightColor(@ColorInt color: Int): ColorHighlightBuilder {
            mShimmer.highlightColor = color
            return `this`
        }

        fun setBaseColor(@ColorInt color: Int): ColorHighlightBuilder {
            mShimmer.baseColor = (mShimmer.baseColor and -0x1000000) or (color and 0x00FFFFFF)
            return `this`
        }

        override fun consumeAttributes(a: TypedArray): ColorHighlightBuilder {
            super.consumeAttributes(a)
            if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_base_color)) {
                setBaseColor(
                    a.getColor(R.styleable.ShimmerFrameLayout_shimmer_base_color, mShimmer.baseColor)
                )
            }
            if (a.hasValue(R.styleable.ShimmerFrameLayout_shimmer_highlight_color)) {
                setHighlightColor(
                    a.getColor(
                        R.styleable.ShimmerFrameLayout_shimmer_highlight_color, mShimmer.highlightColor
                    )
                )
            }
            return `this`
        }
    }
}

private const val COMPONENT_COUNT = 4
