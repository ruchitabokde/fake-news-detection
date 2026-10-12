package com.example.fakenews.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class PredictionController {
    private final ModelService modelService;

    public PredictionController(ModelService modelService) {
        this.modelService = modelService;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ready", modelService.modelName());
    }

    @PostMapping("/predict")
    public PredictionResponse predict(@RequestBody PredictionRequest request) throws Exception {
        if (request == null || request.text() == null || request.text().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide non-empty article or headline text.");
        }
        return modelService.predict(request.text());
    }

    public record PredictionRequest(String text) {}
    public record HealthResponse(String status, String model) {}
}
