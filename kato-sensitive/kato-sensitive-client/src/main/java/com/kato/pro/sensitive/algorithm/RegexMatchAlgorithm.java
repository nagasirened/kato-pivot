package com.kato.pro.sensitive.algorithm;

import com.kato.pro.sensitive.entity.SensitiveWord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 正则匹配算法
 * 用于匹配正则类型的敏感词，如"微[信信]+"可匹配"微信""微信号"等
 */
@Slf4j
@Component
public class RegexMatchAlgorithm {

    /**
     * 编译后的正则模式缓存
     */
    private final List<Pattern> regexPatterns = new ArrayList<>();

    /**
     * 添加正则敏感词
     */
    public void addWord(String regex, String level) {
        try {
            Pattern pattern = Pattern.compile(regex);
            regexPatterns.add(pattern);
            log.debug("添加正则敏感词: {}, 等级: {}", regex, level);
        } catch (PatternSyntaxException e) {
            log.warn("无效的正则表达式: {}, 错误: {}", regex, e.getMessage());
        }
    }

    /**
     * 批量添加正则敏感词
     */
    public void addWords(List<SensitiveWord> words) {
        for (SensitiveWord word : words) {
            if ("REGEX".equalsIgnoreCase(word.getWordType())) {
                addWord(word.getWord(), word.getLevel());
            }
        }
    }

    /**
     * 检测文本中匹配到的所有正则敏感词
     */
    public List<RegexMatchResult> match(String text) {
        List<RegexMatchResult> results = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return results;
        }

        for (Pattern pattern : regexPatterns) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                results.add(new RegexMatchResult(
                        matcher.group(),
                        matcher.start(),
                        matcher.end()
                ));
            }
        }
        return results;
    }

    /**
     * 检查文本是否包含正则敏感词
     */
    public boolean contains(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (Pattern pattern : regexPatterns) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 清空所有正则模式
     */
    public void clear() {
        regexPatterns.clear();
    }

    /**
     * 正则匹配结果
     */
    public static class RegexMatchResult {
        private final String matchedText;
        private final int startIndex;
        private final int endIndex;

        public RegexMatchResult(String matchedText, int startIndex, int endIndex) {
            this.matchedText = matchedText;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        public String getMatchedText() {
            return matchedText;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }
    }
}
