package com.kato.pro.sensitive.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import com.kato.pro.sensitive.dto.SensitiveWordDTO;
import com.kato.pro.sensitive.entity.SensitiveWord;
import com.kato.pro.sensitive.entity.constant.SensitiveCategory;
import com.kato.pro.sensitive.entity.constant.SensitiveLevel;
import com.kato.pro.sensitive.entity.constant.SensitiveStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 敏感词Excel导入监听器
 */
@Slf4j
public class SensitiveWordImportListener implements ReadListener<SensitiveWordDTO> {

    private static final int BATCH_COUNT = 100;

    private final List<SensitiveWord> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

    private final ImportCallback callback;

    public SensitiveWordImportListener(ImportCallback callback) {
        this.callback = callback;
    }

    @Override
    public void invoke(SensitiveWordDTO dto, AnalysisContext context) {
        SensitiveWord word = convertToEntity(dto);
        cachedDataList.add(word);
        if (cachedDataList.size() >= BATCH_COUNT) {
            saveData();
            cachedDataList.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        if (!cachedDataList.isEmpty()) {
            saveData();
        }
    }

    private void saveData() {
        if (callback != null) {
            callback.saveBatch(cachedDataList);
            log.info("批量保存敏感词，数量={}", cachedDataList.size());
        }
    }

    private SensitiveWord convertToEntity(SensitiveWordDTO dto) {
        SensitiveWord word = new SensitiveWord();
        word.setWord(dto.getWord());

        // 转换等级
        SensitiveLevel level = SensitiveLevel.fromCode(dto.getLevel());
        word.setLevel(level != null ? level.getCode() : SensitiveLevel.NORMAL.getCode());

        // 转换分类
        SensitiveCategory category = SensitiveCategory.fromCode(dto.getCategory());
        word.setCategory(category.getCode());

        // 转换状态，默认为启用
        SensitiveStatus status = SensitiveStatus.fromCode(dto.getStatus());
        word.setStatus(status != null ? status.getCode() : SensitiveStatus.ENABLE.getCode());

        word.setRemark(dto.getRemark());
        return word;
    }

    public interface ImportCallback {
        void saveBatch(List<SensitiveWord> words);
    }
}
