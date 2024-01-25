package com.kato.pro.base.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class JsonUtils {

    public static final ObjectMapper jsonMapper = new ObjectMapper();

    /**
     * convert to obj
     */
    public static  <T> T toObj(String str, Class<T> clazz) {
        try {
            return jsonMapper.readValue(str, clazz);
        } catch (JsonProcessingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    /**
     * convert to obj
     */
    public static <T> T toObj(String str, TypeReference<T> clz) {
        try {
            return jsonMapper.readValue(str, clz);
        } catch (JsonProcessingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    /**
     * convert to list<obj>
     */
    public static <T> List<T> toList(String str, Class<T> clazz) {
        try {
            return jsonMapper.readValue(str, new TypeReference<List<T>>() {});
        } catch (JsonProcessingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    /**
     * convert to JsonNode
     */
    public static JsonNode toJsonNode(String str) {
        try {
            return jsonMapper.readTree(str);
        } catch (JsonProcessingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static <T> T nodeToValue(JsonNode node, Class<T> clazz) {
        try {
            return jsonMapper.treeToValue(node, clazz);
        } catch (JsonProcessingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static String toStr(Object obj) {
        try {
            return jsonMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }
}
