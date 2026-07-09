package com.kato.pro.langchain.domain.rag;

import java.io.IOException;

/**
 * 文档加载器 SPI。把字节流解析成纯文本。
 *
 * 实现：
 *   - TextDocumentLoader  : .txt / .md / 无扩展名 / text/*
 *   - HtmlDocumentLoader  : .html / .htm / text/html
 *   - PdfDocumentLoader   : .pdf / application/pdf
 *   - WordDocumentLoader  : .docx / .doc / application/vnd.openxmlformats-officedocument.wordprocessingml.document
 *   - AutoDocumentLoader  : 分发器（按 contentType/filename 选实现）
 */
public interface DocumentLoader {

    /** 是否支持该输入（contentType 优先，filename 兜底） */
    boolean supports(String contentType, String filename);

    /** 把字节流解析成纯文本 */
    String load(byte[] content) throws IOException;
}
