package com.kato.pro.langchain.domain.memory;

import com.kato.pro.langchain.domain.chat.ChatMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 滑动窗口策略（纯方法）。
 *
 * 输入：按时间正序的消息列表 + token 估算器 + 上限 token。
 * 输出：保留下来的消息子集（仍按时间正序）。
 *
 * 规则：
 *   - 从最新一条往前累加 token；累计超过 maxTokens 的部分丢弃。
 *   - 至少保留最后 1 条（哪怕超限）。
 */
public class MemoryWindow {

    /**
     * @param orderedAsc messages 按 created_at 升序（旧→新）
     * @param estimator  token 估算器
     * @param maxTokens  上限（已扣除 reserve-ratio 后）
     * @return 保留的消息（升序）
     */
    public List<ChatMessage> trim(List<ChatMessage> orderedAsc, TokenCountEstimator estimator, int maxTokens) {
        if (orderedAsc == null || orderedAsc.isEmpty()) return List.of();
        if (maxTokens <= 0) {
            // 退化：只保留最后 1 条
            return List.of(orderedAsc.get(orderedAsc.size() - 1));
        }
        int total = 0;
        List<ChatMessage> kept = new ArrayList<>();
        // 从尾到头遍历
        for (int i = orderedAsc.size() - 1; i >= 0; i--) {
            ChatMessage m = orderedAsc.get(i);
            int t = estimator.estimate(m.getContent());
            if (total + t > maxTokens && !kept.isEmpty()) {
                break; // 已经至少保留 1 条
            }
            kept.add(m);
            total += t;
        }
        // 逆序恢复升序
        Collections.reverse(kept);
        return kept;
    }
}
