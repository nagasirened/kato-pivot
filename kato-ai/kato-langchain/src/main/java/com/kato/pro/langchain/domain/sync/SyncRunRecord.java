package com.kato.pro.langchain.domain.sync;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kato.pro.langchain.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 单次同步运行记录（D7）。
 *
 *   - adapterName : 哪个适配器
 *   - startTime / endTime : 起止时间
 *   - successCount / failCount : 成功/失败 record 数
 *   - status : 见 SyncStatus
 *   - errorMsg : 整体错误信息（adapter 抛异常时）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sync_run_record")
public class SyncRunRecord extends BaseEntity {

    private String adapterName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer successCount;
    private Integer failCount;
    private String status;
    private String errorMsg;
}
