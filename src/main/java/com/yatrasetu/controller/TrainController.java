package com.yatrasetu.controller;

import com.yatrasetu.model.Train;
import com.yatrasetu.repository.TrainRepository;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trains")
public class TrainController {

    private final TrainRepository trainRepository;

    public TrainController(TrainRepository trainRepository) {
        this.trainRepository = trainRepository;
    }

    @GetMapping
    public List<Train> searchTrains(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam LocalDate date
    ) {

        return trainRepository.findBySourceAndDestinationAndJourneyDate(
                source,
                destination,
                date
        );
    }
}