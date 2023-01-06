package com.wt.htmltext.span

import android.content.Context
import android.text.style.ClickableSpan
import com.wt.htmltext.OnTagClickListener
import android.text.TextPaint
import android.view.View

class ImageClickSpan(
    private val context: Context,
    private val imageUrls: List<String?>,
    private val position: Int
) : ClickableSpan() {
    private var listener: OnTagClickListener? = null
    fun setListener(listener: OnTagClickListener?) {
        this.listener = listener
    }

    override fun onClick(widget: View) {
        listener?.onImageClick(context, imageUrls, position)
    }

    override fun updateDrawState(ds: TextPaint) {
        ds.color = ds.linkColor
        ds.isUnderlineText = false
    }
}