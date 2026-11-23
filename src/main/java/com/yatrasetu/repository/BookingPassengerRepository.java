package com.yatrasetu.repository;

import com.yatrasetu.model.Booking;
import com.yatrasetu.model.BookingPassenger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingPassengerRepository
        extends JpaRepository<BookingPassenger, Long> {

    List<BookingPassenger> findByBooking(Booking booking);
}