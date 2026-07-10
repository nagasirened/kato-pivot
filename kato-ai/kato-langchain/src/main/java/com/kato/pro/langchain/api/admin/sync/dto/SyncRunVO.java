package com.kato.pro.langchain.api.admin.sync.dto;

import com.kato.pro.langchain.domain.sync.SyncRunRecord;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 同步运行记录 VO（GET /api/v1/admin/sync/runs）。
 */
@Value
@Builder
public class SyncRunVO {
    Long id;
    String adapterName;
    LocalDateTime startTime;
    LocalDateTime endTime;
    Integer successCount;
    Integer failCount;
    String status;
    String errorMsg;

    public static SyncRunVO from(SyncRunRecord r) {
        if (r == null) return null;
        return SyncRunVO.builder()
                .id(r.getId())
                .adapterName(r.getAdapterName())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .successCount(r.getSuccessCount())
                .failCount(r.getFailCount())
                .status(r.getStatus())
                .errorMsg(r.getErrorMsg())
                .build();
    }
}
