package com.kato.pro.langchain.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kato.pro.langchain.domain.chat.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ChatMessage Mapper。继承 TenantAwareBaseMapper → M1 拦截器自动注入 tenant_id。
 */
@Mapper
public interface ChatMessageMapper extends TenantAwareBaseMapper<ChatMessage> {

    /**
     * 分页查询某会话的消息（按时间正序：旧→新）。
     * 内部 Wrapper 会被 M1 拦截器追加 tenant_id 条件。
     */
    default IPage<ChatMessage> selectSessionMessagePage(IPage<ChatMessage> page, Long sessionId) {
        return selectPage(page,
                new QueryWrapper<ChatMessage>()
                        .eq("session_id", sessionId)
                        .orderByAsc("created_at"));
    }

    /**
     * 查询某会话最新 N 条消息（用于 context window 加载）。
     * 取最后 N 条 → 按时间正序返回。
     */
    default List<ChatMessage> selectRecentSessionMessages(Long sessionId, int limit) {
        // 取按 created_at desc 的前 limit 条，再按时间正序排回去
        List<ChatMessage> desc = selectList(new QueryWrapper<ChatMessage>()
                .eq("session_id", sessionId)
                .orderByDesc("created_at")
                .last("LIMIT " + limit));
        return desc.stream()
                .sorted(Comparator.comparing(ChatMessage::getCreateTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }
}
