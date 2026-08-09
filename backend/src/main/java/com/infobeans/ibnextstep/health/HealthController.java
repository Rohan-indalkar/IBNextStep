package com.infobeans.ibnextstep.health;

import com.infobeans.ibnextstep.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final MongoTemplate mongoTemplate;

    @GetMapping
    public ApiResponse<Map<String, String>> health() {
        String mongoStatus;
        try {
            mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1));
            mongoStatus = "UP";
        } catch (Exception e) {
            mongoStatus = "DOWN";
        }
        return ApiResponse.success(Map.of("app", "UP", "mongodb", mongoStatus));
    }
}
