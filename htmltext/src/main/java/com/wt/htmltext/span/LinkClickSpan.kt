package com.wt.htmltext.span

import android.content.Context
import android.text.style.ClickableSpan
import android.view.View
import com.wt.htmltext.OnTagClickListener

class LinkClickSpan(private val context: Context, private val url: String) : ClickableSpan() {

    private var listener: OnTagClickListener? = null

    fun setListener(listener: OnTagClickListener?) {
        this.listener = listener
    }

    override fun onClick(widget: View) {
        listener?.onLinkClick(context, url)
    }
}