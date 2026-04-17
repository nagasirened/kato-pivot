package com.kato.pro.sensitive.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kato.pro.sensitive.entity.SensitiveWord;

import java.util.List;

/**
 * 敏感词服务接口
 */
public interface ISensitiveWordService {

    /**
     * 分页查询敏感词
     */
    IPage<SensitiveWord> pageSensitiveWords(IPage<SensitiveWord> page, SensitiveWord sensitiveWord);

    /**
     * 根据ID查询敏感词
     */
    SensitiveWord getById(Long id);

    /**
     * 新增敏感词
     */
    boolean save(SensitiveWord sensitiveWord);

    /**
     * 更新敏感词
     */
    boolean updateById(SensitiveWord sensitiveWord);

    /**
     * 删除敏感词
     */
    boolean removeById(Long id);

    /**
     * 发送Kafka通知
     */
    void sendKafkaNotice();

    /**
     * 批量保存敏感词
     */
    boolean batchSave(List<SensitiveWord> sensitiveWords);

    /**
     * 条件查询敏感词列表
     */
    List<SensitiveWord> listWords(SensitiveWord sensitiveWord);
}
