package com.kato.pro.langchain.domain.rag;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * 分发器：按 contentType/filename 选具体 loader。
 * 兜底 TextDocumentLoader。
 */
@Component
public class AutoDocumentLoader implements DocumentLoader {

    private final List<DocumentLoader> delegates;

    public AutoDocumentLoader(TextDocumentLoader text, HtmlDocumentLoader html,
                               PdfDocumentLoader pdf, WordDocumentLoader word) {
        // 顺序：Text 在最后兜底
        this.delegates = List.of(pdf, word, html, text);
    }

    @Override
    public boolean supports(String contentType, String filename) {
        return true;
    }

    @Override
    public String load(byte[] content) throws IOException {
        throw new UnsupportedOperationException(
                "AutoDocumentLoader.load requires contentType/filename — use load(content, contentType, filename)");
    }

    public String load(byte[] content, String contentType, String filename) throws IOException {
        // 先按精确匹配的 loader 找
        for (DocumentLoader d : delegates) {
            if (d.supports(contentType, filename)) return d.load(content);
        }
        // 兜底：当作 UTF-8 文本解码（业务方自己保证大致可读）
        return new String(content, java.nio.charset.StandardCharsets.UTF_8);
    }
}
