package com.kato.pro.rec.entity.recall;

import com.google.common.collect.Lists;
import com.jd.platform.async.callback.ICallback;
import com.jd.platform.async.callback.IWorker;
import com.jd.platform.async.worker.WorkResult;
import com.jd.platform.async.wrapper.WorkerWrapper;
import com.kato.pro.common.utils.JsonUtils;
import com.kato.pro.rec.entity.core.RecallBox;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.service.retrieval.RetrieveStrategy;
import com.kato.pro.rec.service.retrieval.RetrieveStrategyHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class RecallWorker implements IWorker<RecallBox, List<RecommendItem>>, ICallback<RecallBox, List<RecommendItem>> {

    private long startTime;

    /**
     * 调用具体的函数拉取数据
     */
    @Override
    public List<RecommendItem> action(RecallBox box, Map<String, WorkerWrapper> map) {
        String rsName = box.getRsInfo().getRsName();
        RetrieveStrategy retrieveStrategy = RetrieveStrategyHelper.retrieveBeanMap.get(rsName);
        if (Objects.isNull(retrieveStrategy)) {
            log.error("不存在的召回源 {}，请检查配置", rsName);
            return Lists.newLinkedList();
        }
        return retrieveStrategy.recall(box);
    }

    // IWorker函数，超时、异常时返回的默认值
    @Override
    public List<RecommendItem> defaultValue() {
        return new LinkedList<>();
    }


    @Override
    public void begin() {
        startTime = System.currentTimeMillis();
    }

    /**
     * 耗时操作执行完毕后，就给value注入值
     */
    @Override
    public void result(boolean success, RecallBox box, WorkResult<List<RecommendItem>> workResult) {
        String rsName = box.getRsInfo().getRsName();
        log.info("召回任务{}完成，耗时：{}ms", rsName, System.currentTimeMillis() - startTime);
        if (success) {
            log.warn("召回任务{}成功，召回数量{}条", rsName, workResult.getResult().size());
        } else {
            log.warn("召回任务{}失败，错误信息：{}", rsName, JsonUtils.toJSONString(workResult.getEx()));
        }
    }

}
