package com.kato.pro.langchain.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.exception.SystemException;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.common.tenant.TenantInfo;
import com.kato.pro.langchain.domain.embedding.EmbeddingRequest;
import com.kato.pro.langchain.domain.embedding.EmbeddingResponse;
import com.kato.pro.langchain.domain.knowledge.IndexedChunk;
import com.kato.pro.langchain.domain.knowledge.RagPipeline;
import com.kato.pro.langchain.domain.knowledge.ScoredChunk;
import com.kato.pro.langchain.domain.rag.InMemoryVectorStore;
import com.kato.pro.langchain.domain.rag.NoOpReranker;
import com.kato.pro.langchain.domain.rag.QueryRewriter;
import com.kato.pro.langchain.domain.rag.Reranker;
import com.kato.pro.langchain.domain.rag.Retriever;
import com.kato.pro.langchain.domain.rag.VectorStore;
import com.kato.pro.langchain.config.RagProperties;
import com.kato.pro.langchain.infrastructure.external.minimax.InMemoryFakeEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * RAG EvalRunner — 跑 Golden Set 并产出报告。
 *
 * 设计要点：
 *   - 手动 wire 流水线（不依赖 Spring 容器）→ sandbox JDK21 友好
 *   - 用 InMemoryFakeEmbeddingModel + InMemoryVectorStore
 *   - 灌入 RagEvalSeed 预设 200 条 IndexedChunk
 *   - 每个 case 调 pipeline.search → EvalMetrics
 *   - 写 JSON 报告到 target/rag-eval-report.json
 *   - 返回 EvalReport 供 JUnit gate 使用
 *
 * CLI 用法：
 *   mvn -pl kato-ai/kato-langchain exec:java -Dexec.mainClass=com.kato.pro.langchain.eval.EvalRunner
 *   mvn -pl kato-ai/kato-langchain exec:java -Dexec.mainClass=com.kato.pro.langchain.eval.EvalRunner -Dexec.args="--file=path/to/golden.json"
 */
@Slf4j
public class EvalRunner {

    public static final Path DEFAULT_REPORT_PATH = Paths.get("target", "rag-eval-report.json");
    public static final long DEFAULT_TENANT_ID = 1L;
    public static final int DEFAULT_TOP_K = 5;
    public static final double DEFAULT_MIN_SCORE = 0.0;

    private final long tenantId;
    private final int topK;
    private final double minScore;

    public EvalRunner() {
        this(DEFAULT_TENANT_ID, DEFAULT_TOP_K, DEFAULT_MIN_SCORE);
    }

    public EvalRunner(long tenantId, int topK, double minScore) {
        this.tenantId = tenantId;
        this.topK = topK;
        this.minScore = minScore;
    }

    /**
     * 跑 golden set 并返回报告（不写文件）。
     */
    public EvalReport run(List<GoldenCase> goldenSet) {
        if (goldenSet == null || goldenSet.isEmpty()) {
            throw new SystemException(ErrorCode.PARAM_INVALID, "Golden set is empty");
        }

        // 1) 构造隔离的 in-memory 流水线
        InMemoryFakeEmbeddingModel embedding = new InMemoryFakeEmbeddingModel(512);
        InMemoryVectorStore vectorStore = new InMemoryVectorStore();
        RagEvalSeed.seed(vectorStore, embedding, tenantId);

        // 2) 装配 pipeline（手动 wire）
        Retriever retriever = new Retriever(embedding, vectorStore, defaultRagProperties());
        Reranker reranker = new NoOpReranker();
        QueryRewriter queryRewriter = noopQueryRewriter();
        RagPipeline pipeline = new RagPipeline(queryRewriter, retriever, reranker, defaultRagProperties());

        // 3) 跑 case
        List<EvalMetrics> perCase = new ArrayList<>(goldenSet.size());
        TenantContext.set(new TenantInfo(tenantId, 0L, "rag-eval"));
        try {
            for (GoldenCase golden : goldenSet) {
                long start = System.nanoTime();
                List<ScoredChunk> hits = pipeline.search(golden.getQuery(), topK, minScore, null, null);
                long latencyMs = (System.nanoTime() - start) / 1_000_000L;
                perCase.add(EvalMetrics.compute(golden, hits, latencyMs));
            }
        } finally {
            TenantContext.clear();
        }

        EvalMetrics.EvalAggregate aggregate = EvalMetrics.aggregate(perCase);
        log.info("RAG eval done: cases={} meanRecall={} meanMRR={} meanHit={} meanLatency={}ms",
                aggregate.getTotal(),
                String.format("%.3f", aggregate.getMeanRecallAtK()),
                String.format("%.3f", aggregate.getMeanMrr()),
                String.format("%.3f", aggregate.getMeanHitRate()),
                String.format("%.1f", aggregate.getMeanLatencyMs()));

        return new EvalReport(perCase, aggregate);
    }

