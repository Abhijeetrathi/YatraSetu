package com.yatrasetu.repository;

import com.yatrasetu.model.Booking;
import com.yatrasetu.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository
        extends JpaRepository<Booking, Long> {

    List<Booking> findByUserOrderByIdDesc(User user);

    Optional<Booking> findByPnr(String pnr);
}