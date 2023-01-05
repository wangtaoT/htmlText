package com.wt.htmltext.span;

import android.content.Context;
import android.text.style.ClickableSpan;
import android.view.View;

import com.wt.htmltext.OnTagClickListener;

public class LinkClickSpan extends ClickableSpan {
    private OnTagClickListener listener;
    private final Context context;
    private final String url;

    public LinkClickSpan(Context context, String url) {
        this.context = context;
        this.url = url;
    }

    public void setListener(OnTagClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onClick(View widget) {
        if (listener != null) {
            listener.onLinkClick(context, url);
        }
    }
}
