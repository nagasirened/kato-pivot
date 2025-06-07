package com.kato.pro.common.utils;

import cn.hutool.core.text.CharSequenceUtil;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 状态流转机 使用
 */

@Getter
public class StateFlowActionConfig<T> {

    private final List<Transition<T>> transitions = new ArrayList<>();

    // 流转规则实体
    @Getter
    public static class Transition<T> {
        private final String source;
        private final String target;
        private final Action<T> action;

        public Transition(String source, String target, Action<T> action) {
            this.source = source;
            this.target = target;
            this.action = action;
        }
    }

    // 事件动作接口
    public interface Action<T> {
        void execute(T t);
    }

    public void addTransition(Transition<T> transition) {
        transitions.add(transition);
    }

    public static class TransitionBuilder<T> {
        private final StateFlowActionConfig<T> config;
        private String source;
        private String target;
        private Action<T> action;

        public TransitionBuilder(StateFlowActionConfig<T> config) {
            this.config = config;
        }

        public TransitionBuilder<T> source(String source) {
            this.source = source;
            return tryCommit();
        }

        public TransitionBuilder<T> target(String target) {
            this.target = target;
            return tryCommit();
        }

        public TransitionBuilder<T> action(Action<T> action) {
            this.action = action;
            return tryCommit();
        }

        public TransitionBuilder<T> tryCommit() {
            if (CharSequenceUtil.isAllNotBlank(source, target) && Objects.nonNull(action)) {
                commit();
            }
            return this;
        }

        private void commit() {
            if (!CharSequenceUtil.isAllNotBlank(source, target) || Objects.isNull(action)) {
                return;
            }
            config.addTransition(new Transition<T>(this.source, this.target, this.action));
            this.source = null;
            this.target = null;
            this.action = null;
        }

    }

}
