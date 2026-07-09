package com.kato.pro.langchain.domain.rag;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentLoaderTest {

    @Test
    void textLoader_supportsMdAndTxt() {
        TextDocumentLoader l = new TextDocumentLoader();
        assertTrue(l.supports("text/plain", "a.txt"));
        assertTrue(l.supports("text/markdown", "a.md"));
        assertTrue(l.supports(null, "x.markdown"));
        assertFalse(l.supports(null, "a.pdf"));
    }

    @Test
    void textLoader_loadsUtf8() {
        TextDocumentLoader l = new TextDocumentLoader();
        String s = l.load("你好 hello".getBytes(StandardCharsets.UTF_8));
        assertEquals("你好 hello", s);
    }

    @Test
    void htmlLoader_stripsTags() {
        HtmlDocumentLoader l = new HtmlDocumentLoader();
        String html = "<html><head><style>body{}</style></head><body><h1>标题</h1><p>段落</p></body></html>";
        String out = l.load(html.getBytes(StandardCharsets.UTF_8));
        assertTrue(out.contains("标题"));
        assertTrue(out.contains("段落"));
        assertFalse(out.contains("<"));
    }

    @Test
    void autoLoader_dispatchesByFilename() throws Exception {
        AutoDocumentLoader auto = new AutoDocumentLoader(
                new TextDocumentLoader(), new HtmlDocumentLoader(),
                new PdfDocumentLoader(), new WordDocumentLoader());

        String html = "<p>hello</p>";
        assertEquals("hello", auto.load(html.getBytes(StandardCharsets.UTF_8), "text/html", "x.html"));

        String txt = "raw text";
        assertEquals("raw text", auto.load(txt.getBytes(StandardCharsets.UTF_8), "text/plain", "x.txt"));
    }

    @Test
    void autoLoader_fallsBackToTextLoader() throws Exception {
        AutoDocumentLoader auto = new AutoDocumentLoader(
                new TextDocumentLoader(), new HtmlDocumentLoader(),
                new PdfDocumentLoader(), new WordDocumentLoader());
        // 未知扩展名 → 兜底 TextDocumentLoader
        String out = auto.load("fallback content".getBytes(StandardCharsets.UTF_8),
                "application/x-binary", "x.bin");
        assertEquals("fallback content", out);
    }
}
