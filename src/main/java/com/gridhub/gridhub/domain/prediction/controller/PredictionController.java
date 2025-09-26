package com.gridhub.gridhub.domain.prediction.controller;

import com.gridhub.gridhub.domain.prediction.dto.PredictionRequest;
import com.gridhub.gridhub.domain.prediction.dto.PredictionResponse;
import com.gridhub.gridhub.domain.prediction.service.PredictionService;
import com.gridhub.gridhub.global.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/predictions")
public class PredictionController {

    private final PredictionService predictionService;

    @PostMapping("/races/{raceId}")
    public ResponseEntity<Void> createPrediction(
            @PathVariable Long raceId,
            @Valid @RequestBody PredictionRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        predictionService.createPrediction(raceId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/races/{raceId}/me")
    public ResponseEntity<PredictionResponse> getMyPrediction(
            @PathVariable Long raceId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        PredictionResponse myPrediction = predictionService.getMyPredictionForRace(raceId, userDetails.getUsername());

        if (myPrediction == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(myPrediction);
    }
}
