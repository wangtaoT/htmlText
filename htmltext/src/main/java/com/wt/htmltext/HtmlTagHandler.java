package com.wt.htmltext;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.TypefaceSpan;
import android.widget.TextView;
import com.wt.htmltext.span.NumberSpan;

import org.xml.sax.XMLReader;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Some parts of this code are based on android.text.Html
 * <p>
 * Modified from https://github.com/SufficientlySecure/html-textview
 */
class HtmlTagHandler implements Html.TagHandler {
    private static final String UNORDERED_LIST = "HTML_TEXT_TAG_UL";
    private static final String ORDERED_LIST = "HTML_TEXT_TAG_OL";
    private static final String LIST_ITEM = "HTML_TEXT_TAG_LI";
    private static final String FONT = "HTML_TEXT_TAG_FONT";
    private static final String DIV = "HTML_TEXT_TAG_DIV";
    private static final String SPAN = "HTML_TEXT_TAG_SPAN";

    private Map<String, String> spanAttributes;
    private int startIndex;

    private Context mContext;
    private TextPaint mTextPaint;

    /**
     * Keeps track of lists (ol, ul). On bottom of Stack is the outermost list
     * and on top of Stack is the most nested list
     */
    private final Stack<String> lists = new Stack<>();
    /**
     * Tracks indexes of ordered lists so that after a nested list ends
     * we can continue with correct index of outer list
     */
    private final Stack<Integer> olNextIndex = new Stack<>();

    private static final int indent = 10;
    private static final int listItemIndent = indent * 2;
    private static final BulletSpan bullet = new BulletSpan(indent);

    void setTextView(TextView textView) {
        mContext = textView.getContext().getApplicationContext();
        mTextPaint = textView.getPaint();
    }

    /**
     * Newer versions of the Android SDK's {@link Html.TagHandler} handles &lt;ul&gt; and &lt;li&gt;
     * tags itself which means they never get delegated to this class. We want to handle the tags
     * ourselves so before passing the string html into Html.fromHtml(), we can use this method to
     * replace the &lt;ul&gt; and &lt;li&gt; tags with tags of our own.
     *
     * @param html String containing HTML, for example: "<b>Hello world!</b>"
     * @return html with replaced <ul> and <li> tags
     * @see <a href="https://github.com/android/platform_frameworks_base/commit/8b36c0bbd1503c61c111feac939193c47f812190">Specific Android SDK Commit</a>
     */
    String overrideTags(String html) {
        if (html == null) {
            return null;
        }

        // Wrap HTML tags to prevent parsing custom tags error
        html = "<html>" + html + "</html>";

        html = html.replace("<ul", "<" + UNORDERED_LIST);
        html = html.replace("</ul>", "</" + UNORDERED_LIST + ">");
        html = html.replace("<ol", "<" + ORDERED_LIST);
        html = html.replace("</ol>", "</" + ORDERED_LIST + ">");
        html = html.replace("<li", "<" + LIST_ITEM);
        html = html.replace("</li>", "</" + LIST_ITEM + ">");
        html = html.replace("<font", "<" + FONT);
        html = html.replace("</font>", "</" + FONT + ">");
        html = html.replace("<div", "<" + DIV);
        html = html.replace("</div>", "</" + DIV + ">");
        html = html.replace("<span", "<" + SPAN);
        html = html.replace("</span>", "</" + SPAN + ">");

        return html;
    }

