package com.yatrasetu.controller;

import com.yatrasetu.model.Train;
import com.yatrasetu.model.TrainClassFare;
import com.yatrasetu.repository.TrainClassFareRepository;
import com.yatrasetu.repository.TrainRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trains")
public class TrainController {

    private final TrainRepository trainRepository;
    private final TrainClassFareRepository trainClassFareRepository;

    public TrainController(
            TrainRepository trainRepository,
            TrainClassFareRepository trainClassFareRepository) {

        this.trainRepository = trainRepository;
        this.trainClassFareRepository = trainClassFareRepository;
    }

    @GetMapping
    public List<Train> searchTrains(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam LocalDate date
    ) {
        String normalizedSource = source.trim().replaceAll("\\s+", " ");
        String normalizedDestination = destination.trim().replaceAll("\\s+", " ");

        return trainRepository
                .findByNormalizedSourceAndNormalizedDestinationAndJourneyDate(
                        normalizedSource,
                        normalizedDestination,
                        date
                );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTrainById(
            @PathVariable Long id
    ) {
        Train train = trainRepository
                .findById(id)
                .orElse(null);

        if (train == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Train not found."
                    ));
        }

        return ResponseEntity.ok(train);
    }

    @GetMapping("/{id}/classes")
    public ResponseEntity<?> getTrainClasses(
            @PathVariable Long id
    ) {
        Train train = trainRepository
                .findById(id)
                .orElse(null);

        if (train == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Train not found."
                    ));
        }

        List<TrainClassFare> classes =
                trainClassFareRepository.findByTrain(train);

        return ResponseEntity.ok(classes);
    }
}