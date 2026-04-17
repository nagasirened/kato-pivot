package com.kato.pro.sensitive.service;

/**
 * 敏感词加载服务接口
 */
public interface SensitiveWordLoadService {

    /**
     * 初始化加载敏感词到Trie树
     */
    void loadSensitiveWords();

    /**
     * 热更新敏感词
     */
    void hotReloadSensitiveWords();

    /**
     * 获取当前敏感词版本
     */
    long getVersion();
}
