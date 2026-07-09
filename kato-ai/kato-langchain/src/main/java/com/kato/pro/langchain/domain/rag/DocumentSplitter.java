package com.kato.pro.langchain.domain.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 文档分段器（D1=C）。
 *
 * 规则（按优先级）：
 *   1. Markdown 标题（# / ## / ### ...）切大段
 *   2. 空行分隔的段落切中段
 *   3. 单段超 maxChars 时，按 maxChars 切小段
 *
 * 默认 maxChars=500（spec 默认值，可通过构造参数覆盖）。
 */
public class DocumentSplitter {

    private static final Pattern MD_HEADING = Pattern.compile("(?m)^#{1,6}\\s+.*$");

    private final int maxChars;

    public DocumentSplitter() { this(500); }

    public DocumentSplitter(int maxChars) {
        if (maxChars < 50) throw new IllegalArgumentException("maxChars must be >= 50");
        this.maxChars = maxChars;
    }

    public List<String> split(String text) {
        if (text == null || text.isBlank()) return List.of();
        String trimmed = text.strip();
        List<String> out = new ArrayList<>();
        // Step 1: 按 Markdown 标题切
        String[] sections = MD_HEADING.split(trimmed);
        for (String section : sections) {
            if (section.isBlank()) continue;
            // Step 2: 按空行切段落
            String[] paragraphs = section.split("\\n\\s*\\n");
            for (String para : paragraphs) {
                String p = para.strip();
                if (p.isEmpty()) continue;
                // Step 3: 长度兜底
                if (p.length() <= maxChars) {
                    out.add(p);
                } else {
                    out.addAll(chopByLength(p));
                }
            }
        }
        return out;
    }

    private List<String> chopByLength(String s) {
        List<String> r = new ArrayList<>();
        for (int i = 0; i < s.length(); i += maxChars) {
            int end = Math.min(i + maxChars, s.length());
            r.add(s.substring(i, end));
        }
        return r;
    }
}
