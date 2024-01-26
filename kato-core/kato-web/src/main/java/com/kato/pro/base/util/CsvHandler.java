package com.kato.pro.base.util;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.csvreader.CsvReader;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class CsvHandler<T> {

    private final Boolean existHeaderLine;
    @Getter private final List<T> result;

    public CsvHandler() {
        this(true);
    }

    public CsvHandler(Boolean existHeaderLine) {
        this(existHeaderLine, 1024);
    }

    public CsvHandler(Boolean existHeaderLine, Integer capacity) {
        this.existHeaderLine = existHeaderLine;
        this.result = new ArrayList<>(capacity);

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
                result.add(doHandleData(data));
            }
            csvReader.close();
        } catch (Exception e) {
            log.error("CsvStreamHandler#handleCsv, parse csv error");
        }
    }

    /**
     * you can use log to cover default
     */
    public void doHandleHeaders(String[] headers) {
        log.error("headers info: {}", JsonUtils.toStr(headers));
    }

    public abstract T doHandleData(String[] data);

}
