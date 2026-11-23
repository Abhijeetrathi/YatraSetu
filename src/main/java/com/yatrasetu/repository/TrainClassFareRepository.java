package com.yatrasetu.repository;

import com.yatrasetu.model.TrainClassFare;
import com.yatrasetu.model.Train;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainClassFareRepository
        extends JpaRepository<TrainClassFare, Long> {

    List<TrainClassFare> findByTrain(Train train);

    Optional<TrainClassFare> findByTrainAndClassCode(
            Train train,
            String classCode
    );
}