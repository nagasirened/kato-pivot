package com.kato.pro.sensitive.trie;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Trie树节点
 */
@Getter
@Setter
public class TrieNode {

    /**
     * 是否为敏感词结尾
     */
    private boolean end;

    /**
     * 敏感词等级：URGENT/MEDIUM/NORMAL
     */
    private String level;

    /**
     * 子节点映射
     */
    private Map<Character, TrieNode> children = new HashMap<>();

    public TrieNode() {
        this.end = false;
        this.level = null;
    }

    /**
     * 添加子节点
     */
    public TrieNode addChild(Character c) {
        return children.computeIfAbsent(c, k -> new TrieNode());
    }

    /**
     * 获取子节点
     */
    public TrieNode getChild(Character c) {
        return children.get(c);
    }

    /**
     * 是否包含子节点
     */
    public boolean hasChild(Character c) {
        return children.containsKey(c);
    }
}
