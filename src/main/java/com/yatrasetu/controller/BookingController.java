package com.yatrasetu.controller;

import com.yatrasetu.model.Booking;
import com.yatrasetu.model.BookingPassenger;
import com.yatrasetu.model.Train;
import com.yatrasetu.model.TrainClassFare;
import com.yatrasetu.model.TrainSeat;
import com.yatrasetu.model.User;
import com.yatrasetu.repository.BookingPassengerRepository;
import com.yatrasetu.repository.BookingRepository;
import com.yatrasetu.repository.TrainClassFareRepository;
import com.yatrasetu.repository.TrainRepository;
import com.yatrasetu.repository.TrainSeatRepository;
import com.yatrasetu.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingRepository bookingRepository;
    private final BookingPassengerRepository bookingPassengerRepository;
    private final TrainRepository trainRepository;
    private final TrainClassFareRepository trainClassFareRepository;
    private final TrainSeatRepository trainSeatRepository;
    private final UserRepository userRepository;

    private final Random random = new Random();

    public BookingController(
            BookingRepository bookingRepository,
            BookingPassengerRepository bookingPassengerRepository,
            TrainRepository trainRepository,
            TrainClassFareRepository trainClassFareRepository,
            TrainSeatRepository trainSeatRepository,
            UserRepository userRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingPassengerRepository = bookingPassengerRepository;
        this.trainRepository = trainRepository;
        this.trainClassFareRepository = trainClassFareRepository;
        this.trainSeatRepository = trainSeatRepository;
        this.userRepository = userRepository;
    }

    // =========================================================
    // CREATE BOOKING
    // =========================================================

    @PostMapping
    public ResponseEntity<?> createBooking(
            @RequestBody BookingRequest request,
            HttpSession session
    ) {

        User user = getLoggedInUser(session);

        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Please login before booking a ticket."
                    ));
        }

        // =====================================================
        // VALIDATE REQUEST
        // =====================================================

        if (request == null) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Booking request is required."
                    ));
        }

        if (request.getTrainId() == null) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Train is required."
                    ));
        }

        if (request.getClassCode() == null
                || request.getClassCode().trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Travel class is required."
                    ));
        }

        if (request.getPassengers() == null
                || request.getPassengers().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "At least one passenger is required."
                    ));
        }

        if (request.getPassengers().size() > 6) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Maximum 6 passengers are allowed per booking."
                    ));
        }

        // =====================================================
        // FIND TRAIN
        // =====================================================

        Train train =
                trainRepository
                        .findById(request.getTrainId())
                        .orElse(null);

        if (train == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Train not found."
                    ));
        }

        // =====================================================
        // FIND CLASS
        // =====================================================

        String classCode =
                request.getClassCode()
                        .trim()
                        .toUpperCase();

        TrainClassFare trainClassFare =
                trainClassFareRepository
                        .findByTrainAndClassCode(
                                train,
                                classCode
                        )
                        .orElse(null);

        if (trainClassFare == null) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Selected class is not available for this train."
                    ));
        }

        // =====================================================
        // PASSENGER COUNT
        // =====================================================

        int passengerCount =
                request.getPassengers().size();

        // =====================================================
        // CHECK AVAILABILITY
        // =====================================================

        if (trainClassFare.getAvailableSeats()
                < passengerCount) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Not enough seats available in "
                                    + trainClassFare.getClassName()
                    ));
        }

        // =====================================================
        // GET AVAILABLE SEATS
        // =====================================================

        List<TrainSeat> availableSeats =
                trainSeatRepository
                        .findByTrainClassFareAndBookedFalseOrderBySeatGroupAscIdAsc(
                                trainClassFare
                        );

        if (availableSeats.size()
                < passengerCount) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Not enough physical seats are available for this class."
                    ));
        }

        // =====================================================
        // SORT SEATS
        // =====================================================

        availableSeats.sort(
                Comparator
                        .comparingInt(
                                TrainSeat::getSeatGroup
                        )
                        .thenComparing(
                                TrainSeat::getCoach,
                                Comparator.nullsFirst(
                                        String::compareTo
                                )
                        )
                        .thenComparing(
                                TrainSeat::getSeatNumber,
                                Comparator.nullsFirst(
                                        String::compareTo
                                )
                        )
        );

        // =====================================================
        // ALLOCATE SEATS
        // =====================================================

        List<TrainSeat> allocatedSeats =
                allocateSeats(
                        availableSeats,
                        request.getPassengers(),
                        classCode,
                        request.getSeatPreference()
                );

        if (allocatedSeats.size()
                != passengerCount) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Unable to allocate the requested seats with the selected preferences."
                    ));
        }

        // =====================================================
        // CALCULATE FARE
        // =====================================================

        double baseFare =
                trainClassFare.getFare()
                        * passengerCount;

        double gstAmount =
                roundToTwoDecimals(
                        baseFare * 0.05
                );

        double totalFare =
                roundToTwoDecimals(
                        baseFare + gstAmount
                );

        // =====================================================
        // CREATE BOOKING
        // =====================================================

        Booking booking =
                new Booking();

        booking.setUser(user);
        booking.setTrain(train);

        booking.setPnr(
                generateUniquePnr()
        );

        PassengerRequest firstPassenger =
                request.getPassengers().get(0);

        booking.setPassengerName(
                firstPassenger.getName()
        );

        booking.setPassengerAge(
                firstPassenger.getAge()
        );

        booking.setPassengerGender(
                firstPassenger.getGender()
        );

        booking.setSeatsBooked(
                passengerCount
        );

        booking.setClassCode(
                trainClassFare.getClassCode()
        );

        booking.setClassName(
                trainClassFare.getClassName()
        );

        booking.setBaseFare(
                baseFare
        );

        booking.setGstAmount(
                gstAmount
        );

        booking.setSeatPreference(
                request.getSeatPreference()
        );

        booking.setAllocatedSeats(
                buildAllocatedSeatText(
                        allocatedSeats
                )
        );

        booking.setTotalFare(
                totalFare
        );

        booking.setJourneyDate(
                train.getJourneyDate()
        );

        booking.setBookingTime(
                LocalDateTime.now()
        );

        booking.setStatus(
                "CONFIRMED"
        );

        Booking savedBooking =
                bookingRepository.save(
                        booking
                );

        // =====================================================
        // MARK SEATS AS BOOKED
        // =====================================================

        for (TrainSeat seat :
                allocatedSeats) {

            seat.setBooked(true);

            trainSeatRepository.save(
                    seat
            );
        }

        // =====================================================
        // DECREASE AVAILABLE SEATS
        // =====================================================

        trainClassFare.setAvailableSeats(
                trainClassFare.getAvailableSeats()
                        - passengerCount
        );

        trainClassFareRepository.save(
                trainClassFare
        );

        // =====================================================
        // SAVE PASSENGERS
        // =====================================================

        for (int i = 0;
             i < passengerCount;
             i++) {

            PassengerRequest passenger =
                    request.getPassengers().get(i);

            TrainSeat seat =
                    allocatedSeats.get(i);

            BookingPassenger bookingPassenger =
                    new BookingPassenger();

            bookingPassenger.setBooking(
                    savedBooking
            );

            bookingPassenger.setPassengerName(
                    passenger.getName()
            );

            bookingPassenger.setPassengerAge(
                    passenger.getAge()
            );

            bookingPassenger.setPassengerGender(
                    passenger.getGender()
            );

            bookingPassenger.setSeatNumber(
                    seat.getSeatNumber()
            );

            bookingPassenger.setSeatType(
                    seat.getSeatType()
            );

            bookingPassenger.setCoach(
                    seat.getCoach()
            );

            bookingPassengerRepository.save(
                    bookingPassenger
            );
        }

        // =====================================================
        // RESPONSE
        // =====================================================

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "message",
                "Ticket booked successfully."
        );

        response.put(
                "bookingId",
                savedBooking.getId()
        );

        response.put(
                "pnr",
                savedBooking.getPnr()
        );

        response.put(
                "status",
                savedBooking.getStatus()
        );

        response.put(
                "train",
                train.getTrainName()
        );

        response.put(
                "trainNumber",
                train.getTrainNumber()
        );

        response.put(
                "classCode",
                trainClassFare.getClassCode()
        );

        response.put(
                "className",
                trainClassFare.getClassName()
        );

        response.put(
                "passengerCount",
                passengerCount
        );

        response.put(
                "allocatedSeats",
                savedBooking.getAllocatedSeats()
        );

        response.put(
                "baseFare",
                baseFare
        );

        response.put(
                "gstAmount",
                gstAmount
        );

        response.put(
                "totalFare",
                totalFare
        );

        return ResponseEntity.ok(
                response
        );
    }

    // =========================================================
    // SIMULATED PAYMENT
    // =========================================================

    @PutMapping("/{id}/pay")
    public ResponseEntity<?> makePayment(
            @PathVariable Long id,
            @RequestBody(required = false)
            PaymentRequest request,
            HttpSession session
    ) {

        User user =
                getLoggedInUser(session);

        if (user == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Please login first."
                    ));
        }

        Booking booking =
                bookingRepository
                        .findById(id)
                        .orElse(null);

        if (booking == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Booking not found."
                    ));
        }

        // =====================================================
        // OWNERSHIP CHECK
        // =====================================================

        if (booking.getUser() == null
                || booking.getUser().getId() == null
                || !booking.getUser()
                .getId()
                .equals(user.getId())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "You are not allowed to pay for this booking."
                    ));
        }

        // =====================================================
        // CHECK CANCELLED BOOKING
        // =====================================================

        if ("CANCELLED".equalsIgnoreCase(
                booking.getStatus()
        )) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Cancelled bookings cannot be paid for."
                    ));
        }

        /*
         * This is a simulated payment.
         *
         * No real bank, UPI or card transaction happens.
         *
         * The frontend validates the demo payment
         * details and calls this endpoint.
         */

        String paymentMethod =
                "SIMULATED";

        if (request != null
                && request.getPaymentMethod() != null
                && !request.getPaymentMethod()
                .trim()
                .isEmpty()) {

            paymentMethod =
                    request.getPaymentMethod()
                            .trim()
                            .toUpperCase();
        }

        /*
         * The booking was already created and seats were
         * already allocated before reaching the payment page.
         *
         * Therefore we keep the booking CONFIRMED after the
         * simulated payment.
         */

        booking.setStatus(
                "CONFIRMED"
        );

        bookingRepository.save(
                booking
        );

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "message",
                "Payment successful."
        );

        response.put(
                "paymentStatus",
                "SUCCESS"
        );

        response.put(
                "paymentMethod",
                paymentMethod
        );

        response.put(
                "bookingId",
                booking.getId()
        );

        response.put(
                "pnr",
                booking.getPnr()
        );

        response.put(
                "status",
                booking.getStatus()
        );

        response.put(
                "totalFare",
                booking.getTotalFare()
        );

        return ResponseEntity.ok(
                response
        );
    }

    // =========================================================
    // GET MY BOOKINGS
    // =========================================================

    @GetMapping
    public ResponseEntity<?> getMyBookings(
            HttpSession session
    ) {

        User user =
                getLoggedInUser(session);

        if (user == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Please login first."
                    ));
        }

        List<Booking> bookings =
                bookingRepository
                        .findByUserOrderByIdDesc(
                                user
                        );

        List<Map<String, Object>> response =
                new ArrayList<>();

        for (Booking booking :
                bookings) {

            if (booking.getPnr() == null
                    || booking.getPnr()
                    .trim()
                    .isEmpty()) {

                booking.setPnr(
                        generateUniquePnr()
                );

                bookingRepository.save(
                        booking
                );
            }

            response.add(
                    buildBookingResponse(
                            booking
                    )
            );
        }

        return ResponseEntity.ok(
                response
        );
    }

    // =========================================================
    // GET BOOKING BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<?> getBookingById(
            @PathVariable Long id,
            HttpSession session
    ) {

        User user =
                getLoggedInUser(session);

        if (user == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Please login first."
                    ));
        }

        Booking booking =
                bookingRepository
                        .findById(id)
                        .orElse(null);

        if (booking == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Booking not found."
                    ));
        }

        if (booking.getUser() == null
                || booking.getUser().getId() == null
                || !booking.getUser()
                .getId()
                .equals(user.getId())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "You are not allowed to view this booking."
                    ));
        }

        if (booking.getPnr() == null
                || booking.getPnr().trim().isEmpty()) {

            booking.setPnr(
                    generateUniquePnr()
            );

            bookingRepository.save(
                    booking
            );
        }

        return ResponseEntity.ok(
                buildBookingResponse(
                        booking
                )
        );
    }

    // =========================================================
    // GET BOOKING BY PNR
    // =========================================================

    @GetMapping("/pnr/{pnr}")
    public ResponseEntity<?> getBookingByPnr(
            @PathVariable String pnr,
            HttpSession session
    ) {

        User user =
                getLoggedInUser(session);

        if (user == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Please login first."
                    ));
        }

        if (pnr == null
                || pnr.trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "PNR is required."
                    ));
        }

        Booking booking =
                bookingRepository
                        .findByPnr(
                                pnr.trim()
                        )
                        .orElse(null);

        if (booking == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Booking not found for this PNR."
                    ));
        }

        if (booking.getUser() == null
                || booking.getUser().getId() == null
                || !booking.getUser()
                .getId()
                .equals(user.getId())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "You are not allowed to view this booking."
                    ));
        }

        return ResponseEntity.ok(
                buildBookingResponse(
                        booking
                )
        );
    }

    // =========================================================
    // CANCEL BOOKING
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Long id,
            HttpSession session
    ) {

        User user =
                getLoggedInUser(session);

        if (user == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Please login first."
                    ));
        }

        Booking booking =
                bookingRepository
                        .findById(id)
                        .orElse(null);

        if (booking == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Booking not found."
                    ));
        }

        if (booking.getUser() == null
                || booking.getUser().getId() == null
                || !booking.getUser()
                .getId()
                .equals(user.getId())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "You are not allowed to cancel this booking."
                    ));
        }

        if ("CANCELLED".equalsIgnoreCase(
                booking.getStatus()
        )) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "This booking is already cancelled."
                    ));
        }

        Train train =
                booking.getTrain();

        TrainClassFare trainClassFare =
                null;

        if (train != null
                && booking.getClassCode() != null) {

            trainClassFare =
                    trainClassFareRepository
                            .findByTrainAndClassCode(
                                    train,
                                    booking.getClassCode()
                            )
                            .orElse(null);
        }

        List<BookingPassenger> passengers =
                bookingPassengerRepository
                        .findByBooking(
                                booking
                        );

        int releasedSeats = 0;

        if (trainClassFare != null) {

            List<TrainSeat> allSeats =
                    trainSeatRepository
                            .findByTrainClassFare(
                                    trainClassFare
                            );

            for (BookingPassenger passenger :
                    passengers) {

                if (passenger.getSeatNumber()
                        == null) {

                    continue;
                }

                String seatNumber =
                        passenger.getSeatNumber();

                String coach =
                        passenger.getCoach();

                TrainSeat matchingSeat =
                        null;

                for (TrainSeat seat :
                        allSeats) {

                    boolean seatMatches =
                            seat.getSeatNumber() != null
                                    && seat.getSeatNumber()
                                    .equals(
                                            seatNumber
                                    );

                    boolean coachMatches =
                            coach == null
                                    || seat.getCoach() == null
                                    || seat.getCoach()
                                    .equals(coach);

                    if (seatMatches
                            && coachMatches) {

                        matchingSeat = seat;

                        break;
                    }
                }

                if (matchingSeat != null
                        && matchingSeat.isBooked()) {

                    matchingSeat.setBooked(
                            false
                    );

                    trainSeatRepository.save(
                            matchingSeat
                    );

                    releasedSeats++;
                }
            }
        }

        if (trainClassFare != null
                && releasedSeats > 0) {

            trainClassFare.setAvailableSeats(
                    trainClassFare.getAvailableSeats()
                            + releasedSeats
            );

            trainClassFareRepository.save(
                    trainClassFare
            );
        }

        booking.setStatus(
                "CANCELLED"
        );

        bookingRepository.save(
                booking
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Booking cancelled successfully.",

                        "bookingId",
                        booking.getId(),

                        "pnr",
                        booking.getPnr(),

                        "releasedSeats",
                        releasedSeats,

                        "status",
                        booking.getStatus()
                )
        );
    }

    // =========================================================
    // ALLOCATE SEATS
    // =========================================================

    private List<TrainSeat> allocateSeats(
            List<TrainSeat> availableSeats,
            List<PassengerRequest> passengers,
            String classCode,
            String bookingPreference
    ) {

        List<TrainSeat> allocated =
                new ArrayList<>();

        if (passengers == null
                || passengers.isEmpty()) {

            return allocated;
        }

        Map<Integer, List<TrainSeat>> groups =
                new LinkedHashMap<>();

        for (TrainSeat seat :
                availableSeats) {

            groups.computeIfAbsent(
                    seat.getSeatGroup(),
                    key -> new ArrayList<>()
            ).add(seat);
        }

        // =====================================================
        // TRY SAME GROUP FIRST
        // =====================================================

        for (List<TrainSeat> groupSeats :
                groups.values()) {

            if (groupSeats.size()
                    < passengers.size()) {

                continue;
            }

            List<TrainSeat> possible =
                    new ArrayList<>();

            boolean allMatch = true;

            for (PassengerRequest passenger :
                    passengers) {

                String preference =
                        passenger.getSeatPreference();

                if (preference == null
                        || preference.trim().isEmpty()) {

                    preference =
                            bookingPreference;
                }

                TrainSeat matching =
                        findMatchingSeat(
                                groupSeats,
                                possible,
                                preference,
                                classCode
                        );

                if (matching == null) {

                    allMatch = false;

                    break;
                }

                possible.add(
                        matching
                );
            }

            if (allMatch
                    && possible.size()
                    == passengers.size()) {

                return possible;
            }
        }

        // =====================================================
        // TRY DIFFERENT GROUPS
        // =====================================================

        List<TrainSeat> remaining =
                new ArrayList<>(
                        availableSeats
                );

        for (PassengerRequest passenger :
                passengers) {

            String preference =
                    passenger.getSeatPreference();

            if (preference == null
                    || preference.trim().isEmpty()) {

                preference =
                        bookingPreference;
            }

            TrainSeat matching =
                    findMatchingSeat(
                            remaining,
                            allocated,
                            preference,
                            classCode
                    );

            if (matching == null) {

                return Collections.emptyList();
            }

            allocated.add(
                    matching
            );

            remaining.remove(
                    matching
            );
        }

        return allocated;
    }

    // =========================================================
    // FIND MATCHING SEAT
    // =========================================================

    private TrainSeat findMatchingSeat(
            List<TrainSeat> seats,
            List<TrainSeat> alreadyAllocated,
            String preference,
            String classCode
    ) {

        for (TrainSeat seat :
                seats) {

            if (alreadyAllocated.contains(
                    seat
            )) {

                continue;
            }

            if (matchesPreference(
                    seat,
                    preference,
                    classCode
            )) {

                return seat;
            }
        }

        if (preference == null
                || preference.trim().isEmpty()
                || preference.equalsIgnoreCase(
                "No Preference"
        )
                || preference.equalsIgnoreCase(
                "NO_PREFERENCE"
        )) {

            for (TrainSeat seat :
                    seats) {

                if (!alreadyAllocated.contains(
                        seat
                )) {

                    return seat;
                }
            }
        }

        return null;
    }

    // =========================================================
    // MATCH PREFERENCE
    // =========================================================

    private boolean matchesPreference(
            TrainSeat seat,
            String preference,
            String classCode
    ) {

        if (seat == null) {
            return false;
        }

        if (preference == null
                || preference.trim().isEmpty()
                || preference.equalsIgnoreCase(
                "No Preference"
        )
                || preference.equalsIgnoreCase(
                "NO_PREFERENCE"
        )) {

            return true;
        }

        String requested =
                preference.trim();

        // =====================================================
        // CC
        // =====================================================

        if ("CC".equalsIgnoreCase(
                classCode
        )) {

            if (requested.equalsIgnoreCase(
                    "Window Side"
            )
                    || requested.equalsIgnoreCase(
                    "Window"
            )
                    || requested.equalsIgnoreCase(
                    "Window Seat"
            )) {

                return "Window Side"
                        .equalsIgnoreCase(
                                seat.getSeatType()
                        );
            }

            return true;
        }

        // =====================================================
        // 1A
        // =====================================================

        if ("1A".equalsIgnoreCase(
                classCode
        )) {

            if (requested.equalsIgnoreCase(
                    "Cabin"
            )) {

                return "Cabin"
                        .equalsIgnoreCase(
                                seat.getAccommodationType()
                        );
            }

            if (requested.equalsIgnoreCase(
                    "Coupe"
            )) {

                return "Coupe"
                        .equalsIgnoreCase(
                                seat.getAccommodationType()
                        );
            }

            return seat.getSeatType() != null
                    && seat.getSeatType()
                    .equalsIgnoreCase(
                            requested
                    );
        }

        // =====================================================
        // OTHER CLASSES
        // =====================================================

        return seat.getSeatType() != null
                && seat.getSeatType()
                .equalsIgnoreCase(
                        requested
                );
    }

    // =========================================================
    // BUILD ALLOCATED SEAT TEXT
    // =========================================================

    private String buildAllocatedSeatText(
            List<TrainSeat> seats
    ) {

        List<String> values =
                new ArrayList<>();

        for (TrainSeat seat :
                seats) {

            String coach =
                    seat.getCoach() == null
                            ? ""
                            : seat.getCoach();

            String seatNumber =
                    seat.getSeatNumber() == null
                            ? ""
                            : seat.getSeatNumber();

            String seatType =
                    seat.getSeatType() == null
                            ? ""
                            : seat.getSeatType();

            String value;

            if (coach.isEmpty()) {

                value =
                        seatNumber
                                + " ("
                                + seatType
                                + ")";

            } else {

                value =
                        coach
                                + "-"
                                + seatNumber
                                + " ("
                                + seatType
                                + ")";
            }

            values.add(
                    value
            );
        }

        return String.join(
                ", ",
                values
        );
    }

    // =========================================================
    // BUILD COMPLETE BOOKING RESPONSE
    // =========================================================

    private Map<String, Object> buildBookingResponse(
            Booking booking
    ) {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "id",
                booking.getId()
        );

        response.put(
                "bookingId",
                booking.getId()
        );

        response.put(
                "pnr",
                booking.getPnr()
        );

        response.put(
                "passengerName",
                booking.getPassengerName()
        );

        response.put(
                "passengerAge",
                booking.getPassengerAge()
        );

        response.put(
                "passengerGender",
                booking.getPassengerGender()
        );

        response.put(
                "seatsBooked",
                booking.getSeatsBooked()
        );

        response.put(
                "classCode",
                booking.getClassCode()
        );

        response.put(
                "className",
                booking.getClassName()
        );

        response.put(
                "baseFare",
                booking.getBaseFare()
        );

        response.put(
                "gstAmount",
                booking.getGstAmount()
        );

        response.put(
                "seatPreference",
                booking.getSeatPreference()
        );

        response.put(
                "allocatedSeats",
                booking.getAllocatedSeats()
        );

        response.put(
                "totalFare",
                booking.getTotalFare()
        );

        response.put(
                "journeyDate",
                booking.getJourneyDate()
        );

        response.put(
                "bookingTime",
                booking.getBookingTime()
        );

        response.put(
                "status",
                booking.getStatus()
        );

        // =====================================================
        // TRAIN
        // =====================================================

        Train train =
                booking.getTrain();

        if (train != null) {

            Map<String, Object> trainData =
                    new LinkedHashMap<>();

            trainData.put(
                    "id",
                    train.getId()
            );

            trainData.put(
                    "trainName",
                    train.getTrainName()
            );

            trainData.put(
                    "trainNumber",
                    train.getTrainNumber()
            );

            trainData.put(
                    "source",
                    train.getSource()
            );

            trainData.put(
                    "destination",
                    train.getDestination()
            );

            trainData.put(
                    "departureTime",
                    train.getDepartureTime()
            );

            trainData.put(
                    "arrivalTime",
                    train.getArrivalTime()
            );

            trainData.put(
                    "journeyDate",
                    train.getJourneyDate()
            );

            response.put(
                    "train",
                    trainData
            );
        }

        // =====================================================
        // PASSENGERS
        // =====================================================

        List<BookingPassenger> passengers =
                bookingPassengerRepository
                        .findByBooking(
                                booking
                        );

        response.put(
                "passengers",
                passengers
        );

        return response;
    }

    // =========================================================
    // GET LOGGED-IN USER
    // =========================================================

    private User getLoggedInUser(
            HttpSession session
    ) {

        Object sessionUserId =
                session.getAttribute(
                        "userId"
                );

        if (sessionUserId == null) {
            return null;
        }

        try {

            Long userId =
                    Long.valueOf(
                            sessionUserId.toString()
                    );

            return userRepository
                    .findById(userId)
                    .orElse(null);

        } catch (Exception e) {

            return null;
        }
    }

    // =========================================================
    // GENERATE UNIQUE PNR
    // =========================================================

    private String generateUniquePnr() {

        String pnr;

        do {

            long number =
                    1000000000L
                            + (long) (
                            random.nextDouble()
                                    * 900000000L
                    );

            pnr =
                    String.valueOf(
                            number
                    );

        } while (
                bookingRepository
                        .findByPnr(
                                pnr
                        )
                        .isPresent()
        );

        return pnr;
    }

    // =========================================================
    // ROUND MONEY
    // =========================================================

    private double roundToTwoDecimals(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }

    // =========================================================
    // BOOKING REQUEST
    // =========================================================

    public static class BookingRequest {

        private Long trainId;

        private String classCode;

        private String seatPreference;

        private List<PassengerRequest> passengers;

        public BookingRequest() {
        }

        public Long getTrainId() {
            return trainId;
        }

        public void setTrainId(
                Long trainId
        ) {
            this.trainId = trainId;
        }

        public String getClassCode() {
            return classCode;
        }

        public void setClassCode(
                String classCode
        ) {
            this.classCode = classCode;
        }

        public String getSeatPreference() {
            return seatPreference;
        }

        public void setSeatPreference(
                String seatPreference
        ) {
            this.seatPreference =
                    seatPreference;
        }

        public List<PassengerRequest> getPassengers() {
            return passengers;
        }

        public void setPassengers(
                List<PassengerRequest> passengers
        ) {
            this.passengers = passengers;
        }
    }

    // =========================================================
    // PASSENGER REQUEST
    // =========================================================

    public static class PassengerRequest {

        private String name;

        private int age;

        private String gender;

        private String seatPreference;

        public PassengerRequest() {
        }

        public String getName() {
            return name;
        }

        public void setName(
                String name
        ) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(
                int age
        ) {
            this.age = age;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(
                String gender
        ) {
            this.gender = gender;
        }

        public String getSeatPreference() {
            return seatPreference;
        }

        public void setSeatPreference(
                String seatPreference
        ) {
            this.seatPreference =
                    seatPreference;
        }
    }

    // =========================================================
    // PAYMENT REQUEST
    // =========================================================

    public static class PaymentRequest {

        private String paymentMethod;

        public PaymentRequest() {
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(
                String paymentMethod
        ) {
            this.paymentMethod =
                    paymentMethod;
        }
    }
}