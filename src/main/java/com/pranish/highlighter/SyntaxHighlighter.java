package com.pranish.highlighter;

import org.fxmisc.richtext.model.StyleSpan;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {

    private static  final String[] keywords = {
            "if","else","while","let","int","double","string","return","bool","exit","print","printnl","input", "for"
    };

    private static final String KEYWORDS_PATTERN = "\\b(" + String.join("|", keywords) + ")\\b";
    private static final String COMMENTS_PATTERN = "//[^\n]*|/\\*(.|\\R)*?\\*/";
    private static final String STRINGS_PATTERN  = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String NUMBERS_PATTERN  = "\\b\\d+(\\.\\d+)?\\b";
    private static final String PARENS_PATTERN   = "[()]";
    private static final String BRACES_PATTERN   = "[{}]";

    // to compile a regex to a pattern so we can fuckign match it or idk sreach it
    private static final Pattern PATTERN = Pattern.compile(
            "(?<COMMENT>" + COMMENTS_PATTERN + ")"
                    + "|(?<STRING>" + STRINGS_PATTERN + ")"
                    + "|(?<KEYWORD>" + KEYWORDS_PATTERN + ")"
                    + "|(?<NUMBER>" + NUMBERS_PATTERN + ")"
                    + "|(?<PAREN>" + PARENS_PATTERN + ")"
                    + "|(?<BRACE>" + BRACES_PATTERN + ")"
    );

    public static StyleSpans<Collection<String>> highlight(String text) {
        Matcher matcher = PATTERN.matcher(text);
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int last = 0;

        while(matcher.find()){
            String styleClass = matcher.group("COMMENT") != null ? "comment":
                                matcher.group("STRING") != null ? "string" :
                                matcher.group("KEYWORD") != null ? "keyword" :
                                matcher.group("NUMBER") != null ? "number" :
                                matcher.group("PAREN") != null ? "paren" :
                                matcher.group("BRACE") != null ? "brace" : null;

            spansBuilder.add(Collections.emptyList(), matcher.start() - last);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            last = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - last);
        return spansBuilder.create();
    }
}
