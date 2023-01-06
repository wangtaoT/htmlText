package com.wt.htmltext

import android.text.*
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.text.style.URLSpan
import android.widget.TextView
import com.wt.htmltext.span.ImageClickSpan
import com.wt.htmltext.span.LinkClickSpan

class HtmlText private constructor(private var source: String?) {

    companion object {
        /**
         * 设置源文本
         */
        fun from(source: String?): HtmlText {
            return HtmlText(source)
        }
    }


    private var imageLoader: HtmlImageLoader? = null
    private var onTagClickListener: OnTagClickListener? = null
    private var after: After? = null

    interface After {
        fun after(ssb: SpannableStringBuilder?): CharSequence?
    }

    /**
     * 设置加载器
     */
    fun setImageLoader(imageLoader: HtmlImageLoader?): HtmlText {
        this.imageLoader = imageLoader
        return this
    }

    /**
     * 设置图片、链接点击监听器
     */
    fun setOnTagClickListener(onTagClickListener: OnTagClickListener?): HtmlText {
        this.onTagClickListener = onTagClickListener
        return this
    }

    /**
     * 对处理完成的文本再次处理
     */
    fun after(after: After?): HtmlText {
        this.after = after
        return this
    }

    /**
     * 注入TextView
     */
    fun into(textView: TextView) {
        if (source.isNullOrEmpty()){
            textView.text = ""
            return
        }
        val imageGetter = HtmlImageGetter()
        val tagHandler = HtmlTagHandler()
        val imageUrls: MutableList<String?> = ArrayList()
        imageGetter.setTextView(textView)
        imageGetter.setImageLoader(imageLoader)
        imageGetter.getImageSize(source)
        tagHandler.setTextView(textView)
        source = tagHandler.overrideTags(source)
        val spanned = Html.fromHtml(source, imageGetter, tagHandler)
        val ssb: SpannableStringBuilder = if (spanned is SpannableStringBuilder) {
            spanned
        } else {
            SpannableStringBuilder(spanned)
        }
        val imageSpans = ssb.getSpans(0, ssb.length, ImageSpan::class.java)
        for (i in imageSpans.indices) {
            val imageSpan = imageSpans[i]
            val imageUrl = imageSpan.source
            val start = ssb.getSpanStart(imageSpan)
            val end = ssb.getSpanEnd(imageSpan)
            imageUrls.add(imageUrl)
            val imageClickSpan = ImageClickSpan(textView.context, imageUrls, i)
            imageClickSpan.setListener(onTagClickListener)
            val clickableSpans = ssb.getSpans(start, end, ClickableSpan::class.java)
            if (clickableSpans != null) {
                for (cs in clickableSpans) {
                    ssb.removeSpan(cs)
                }
            }
            ssb.setSpan(imageClickSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // Hold text url link
        val urlSpans = ssb.getSpans(0, ssb.length, URLSpan::class.java)
        if (urlSpans != null) {
            for (urlSpan in urlSpans) {
                val start = ssb.getSpanStart(urlSpan)
                val end = ssb.getSpanEnd(urlSpan)
                ssb.removeSpan(urlSpan)
                val linkClickSpan = LinkClickSpan(textView.context, urlSpan.url)
                linkClickSpan.setListener(onTagClickListener)
                ssb.setSpan(linkClickSpan, start, end, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
            }
        }
        var charSequence: CharSequence? = ssb
        if (after != null) {
            charSequence = after!!.after(ssb)
        }
        textView.text = charSequence
    }
}