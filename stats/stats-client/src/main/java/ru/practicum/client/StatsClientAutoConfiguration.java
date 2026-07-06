package ru.practicum.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class StatsClientAutoConfiguration {

    @Bean
    public AnalyzerClient analyzerClient() {
        return new AnalyzerClient();
    }

    @Bean
    public CollectorClient collectorClient() {
        return new CollectorClient();
    }
}
