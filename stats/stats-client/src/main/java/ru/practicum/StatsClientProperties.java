package ru.practicum;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "stats")
public class StatsClientProperties {
    private String serviceId;
}
