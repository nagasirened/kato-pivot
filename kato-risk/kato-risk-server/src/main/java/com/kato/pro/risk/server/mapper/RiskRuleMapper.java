package com.kato.pro.risk.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kato.pro.risk.server.entity.RiskRule;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 风控规则 Mapper。
 */
public interface RiskRuleMapper extends BaseMapper<RiskRule> {

    @Select("SELECT * FROM risk_rule WHERE scene = #{scene} AND enabled = 1 ORDER BY priority ASC")
    List<RiskRule> findActiveRulesByScene(@Param("scene") String scene);

    @Select("SELECT * FROM risk_rule WHERE id = #{id}")
    RiskRule findById(@Param("id") Long id);
}