    @Override
    public void handleTag(final boolean opening, final String tag, Editable output, final XMLReader xmlReader) {
        if (opening) {
            // opening tag
            if (tag.equalsIgnoreCase(UNORDERED_LIST)) {
                lists.push(tag);
            } else if (tag.equalsIgnoreCase(ORDERED_LIST)) {
                lists.push(tag);
                olNextIndex.push(1);
            } else if (tag.equalsIgnoreCase(LIST_ITEM)) {
                if (output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
                    output.append("\n");
                }
                if (!lists.isEmpty()) {
                    String parentList = lists.peek();
                    if (parentList.equalsIgnoreCase(ORDERED_LIST)) {
                        start(output, new Ol());
                        olNextIndex.push(olNextIndex.pop() + 1);
                    } else if (parentList.equalsIgnoreCase(UNORDERED_LIST)) {
                        start(output, new Ul());
                    }
                }
            } else if (tag.equalsIgnoreCase(FONT)) {
                startFont(output, xmlReader);
            } else if (tag.equalsIgnoreCase(DIV)) {
                handleDiv(output);
            } else if (tag.equalsIgnoreCase(SPAN)) {
                startSpan(output, xmlReader);
            } else if (tag.equalsIgnoreCase("code")) {
                start(output, new Code());
            } else if (tag.equalsIgnoreCase("center")) {
                start(output, new Center());
            } else if (tag.equalsIgnoreCase("s") || tag.equalsIgnoreCase("strike")) {
                start(output, new Strike());
            } else if (tag.equalsIgnoreCase("tr")) {
                start(output, new Tr());
            } else if (tag.equalsIgnoreCase("th")) {
                start(output, new Th());
            } else if (tag.equalsIgnoreCase("td")) {
                start(output, new Td());
            }
        } else {
            // closing tag
            if (tag.equalsIgnoreCase(UNORDERED_LIST)) {
                lists.pop();
            } else if (tag.equalsIgnoreCase(ORDERED_LIST)) {
                lists.pop();
                olNextIndex.pop();
            } else if (tag.equalsIgnoreCase(LIST_ITEM)) {
                if (!lists.isEmpty()) {
                    if (lists.peek().equalsIgnoreCase(UNORDERED_LIST)) {
                        if (output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
                            output.append("\n");
                        }
                        // Nested BulletSpans increases distance between bullet and text, so we must prevent it.
                        int bulletMargin = indent;
                        if (lists.size() > 1) {
                            bulletMargin = indent - bullet.getLeadingMargin(true);
                            if (lists.size() > 2) {
                                // This get's more complicated when we add a LeadingMarginSpan into the same line:
                                // we have also counter it's effect to BulletSpan
                                bulletMargin -= (lists.size() - 2) * listItemIndent;
                            }
                        }
                        BulletSpan newBullet = new BulletSpan(bulletMargin);
                        end(output, Ul.class, false,
                                new LeadingMarginSpan.Standard(listItemIndent * (lists.size() - 1)),
                                newBullet);
                    } else if (lists.peek().equalsIgnoreCase(ORDERED_LIST)) {
                        if (output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
                            output.append("\n");
                        }
                        int numberMargin = listItemIndent * (lists.size() - 1);
                        if (lists.size() > 2) {
                            // Same as in ordered lists: counter the effect of nested Spans
                            numberMargin -= (lists.size() - 2) * listItemIndent;
                        }
                        NumberSpan numberSpan = new NumberSpan(mTextPaint, olNextIndex.lastElement() - 1);
                        end(output, Ol.class, false,
                                new LeadingMarginSpan.Standard(numberMargin),
                                numberSpan);
                    }
                }
            } else if (tag.equalsIgnoreCase(FONT)) {
                endFont(output);
            } else if (tag.equalsIgnoreCase(DIV)) {
                handleDiv(output);
            } else if (tag.equalsIgnoreCase(SPAN)) {
                endSpan(output);
                spanAttributes.clear();
            } else if (tag.equalsIgnoreCase("code")) {
                end(output, Code.class, false, new TypefaceSpan("monospace"));
            } else if (tag.equalsIgnoreCase("center")) {
                end(output, Center.class, true, new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER));
            } else if (tag.equalsIgnoreCase("s") || tag.equalsIgnoreCase("strike")) {
                end(output, Strike.class, false, new StrikethroughSpan());
            } else if (tag.equalsIgnoreCase("tr")) {
                end(output, Tr.class, false);
            } else if (tag.equalsIgnoreCase("th")) {
                end(output, Th.class, false);
            } else if (tag.equalsIgnoreCase("td")) {
                end(output, Td.class, false);
            }
        }
    }

    private static class Ul {
    }

    private static class Ol {
    }

    private static class Code {
    }

    private static class Center {
    }

    private static class Strike {
    }

    private static class Tr {
    }

    private static class Th {
    }

    private static class Td {
    }

    private static class Font {
        public String color;
        public String size;

        public Font(String color, String size) {
            this.color = color;
            this.size = size;
        }
    }

    /**
     * Mark the opening tag by using private classes
     */
    private void start(Editable output, Object mark) {
        int len = output.length();
        output.setSpan(mark, len, len, Spannable.SPAN_MARK_MARK);
    }

    /**
     * Modified from {@link Html}
     */
    private void end(Editable output, Class kind, boolean paragraphStyle, Object... replaces) {
        Object obj = getLast(output, kind);
        // start of the tag
        int where = output.getSpanStart(obj);
        // end of the tag
        int len = output.length();

        output.removeSpan(obj);

        if (where != len) {
            int thisLen = len;
            // paragraph styles like AlignmentSpan need to end with a new line!
            if (paragraphStyle) {
                output.append("\n");
                thisLen++;
            }
            for (Object replace : replaces) {
                output.setSpan(replace, where, thisLen, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private void startFont(Editable output, XMLReader xmlReader) {
        int len = output.length();
        Map<String, String> attributes = getAttributes(xmlReader);
        String color = attributes.get("color");
        String size = attributes.get("size");
        output.setSpan(new Font(color, size), len, len, Spannable.SPAN_MARK_MARK);
    }

    private void endFont(Editable output) {
        int len = output.length();
        Object obj = getLast(output, Font.class);
        int where = output.getSpanStart(obj);

        output.removeSpan(obj);

        if (where != len) {
            Font f = (Font) obj;
            int color = parseColor(f.color);
            int size = parseSize(f.size);

            if (color != -1) {
                output.setSpan(new ForegroundColorSpan(color | 0xFF000000), where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (size > 0) {
                output.setSpan(new AbsoluteSizeSpan(size, true), where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private void startSpan(Editable output, XMLReader xmlReader) {
        startIndex = output.length();
        spanAttributes = getAttributes(xmlReader);
    }

    private void endSpan(Editable output) {
        int endIndex = output.length();
        String color = spanAttributes.get("color");
        String bacgroundColor = spanAttributes.get("background-color");
        String size = spanAttributes.get("size");
        String style = spanAttributes.get("style");
        if (!TextUtils.isEmpty(style)) {
            analysisStyle(startIndex, endIndex, output, style);
        }
        if (!TextUtils.isEmpty(size)) {
            size = size.split("px")[0];
        }
        if (!TextUtils.isEmpty(bacgroundColor)) {
            if (bacgroundColor.startsWith("@")) {
                Resources res = Resources.getSystem();
                String name = bacgroundColor.substring(1);
                int colorRes = res.getIdentifier(name, "color", "android");
                if (colorRes != 0) {
                    output.setSpan(new BackgroundColorSpan(colorRes), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else {
                if (bacgroundColor.startsWith("rgb")) {
                    bacgroundColor = bacgroundColor.replace("rgb(", "");
                    bacgroundColor = bacgroundColor.replace(")", "");
                    String[] rgbs = bacgroundColor.split(", ");
                    bacgroundColor = toHex(Integer.parseInt(rgbs[0]), Integer.parseInt(rgbs[1]), Integer.parseInt(rgbs[2]));
                }
                try {
                    output.setSpan(new BackgroundColorSpan(Color.parseColor(bacgroundColor)), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } catch (Exception e) {
                    e.printStackTrace();
                    reductionFontColor(startIndex, endIndex, output);
                }
            }
        }
        if (!TextUtils.isEmpty(color)) {
            if (color.startsWith("@")) {
                Resources res = Resources.getSystem();
                String name = color.substring(1);
                int colorRes = res.getIdentifier(name, "color", "android");
                if (colorRes != 0) {
                    output.setSpan(new ForegroundColorSpan(colorRes), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else {
                try {
                    output.setSpan(new ForegroundColorSpan(Color.parseColor(color)), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } catch (Exception e) {
                    e.printStackTrace();
                    reductionFontColor(startIndex, endIndex, output);
                }
            }
        }
        if (!TextUtils.isEmpty(size)) {
            int fontSizePx = 16;
            if (null != mContext) {
                fontSizePx = sp2px(Integer.parseInt(size));
            }
            output.setSpan(new AbsoluteSizeSpan(fontSizePx), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void handleDiv(Editable output) {
        int len = output.length();

        if (len >= 1 && output.charAt(len - 1) == '\n') {
            return;
        }

        if (len != 0) {
            output.append("\n");
        }
    }

    private HashMap<String, String> getAttributes(XMLReader xmlReader) {
        HashMap<String, String> attributes = new HashMap<>();
        try {
            Field elementField = xmlReader.getClass().getDeclaredField("theNewElement");
            elementField.setAccessible(true);
            Object element = elementField.get(xmlReader);
            Field attrsField = element.getClass().getDeclaredField("theAtts");
            attrsField.setAccessible(true);
            Object attrs = attrsField.get(element);
            Field dataField = attrs.getClass().getDeclaredField("data");
            dataField.setAccessible(true);
            String[] data = (String[]) dataField.get(attrs);
            Field lengthField = attrs.getClass().getDeclaredField("length");
            lengthField.setAccessible(true);
            int len = (Integer) lengthField.get(attrs);

            /**
             * MSH: Look for supported attributes and add to hash map.
             * This is as tight as things can get :)
             * The data index is "just" where the keys and values are stored.
             */
            for (int i = 0; i < len; i++)
                attributes.put(data[i * 5 + 1], data[i * 5 + 4]);
        } catch (Exception ignored) {
        }
        return attributes;
    }

    private void analysisStyle(int startIndex, int stopIndex, Editable editable, String style) {
        String[] attrArray = style.split(";");
        Map<String, String> attrMap = new HashMap<>();
        if (null != attrArray) {
            for (String attr : attrArray) {
                String[] keyValueArray = attr.split(":");
                if (null != keyValueArray && keyValueArray.length == 2) {
                    // 记住要去除前后空格
                    attrMap.put(keyValueArray[0].trim(), keyValueArray[1].trim());
                }
            }
        }
        String color = attrMap.get("color");
        String bacgroundColor = attrMap.get("background-color");
        String fontSize = attrMap.get("font-size");
        if (!TextUtils.isEmpty(fontSize)) {
            fontSize = fontSize.split("px")[0];
        }
        if (!TextUtils.isEmpty(bacgroundColor)) {
            if (bacgroundColor.startsWith("@")) {
                Resources res = Resources.getSystem();
                String name = bacgroundColor.substring(1);
                int colorRes = res.getIdentifier(name, "color", "android");
                if (colorRes != 0) {
                    editable.setSpan(new BackgroundColorSpan(colorRes), startIndex, stopIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else {
                if (bacgroundColor.startsWith("rgb")) {
                    bacgroundColor = bacgroundColor.replace("rgb(", "");
                    bacgroundColor = bacgroundColor.replace(")", "");
                    String[] rgbs = bacgroundColor.split(", ");
                    bacgroundColor = toHex(Integer.parseInt(rgbs[0]), Integer.parseInt(rgbs[1]), Integer.parseInt(rgbs[2]));
                }
                try {
                    editable.setSpan(new BackgroundColorSpan(Color.parseColor(bacgroundColor)), startIndex, stopIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } catch (Exception e) {
                    e.printStackTrace();
                    reductionFontColor(startIndex, stopIndex, editable);
                }
            }
        }
        if (!TextUtils.isEmpty(color)) {
            if (color.startsWith("@")) {
                Resources res = Resources.getSystem();
                String name = color.substring(1);
                int colorRes = res.getIdentifier(name, "color", "android");
                if (colorRes != 0) {
                    editable.setSpan(new ForegroundColorSpan(colorRes), startIndex, stopIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else {
                if (color.startsWith("rgb")) {
                    color = color.replace("rgb(", "");
                    color = color.replace(")", "");
                    String[] rgbs = color.split(", ");
                    color = toHex(Integer.parseInt(rgbs[0]), Integer.parseInt(rgbs[1]), Integer.parseInt(rgbs[2]));
                }

                try {
                    editable.setSpan(new ForegroundColorSpan(Color.parseColor(color)), startIndex, stopIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } catch (Exception e) {
                    e.printStackTrace();
                    reductionFontColor(startIndex, stopIndex, editable);
                }
            }
        }
        if (!TextUtils.isEmpty(fontSize)) {
            int fontSizePx = 14;
            if (null != mContext) {
                fontSizePx = sp2px(Integer.parseInt(fontSize));
            }
            editable.setSpan(new AbsoluteSizeSpan(fontSizePx), startIndex, stopIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /**
     * Get last marked position of a specific tag kind (private class)
     */
    private static Object getLast(Editable text, Class kind) {
        Object[] objs = text.getSpans(0, text.length(), kind);
        if (objs.length == 0) {
            return null;
        } else {
            for (int i = objs.length; i > 0; i--) {
                if (text.getSpanFlags(objs[i - 1]) == Spannable.SPAN_MARK_MARK) {
                    return objs[i - 1];
                }
            }
            return null;
        }
    }

    private static int parseColor(String colorString) {
        try {
            return Color.parseColor(colorString);
        } catch (Exception ignored) {
            return -1;
        }
    }

    /**
     * dpValue
     */
    private int parseSize(String size) {
        int s;
        try {
            s = Integer.parseInt(size);
        } catch (NumberFormatException ignored) {
            return 0;
        }

        s = Math.max(s, 1);
        s = Math.min(s, 7);

        int baseSize = px2dp(mTextPaint.getTextSize());

        return (s - 3) + baseSize;
    }

    private int px2dp(float pxValue) {
        float density = mContext.getResources().getDisplayMetrics().density;
        return (int) (pxValue / density + 0.5f);
    }

    public int sp2px(final float spValue) {
        final float fontScale = Resources.getSystem().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    private void reductionFontColor(int startIndex, int stopIndex, Editable editable) {
        editable.setSpan(new ForegroundColorSpan(0xff2b2b2b), startIndex, stopIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public static String toHex(int r, int g, int b) {
        return "#" + toBrowserHexValue(r) + toBrowserHexValue(g)
                + toBrowserHexValue(b);
    }

    private static String toBrowserHexValue(int number) {
        StringBuilder builder = new StringBuilder(
                Integer.toHexString(number & 0xff));
        while (builder.length() < 2) {
            builder.append("0");
        }
        return builder.toString().toUpperCase();
    }
}
