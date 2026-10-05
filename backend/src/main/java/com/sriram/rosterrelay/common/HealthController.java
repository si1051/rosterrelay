package com.sriram.rosterrelay.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        try (Connection c = dataSource.getConnection()) {
            return Map.of("status", c.isValid(2) ? "UP" : "DEGRADED");
        } catch (Exception e) {
            return Map.of("status", "DOWN");
        }
    }
}
