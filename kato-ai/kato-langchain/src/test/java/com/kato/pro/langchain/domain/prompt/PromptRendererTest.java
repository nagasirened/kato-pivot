package com.kato.pro.langchain.domain.prompt;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PromptRendererTest {

    private final PromptRenderer r = new PromptRenderer();

    @Test
    void substitute_nullTemplate_returnsEmpty() {
        assertEquals("", r.substitute(null, Map.of("a", "1")));
    }

    @Test
    void substitute_noVars_returnsTemplate() {
        assertEquals("hello", r.substitute("hello", null));
        assertEquals("hello", r.substitute("hello", Map.of()));
    }

    @Test
    void substitute_singleVar() {
        assertEquals("hi 张三", r.substitute("hi ${name}", Map.of("name", "张三")));
    }

    @Test
    void substitute_multipleVars() {
        Map<String, Object> v = new HashMap<>();
        v.put("name", "Alice");
        v.put("age", 30);
        assertEquals("I'm Alice, 30 years old.", r.substitute("I'm ${name}, ${age} years old.", v));
    }

    @Test
    void substitute_missingVar_keepsOriginal() {
        assertEquals("hello ${unknown}", r.substitute("hello ${unknown}", Map.of("name", "x")));
    }

    @Test
    void substitute_nullVarValue_replacesWithEmpty() {
        Map<String, Object> v = new HashMap<>();
        v.put("name", null);
        assertEquals("hi ", r.substitute("hi ${name}", v));
    }

    @Test
    void substitute_specialCharsAreEscaped() {
        Map<String, Object> v = Map.of("price", "$100");
        String out = r.substitute("Price: ${price}", v);
        assertEquals("Price: $100", out);
    }
}
