package com.bank.ingestion.adapter.inbound.rest;

import com.bank.ingestion.adapter.inbound.rest.dto.HealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/internal/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final MongoTemplate mongoTemplate;

    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        Map<String, String> components = new LinkedHashMap<>();
        boolean allHealthy = true;

        // Postgres
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            components.put("postgres", "UP");
        } catch (Exception e) {
            components.put("postgres", "DOWN");
            allHealthy = false;
        }

        // MongoDB
        try {
            mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1));
            components.put("mongodb", "UP");
        } catch (Exception e) {
            components.put("mongodb", "DOWN");
            allHealthy = false;
        }

        String overall = allHealthy ? "UP" : "DOWN";
        HttpStatus httpStatus = allHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(new HealthResponse(overall, components));
    }
}
