package com.wt.htmltext.span;

import android.content.Context;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import com.wt.htmltext.OnTagClickListener;

import java.util.List;


public class ImageClickSpan extends ClickableSpan {
    private OnTagClickListener listener;
    private final Context context;
    private final List<String> imageUrls;
    private final int position;

    public ImageClickSpan(Context context, List<String> imageUrls, int position) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.position = position;
    }

    public void setListener(OnTagClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onClick(View widget) {
        if (listener != null) {
            listener.onImageClick(context, imageUrls, position);
        }
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setColor(ds.linkColor);
        ds.setUnderlineText(false);
    }
}
