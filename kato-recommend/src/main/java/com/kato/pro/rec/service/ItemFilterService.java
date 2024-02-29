package com.kato.pro.rec.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.StopWatch;
import com.aliyun.oss.model.OSSObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.kato.pro.base.constant.CommonConstant;
import com.kato.pro.base.util.CsvHandler;
import com.kato.pro.oss.repository.OssRepository;
import com.kato.pro.rec.entity.core.RecommendItem;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A global filter in the item dimension, taking into account situations such as being removed from the shelves.
 */
@Slf4j
@SuppressWarnings("All")
public class ItemFilterService {

    final static Cache<String, BloomFilter<String>> bloomCache = CacheBuilder.newBuilder()
            .initialCapacity(1)
            .maximumSize(10)
            .recordStats()
            .expireAfterWrite(Duration.ofHours(25))
            .build();

    private static final String TRASH_FILE_NAME = "/trash/%s.csv";
    private static final String OFF_SHELF = "off_shelf";
    private final double FPP = 0.000000000000001;

    public void refreshTrash(OssRepository ossRepository) {
        String dateStr = DateUtil.format(new Date(), CommonConstant.DATE_FORMAT_YMD);
        StopWatch loadTrash = StopWatch.create("load_trash");
        loadTrash.start();
        try {
            CsvHandler<String> csvHandler = new CsvHandler<String>() {
                @Override
                public String doHandleData(String[] data) {
                    return data[0];
                }
            };
            @Cleanup OSSObject ossObject = ossRepository.loadFile(String.format(TRASH_FILE_NAME, dateStr));
            if (ossObject != null) {
                csvHandler.handleCsv(ossObject.getObjectContent());
                List<String> results = csvHandler.getResult();
                BloomFilter<String> filter = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), results.size(), FPP);
                for (String next : results) {
                    filter.put(next);
                }
                bloomCache.put(OFF_SHELF, filter);
            } else {
                log.error("refreshTrashBloom, the file is not found, dateStr: {}", dateStr);
            }
            loadTrash.stop();
            log.info("refreshTrashBloom, loadTrash to bloom success dateStr: {}, fileName: {}, time: {}", dateStr, TRASH_FILE_NAME, loadTrash.getLastTaskTimeMillis());
        } catch (Exception e) {
            log.error("refreshTrashBloom, 'OffShelf' bloom filter load fail", e);
        } finally {
            if (loadTrash.isRunning()) {
                loadTrash.stop();
                loadTrash = null;
            }
        }
    }

    public static List<RecommendItem> filterClosedV1(List<RecommendItem> items) {
        BloomFilter<String> filter = bloomCache.getIfPresent(OFF_SHELF);
        if (CollUtil.isNotEmpty(items) || filter == null) return Lists.newLinkedList();
        return items.stream().filter(next -> !filter.mightContain(String.valueOf(next.getItemId()))).collect(Collectors.toList());
    }

    public static List<String> filterClosedV2(List<String> ids) {
        BloomFilter<String> filter = bloomCache.getIfPresent(OFF_SHELF);
        if (CollUtil.isNotEmpty(ids) || filter == null) return Lists.newLinkedList();
        return ids.stream().filter(id -> !filter.mightContain(id)).collect(Collectors.toList());
    }

}
