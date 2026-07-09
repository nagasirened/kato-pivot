package com.kato.pro.langchain.domain.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Word loader：用 LangChain4j 1.2.0-beta8 `apache-poi` 解析器（支持 .docx）。
 *
 * .doc（旧格式）解析质量差但能跑；v2 可换 Aspose 等。
 */
@Component
public class WordDocumentLoader implements DocumentLoader {

    private final ApachePoiDocumentParser parser = new ApachePoiDocumentParser();

    @Override
    public boolean supports(String contentType, String filename) {
        if (contentType != null) {
            String c = contentType.toLowerCase();
            if (c.contains("officedocument.wordprocessingml") || c.contains("msword")) return true;
        }
        if (filename == null) return false;
        String f = filename.toLowerCase();
        return f.endsWith(".docx") || f.endsWith(".doc");
    }

    @Override
    public String load(byte[] content) throws IOException {
        Document doc = parser.parse(new java.io.ByteArrayInputStream(content));
        return doc.text();
    }
}
