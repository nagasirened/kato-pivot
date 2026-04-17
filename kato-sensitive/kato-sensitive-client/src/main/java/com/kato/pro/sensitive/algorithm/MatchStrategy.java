package com.kato.pro.sensitive.algorithm;

import com.kato.pro.sensitive.entity.SensitiveWord;
import com.kato.pro.sensitive.trie.SensitiveWordTrie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 匹配策略工厂
 * 根据敏感词类型选择对应的匹配算法
 */
@Slf4j
@Component
public class MatchStrategy {

    @Resource
    private SensitiveWordTrie sensitiveWordTrie;

    @Resource
    private RegexMatchAlgorithm regexMatchAlgorithm;

    /**
     * 敏感词分类存储：wordType -> 敏感词列表
     */
    private final Map<String, List<SensitiveWord>> wordsByType = new ConcurrentHashMap<>();

    /**
     * 初始化，从Trie树获取所有敏感词并按类型分组
     */
    @PostConstruct
    public void init() {
        // Trie树加载完成后，分类敏感词
        log.info("MatchStrategy 初始化完成");
    }

    /**
     * 按类型添加敏感词
     */
    public void addWord(SensitiveWord word) {
        String type = word.getWordType() != null ? word.getWordType() : "EXACT";
        wordsByType.computeIfAbsent(type, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(word);
    }

    /**
     * 按类型批量添加敏感词
     */
    public void addWords(List<SensitiveWord> words) {
        Map<String, List<SensitiveWord>> byType = words.stream()
                .collect(Collectors.groupingBy(
                        w -> w.getWordType() != null ? w.getWordType() : "EXACT",
                        ConcurrentHashMap::new,
                        Collectors.toList()
                ));
        wordsByType.putAll(byType);

        // 同时更新各个算法的数据
        List<SensitiveWord> exactWords = wordsByType.getOrDefault("EXACT", List.of());
        List<SensitiveWord> regexWords = wordsByType.getOrDefault("REGEX", List.of());

        // 重新构建Trie树（精确词）
        sensitiveWordTrie.clear();
        sensitiveWordTrie.addWords(exactWords);

        // 重新编译正则模式
        regexMatchAlgorithm.clear();
        regexMatchAlgorithm.addWords(regexWords);

        log.info("敏感词策略更新完成: EXACT={}, REGEX={}", exactWords.size(), regexWords.size());
    }

    /**
     * 获取精确匹配算法
     */
    public SensitiveWordTrie getExactAlgorithm() {
        return sensitiveWordTrie;
    }

    /**
     * 获取正则匹配算法
     */
    public RegexMatchAlgorithm getRegexAlgorithm() {
        return regexMatchAlgorithm;
    }

    /**
     * 获取所有敏感词（按类型分组）
     */
    public Map<String, List<SensitiveWord>> getWordsByType() {
        return wordsByType;
    }
}
