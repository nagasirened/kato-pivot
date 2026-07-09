package com.kato.pro.langchain.domain.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentSplitterTest {

    @Test
    void split_nullOrBlank_returnsEmpty() {
        DocumentSplitter s = new DocumentSplitter();
        assertTrue(s.split(null).isEmpty());
        assertTrue(s.split("").isEmpty());
        assertTrue(s.split("   \n\n  ").isEmpty());
    }

    @Test
    void split_singleShortParagraph_returnsOne() {
        DocumentSplitter s = new DocumentSplitter();
        assertEquals(List.of("hello world"), s.split("hello world"));
    }

    @Test
    void split_paragraphsByBlankLine() {
        DocumentSplitter s = new DocumentSplitter();
        List<String> r = s.split("段落一。\n\n段落二。\n\n段落三。");
        assertEquals(3, r.size());
        assertEquals("段落一。", r.get(0));
    }

    @Test
    void split_markdownHeadings() {
        DocumentSplitter s = new DocumentSplitter();
        String text = "# 标题一\n段落A\n\n## 标题二\n段落B\n段落C";
        List<String> r = s.split(text);
        assertEquals(2, r.size());
        assertEquals("段落A", r.get(0));
        assertTrue(r.get(1).contains("段落B"));
        assertTrue(r.get(1).contains("段落C"));
    }

    @Test
    void split_longParagraph_chopsByLength() {
        DocumentSplitter s = new DocumentSplitter(100);
        String longPara = "a".repeat(250);
        List<String> r = s.split(longPara);
        assertEquals(3, r.size()); // 100 + 100 + 50
        assertEquals(100, r.get(0).length());
        assertEquals(50, r.get(2).length());
    }

    @Test
    void split_maxCharsTooSmall_throws() {
        assertThrows(IllegalArgumentException.class, () -> new DocumentSplitter(10));
    }
}
