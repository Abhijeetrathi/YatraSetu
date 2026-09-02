package com.yatrasetu.repository;

import com.yatrasetu.model.Train;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TrainRepository extends JpaRepository<Train, Long> {

    List<Train> findBySourceAndDestinationAndJourneyDate(
            String source,
            String destination,
            LocalDate journeyDate
    );
}