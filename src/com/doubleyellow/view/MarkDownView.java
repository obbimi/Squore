package com.doubleyellow.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.webkit.WebView;

import com.doubleyellow.android.util.ContentUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.util.MarkdownProcessor;
import com.doubleyellow.util.StringUtil;

/**
 * NOT
 * #https://code.google.com/p/markdown4j
 * #wget -O markdown4j-2.2.jar "https://markdown4j.googlecode.com/files/markdown4j-2.2.jar"
 *
 * BUT
 * wget -O markdownview-1.2.jar "https://github.com/falnatsheh/MarkdownView/blob/master/jar/markdownview-1.2.jar?raw=true"
 *
 * TODO: ensure it can be used in Android designer
 */
public class MarkDownView extends WebView
{
    public MarkDownView(final Context context, final AttributeSet attrs) {
        super(context, attrs, 0 /*R.attr.markDownViewStyle*/);

        final TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.MarkDownView, 0, 0);
        if ( attributes != null ) {
            try {
                String sContent = attributes.getString(R.styleable.MarkDownView_content_string);
                if ( StringUtil.isEmpty(sContent) ) {
                    final int iResId = attributes.getResourceId(R.styleable.MarkDownView_content_raw, 0);
                    if ( iResId != 0 ) {
                        init(iResId);
                    }
                } else {
                    init(sContent);
                }
            } finally {
                // make sure recycle is always called.
                attributes.recycle();
            }
        }
    }

    public void init(int iRawOrStringResId) {
        String resourceEntryName = null;
        if ( this.isInEditMode() == false ) {
            resourceEntryName = getContext().getResources().getResourceName(iRawOrStringResId);
        }
        String sMD = null;
        if ( resourceEntryName == null || resourceEntryName.contains(":raw/")) {
            sMD = ContentUtil.readRaw(getContext(), iRawOrStringResId);
        } else {
            sMD = getContext().getString(iRawOrStringResId);
        }
        init(sMD);
    }

    private void init(String sMD) {
        try {
            sMD = sMD.replaceAll("__EOF__[^~]*", "");

            MarkdownProcessor processor = new MarkdownProcessor();
            String sHtml = processor.markdown(sMD);

            //sHtml = new Markdown4jProcessor().process(sMD);
            //sHtml = URLEncoder.encode(sHtml, "UTF-8").replaceAll("\\+", " ");
            sHtml = "<html>"
                    + "<head><style>"
                    + "ul { padding-left: 15px; }"
                    + "</style></head><body>"
                    + sHtml
                    + "</body></html>";
            super.loadData(sHtml,"text/html; charset=utf-8", "UTF-8"); // although this code is invoked in edit mode: the content is not shown decently in the preview mode
        } catch (Throwable e) {
            e.printStackTrace();
            if ( this.isInEditMode() ) {
                throw new RuntimeException(e);
            }
        }
    }

    //--------------------------------------------------------
    // MD conversion to HTML
    //--------------------------------------------------------

/*
    private static final String HEADER_PREFIX   = "#";
    private static final String LISTITEM_PREFIX = "-";

    private String simpleConvert(String sContentMD) {
        StringBuilder sb = new StringBuilder();
        List<String> lInput = new ArrayList<String>(Arrays.asList(sContentMD.split("\n")));
        ListUtil.removeEmpty(lInput);
        for(String sLine: lInput) {
            if ( sLine.startsWith(HEADER_PREFIX) ) {
                String sHeader = sLine.replaceFirst(HEADER_PREFIX, "").trim();
                addTag(sb, "h3", sHeader);
            }
            if ( sLine.startsWith(LISTITEM_PREFIX) ) {
                String sListItem = sLine.replaceFirst(LISTITEM_PREFIX, "").trim();
                addTag(sb, "li", sListItem);
            }
        }
        return sb.toString();
    }

    private static final Map<String,String> mParentTag = MapUtil.getMap("li", "ul");
    private String sOpenedParentTag = null;

    private void addTag(StringBuilder sb, String sTag, String sContent) {
        String sParentTag = mParentTag.get(sTag);
        if ( sOpenedParentTag == null ) {
            if ( sParentTag != null ) {
                sOpenedParentTag = sParentTag;
                sb.append("<").append(sParentTag).append(">");
            }
        } else if (sOpenedParentTag.equals(sParentTag) ) {
            // stay within same parent
        } else {
            // switch parent tag
            sb.append("</").append(sOpenedParentTag).append(">");
        }
        sb.append("<").append(sTag).append(">");
        sb.append(sContent);
        sb.append("</").append(sTag).append(">");
    }
*/
}
