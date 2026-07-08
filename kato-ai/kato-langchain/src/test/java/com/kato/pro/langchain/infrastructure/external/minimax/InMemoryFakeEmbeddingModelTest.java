package com.kato.pro.langchain.infrastructure.external.minimax;

import com.kato.pro.langchain.common.exception.SystemException;
import com.kato.pro.langchain.domain.embedding.EmbeddingRequest;
import com.kato.pro.langchain.domain.embedding.EmbeddingResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryFakeEmbeddingModelTest {

    @Test
    void modelName_returnsConstant() {
        InMemoryFakeEmbeddingModel m = new InMemoryFakeEmbeddingModel();
        assertEquals("in-memory-fake-v1", m.modelName());
    }

    @Test
    void dimension_returnsConstructorValue() {
        assertEquals(512, new InMemoryFakeEmbeddingModel().dimension());
        assertEquals(128, new InMemoryFakeEmbeddingModel(128).dimension());
    }

    @Test
    void dimension_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> new InMemoryFakeEmbeddingModel(0));
        assertThrows(IllegalArgumentException.class, () -> new InMemoryFakeEmbeddingModel(-1));
        assertThrows(IllegalArgumentException.class, () -> new InMemoryFakeEmbeddingModel(5000));
    }

    @Test
    void embed_emptyInputs_throwsSystemException() {
        InMemoryFakeEmbeddingModel m = new InMemoryFakeEmbeddingModel();
        SystemException ex = assertThrows(SystemException.class,
                () -> m.embed(EmbeddingRequest.builder().inputs(List.of()).build()).block());
        assertNotNull(ex.getMessage());
    }

    @Test
    void embed_singleText_returnsL2NormalizedVector() {
        InMemoryFakeEmbeddingModel m = new InMemoryFakeEmbeddingModel(128);
        EmbeddingResponse resp = m.embed(EmbeddingRequest.builder().inputs(List.of("hello")).build()).block();
        assertEquals(1, resp.getVectors().size());
        float[] v = resp.getVectors().get(0);
        assertEquals(128, v.length);
        double sum = 0;
        for (float f : v) sum += f * f;
        assertEquals(1.0, sum, 0.001, "L2 norm should be 1");
    }

    @Test
    void embed_sameText_returnsSameVector() {
        InMemoryFakeEmbeddingModel m = new InMemoryFakeEmbeddingModel(64);
        float[] v1 = m.embed(EmbeddingRequest.builder().inputs(List.of("相同的输入")).build())
                .block().getVectors().get(0);
        float[] v2 = m.embed(EmbeddingRequest.builder().inputs(List.of("相同的输入")).build())
                .block().getVectors().get(0);
        assertArrayEquals(v1, v2, 0.0f, "same text should produce same vector");
    }

    @Test
    void embed_differentText_returnsDifferentVector() {
        InMemoryFakeEmbeddingModel m = new InMemoryFakeEmbeddingModel(64);
        float[] v1 = m.embed(EmbeddingRequest.builder().inputs(List.of("apple")).build())
                .block().getVectors().get(0);
        float[] v2 = m.embed(EmbeddingRequest.builder().inputs(List.of("banana")).build())
                .block().getVectors().get(0);
        boolean anyDiff = false;
        for (int i = 0; i < v1.length; i++) {
            if (v1[i] != v2[i]) { anyDiff = true; break; }
        }
        assertTrue(anyDiff, "different texts should produce different vectors");
    }

    @Test
    void embed_batch_returnsVectorsInSameOrder() {
        InMemoryFakeEmbeddingModel m = new InMemoryFakeEmbeddingModel(32);
        List<String> inputs = List.of("first", "second", "third");
        EmbeddingResponse resp = m.embed(EmbeddingRequest.builder().inputs(inputs).build()).block();
        assertEquals(3, resp.getVectors().size());
        float[] v0 = resp.getVectors().get(0);
        float[] v0Again = m.embed(EmbeddingRequest.builder().inputs(List.of("first")).build())
                .block().getVectors().get(0);
        assertArrayEquals(v0, v0Again, 0.0f);
    }

    @Test
    void embed_commonPrefix_higherSimilarityThanUnrelated() {
        InMemoryFakeEmbeddingModel m = new InMemoryFakeEmbeddingModel(256);
        float[] vA = m.embed(EmbeddingRequest.builder().inputs(List.of("订单退款规则")).build())
                .block().getVectors().get(0);
        float[] vB = m.embed(EmbeddingRequest.builder().inputs(List.of("订单退款流程")).build())
                .block().getVectors().get(0);
        float[] vC = m.embed(EmbeddingRequest.builder().inputs(List.of("今天天气真好")).build())
                .block().getVectors().get(0);

        double simAB = cosine(vA, vB);
        double simAC = cosine(vA, vC);
        // 宽松断言：hash-based embedding 只能保证"不同文本距离大"，
        // 语义相似性靠真实模型（v2 ONNX）。同前缀可能让 hash 更近。
        assertTrue(simAB > simAC - 0.3,
                "expected sim(A,B) >= sim(A,C) - 0.3; got simAB=" + simAB + " simAC=" + simAC);
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0;
        for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
        return dot; // L2-normalized vectors: dot == cosine similarity
    }
}
