package com.kato.pro.sensitive.dfa;

import com.kato.pro.sensitive.entity.SensitiveWord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * DFA敏感词过滤器
 * 使用确定有限自动机思想，构建类似：
 * 我 -> 我们的 -> 我们的人民
 *       ↑
 *       └── 我们是
 *
 * 优势：
 * - 匹配复杂度 O(1)，与敏感词数量无关
 * - 内存占用优化，构建联合前缀链
 * - 支持最长匹配原则
 */
@Slf4j
@Component
public class SensitiveWordDFA {

    /**
     * DFA状态机根节点
     */
    private final Map<Character, DFAState> stateMap = new ConcurrentHashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 添加敏感词到DFA
     */
    public void addWord(String word, String level) {
        if (word == null || word.isEmpty()) {
            return;
        }
        lock.writeLock().lock();
        try {
            char[] chars = word.toCharArray();
            Map<Character, DFAState> currentMap = stateMap;

            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                DFAState state = currentMap.get(c);

                if (state == null) {
                    state = new DFAState();
                    currentMap.put(c, state);
                }

                // 如果是最后一个字符，标记为结束状态
                if (i == chars.length - 1) {
                    state.setEnd(true);
                    state.setLevel(level);
                    state.setWord(word);
                }

                currentMap = state.getNext();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 批量添加敏感词
     */
    public void addWords(List<SensitiveWord> words) {
        lock.writeLock().lock();
        try {
            for (SensitiveWord word : words) {
                addWord(word.getWord(), word.getLevel());
            }
            log.info("DFA敏感词树构建完成，共加载 {} 个敏感词", words.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 检测文本中所有匹配的敏感词
     */
    public List<DFAMatchResult> match(String text) {
        List<DFAMatchResult> results = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return results;
        }

        lock.readLock().lock();
        try {
            for (int i = 0; i < text.length(); i++) {
                int startIndex = i;
                DFAState currentState = stateMap.get(text.charAt(i));

                if (currentState != null) {
                    int j = i;
                    StringBuilder matchedWord = new StringBuilder();
                    matchedWord.append(text.charAt(i));

                    while (currentState != null && j + 1 < text.length()) {
                        currentState = currentState.getNext().get(text.charAt(++j));
                        if (currentState != null && currentState.isEnd()) {
                            matchedWord.append(text.charAt(j));
                            results.add(new DFAMatchResult(
                                    true,
                                    matchedWord.toString(),
                                    currentState.getLevel(),
                                    startIndex,
                                    j + 1
                            ));
                        } else if (currentState != null) {
                            matchedWord.append(text.charAt(j));
                        }
                    }
                }
            }

            // 去重（同一个位置可能匹配到多个长度的词，保留最长的）
            return deduplicateResults(results);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 检测文本是否包含敏感词
     */
    public boolean contains(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        lock.readLock().lock();
        try {
            for (int i = 0; i < text.length(); i++) {
                DFAState currentState = stateMap.get(text.charAt(i));
                if (currentState != null) {
                    int j = i;
                    while (currentState != null && j + 1 < text.length()) {
                        currentState = currentState.getNext().get(text.charAt(++j));
                        if (currentState != null && currentState.isEnd()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 脱敏文本
     */
    public String sanitize(String text, char maskChar) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        lock.readLock().lock();
        try {
            char[] chars = text.toCharArray();
            boolean[] markedForMask = new boolean[chars.length];

            for (int i = 0; i < chars.length; i++) {
                DFAState currentState = stateMap.get(chars[i]);
                if (currentState != null) {
                    int matchEnd = -1;
                    String matchLevel = null;

                    int j = i;
                    while (currentState != null && j < chars.length) {
                        if (currentState.isEnd()) {
                            matchEnd = j + 1;
                            matchLevel = currentState.getLevel();
                        }
                        j++;
                        if (j < chars.length) {
                            currentState = currentState.getNext().get(chars[j]);
                        } else {
                            break;
                        }
                    }

                    if (matchEnd > i) {
                        Arrays.fill(markedForMask, i, matchEnd, true);
                    }
                }
            }

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < chars.length; i++) {
                result.append(markedForMask[i] ? maskChar : chars[i]);
            }
            return result.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 清空DFA
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            stateMap.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 去重结果，保留最长匹配
     */
    private List<DFAMatchResult> deduplicateResults(List<DFAMatchResult> results) {
        if (results.isEmpty()) {
            return results;
        }

        // 按起始位置和长度排序
        Collections.sort(results, (a, b) -> {
            int startCompare = Integer.compare(a.getStartIndex(), b.getStartIndex());
            if (startCompare != 0) {
                return startCompare;
            }
            return Integer.compare(b.getEndIndex() - b.getStartIndex(), a.getEndIndex() - a.getStartIndex());
        });

        List<DFAMatchResult> deduplicated = new ArrayList<>();
        int lastEnd = -1;

        for (DFAMatchResult result : results) {
            if (result.getStartIndex() >= lastEnd) {
                deduplicated.add(result);
                lastEnd = result.getEndIndex();
            }
        }

        return deduplicated;
    }

    /**
     * DFA状态
     */
    private static class DFAState {
        private boolean end = false;
        private String level;
        private String word;
        private final Map<Character, DFAState> next = new ConcurrentHashMap<>();

        public boolean isEnd() {
            return end;
        }

        public void setEnd(boolean end) {
            this.end = end;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getWord() {
            return word;
        }

        public void setWord(String word) {
            this.word = word;
        }

        public Map<Character, DFAState> getNext() {
            return next;
        }
    }

    /**
     * DFA匹配结果
     */
    public static class DFAMatchResult {
        private final boolean matched;
        private final String word;
        private final String level;
        private final int startIndex;
        private final int endIndex;

        public DFAMatchResult(boolean matched, String word, String level, int startIndex, int endIndex) {
            this.matched = matched;
            this.word = word;
            this.level = level;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        public boolean isMatched() {
            return matched;
        }

        public String getWord() {
            return word;
        }

        public String getLevel() {
            return level;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }
    }
}
