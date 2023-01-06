package com.wt.htmltext.span

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.TextPaint
import android.text.style.LeadingMarginSpan
import android.text.Spanned

class NumberSpan(textPaint: TextPaint, number: Int) : LeadingMarginSpan {
    private val mNumber: String
    private val mTextWidth: Int
    override fun getLeadingMargin(first: Boolean): Int {
        return mTextWidth
    }

    override fun drawLeadingMargin(
        c: Canvas, p: Paint, x: Int, dir: Int, top: Int, baseline: Int,
        bottom: Int, text: CharSequence, start: Int, end: Int,
        first: Boolean, l: Layout
    ) {
        if (text is Spanned) {
            val spanStart = text.getSpanStart(this)
            if (spanStart == start) {
                c.drawText(mNumber, x.toFloat(), baseline.toFloat(), p)
            }
        }
    }

    init {
        mNumber = "$number. "
        mTextWidth = textPaint.measureText(mNumber).toInt()
    }
}