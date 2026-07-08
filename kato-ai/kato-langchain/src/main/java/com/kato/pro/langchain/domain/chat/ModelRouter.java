package com.kato.pro.langchain.domain.chat;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * 模型路由器：按 TaskType 路由到对应 ChatModel。
 *
 * 设计原则：
 *   - 单一不可变 EnumMap（启动时构建，运行时不再变化）
 *   - 未注册的任务类型直接抛 BusinessException（fail-fast，防止漏配）
 *   - 调用前 log 任务类型 + model name（可观测性）
 *
 * 后续扩展：增加缓存、重试、metrics 等在调用层做，不进 Router。
 */
@Slf4j
public class ModelRouter {

    private final Map<TaskType, ChatLanguageModel> routingTable;

    public ModelRouter(Map<TaskType, ChatLanguageModel> routingTable) {
        Objects.requireNonNull(routingTable, "routingTable must not be null");
        // EnumMap 不允许空 map 拷贝；用空 EnumMap 兜底
        this.routingTable = new EnumMap<>(TaskType.class);
        this.routingTable.putAll(routingTable);
        if (this.routingTable.isEmpty()) {
            log.warn("ModelRouter initialized with EMPTY routing table — all task types will fail");
        } else {
            log.info("ModelRouter initialized: {}", this.routingTable);
        }
    }

    /**
     * 按任务类型路由并调用模型。
     * @throws BusinessException 未注册的任务类型
     */
    public Mono<String> route(TaskType taskType, String systemPrompt, String userMessage) {
        ChatLanguageModel model = routingTable.get(taskType);
        if (model == null) {
            return Mono.error(new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "No model registered for task type: " + taskType));
        }
        log.debug("Routing task={} → model={}", taskType, model.modelName());
        return model.chat(systemPrompt, userMessage);
    }

    /** 测试用：检查某个任务是否已注册（避免抛错）。 */
    public boolean supports(TaskType taskType) {
        return routingTable.containsKey(taskType);
    }

    /** 测试用：当前路由快照。 */
    public Map<TaskType, ChatLanguageModel> routingTable() {
        return Map.copyOf(routingTable);
    }
}
