package com.kato.pro.common.utils;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.csvreader.CsvReader;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class CsvHandler<T> {

    // 是否包含表头行
    private final Boolean existHeaderLine;
    // 表头字段所在的索引位置
    private volatile Map<String, Integer> headerIndexMap;

    @Getter private Boolean result = true;
    // 存储结果的集合
    @Getter private final List<T> datas;
    // 是否有异常
    @Getter private Exception exception;

    public CsvHandler() {
        this(true);
    }

    public CsvHandler(Boolean existHeaderLine) {
        this(existHeaderLine, 1024);
    }

    public CsvHandler(Boolean existHeaderLine, Integer capacity) {
        this.existHeaderLine = existHeaderLine;
        this.datas = new ArrayList<>(capacity);
        if (existHeaderLine) {
            headerIndexMap = new HashMap<>();
        }
    }

    public void handleCsv(InputStream inputStream) {
        if (ObjectUtil.isNull(inputStream)) {
            log.error("CsvStreamHandler#handleCsv, inputStream is null, check param");
            return;
        }
        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {
            CsvReader csvReader = new CsvReader(inputStreamReader);
            if (existHeaderLine) {
                String[] headers = csvReader.readRecord() ? csvReader.getValues() : new String[0];
                if (ArrayUtil.isEmpty(headers)) {
                    log.error("CsvStreamHandler#handleCsv, csv file is empty");
                    return;
                }
                doHandleHeaders(headers);
            }
            String[] data;
            while (csvReader.readRecord()) {
                data = csvReader.getValues();
                T t = doHandleData(data);
                if (t != null) {
                    datas.add(t);
                }
            }
            csvReader.close();
        } catch (Exception e) {
            log.info("CsvStreamHandler#handleCsv, parse csv fail");
            this.result = false;
            this.exception = e;
        }
    }

    /**
     * you can use log to cover default
     */
    public void doHandleHeaders(String[] headers) {
        for (int i = 0; i < headers.length; i++) {
            headerIndexMap.put(headers[i], i);
        }
        log.info("headers info: {}", JsonUtils.toJSONString(headers));
    }

    public abstract T doHandleData(String[] data);

    public Integer headerIndex(String field) {
        return headerIndexMap.get(field);
    }

}
