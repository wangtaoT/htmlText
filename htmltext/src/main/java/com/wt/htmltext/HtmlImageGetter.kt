package com.wt.htmltext

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Looper
import android.text.Html.ImageGetter
import android.widget.TextView
import java.util.regex.Pattern

internal class HtmlImageGetter : ImageGetter {
    companion object {
        private const val IMAGE_TAG_REGULAR = "<(img|IMG)\\s+([^>]*)>"
        private val IMAGE_TAG_PATTERN = Pattern.compile(IMAGE_TAG_REGULAR)
        private val IMAGE_WIDTH_PATTERN = Pattern.compile("(width|WIDTH)\\s*=\\s*\"?(\\w+)\"?")
        private val IMAGE_HEIGHT_PATTERN = Pattern.compile("(height|HEIGHT)\\s*=\\s*\"?(\\w+)\"?")
        private fun parseSize(size: String): Int {
            return try {
                Integer.valueOf(size)
            } catch (e: NumberFormatException) {
                -1
            }
        }
    }

    private var textView: TextView? = null
    private var imageLoader: HtmlImageLoader? = null
    private val imageSizeList: MutableList<ImageSize> = ArrayList()
    private var index = 0
    fun setTextView(textView: TextView?) {
        this.textView = textView
    }

    fun setImageLoader(imageLoader: HtmlImageLoader?) {
        this.imageLoader = imageLoader
    }

    fun getImageSize(source: String?) {
        val imageMatcher = IMAGE_TAG_PATTERN.matcher(source)
        while (imageMatcher.find()) {
            val attrs = imageMatcher.group(2).trim { it <= ' ' }
            var width = -1
            var height = -1
            val widthMatcher = IMAGE_WIDTH_PATTERN.matcher(attrs)
            if (widthMatcher.find()) {
                width = parseSize(widthMatcher.group(2).trim { it <= ' ' })
            }
            val heightMatcher = IMAGE_HEIGHT_PATTERN.matcher(attrs)
            if (heightMatcher.find()) {
                height = parseSize(heightMatcher.group(2).trim { it <= ' ' })
            }
            val imageSize = ImageSize(width, height)
            imageSizeList.add(imageSize)
        }
    }

    override fun getDrawable(source: String): Drawable {
        val imageDrawable = ImageDrawable(index++)
        imageDrawable.setDrawable(imageLoader!!.defaultDrawable, false)
        imageLoader?.loadImage(source, object : HtmlImageLoader.Callback {
            override fun onLoadComplete(bitmap: Bitmap?) {
                runOnUi {
                    val drawable: Drawable = BitmapDrawable(
                        textView?.resources, bitmap
                    )
                    imageDrawable.setDrawable(drawable, true)
                    textView?.text = textView!!.text
                }
            }

            override fun onLoadFailed() {
                runOnUi {
                    imageDrawable.setDrawable(imageLoader!!.errorDrawable, false)
                    textView?.text = textView!!.text
                }
            }
        })
        return imageDrawable
    }

    private fun runOnUi(r: Runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run()
        } else {
            textView?.post(r)
        }
    }

    private class ImageSize(val width: Int, val height: Int) {
        fun valid(): Boolean {
            return width >= 0 && height >= 0
        }
    }

    private inner class ImageDrawable(  // img 标签出现的位置
        private val position: Int
    ) : BitmapDrawable() {
        private var mDrawable: Drawable? = null
        fun setDrawable(drawable: Drawable?, fitSize: Boolean) {
            mDrawable = drawable
            if (mDrawable == null) {
                setBounds(0, 0, 0, 0)
                return
            }
            val maxWidth = if (imageLoader == null) 0 else imageLoader!!.maxWidth
            val fitWidth = imageLoader != null && imageLoader!!.fitWidth()
            var width: Int
            var height: Int
            if (fitSize) { // real image
                val imageSize = if (imageSizeList.size > position) imageSizeList[position] else null
                if (imageSize != null && imageSize.valid()) {
                    width = dp2px(imageSize.width.toFloat())
                    height = dp2px(imageSize.height.toFloat())
                } else {
                    width = mDrawable!!.intrinsicWidth
                    height = mDrawable!!.intrinsicHeight
                }
            } else { // placeholder image
                width = mDrawable!!.intrinsicWidth
                height = mDrawable!!.intrinsicHeight
            }
            if (width > 0 && height > 0) {
                // too large or should fit width
                if (maxWidth > 0 && (width > maxWidth || fitWidth)) {
                    height = (height.toFloat() / width * maxWidth).toInt()
                    width = maxWidth
                }
            }
            mDrawable!!.setBounds(0, 0, width, height)
            setBounds(0, 0, width, height)
        }

        override fun draw(canvas: Canvas) {
            // override the draw to facilitate refresh function later
            if (mDrawable != null) {
                if (mDrawable is BitmapDrawable) {
                    val bitmapDrawable = mDrawable as BitmapDrawable
                    val bitmap = bitmapDrawable.bitmap
                    if (bitmap == null || bitmap.isRecycled) {
                        return
                    }
                }
                mDrawable!!.draw(canvas)
            }
        }

        private fun dp2px(dpValue: Float): Int {
            val scale = textView!!.resources.displayMetrics.density
            return (dpValue * scale + 0.5f).toInt()
        }
    }
}