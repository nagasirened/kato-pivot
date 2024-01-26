package com.kato.pro.rec.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.BloomFilter;
import com.kato.pro.base.util.CsvHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

/**
 * A global filter in the item dimension, taking into account situations such as being removed from the shelves.
 */
@Slf4j
@Service
public class ItemFilterService {

    @SuppressWarnings("ALL")
    Cache<String, BloomFilter<Integer>> bloomCache = CacheBuilder.newBuilder()
            .initialCapacity(1)
            .maximumSize(10)
            .recordStats()
            .expireAfterWrite(Duration.ofHours(25))
            .build();

    public static final String OFF_SHELF = "off_shelf";
    private final double fpp = 0.000000000000001;

    public void refreshBloom() {
        try {
            CsvHandler<Integer> csvHandler = new CsvHandler<Integer>() {
                @Override
                public Integer doHandleData(String[] data) {
                    return Integer.parseInt(data[0]);
                }
            };
            InputStream inputStream = Files.newInputStream(Paths.get(""));
            csvHandler.handleCsv(inputStream);
            List<Integer> result = csvHandler.getResult();
        } catch (Exception e) {
            log.error("OffShelf bloom filter load fail", e);
        }
    }

}