    /**
     * 跑并写报告到文件。返回 report 对象。
     */
    public EvalReport runAndWrite(List<GoldenCase> goldenSet, Path reportPath) {
        EvalReport report = run(goldenSet);
        writeReport(report, reportPath);
        return report;
    }

    private static void writeReport(EvalReport report, Path reportPath) {
        try {
            Path parent = reportPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(reportPath.toFile(), report);
            log.info("Eval report written to {}", reportPath.toAbsolutePath());
        } catch (IOException e) {
            throw new SystemException(ErrorCode.INTERNAL_ERROR,
                    "Failed to write eval report to: " + reportPath, e);
        }
    }

    private static RagProperties defaultRagProperties() {
        RagProperties props = new RagProperties();
        // 关闭 query rewrite / rerank，让 EvalRunner 完全 in-memory 可控
        props.getQueryRewriter().setEnabled(false);
        props.getReranker().setEnabled(false);
        props.getRetriever().setTopK(DEFAULT_TOP_K);
        props.getRetriever().setMinScore(DEFAULT_MIN_SCORE);
        return props;
    }

    private static QueryRewriter noopQueryRewriter() {
        return new QueryRewriter() {
            @Override
            public String rewrite(String query) {
                return query == null ? "" : query;
            }
        };
    }

    /**
     * CLI 入口：
     *   --file=PATH      指定 golden set 路径（不传则用 classpath 默认）
     *   --out=PATH       报告输出路径（默认 target/rag-eval-report.json）
     *   --topk=N         topK（默认 5）
     */
    public static void main(String[] args) {
        Path file = null;
        Path out = DEFAULT_REPORT_PATH;
        int topK = DEFAULT_TOP_K;
        for (String arg : args) {
            if (arg.startsWith("--file=")) {
                file = Paths.get(arg.substring("--file=".length()));
            } else if (arg.startsWith("--out=")) {
                out = Paths.get(arg.substring("--out=".length()));
            } else if (arg.startsWith("--topk=")) {
                topK = Integer.parseInt(arg.substring("--topk=".length()));
            }
        }

        List<GoldenCase> golden = file == null
                ? GoldenSetLoader.loadDefault()
                : GoldenSetLoader.loadFromFile(file);

        EvalRunner runner = new EvalRunner(DEFAULT_TENANT_ID, topK, DEFAULT_MIN_SCORE);
        EvalReport report = runner.runAndWrite(golden, out);
        EvalMetrics.EvalAggregate agg = report.getAggregate();
        System.out.printf("RAG eval finished. cases=%d recall@K=%.3f mrr=%.3f hitRate=%.3f latency=%.1fms%n",
                agg.getTotal(), agg.getMeanRecallAtK(), agg.getMeanMrr(),
                agg.getMeanHitRate(), agg.getMeanLatencyMs());
    }

    /**
     * 报告对象（per-case + aggregate）。
     */
    @lombok.Value
    public static class EvalReport {
        List<EvalMetrics> perCase;
        EvalMetrics.EvalAggregate aggregate;
    }
}
