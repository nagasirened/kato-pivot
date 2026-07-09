package com.kato.pro.langchain.domain.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * PDF loader：用 LangChain4j 1.2.0-beta8 `apache-pdfbox` 解析器。
 *
 * 依赖（pom 引入）：dev.langchain4j:langchain4j-document-parser-apache-pdfbox:1.2.0-beta8
 */
@Component
public class PdfDocumentLoader implements DocumentLoader {

    private final ApachePdfBoxDocumentParser parser = new ApachePdfBoxDocumentParser();

    @Override
    public boolean supports(String contentType, String filename) {
        if (contentType != null && contentType.toLowerCase().contains("pdf")) return true;
        if (filename == null) return false;
        return filename.toLowerCase().endsWith(".pdf");
    }

    @Override
    public String load(byte[] content) throws IOException {
        Document doc = parser.parse(new java.io.ByteArrayInputStream(content));
        return doc.text();
    }
}
