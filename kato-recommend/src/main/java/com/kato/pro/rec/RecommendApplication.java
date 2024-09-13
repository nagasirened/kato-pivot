package com.kato.pro.rec;


import com.kato.pro.rec.service.retrieval.filter.RetrievalFilterProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude={DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EnableScheduling
@EnableDiscoveryClient
public class RecommendApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(RecommendApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        RetrievalFilterProcessor.loadFilters();
    }
}
