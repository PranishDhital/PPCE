package com.pranish.highlighter;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Adds language-aware style classes to the PPCE editor text. */
public final class SyntaxHighlighter {

    private static final String COMMENT = "//[^\\n]*|/\\*[\\s\\S]*?\\*/";
    private static final String STRING = "\"(?:[^\"\\\\]|\\\\.)*\"";
    private static final String NUMBER = "\\b(?:0[xX][0-9a-fA-F]+|\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)\\b";
    private static final String TYPE = "\\b(?:int|double|string|bool)\\b";
    private static final String KEYWORD = "\\b(?:if|else|while|for|return|let|exit)\\b";
    private static final String BOOLEAN = "\\b(?:true|false)\\b";
    private static final String BUILTIN = "\\b(?:printnl|print|input)\\b(?=\\s*\\()";
    private static final String FUNCTION = "\\b[A-Za-z_][A-Za-z0-9_]*(?=\\s*\\()";
    private static final String OPERATOR = "(?:==|!=|<=|>=|&&|\\|\\||\\+\\+|--|[+*/%<>=!&|^-])";
    private static final String DELIMITER = "[(){}\\[\\],;.]";

    /* Match comments and strings first so their contents are never read as code. */
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(?<COMMENT>" + COMMENT + ")"
                    + "|(?<STRING>" + STRING + ")"
                    + "|(?<BOOLEAN>" + BOOLEAN + ")"
                    + "|(?<BUILTIN>" + BUILTIN + ")"
                    + "|(?<TYPE>" + TYPE + ")"
                    + "|(?<KEYWORD>" + KEYWORD + ")"
                    + "|(?<NUMBER>" + NUMBER + ")"
                    + "|(?<FUNCTION>" + FUNCTION + ")"
                    + "|(?<OPERATOR>" + OPERATOR + ")"
                    + "|(?<DELIMITER>" + DELIMITER + ")");

    private SyntaxHighlighter() {
    }

    public static StyleSpans<Collection<String>> highlight(String text) {
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        StyleSpansBuilder<Collection<String>> spans = new StyleSpansBuilder<>();
        int previousEnd = 0;

        while (matcher.find()) {
            spans.add(Collections.emptyList(), matcher.start() - previousEnd);
            spans.add(Collections.singleton(styleFor(matcher)), matcher.end() - matcher.start());
            previousEnd = matcher.end();
        }

        spans.add(Collections.emptyList(), text.length() - previousEnd);
        return spans.create();
    }

    private static String styleFor(Matcher matcher) {
        if (matcher.group("COMMENT") != null) return "comment";
        if (matcher.group("STRING") != null) return "string";
        if (matcher.group("BOOLEAN") != null) return "boolean";
        if (matcher.group("BUILTIN") != null) return "builtin";
        if (matcher.group("TYPE") != null) return "type";
        if (matcher.group("KEYWORD") != null) return "keyword";
        if (matcher.group("NUMBER") != null) return "number";
        if (matcher.group("FUNCTION") != null) return "function";
        if (matcher.group("OPERATOR") != null) return "operator";
        if (matcher.group("DELIMITER") != null) return "delimiter";
        throw new IllegalStateException("Matched text without a syntax style");
    }
}
