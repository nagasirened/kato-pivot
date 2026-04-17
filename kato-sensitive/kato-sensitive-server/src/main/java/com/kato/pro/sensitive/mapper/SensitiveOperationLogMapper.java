package com.kato.pro.sensitive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kato.pro.base.entity.SuperMapper;
import com.kato.pro.sensitive.entity.SensitiveOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感词操作日志Mapper
 */
@Mapper
public interface SensitiveOperationLogMapper extends SuperMapper<SensitiveOperationLog> {

}
