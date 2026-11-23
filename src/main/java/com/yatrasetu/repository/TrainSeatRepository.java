package com.yatrasetu.repository;

import com.yatrasetu.model.TrainClassFare;
import com.yatrasetu.model.TrainSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainSeatRepository
        extends JpaRepository<TrainSeat, Long> {

    // Find all seats belonging to a train class
    List<TrainSeat> findByTrainClassFare(
            TrainClassFare trainClassFare
    );

    // Find available seats for a train class
    List<TrainSeat> findByTrainClassFareAndBookedFalse(
            TrainClassFare trainClassFare
    );

    // Find available seats ordered by seat group and ID
    List<TrainSeat> findByTrainClassFareAndBookedFalseOrderBySeatGroupAscIdAsc(
            TrainClassFare trainClassFare
    );
}