package com.kato.pro.sensitive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kato.pro.base.entity.SuperMapper;
import com.kato.pro.sensitive.entity.SensitiveWord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感词Mapper
 */
@Mapper
public interface SensitiveWordMapper extends SuperMapper<SensitiveWord> {

}
