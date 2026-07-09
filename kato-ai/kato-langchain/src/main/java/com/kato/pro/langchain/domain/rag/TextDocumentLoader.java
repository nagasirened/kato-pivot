package com.kato.pro.langchain.domain.rag;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 纯文本/Markdown loader。UTF-8 解码。
 */
@Component
public class TextDocumentLoader implements DocumentLoader {

    @Override
    public boolean supports(String contentType, String filename) {
        if (contentType != null && (contentType.startsWith("text/plain") || contentType.startsWith("text/markdown")))
            return true;
        if (filename == null) return false;
        String f = filename.toLowerCase();
        return f.endsWith(".txt") || f.endsWith(".md") || f.endsWith(".markdown");
    }

    @Override
    public String load(byte[] content) {
        return new String(content, StandardCharsets.UTF_8);
    }
}
