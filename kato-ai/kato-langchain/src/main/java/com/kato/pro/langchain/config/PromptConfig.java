package com.kato.pro.langchain.config;

import com.kato.pro.langchain.domain.prompt.DbPromptSource;
import com.kato.pro.langchain.domain.prompt.PromptTemplateVo;
import com.kato.pro.langchain.infrastructure.persistence.PromptTemplateMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableScheduling
public class PromptConfig {

    @Bean
    public com.kato.pro.langchain.domain.prompt.YamlPromptSource yamlPromptSource(ResourceLoader loader) {
        return new com.kato.pro.langchain.domain.prompt.YamlPromptSource(loader);
    }

    @Bean
    public DbPromptSource dbPromptSource(PromptTemplateMapper mapper) {
        return () -> {
            List<PromptTemplateVo> out = new ArrayList<>();
            for (var e : mapper.loadAll()) {
                out.add(new PromptTemplateVo(e.getTenantId(), e.getTemplateKey(),
                        e.getContent(), e.getVersion(), "DB"));
            }
            return out;
        };
    }

    @Bean
    public com.kato.pro.langchain.domain.prompt.PromptTemplateRegistry promptTemplateRegistry(
            com.kato.pro.langchain.domain.prompt.YamlPromptSource yaml,
            DbPromptSource db) {
        return new com.kato.pro.langchain.domain.prompt.PromptTemplateRegistry(yaml, db);
    }

    @Bean
    public com.kato.pro.langchain.domain.prompt.PromptRenderer promptRenderer() {
        return new com.kato.pro.langchain.domain.prompt.PromptRenderer();
    }
}
