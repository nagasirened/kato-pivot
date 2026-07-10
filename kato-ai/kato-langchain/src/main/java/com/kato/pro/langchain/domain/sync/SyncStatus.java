package com.kato.pro.langchain.domain.sync;

/**
 * 同步运行状态。
 *
 *   RUNNING  — 正在执行（非终态）
 *   SUCCESS  — 全部成功（终态）
 *   PARTIAL  — 部分失败（终态）
 *   FAILED   — 整体失败（终态）
 */
public enum SyncStatus {

    RUNNING(false),
    SUCCESS(true),
    PARTIAL(true),
    FAILED(true);

    private final boolean terminal;

    SyncStatus(boolean terminal) {
        this.terminal = terminal;
    }

    /** 是否为终态（不再流转） */
    public boolean isTerminal() {
        return terminal;
    }
}
