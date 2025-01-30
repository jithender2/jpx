package com.proxy.utils;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.graphics.Color;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
*Use this if you want html color highlighting in responses change colors as preferred
**/
public class HtmlHighlighter {

    // Dracula theme colors
    private static final int DRACULA_CYAN = Color.parseColor("#8be9fd"); // Tags, selectors
    private static final int DRACULA_PURPLE = Color.parseColor("#bd93f9"); // Attributes, properties
    private static final int DRACULA_GREEN = Color.parseColor("#50fa7b"); // Attribute values, CSS values
    private static final int DRACULA_FOREGROUND = Color.parseColor("#f8f8f2"); // Text, braces, colons
	private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("(\\w+)=\"([^\"]*)\"" // Match attribute="value"
	);

    // Regex pattern to match HTML tokens
    private static final Pattern HTML_PATTERN = Pattern.compile(
        "(<script[^>]*>)(.*?)(</script>)|" + // Match <script> tags and content
        "(<style[^>]*>)(.*?)(</style>)|" + // Match <style> tags and content
        "(<\\/?[a-zA-Z][^>]*>)|([^<]+)" // Match other tags or text
    );

    // Regex pattern to match CSS syntax
    private static final Pattern CSS_PATTERN = Pattern.compile(
        "(\\{[^}]*\\})|" + // Match CSS rules (inside braces)
        "(\\b[a-zA-Z-]+\\b)(?=\\s*:)|" + // Match CSS properties
        "(:\\s*)([^;]+);" // Match CSS values
    );

    // Cache frequently used spans
    private static final ForegroundColorSpan TAG_SPAN = new ForegroundColorSpan(DRACULA_CYAN);
    private static final ForegroundColorSpan ATTRIBUTE_SPAN = new ForegroundColorSpan(DRACULA_PURPLE);
    private static final ForegroundColorSpan VALUE_SPAN = new ForegroundColorSpan(DRACULA_GREEN);
    private static final ForegroundColorSpan TEXT_SPAN = new ForegroundColorSpan(DRACULA_FOREGROUND);

    public SpannableStringBuilder highlightHtml(String html) {
        if (html == null || html.isEmpty()) {
            return new SpannableStringBuilder();
        }

        // Unescape HTML entities
        

        SpannableStringBuilder builder = new SpannableStringBuilder();
        Matcher matcher = HTML_PATTERN.matcher(html);

        while (matcher.find()) {
            String scriptTagOpen = matcher.group(1); // <script> opening tag
            String scriptContent = matcher.group(2); // JavaScript content
            String scriptTagClose = matcher.group(3); // </script> closing tag

            String styleTagOpen = matcher.group(4); // <style> opening tag
            String styleContent = matcher.group(5); // CSS content
            String styleTagClose = matcher.group(6); // </style> closing tag

            String tag = matcher.group(7); // Other HTML tags
            String text = matcher.group(8); // Text content

            if (scriptTagOpen != null) {
                // Highlight <script> tags and content
                highlightScript(builder, scriptTagOpen, scriptContent, scriptTagClose);
            } else if (styleTagOpen != null) {
                // Highlight <style> tags and CSS content
                highlightStyle(builder, styleTagOpen, styleContent, styleTagClose);
            } else if (tag != null) {
                // Highlight regular HTML tags
                highlightTag(builder, tag);
            } else if (text != null) {
                // Highlight text content
                highlightText(builder, text);
            }
        }

        return builder;
    }

    private void highlightScript(SpannableStringBuilder builder, String openTag, String content, String closeTag) {
        int start = builder.length();
        builder.append(openTag);
        builder.setSpan(TAG_SPAN, start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        start = builder.length();
        builder.append(content);
        builder.setSpan(TEXT_SPAN, start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        start = builder.length();
        builder.append(closeTag);
        builder.setSpan(TAG_SPAN, start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void highlightStyle(SpannableStringBuilder builder, String openTag, String content, String closeTag) {
        int start = builder.length();
        builder.append(openTag);
        builder.setSpan(TAG_SPAN, start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        start = builder.length();
        highlightCssSyntax(builder, content);

        start = builder.length();
        builder.append(closeTag);
        builder.setSpan(TAG_SPAN, start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void highlightTag(SpannableStringBuilder builder, String tag) {
        int start = builder.length();
        builder.append(tag);
        builder.setSpan(TAG_SPAN, start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Highlight attributes within the tag
        Matcher attrMatcher = ATTRIBUTE_PATTERN.matcher(tag);
        while (attrMatcher.find()) {
            int attrStart = start + attrMatcher.start(1);
            int attrEnd = start + attrMatcher.end(1);
            int valueStart = start + attrMatcher.start(2);
            int valueEnd = start + attrMatcher.end(2);

            // Highlight attribute name
            builder.setSpan(ATTRIBUTE_SPAN, attrStart, attrEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Highlight attribute value
            builder.setSpan(VALUE_SPAN, valueStart, valueEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void highlightText(SpannableStringBuilder builder, String text) {
        int start = builder.length();
        builder.append(text);
        builder.setSpan(TEXT_SPAN, start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void highlightCssSyntax(SpannableStringBuilder builder, String cssContent) {
        Matcher cssMatcher = CSS_PATTERN.matcher(cssContent);
        int start = builder.length();

        while (cssMatcher.find()) {
            String cssRule = cssMatcher.group(1); // Matches CSS rules (inside braces)
            String cssProperty = cssMatcher.group(2); // Matches CSS properties
            String cssColon = cssMatcher.group(3); // Matches the colon
            String cssValue = cssMatcher.group(4); // Matches CSS values

            if (cssRule != null) {
                // Highlight CSS rules (inside braces)
                builder.append(cssRule);
                builder.setSpan(TEXT_SPAN, start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (cssProperty != null) {
                // Highlight CSS properties
                builder.append(cssProperty);
                builder.setSpan(ATTRIBUTE_SPAN, start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (cssColon != null && cssValue != null) {
                // Highlight the colon and CSS values
                builder.append(cssColon);
                builder.setSpan(TEXT_SPAN, start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                start = builder.length();
                builder.append(cssValue);
                builder.setSpan(VALUE_SPAN, start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    // Unescape HTML entities
    
}
