package com.kato.pro.common.utils;

import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.kato.pro.common.constant.BaseConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public final class JsonUtils {

    private final static ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);

    static {
        // 忽略在json字符串中存在，但是在java对象中不存在对应属性的情况
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 忽略空Bean转json的错误
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS,false);
        // 允许不带引号的字段名称
        MAPPER.configure(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature(), true);
        // 允许单引号
        MAPPER.configure(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature(), true);
        // allow int startWith 0
        MAPPER.configure(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS.mappedFeature(), true);
        // 允许字符串存在转义字符：\r \n \t
        MAPPER.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
        // 排除空值字段
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 使用驼峰式
        MAPPER.setPropertyNamingStrategy(new PropertyNamingStrategies.LowerCamelCaseStrategy());
        // 使用bean名称
        MAPPER.enable(MapperFeature.USE_STD_BEAN_NAMING);
        // 所有日期格式都统一为固定格式
        MAPPER.setDateFormat(new SimpleDateFormat(BaseConstant.DATETIME_FORMAT));
        MAPPER.setTimeZone(TimeZone.getTimeZone(BaseConstant.TIME_ZONE_GMT8));
    }

    /**
     * 对象转换为json字符串
     * @param o 要转换的对象
     */
    public static String toJSONString(Object o) {
        return toJSONString(o, false);
    }

    /**
     * 对象转换为json字符串
     * @param o 要转换的对象
     * @param format 是否格式化json
     */
    public static String toJSONString(Object o, boolean format) {
        try {
            if (o == null) {
                return "";
            }
            if (o instanceof Number) {
                return o.toString();
            }
            if (o instanceof String) {
                return (String)o;
            }
            if (format) {
                return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(o);
            }
            return MAPPER.writeValueAsString(o);
        } catch ( JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 字符串转换为指定对象
     * @param json json字符串
     * @param cls 目标对象
     */
    public static <T> T toObject(JsonNode json, Class<T> cls) {
        if(json == null || cls == null){
            return null;
        }
        return MAPPER.convertValue(json, cls);
    }

    /**
     * 字符串转换为指定对象，并增加泛型转义
     * 例如：List<Integer> test = toObject(jsonStr, List.class, Integer.class);
     * @param json json字符串
     * @param parametrized 目标对象
     * @param parameterClasses 泛型对象
     */
    public static <T> T toObject(String json, Class<?> parametrized, Class<?>... parameterClasses) {
        if(CharSequenceUtil.isBlank(json) || parametrized == null){
            return null;
        }
        try {
            JavaType javaType = MAPPER.getTypeFactory().constructParametricType(parametrized, parameterClasses);
            return MAPPER.readValue(json, javaType);
        } catch ( IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 字符串转换为指定对象
     * @param json json字符串
     * @param typeReference 目标对象类型
     */
    public static <T> T toObject(String json, TypeReference<T> typeReference) {
        if(CharSequenceUtil.isBlank(json) || typeReference == null){
            return null;
        }
        try {
            return MAPPER.readValue(json, typeReference);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 字符串转换为指定对象
     * @param json json字符串
     * @param cls 目标对象
     */
    public static <T> T toObject(String json, Class<T> cls) {
        if(CharSequenceUtil.isBlank(json) || cls == null){
            return null;
        }
        try {
            return MAPPER.readValue(json, cls);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * byte数组转换为指定对象
     * @param bytes 字符组
     * @param cls 目标对象
     */
    public static <T> T toObject(byte[] bytes, Class<T> cls) {
        if(bytes == null || cls == null || bytes.length == 0){
            return null;
        }
        try {
            return MAPPER.readValue(bytes, cls);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 字符串转换为JsonNode对象
     * @param json json字符串
     */
    public static JsonNode parse(String json) {
        if (CharSequenceUtil.isBlank(json)) {
            return null;
        }
        try {
            return MAPPER.readTree(json);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 对象转换为map对象
     * @param o 要转换的对象
     */
    public static <K, V> Map<K, V> toMap(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof String) {
            return toObject((String) o, new TypeReference<Map<K, V>>() {});
        }
        return MAPPER.convertValue(o, new TypeReference<Map<K, V>>() {});
    }

    /**
     * 对象转换为map对象
     * @param o 要转换的对象
     */
    public static <K, V> Map<K, V> toMap(Object o, Class<K> k, Class<V> v) {
        if (o == null) {
            return null;
        }
        if (o instanceof String) {
            return toObject((String) o, new TypeReference<Map<K, V>>() {});
        }
        return MAPPER.convertValue(o, new TypeReference<Map<K, V>>() {});
    }

    /**
     * json字符串转换为list对象
     * @param json json字符串
     */
    public static <T> List<T> toList(String json) {
        if (CharSequenceUtil.isBlank(json)) {
            return null;
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<T>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * json字符串转换为list对象，并指定元素类型
     * @param json json字符串
     * @param cls list的元素类型
     */
    public static <T> List<T> toList(String json, Class<T> cls) {
        if (CharSequenceUtil.isBlank(json)) {
            return null;
        }
        try {
            JavaType javaType = MAPPER.getTypeFactory().constructParametricType(List.class, cls);
            return MAPPER.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            log.error("MAPPER toList fail, json: {}, clazz: {}", json, cls);
            return new LinkedList<>();
        }
    }
}
