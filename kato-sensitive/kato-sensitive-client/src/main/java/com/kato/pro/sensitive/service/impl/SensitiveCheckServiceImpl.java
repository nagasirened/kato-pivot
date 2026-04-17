package com.kato.pro.sensitive.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kato.pro.sensitive.algorithm.RegexMatchAlgorithm;
import com.kato.pro.sensitive.dfa.SensitiveWordDFA;
import com.kato.pro.sensitive.dto.SensitiveCheckResult;
import com.kato.pro.sensitive.dto.SensitiveCheckResult.MatchItem;
import com.kato.pro.sensitive.service.ISensitiveCheckService;
import com.kato.pro.sensitive.service.SensitiveSanitizer;
import com.kato.pro.sensitive.trie.SensitiveWordTrie;
import com.kato.pro.sensitive.util.CharacterConverter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 敏感词检测服务实现
 */
@Slf4j
@Service
public class SensitiveCheckServiceImpl implements ISensitiveCheckService {

    /**
     * 无敏感词结果缓存（30秒）
     */
    private Cache<String, SensitiveCheckResult> cleanCache;

    /**
     * 有敏感词结果缓存（5分钟）
     */
    private Cache<String, SensitiveCheckResult> hitCache;

    @Resource
    private SensitiveWordTrie sensitiveWordTrie;

    @Resource
    private SensitiveWordDFA sensitiveWordDFA;

    @Resource
    private RegexMatchAlgorithm regexMatchAlgorithm;

    @Autowired
    private CharacterConverter characterConverter;

    @Resource
    private SensitiveSanitizer sensitiveSanitizer;

