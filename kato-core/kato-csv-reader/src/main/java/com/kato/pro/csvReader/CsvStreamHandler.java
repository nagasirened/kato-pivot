package com.kato.pro.csvReader;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.csvreader.CsvReader;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 读取csv文件，按照按照行为单位进行处理
 */
@Slf4j
public abstract class CsvStreamHandler {

    public void handleCsv(InputStream inputStream) {
        if (ObjectUtil.isNull(inputStream)) {
            log.error("CsvStreamHandler#handleCsv, inputStream is null, check param");
            return;
        }
        CsvReader csvReader = null;
        InputStreamReader inputStreamReader = null;
        try {
            inputStreamReader = new InputStreamReader(inputStream);
            csvReader = new CsvReader(inputStreamReader);
            String[] headers = csvReader.readRecord() ? csvReader.getValues() : new String[0];
            if (ArrayUtil.isEmpty(headers)) {
                log.error("CsvStreamHandler#handleCsv, csv file is empty");
                return;
            }
            doHandleHeaders(headers);
            String[] data;
            while (csvReader.readRecord()) {
                data = csvReader.getValues();
                doHandleData(data);
            }
        } catch (Exception e) {
            log.error("CsvStreamHandler#handleCsv, parse csv error");
        } finally {
            if (csvReader != null) {
                csvReader.close();
            }
        }
    }

    public void doHandleHeaders(String[] headers) {
        // 可重写该方法，获取到headers并处理
    }

    public abstract void doHandleData(String[] data);

}
