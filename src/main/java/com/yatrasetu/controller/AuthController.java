package com.yatrasetu.controller;

import com.yatrasetu.model.User;
import com.yatrasetu.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder;


    public AuthController(UserRepository userRepository) {

        this.userRepository = userRepository;

        this.passwordEncoder =
                new BCryptPasswordEncoder();
    }


    // =====================================================
    // REGISTER
    // =====================================================

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody User user) {

        if (user.getName() == null
                || user.getName().isBlank()
                || user.getEmail() == null
                || user.getEmail().isBlank()
                || user.getPassword() == null
                || user.getPassword().isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    "All fields are required."
                            )
                    );
        }


        String email =
                user.getEmail()
                        .trim()
                        .toLowerCase();


        if (userRepository.existsByEmail(email)) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(
                            Map.of(
                                    "message",
                                    "Email is already registered."
                            )
                    );
        }


        user.setEmail(email);


        String hashedPassword =
                passwordEncoder.encode(
                        user.getPassword()
                );


        user.setPassword(hashedPassword);


        userRepository.save(user);


        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        Map.of(
                                "message",
                                "Registration successful."
                        )
                );
    }


    // =====================================================
    // LOGIN
    // =====================================================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody User loginUser,
            HttpSession session) {

        if (loginUser.getEmail() == null
                || loginUser.getPassword() == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    "Email and password are required."
                            )
                    );
        }


        String email =
                loginUser.getEmail()
                        .trim()
                        .toLowerCase();


        return userRepository
                .findByEmail(email)
                .map(user -> {

                    boolean passwordMatches =
                            passwordEncoder.matches(
                                    loginUser.getPassword(),
                                    user.getPassword()
                            );


                    if (!passwordMatches) {

                        return ResponseEntity
                                .status(
                                        HttpStatus.UNAUTHORIZED
                                )
                                .body(
                                        Map.of(
                                                "message",
                                                "Invalid email or password."
                                        )
                                );
                    }


                    session.setAttribute(
                            "userId",
                            user.getId()
                    );


                    session.setAttribute(
                            "userName",
                            user.getName()
                    );


                    session.setAttribute(
                            "userEmail",
                            user.getEmail()
                    );


                    return ResponseEntity
                            .ok(
                                    Map.of(
                                            "message",
                                            "Login successful.",
                                            "userId",
                                            user.getId(),
                                            "name",
                                            user.getName(),
                                            "email",
                                            user.getEmail()
                                    )
                            );

                })
                .orElseGet(() ->

                        ResponseEntity
                                .status(
                                        HttpStatus.UNAUTHORIZED
                                )
                                .body(
                                        Map.of(
                                                "message",
                                                "Invalid email or password."
                                        )
                                )
                );
    }


    // =====================================================
    // GET CURRENT USER
    // =====================================================

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            HttpSession session) {

        Object userIdObject =
                session.getAttribute("userId");


        if (userIdObject == null) {

            return ResponseEntity
                    .status(
                            HttpStatus.UNAUTHORIZED
                    )
                    .body(
                            Map.of(
                                    "message",
                                    "Not logged in."
                            )
                    );
        }


        Long userId =
                (Long) userIdObject;


        User user =
                userRepository
                        .findById(userId)
                        .orElse(null);


        if (user == null) {

            session.invalidate();


            return ResponseEntity
                    .status(
                            HttpStatus.UNAUTHORIZED
                    )
                    .body(
                            Map.of(
                                    "message",
                                    "User not found."
                            )
                    );
        }


        Map<String, Object> userData =
                new HashMap<>();


        userData.put(
                "userId",
                user.getId()
        );


        userData.put(
                "name",
                user.getName()
        );


        userData.put(
                "email",
                user.getEmail()
        );


        userData.put(
                "mobileNumber",
                user.getMobileNumber()
        );


        userData.put(
                "dateOfBirth",
                user.getDateOfBirth()
        );


        userData.put(
                "gender",
                user.getGender()
        );


        userData.put(
                "nationality",
                user.getNationality()
        );


        return ResponseEntity.ok(
                userData
        );
    }


    // =====================================================
    // UPDATE PROFILE
    // =====================================================

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody ProfileRequest request,
            HttpSession session) {


        Object userIdObject =
                session.getAttribute("userId");


        if (userIdObject == null) {

            return ResponseEntity
                    .status(
                            HttpStatus.UNAUTHORIZED
                    )
                    .body(
                            Map.of(
                                    "message",
                                    "Please login first."
                            )
                    );
        }


        Long userId =
                (Long) userIdObject;


        User user =
                userRepository
                        .findById(userId)
                        .orElse(null);


        if (user == null) {

            session.invalidate();


            return ResponseEntity
                    .status(
                            HttpStatus.UNAUTHORIZED
                    )
                    .body(
                            Map.of(
                                    "message",
                                    "User account not found."
                            )
                    );
        }


        // NAME

        if (request.name() == null
                || request.name().isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    "Name is required."
                            )
                    );
        }


        user.setName(
                request.name().trim()
        );


        // MOBILE NUMBER

        if (request.mobileNumber() != null) {

            String mobile =
                    request.mobileNumber()
                            .trim();


            if (!mobile.isEmpty()
                    && !mobile.matches(
                            "\\d{10}"
                    )) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                Map.of(
                                        "message",
                                        "Mobile number must contain exactly 10 digits."
                                )
                        );
            }


            user.setMobileNumber(
                    mobile.isEmpty()
                            ? null
                            : mobile
            );
        }


        // DATE OF BIRTH

        user.setDateOfBirth(
                request.dateOfBirth()
        );


        // GENDER

        if (request.gender() != null
                && !request.gender().isBlank()) {

            user.setGender(
                    request.gender().trim()
            );

        } else {

            user.setGender(null);
        }


        // NATIONALITY

        if (request.nationality() != null
                && !request.nationality().isBlank()) {

            user.setNationality(
                    request.nationality().trim()
            );

        } else {

            user.setNationality(null);
        }


        userRepository.save(user);


        session.setAttribute(
                "userName",
                user.getName()
        );


        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Profile updated successfully."
                )
        );
    }


    // =====================================================
    // CHANGE PASSWORD
    // =====================================================

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @RequestBody PasswordRequest request,
            HttpSession session) {


        // -----------------------------------------------
        // CHECK LOGIN
        // -----------------------------------------------

        Object userIdObject =
                session.getAttribute("userId");


        if (userIdObject == null) {

            return ResponseEntity
                    .status(
                            HttpStatus.UNAUTHORIZED
                    )
                    .body(
                            Map.of(
                                    "message",
                                    "Please login first."
                            )
                    );
        }


        // -----------------------------------------------
        // VALIDATE REQUEST
        // -----------------------------------------------

        if (request.currentPassword() == null
                || request.currentPassword().isBlank()
                || request.newPassword() == null
                || request.newPassword().isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    "All password fields are required."
                            )
                    );
        }


        // -----------------------------------------------
        // NEW PASSWORD LENGTH
        // -----------------------------------------------

        if (request.newPassword().length() < 6) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    "New password must contain at least 6 characters."
                            )
                    );
        }


        Long userId =
                (Long) userIdObject;


        User user =
                userRepository
                        .findById(userId)
                        .orElse(null);


        if (user == null) {

            session.invalidate();


            return ResponseEntity
                    .status(
                            HttpStatus.UNAUTHORIZED
                    )
                    .body(
                            Map.of(
                                    "message",
                                    "User account not found."
                            )
                    );
        }


        // -----------------------------------------------
        // CHECK CURRENT PASSWORD
        // -----------------------------------------------

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.currentPassword(),
                        user.getPassword()
                );


        if (!passwordMatches) {

            return ResponseEntity
                    .status(
                            HttpStatus.BAD_REQUEST
                    )
                    .body(
                            Map.of(
                                    "message",
                                    "Current password is incorrect."
                            )
                    );
        }


        // -----------------------------------------------
        // PREVENT SAME PASSWORD
        // -----------------------------------------------

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPassword()
        )) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    "New password must be different from the current password."
                            )
                    );
        }


        // -----------------------------------------------
        // HASH NEW PASSWORD
        // -----------------------------------------------

        String hashedPassword =
                passwordEncoder.encode(
                        request.newPassword()
                );


        user.setPassword(
                hashedPassword
        );


        userRepository.save(user);


        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Password changed successfully."
                )
        );
    }


    // =====================================================
    // LOGOUT
    // =====================================================

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            HttpSession session) {

        session.invalidate();


        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Logout successful."
                )
        );
    }


    // =====================================================
    // DELETE ACCOUNT
    // =====================================================

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(
            HttpSession session) {


        Object userIdObject =
                session.getAttribute("userId");


        if (userIdObject == null) {

            return ResponseEntity
                    .status(
                            HttpStatus.UNAUTHORIZED
                    )
                    .body(
                            Map.of(
                                    "message",
                                    "Please login first."
                            )
                    );
        }


        Long userId =
                (Long) userIdObject;


        if (!userRepository.existsById(userId)) {

            session.invalidate();


            return ResponseEntity
                    .status(
                            HttpStatus.NOT_FOUND
                    )
                    .body(
                            Map.of(
                                    "message",
                                    "User account not found."
                            )
                    );
        }


        userRepository.deleteById(
                userId
        );


        session.invalidate();


        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Account deleted successfully."
                )
        );
    }


    // =====================================================
    // PROFILE REQUEST
    // =====================================================

    public record ProfileRequest(

            String name,

            String mobileNumber,

            LocalDate dateOfBirth,

            String gender,

            String nationality

    ) {
    }


    // =====================================================
    // PASSWORD REQUEST
    // =====================================================

    public record PasswordRequest(

            String currentPassword,

            String newPassword

    ) {
    }

}