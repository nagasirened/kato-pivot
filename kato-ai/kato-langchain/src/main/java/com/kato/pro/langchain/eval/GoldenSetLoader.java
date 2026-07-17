package com.kato.pro.langchain.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.exception.SystemException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Golden Set 加载器。
 *
 * 数据源优先级：
 *   1) 显式传入的 Path（CLI -f 使用）
 *   2) loadDefault() — classpath:rag/golden-set.json
 *
 * 失败语义：
 *   - 文件不存在 / 解析失败 → SystemException（INTERNAL_ERROR），错误信息明确指出路径
 *   - 期望任何业务方都能从异常信息直接判断问题在哪
 */
@Slf4j
public final class GoldenSetLoader {

    public static final String DEFAULT_CLASSPATH = "rag/golden-set.json";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GoldenSetLoader() {
    }

    /**
     * 加载 classpath 默认 golden set。
     */
    public static List<GoldenCase> loadDefault() {
        return loadFromClasspath(DEFAULT_CLASSPATH);
    }

    /**
     * 从 classpath 加载。
     */
    public static List<GoldenCase> loadFromClasspath(String classpath) {
        try (InputStream in = GoldenSetLoader.class.getClassLoader().getResourceAsStream(classpath)) {
            if (in == null) {
                throw new SystemException(ErrorCode.INTERNAL_ERROR,
                        "Golden set not found on classpath: " + classpath);
            }
            List<GoldenCase> cases = MAPPER.readValue(in, new TypeReference<List<GoldenCase>>() {});
            log.info("Loaded {} golden cases from classpath: {}", cases.size(), classpath);
            return cases;
        } catch (IOException e) {
            throw new SystemException(ErrorCode.INTERNAL_ERROR,
                    "Failed to parse golden set from classpath: " + classpath, e);
        }
    }

    /**
     * 从文件路径加载（CLI 入口用）。
     */
    public static List<GoldenCase> loadFromFile(Path path) {
        if (path == null || !Files.exists(path)) {
            throw new SystemException(ErrorCode.INTERNAL_ERROR,
                    "Golden set file not found: " + path);
        }
        try (InputStream in = Files.newInputStream(path)) {
            List<GoldenCase> cases = MAPPER.readValue(in, new TypeReference<List<GoldenCase>>() {});
            log.info("Loaded {} golden cases from file: {}", cases.size(), path);
            return cases;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (Exception e) {
            throw new SystemException(ErrorCode.INTERNAL_ERROR,
                    "Failed to parse golden set from file: " + path, e);
        }
    }
}
