package com.kato.pro.rec.service.rank;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.RandomUtil;
import com.kato.pro.base.util.SpringUtils;
import com.kato.pro.rec.entity.constant.RecommendConstant;
import com.kato.pro.rec.entity.core.RecommendItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 精排服务（Rank）：将召回候选商品列表通过外部模型服务进行分数预测，返回按模型分数重新排序后的商品列表。
 *
 * <p>数据流向：
 * <ol>
 *   <li>将 {@link RecommendItem} 列表构建为模型输入数据（Map&lt;String, Object&gt;）</li>
 *   <li>调用外部 TF Serving 接口（支持 Mock 降级）</li>
 *   <li>解析模型返回的分数向量，回填商品分数并重排序</li>
 * </ol>
 *
 * <p>外部接口地址通过 {@link RecommendConstant#PROP_RANK_TARGET_URL} 配置；
 * 若未配置或调用失败，服务降级为直接返回原列表（不阻塞推荐流程）。
 */
@Slf4j
@Service
public class RankService {

    /**
     * 对候选商品列表进行精排（模型打分并重排序）。
     *
     * @param items 待排序的候选商品列表（通常来自召回阶段）
     * @return 按模型分数降序排列后的商品列表；若调用失败或配置为空，返回原列表（降级透传）
     */
    public List<RecommendItem> rank(List<RecommendItem> items) {
        if (CollUtil.isEmpty(items)) {
            return new ArrayList<>();
        }
        String targetUrl = SpringUtils.getProperty(RecommendConstant.PROP_RANK_TARGET_URL, "");
        if (CharSequenceUtil.isBlank(targetUrl)) {
            log.debug("RankService#rank, rank target url not configured, skip rank");
            return items;
        }

        try {
            // 1. 将商品列表构建为模型输入 Map
            Map<String, Object> rankInput = buildRankInput(items);
            log.debug("RankService#rank, input built, itemCount={}, target={}", items.size(), targetUrl);

            // 2. 发送请求到 TF Serving（Mock 降级），获取模型分数
            List<Double> modelScores = doRankPredict(targetUrl, rankInput, items.size());

            // 3. 回填 rankScore 并按分数降序重排序
            return parseAndResort(items, modelScores);
        } catch (Exception e) {
            // 外部调用失败时降级为透传，不阻塞推荐主流程
            log.warn("RankService#rank, rank fail, fallback to original, msg={}", e.getMessage(), e);
            return items;
        }
    }

    /**
     * 构建精排模型的输入数据（与 TF Serving PredictRequest 的 inputs 字段对应）。
     * <p>输入字段：
     * <ul>
     *   <li>item_ids：商品 ID 列表（int64），与 items 顺序对应</li>
     *   <li>retrieval_scores：召回阶段分数（float），与 items 顺序对应</li>
     *   <li>labels：商品标签集合打平后的列表（int64）</li>
     *   <li>labels_count：每个商品对应的 label 数量（int64），用于变长解析</li>
     * </ul>
     *
     * @param items 商品列表
     * @return 模型输入 Map，key 为 tensor 名称，value 为对应数据
     */
    Map<String, Object> buildRankInput(List<RecommendItem> items) {
        // item_ids
        List<Long> itemIds = items.stream()
                .map(it -> it.getItemId() == null ? 0L : it.getItemId().longValue())
                .collect(Collectors.toList());

        // retrieval_scores
        List<Double> retrievalScores = items.stream()
                .map(it -> it.getScore() == null ? 0.0D : it.getScore())
                .collect(Collectors.toList());

        // labels（打平）及其计数
        List<Long> allLabels = new ArrayList<>();
        List<Long> labelCounts = new ArrayList<>();
        for (RecommendItem it : items) {
            if (it.getLabels() != null && !it.getLabels().isEmpty()) {
                List<Integer> sorted = it.getLabels().stream().sorted().collect(Collectors.toList());
                allLabels.addAll(sorted.stream().map(i -> i.longValue()).collect(Collectors.toList()));
                labelCounts.add((long) sorted.size());
            } else {
                labelCounts.add(0L);
            }
        }

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("item_ids", itemIds);
        inputs.put("retrieval_scores", retrievalScores);
        inputs.put("labels", allLabels);
        inputs.put("labels_count", labelCounts);
        return inputs;
    }

    /**
     * 调用外部 TensorFlow Serving 预测接口，获取每个商品的模型分数。
     *
     * <p>当前为 Mock 实现：在原始 retrieval_scores 基础上附加随机扰动，模拟模型排序效果。
     * <p>正式接入时：
     * <ol>
     *   <li>将 inputs 构建为 {@code tensorflow.serving.PredictRequest}（含 ModelSpec 和 TensorProto）</li>
     *   <li>通过 gRPC 调用 TF Serving（如 {@code PredictionServiceGrpc.PredictionServiceBlockingStub}）</li>
     *   <li>解析返回的 {@code PredictResponse.outputs["output_scores"]}，提取 float 数组</li>
     * </ol>
     *
     * @param targetUrl  TF Serving 地址（如 "localhost:8500"）
     * @param inputs     模型输入数据（来自 {@link #buildRankInput}）
     * @param itemCount  商品数量，用于校验返回分数长度
     * @return 模型预测分数列表，顺序与输入 items 一致
     */
    List<Double> doRankPredict(String targetUrl, Map<String, Object> inputs, int itemCount) {
        // ================================================================
        // TODO: 替换为真实的 TF Serving gRPC 调用
        //
        // 伪代码示例（需要添加 tensorflow-core-api 依赖）：
        //
        // ManagedChannel channel = ManagedChannelBuilder.forTarget(targetUrl)
        //         .usePlaintext()
        //         .build();
        // PredictionServiceGrpc.PredictionServiceBlockingStub stub =
        //         PredictionServiceGrpc.newBlockingStub(channel);
        //
        // // 构建 PredictRequest
        // TensorProto itemIdsTensor = buildTensorProto(inputs.get("item_ids"), DataType.DT_INT64);
        // TensorProto scoresTensor = buildTensorProto(inputs.get("retrieval_scores"), DataType.DT_FLOAT);
        // TensorProto labelsTensor = buildTensorProto(inputs.get("labels"), DataType.DT_INT64);
        // TensorProto labelCountsTensor = buildTensorProto(inputs.get("labels_count"), DataType.DT_INT64);
        //
        // PredictRequest request = PredictRequest.newBuilder()
        //         .setModelSpec(ModelSpec.newBuilder()
        //                 .setName(SpringUtils.getProperty(PROP_RANK_MODEL_NAME, "recommend_rank_model"))
        //                 .setSignatureName(SpringUtils.getProperty(PROP_RANK_SIGNATURE_NAME, "serving_default"))
        //                 .build())
        //         .putInputs("item_ids", itemIdsTensor)
        //         .putInputs("retrieval_scores", scoresTensor)
        //         .putInputs("labels", labelsTensor)
        //         .putInputs("labels_count", labelCountsTensor)
        //         .build();
        //
        // PredictResponse response = stub.predict(request);
        // List<Float> scores = response.getOutputsOrThrow("output_scores").getFloatValList();
        // channel.shutdown();
        // return scores;
        // ================================================================

        // Mock：读取原始 retrieval_scores，附加随机扰动模拟模型排序
        @SuppressWarnings("unchecked")
        List<Double> retrievalScores = (List<Double>) inputs.get("retrieval_scores");
        List<Double> mockScores = new ArrayList<>(itemCount);
        for (Double s : retrievalScores) {
            // 在原始分数上加 [-0.1, 0.1] 的随机扰动，模拟模型打分差异
            double perturbed = s + (RandomUtil.randomDouble() - 0.5) * 0.2;
            mockScores.add(perturbed);
        }

        log.debug("RankService#doRankPredict, mock scores generated, itemCount={}", itemCount);
        return mockScores;
    }

    /**
     * 解析模型分数，回填到商品列表并按分数降序重排。
     *
     * @param items        商品列表（顺序与 modelScores 对应）
     * @param modelScores  模型预测分数列表
     * @return 按模型分数降序排列后的商品列表
     */
    List<RecommendItem> parseAndResort(List<RecommendItem> items, List<Double> modelScores) {
        if (modelScores.size() != items.size()) {
            log.warn("RankService#parseAndResort, score size mismatch, items={}, scores={}",
                    items.size(), modelScores.size());
            return items;
        }

        // 回填 rankScore（模型预测分）
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setRankScore(modelScores.get(i));
        }

        // 按 rankScore 降序重排序，rankScore 为 null 时视为 0D
        items.sort(Comparator
                .comparing((RecommendItem i) -> i.getRankScore() == null ? 0D : i.getRankScore())
                .reversed()
                .thenComparing(i -> i.getItemId() == null ? Integer.MIN_VALUE : i.getItemId()));
        return items;
    }
}
