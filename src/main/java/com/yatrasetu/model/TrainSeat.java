package com.yatrasetu.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "train_seats",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "train_class_fare_id",
                                "coach",
                                "seat_number"
                        }
                )
        }
)
public class TrainSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "train_class_fare_id", nullable = false)
    private TrainClassFare trainClassFare;

    @Column(nullable = false)
    private String coach;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @Column(nullable = false)
    private String seatType;

    @Column(nullable = false)
    private int seatGroup;

    @Column
    private String accommodationType;

    @Column(nullable = false)
    private boolean booked;

    public TrainSeat() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TrainClassFare getTrainClassFare() {
        return trainClassFare;
    }

    public void setTrainClassFare(TrainClassFare trainClassFare) {
        this.trainClassFare = trainClassFare;
    }

    public String getCoach() {
        return coach;
    }

    public void setCoach(String coach) {
        this.coach = coach;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public int getSeatGroup() {
        return seatGroup;
    }

    public void setSeatGroup(int seatGroup) {
        this.seatGroup = seatGroup;
    }

    public String getAccommodationType() {
        return accommodationType;
    }

    public void setAccommodationType(String accommodationType) {
        this.accommodationType = accommodationType;
    }

    public boolean isBooked() {
        return booked;
    }

    public void setBooked(boolean booked) {
        this.booked = booked;
    }
}