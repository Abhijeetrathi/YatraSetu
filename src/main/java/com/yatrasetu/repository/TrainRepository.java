package com.yatrasetu.repository;

import com.yatrasetu.model.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TrainRepository extends JpaRepository<Train, Long> {

    @Query("SELECT t FROM Train t WHERE LOWER(TRIM(t.source)) = LOWER(TRIM(:source)) " +
           "AND LOWER(TRIM(t.destination)) = LOWER(TRIM(:destination)) " +
           "AND t.journeyDate = :journeyDate ORDER BY t.departureTime ASC")
    List<Train> findByNormalizedSourceAndNormalizedDestinationAndJourneyDate(
            @Param("source") String source,
            @Param("destination") String destination,
            @Param("journeyDate") LocalDate journeyDate
    );
}