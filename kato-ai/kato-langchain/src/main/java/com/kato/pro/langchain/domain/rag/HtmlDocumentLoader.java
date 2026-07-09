package com.kato.pro.langchain.domain.rag;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * HTML loader：用正则剥掉 `<script>`/`<style>` 与所有标签。
 *
 * 不引 jsoup（M5 阶段保持依赖最小）；后续 v2 可换 jsoup。
 */
@Component
public class HtmlDocumentLoader implements DocumentLoader {

    private static final Pattern SCRIPT_STYLE = Pattern.compile(
            "<(script|style)[^>]*>.*?</\\1>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern TAGS = Pattern.compile("<[^>]+>");

    @Override
    public boolean supports(String contentType, String filename) {
        if (contentType != null && contentType.toLowerCase().contains("html")) return true;
        if (filename == null) return false;
        String f = filename.toLowerCase();
        return f.endsWith(".html") || f.endsWith(".htm");
    }

    @Override
    public String load(byte[] content) {
        String html = new String(content, java.nio.charset.StandardCharsets.UTF_8);
        String stripped = SCRIPT_STYLE.matcher(html).replaceAll("");
        stripped = TAGS.matcher(stripped).replaceAll(" ");
        return decodeEntities(stripped).replaceAll("\\s+", " ").trim();
    }

    private static String decodeEntities(String s) {
        return s.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }
}
