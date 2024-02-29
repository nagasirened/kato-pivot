package com.kato.pro.rec.service.core;

import com.kato.pro.oss.repository.OssRepository;
import com.kato.pro.rec.service.ItemFilterService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class ScheduleService implements CommandLineRunner {

    @Resource
    private ItemFilterService itemFilterService;
    @Resource
    private OssRepository ossRepository;

    @Override
    public void run(String... args) throws Exception {

    }

    public void refreshTrash(OssRepository ossRepository) {
        itemFilterService.refreshTrash(ossRepository);
    }


}