    @PostConstruct
    public void init() {
        cleanCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .recordStats()
                .build();

        hitCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Override
    @CircuitBreaker(name = "sensitiveCheck", fallbackMethod = "checkFallback")
    public SensitiveCheckResult check(String text) {
        return check(text, true);
    }

    @Override
    @CircuitBreaker(name = "sensitiveCheck", fallbackMethod = "checkFallback")
    public SensitiveCheckResult check(String text, boolean fuzzyMatch) {
        if (text == null || text.isEmpty()) {
            return buildEmptyResult();
        }

        // 先检查缓存
        String cacheKey = text.hashCode() + "_" + fuzzyMatch;
        SensitiveCheckResult cached = cleanCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        cached = hitCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<MatchItem> matches = new ArrayList<>();

        // DFA精确匹配
        List<SensitiveWordDFA.DFAMatchResult> dfaResults = sensitiveWordDFA.match(text);
        for (SensitiveWordDFA.DFAMatchResult result : dfaResults) {
            matches.add(MatchItem.builder()
                    .word(result.getWord())
                    .level(result.getLevel())
                    .matchType("EXACT")
                    .startIndex(result.getStartIndex())
                    .endIndex(result.getEndIndex())
                    .build());
        }

        // 正则匹配
        List<RegexMatchAlgorithm.RegexMatchResult> regexResults = regexMatchAlgorithm.match(text);
        for (RegexMatchAlgorithm.RegexMatchResult result : regexResults) {
            matches.add(MatchItem.builder()
                    .word(result.getMatchedText())
                    .level("MEDIUM")
                    .matchType("REGEX")
                    .startIndex(result.getStartIndex())
                    .endIndex(result.getEndIndex())
                    .build());
        }

        // 模糊匹配
        if (fuzzyMatch) {
            List<MatchItem> fuzzyMatches = checkFuzzyMatch(text);
            matches.addAll(fuzzyMatches);
        }

        SensitiveCheckResult result = buildResult(matches);

        // 放入缓存
        if (result.isHasSensitive()) {
            hitCache.put(cacheKey, result);
        } else {
            cleanCache.put(cacheKey, result);
        }

        return result;
    }

    /**
     * 检测服务降级方法
     */
    public SensitiveCheckResult checkFallback(String text, Exception e) {
        log.warn("敏感词检测服务降级，原因: {}", e.getMessage());
        return SensitiveCheckResult.builder()
                .hasSensitive(false)
                .matchCount(0)
                .matches(new ArrayList<>())
                .maxLevel("NONE")
                .fallback(true)
                .build();
    }

    @Override
    @CircuitBreaker(name = "sensitiveCheck", fallbackMethod = "checkByLevelFallback")
    public SensitiveCheckResult checkByLevel(String text, String minLevel) {
        SensitiveCheckResult result = check(text);
        if (!result.isHasSensitive()) {
            return result;
        }

        int minLevelValue = getLevelValue(minLevel);
        List<MatchItem> filteredMatches = result.getMatches().stream()
                .filter(m -> getLevelValue(m.getLevel()) >= minLevelValue)
                .collect(Collectors.toList());

        return buildResult(filteredMatches);
    }

    public SensitiveCheckResult checkByLevelFallback(String text, String minLevel, Exception e) {
        log.warn("敏感词等级检测服务降级，原因: {}", e.getMessage());
        return SensitiveCheckResult.builder()
                .hasSensitive(false)
                .matchCount(0)
                .matches(new ArrayList<>())
                .maxLevel("NONE")
                .fallback(true)
                .build();
    }

    @Override
    @CircuitBreaker(name = "sensitiveCheck", fallbackMethod = "checkFallback")
    public SensitiveCheckResult checkExact(String text) {
        if (text == null || text.isEmpty()) {
            return buildEmptyResult();
        }

        // 先检查缓存
        String cacheKey = text.hashCode() + "_exact";
        SensitiveCheckResult cached = cleanCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        cached = hitCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<MatchItem> matches = new ArrayList<>();

        // DFA精确匹配
        List<SensitiveWordDFA.DFAMatchResult> dfaResults = sensitiveWordDFA.match(text);
        for (SensitiveWordDFA.DFAMatchResult result : dfaResults) {
            matches.add(MatchItem.builder()
                    .word(result.getWord())
                    .level(result.getLevel())
                    .matchType("EXACT")
                    .startIndex(result.getStartIndex())
                    .endIndex(result.getEndIndex())
                    .build());
        }

        // 正则匹配
        List<RegexMatchAlgorithm.RegexMatchResult> regexResults = regexMatchAlgorithm.match(text);
        for (RegexMatchAlgorithm.RegexMatchResult result : regexResults) {
            matches.add(MatchItem.builder()
                    .word(result.getMatchedText())
                    .level("MEDIUM")
                    .matchType("REGEX")
                    .startIndex(result.getStartIndex())
                    .endIndex(result.getEndIndex())
                    .build());
        }

        SensitiveCheckResult result = buildResult(matches);

        // 放入缓存
        if (result.isHasSensitive()) {
            hitCache.put(cacheKey, result);
        } else {
            cleanCache.put(cacheKey, result);
        }

        return result;
    }

    @Override
    @CircuitBreaker(name = "sensitiveCheck", fallbackMethod = "checkFallback")
    public SensitiveCheckResult checkFuzzy(String text) {
        if (text == null || text.isEmpty()) {
            return buildEmptyResult();
        }

        List<MatchItem> matches = checkFuzzyMatch(text);
        return buildResult(matches);
    }

    private List<MatchItem> checkFuzzyMatch(String text) {
        List<MatchItem> matches = new ArrayList<>();

        // 过滤特殊符号
        String filteredText = characterConverter.filterSpecialChars(text);

        // 简繁转换后检测
        String traditionalText = characterConverter.toTraditional(filteredText);
        if (!traditionalText.equals(filteredText)) {
            List<SensitiveWordTrie.MatchResult> results = sensitiveWordTrie.matchContains(traditionalText);
            for (SensitiveWordTrie.MatchResult result : results) {
                matches.add(MatchItem.builder()
                        .word(result.word())
                        .level(result.level())
                        .matchType("FUZZY")
                        .startIndex(result.endIndex() - result.word().length())
                        .endIndex(result.endIndex())
                        .build());
            }
        }

        // 生成同音字变体检测
        List<String> homophoneVariants = characterConverter.generateHomophoneVariants(filteredText);
        for (String variant : homophoneVariants) {
            List<SensitiveWordTrie.MatchResult> results = sensitiveWordTrie.matchContains(variant);
            for (SensitiveWordTrie.MatchResult result : results) {
                if (result.matched()) {
                    matches.add(MatchItem.builder()
                            .word(result.word())
                            .level(result.level())
                            .matchType("FUZZY")
                            .startIndex(result.endIndex() - result.word().length())
                            .endIndex(result.endIndex())
                            .build());
                }
            }
        }

        // 生成形近字变体检测
        List<String> similarVariants = characterConverter.generateSimilarVariants(filteredText);
        for (String variant : similarVariants) {
            List<SensitiveWordTrie.MatchResult> results = sensitiveWordTrie.matchContains(variant);
            for (SensitiveWordTrie.MatchResult result : results) {
                if (result.matched()) {
                    matches.add(MatchItem.builder()
                            .word(result.word())
                            .level(result.level())
                            .matchType("FUZZY")
                            .startIndex(result.endIndex() - result.word().length())
                            .endIndex(result.endIndex())
                            .build());
                }
            }
        }

        return matches;
    }

    private SensitiveCheckResult buildResult(List<MatchItem> matches) {
        if (matches.isEmpty()) {
            return buildEmptyResult();
        }

        // 去重
        List<MatchItem> distinctMatches = matches.stream()
                .distinct()
                .collect(Collectors.toList());

        // 找出最高等级
        String maxLevel = distinctMatches.stream()
                .map(MatchItem::getLevel)
                .reduce((l1, l2) -> getLevelValue(l1) >= getLevelValue(l2) ? l1 : l2)
                .orElse("NONE");

        // 收集分类
        Set<String> categories = distinctMatches.stream()
                .map(MatchItem::getWord)
                .collect(Collectors.toSet());

        return SensitiveCheckResult.builder()
                .hasSensitive(true)
                .matchCount(distinctMatches.size())
                .matches(distinctMatches)
                .maxLevel(maxLevel)
                .categories(categories)
                .build();
    }

    private SensitiveCheckResult buildEmptyResult() {
        return SensitiveCheckResult.builder()
                .hasSensitive(false)
                .matchCount(0)
                .matches(new ArrayList<>())
                .maxLevel("NONE")
                .build();
    }

    private int getLevelValue(String level) {
        if (level == null) {
            return 0;
        }
        String upperLevel = level.toUpperCase();
        if ("URGENT".equals(upperLevel)) {
            return 3;
        } else if ("MEDIUM".equals(upperLevel)) {
            return 2;
        } else if ("NORMAL".equals(upperLevel)) {
            return 1;
        }
        return 0;
    }

    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        return "cleanCache: " + cleanCache.stats() + ", hitCache: " + hitCache.stats();
    }

    /**
     * 清空所有缓存
     */
    public void clearCache() {
        cleanCache.invalidateAll();
        hitCache.invalidateAll();
    }
}
