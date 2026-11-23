package com.yatrasetu.config;

import com.yatrasetu.model.TrainClassFare;
import com.yatrasetu.model.TrainSeat;
import com.yatrasetu.repository.TrainClassFareRepository;
import com.yatrasetu.repository.TrainSeatRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrainSeatDataInitializer implements CommandLineRunner {

    private final TrainClassFareRepository trainClassFareRepository;
    private final TrainSeatRepository trainSeatRepository;

    public TrainSeatDataInitializer(
            TrainClassFareRepository trainClassFareRepository,
            TrainSeatRepository trainSeatRepository) {

        this.trainClassFareRepository = trainClassFareRepository;
        this.trainSeatRepository = trainSeatRepository;
    }

    @Override
    public void run(String... args) {

        List<TrainClassFare> classFares =
                trainClassFareRepository.findAll();

        for (TrainClassFare classFare : classFares) {

            List<TrainSeat> existingSeats =
                    trainSeatRepository
                            .findByTrainClassFareAndBookedFalseOrderBySeatGroupAscIdAsc(
                                    classFare
                            );

            if (existingSeats.isEmpty()) {

                createSeats(
                        classFare
                );

            } else {

                updateExistingSeats(
                        classFare
                );
            }
        }
    }

    private void updateExistingSeats(
            TrainClassFare classFare) {

        List<TrainSeat> allSeats =
                trainSeatRepository.findAll();

        for (TrainSeat seat : allSeats) {

            if (!seat.getTrainClassFare()
                    .getId()
                    .equals(classFare.getId())) {

                continue;
            }

            String classCode =
                    classFare.getClassCode()
                            .toUpperCase();

            if ("SL".equals(classCode)
                    || "3A".equals(classCode)) {

                seat.setSeatType(
                        normalizeBerthType(
                                seat.getSeatType()
                        )
                );

            } else if ("2A".equals(classCode)) {

                seat.setSeatType(
                        normalize2ABerthType(
                                seat.getSeatType()
                        )
                );

            } else if ("1A".equals(classCode)) {

                seat.setSeatType(
                        normalize1ABerthType(
                                seat.getSeatType()
                        )
                );

                if (seat.getAccommodationType()
                        == null) {

                    if (seat.getSeatGroup() % 2 == 0) {
                        seat.setAccommodationType(
                                "Coupe"
                        );
                    } else {
                        seat.setAccommodationType(
                                "Cabin"
                        );
                    }
                }

            } else if ("CC".equals(classCode)) {

                seat.setSeatType(
                        normalizeChairSeatType(
                                seat.getSeatType()
                        )
                );
            }

            trainSeatRepository.save(
                    seat
            );
        }
    }

    private String normalizeBerthType(
            String type) {

        if (type == null) {
            return "Lower Berth";
        }

        return switch (type) {

            case "Lower" ->
                    "Lower Berth";

            case "Middle" ->
                    "Middle Berth";

            case "Upper" ->
                    "Upper Berth";

            default ->
                    type;
        };
    }

    private String normalize2ABerthType(
            String type) {

        if (type == null) {
            return "Lower Berth";
        }

        if ("Middle".equalsIgnoreCase(type)) {
            return "Lower Berth";
        }

        if ("Lower".equalsIgnoreCase(type)) {
            return "Lower Berth";
        }

        if ("Upper".equalsIgnoreCase(type)) {
            return "Upper Berth";
        }

        return type;
    }

    private String normalize1ABerthType(
            String type) {

        if (type == null) {
            return "Lower Berth";
        }

        if ("Lower".equalsIgnoreCase(type)) {
            return "Lower Berth";
        }

        if ("Upper".equalsIgnoreCase(type)) {
            return "Upper Berth";
        }

        return type;
    }

    private String normalizeChairSeatType(
            String type) {

        if (type == null) {
            return "Window Side";
        }

        if ("Window".equalsIgnoreCase(type)) {
            return "Window Side";
        }

        if ("Non-Window".equalsIgnoreCase(type)) {
            return "Non-Window";
        }

        return type;
    }

    private void createSeats(
            TrainClassFare classFare) {

        String classCode =
                classFare.getClassCode()
                        .toUpperCase();

        int totalSeats =
                classFare.getAvailableSeats();

        if (totalSeats <= 0) {
            return;
        }

        if ("CC".equals(classCode)) {

            createChairCarSeats(
                    classFare,
                    totalSeats
            );

            return;
        }

        if ("1A".equals(classCode)) {

            createFirstACSeats(
                    classFare,
                    totalSeats
            );

            return;
        }

        if ("2A".equals(classCode)) {

            createTwoACSeats(
                    classFare,
                    totalSeats
            );

            return;
        }

        createSleeperOrThreeACSeats(
                classFare,
                totalSeats
        );
    }

    // =========================================================
    // CC
    // =========================================================

    private void createChairCarSeats(
            TrainClassFare classFare,
            int totalSeats) {

        int seatCounter = 1;
        int group = 1;

        while (seatCounter <= totalSeats) {

            createSeat(
                    classFare,
                    "C1",
                    group + "A",
                    "Window Side",
                    group,
                    null
            );

            seatCounter++;

            if (seatCounter <= totalSeats) {

                createSeat(
                        classFare,
                        "C1",
                        group + "B",
                        "Non-Window",
                        group,
                        null
                );

                seatCounter++;
            }

            if (seatCounter <= totalSeats) {

                createSeat(
                        classFare,
                        "C1",
                        group + "C",
                        "Non-Window",
                        group,
                        null
                );

                seatCounter++;
            }

            if (seatCounter <= totalSeats) {

                createSeat(
                        classFare,
                        "C1",
                        group + "D",
                        "Window Side",
                        group,
                        null
                );

                seatCounter++;
            }

            group++;
        }
    }

    // =========================================================
    // 1A
    // =========================================================

    private void createFirstACSeats(
            TrainClassFare classFare,
            int totalSeats) {

        int seatCounter = 1;
        int group = 1;

        while (seatCounter <= totalSeats) {

            String accommodation =
                    group % 2 == 0
                            ? "Coupe"
                            : "Cabin";

            createSeat(
                    classFare,
                    "H1",
                    String.valueOf(seatCounter),
                    "Lower Berth",
                    group,
                    accommodation
            );

            seatCounter++;

            if (seatCounter <= totalSeats) {

                createSeat(
                        classFare,
                        "H1",
                        String.valueOf(seatCounter),
                        "Upper Berth",
                        group,
                        accommodation
                );

                seatCounter++;
            }

            group++;
        }
    }

    // =========================================================
    // 2A
    // =========================================================

    private void createTwoACSeats(
            TrainClassFare classFare,
            int totalSeats) {

        String[] berthTypes = {
                "Lower Berth",
                "Upper Berth",
                "Side Lower",
                "Side Upper"
        };

        int seatCounter = 1;
        int group = 1;

        while (seatCounter <= totalSeats) {

            for (String berthType :
                    berthTypes) {

                if (seatCounter > totalSeats) {
                    break;
                }

                createSeat(
                        classFare,
                        "A1",
                        String.valueOf(seatCounter),
                        berthType,
                        group,
                        null
                );

                seatCounter++;
            }

            group++;
        }
    }

    // =========================================================
    // SL / 3A
    // =========================================================

    private void createSleeperOrThreeACSeats(
            TrainClassFare classFare,
            int totalSeats) {

        String[] berthTypes = {
                "Lower Berth",
                "Middle Berth",
                "Upper Berth",
                "Lower Berth",
                "Middle Berth",
                "Upper Berth",
                "Side Lower",
                "Side Upper"
        };

        int seatCounter = 1;
        int group = 1;

        while (seatCounter <= totalSeats) {

            for (String berthType :
                    berthTypes) {

                if (seatCounter > totalSeats) {
                    break;
                }

                createSeat(
                        classFare,
                        getCoachName(classFare),
                        String.valueOf(seatCounter),
                        berthType,
                        group,
                        null
                );

                seatCounter++;
            }

            group++;
        }
    }

    private String getCoachName(
            TrainClassFare classFare) {

        return switch (
                classFare
                        .getClassCode()
                        .toUpperCase()
        ) {

            case "SL" -> "S1";

            case "3A" -> "B1";

            default -> "S1";
        };
    }

    // =========================================================
    // CREATE SEAT
    // =========================================================

    private void createSeat(
            TrainClassFare classFare,
            String coach,
            String seatNumber,
            String seatType,
            int group,
            String accommodationType) {

        TrainSeat seat =
                new TrainSeat();

        seat.setTrainClassFare(
                classFare
        );

        seat.setCoach(
                coach
        );

        seat.setSeatNumber(
                seatNumber
        );

        seat.setSeatType(
                seatType
        );

        seat.setSeatGroup(
                group
        );

        seat.setAccommodationType(
                accommodationType
        );

        seat.setBooked(
                false
        );

        trainSeatRepository.save(
                seat
        );
    }
}