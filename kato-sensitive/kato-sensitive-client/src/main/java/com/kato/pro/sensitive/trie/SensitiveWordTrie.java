package com.kato.pro.sensitive.trie;

import com.kato.pro.sensitive.entity.SensitiveWord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 敏感词Trie树
 * 支持精确匹配和模糊匹配
 */
@Slf4j
@Component
public class SensitiveWordTrie {

    private final TrieNode root = new TrieNode();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 添加敏感词
     */
    public void addWord(String word, String level) {
        if (word == null || word.isEmpty()) {
            return;
        }
        lock.writeLock().lock();
        try {
            TrieNode current = root;
            for (char c : word.toCharArray()) {
                current = current.addChild(c);
            }
            current.setEnd(true);
            current.setLevel(level);
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
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 精确匹配检测
     */
    public MatchResult matchExact(String text) {
        if (text == null || text.isEmpty()) {
            return MatchResult.none();
        }
        lock.readLock().lock();
        try {
            TrieNode current = root;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                TrieNode node = current.getChild(c);
                if (node == null) {
                    // 重新从根节点开始
                    current = root;
                    if (root.hasChild(c)) {
                        current = root.getChild(c);
                    }
                } else {
                    current = node;
                }

                if (current.isEnd()) {
                    return new MatchResult(true, text.substring(0, i + 1), i + 1, current.getLevel());
                }
            }
            return MatchResult.none();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 包含检测（返回所有匹配结果）
     */
    public List<MatchResult> matchContains(String text) {
        List<MatchResult> results = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return results;
        }
        lock.readLock().lock();
        try {
            TrieNode current = root;
            int startIndex = 0;

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                TrieNode node = current.getChild(c);

                if (node == null) {
                    // 匹配失败，重新从根节点开始
                    current = root;
                    startIndex = i + 1;
                    if (root.hasChild(c)) {
                        current = root.getChild(c);
                    }
                } else {
                    current = node;
                    if (current.isEnd()) {
                        results.add(new MatchResult(true, text.substring(startIndex, i + 1), i + 1, current.getLevel()));
                        // 继续匹配更长的词
                    }
                }
            }
            return results;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 清空Trie树
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            root.getChildren().clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 匹配结果
     */
    public static class MatchResult {
        private final boolean matched;
        private final String word;
        private final int endIndex;
        private final String level;

        public MatchResult(boolean matched, String word, int endIndex, String level) {
            this.matched = matched;
            this.word = word;
            this.endIndex = endIndex;
            this.level = level;
        }

        public boolean matched() {
            return matched;
        }

        public String word() {
            return word;
        }

        public int endIndex() {
            return endIndex;
        }

        public String level() {
            return level;
        }

        public static MatchResult none() {
            return new MatchResult(false, null, -1, null);
        }
    }
}
