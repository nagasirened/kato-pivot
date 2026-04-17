package com.kato.pro.sensitive.util;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 字符转换工具
 * 支持同音字、形近字、简繁体转换
 */
@Component
public class CharacterConverter {

    /**
     * 同音字映射表（常用）
     */
    private static final Map<Character, Set<Character>> HOMOPHONE_MAP = new HashMap<>();

    /**
     * 形近字映射表（常用）
     */
    private static final Map<Character, Set<Character>> SIMILAR_CHAR_MAP = new HashMap<>();

    /**
     * 简体到繁体的映射
     */
    private static final Map<Character, Character> S2T_MAP = new HashMap<>();

    /**
     * 繁体到简体的映射
     */
    private static final Map<Character, Character> T2S_MAP = new HashMap<>();

    static {
        initHomophoneMap();
        initSimilarCharMap();
        initSTMap();
    }

    private static void initHomophoneMap() {
        // 常用同音字（示例）
        addHomophone('我', '哦', '卧', '沃');
        addHomophone('你', '尼', '泥', '腻');
        addHomophone('他', '它', '她', '塌');
        addHomophone('是', '事', '市', '时');
        addHomophone('不', '布', '部', '步');
        addHomophone('的', '得', '地', '底');
        addHomophone('了', '辽', '疗', '料');
        addHomophone('在', '再', '载', '仔');
    }

    private static void addHomophone(char c, char... homophones) {
        Set<Character> set = new HashSet<>();
        set.add(c);
        for (char h : homophones) {
            set.add(h);
        }
        HOMOPHONE_MAP.put(c, set);
    }

    private static void initSimilarCharMap() {
        // 形近字（示例）
        addSimilar('天', '夫', '夭');
        addSimilar('大', '太', '犬', '丈');
        addSimilar('人', '入', '八');
        addSimilar('日', '曰', '目');
        addSimilar('目', '日', '自');
        addSimilar('土', '士');
        addSimilar('了', '子', '孑');
    }

    private static void addSimilar(char c, char... similar) {
        Set<Character> set = new HashSet<>();
        set.add(c);
        for (char s : similar) {
            set.add(s);
        }
        SIMILAR_CHAR_MAP.put(c, set);
    }

    private static void initSTMap() {
        // 常用简繁转换（示例）
        putST("爱国", "愛國");
        putST("中国", "中國");
        putST("人民", "人民");
        putST("和平", "和平");
        putST("统一", "統一");
        putST("发展", "發展");
        putST("社会", "社會");
        putST("经济", "經濟");
        putST("政治", "政治");
        putST("文化", "文化");
    }

    private static void putST(String simplified, String traditional) {
        for (int i = 0; i < simplified.length(); i++) {
            S2T_MAP.put(simplified.charAt(i), traditional.charAt(i));
            T2S_MAP.put(traditional.charAt(i), simplified.charAt(i));
        }
    }

    /**
     * 简转繁
     */
    public String toTraditional(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(S2T_MAP.getOrDefault(c, c));
        }
        return sb.toString();
    }

    /**
     * 繁转简
     */
    public String toSimplified(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(T2S_MAP.getOrDefault(c, c));
        }
        return sb.toString();
    }

    /**
     * 获取同音字集合
     */
    public Set<Character> getHomophones(char c) {
        return HOMOPHONE_MAP.getOrDefault(c, Collections.emptySet());
    }

    /**
     * 获取形近字集合
     */
    public Set<Character> getSimilarChars(char c) {
        return SIMILAR_CHAR_MAP.getOrDefault(c, Collections.emptySet());
    }

    /**
     * 过滤特殊符号干扰
     */
    public String filterSpecialChars(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.replaceAll("[\\s\\*#@~$^&()_+=\\[\\]{}|;:'\",.<>/?！￥…（）【】《》？：；‘’\"\"、]+", "");
    }

    /**
     * 生成同音字变体列表
     */
    public List<String> generateHomophoneVariants(String text) {
        List<String> variants = new ArrayList<>();
        generateVariantsInternal(text, 0, new StringBuilder(), variants, HOMOPHONE_MAP);
        return variants;
    }

    /**
     * 生成形近字变体列表
     */
    public List<String> generateSimilarVariants(String text) {
        List<String> variants = new ArrayList<>();
        generateVariantsInternal(text, 0, new StringBuilder(), variants, SIMILAR_CHAR_MAP);
        return variants;
    }

    private void generateVariantsInternal(String text, int index, StringBuilder current,
                                           List<String> variants, Map<Character, Set<Character>> charMap) {
        if (index >= text.length()) {
            variants.add(current.toString());
            return;
        }
        char c = text.charAt(index);
        Set<Character> alternatives = charMap.get(c);

        if (alternatives != null && !alternatives.isEmpty()) {
            for (char alt : alternatives) {
                current.append(alt);
                generateVariantsInternal(text, index + 1, current, variants, charMap);
                current.deleteCharAt(current.length() - 1);
            }
        } else {
            current.append(c);
            generateVariantsInternal(text, index + 1, current, variants, charMap);
            current.deleteCharAt(current.length() - 1);
        }
    }
}
