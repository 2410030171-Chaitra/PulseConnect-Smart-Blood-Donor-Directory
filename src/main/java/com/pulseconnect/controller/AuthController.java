package com.pulseconnect.controller;

import com.pulseconnect.dto.LoginRequestDTO;
import com.pulseconnect.dto.LoginResponseDTO;
import com.pulseconnect.dto.UserRegistrationDTO;
import com.pulseconnect.model.Donor;
import com.pulseconnect.model.User;
import com.pulseconnect.repository.DonorRepository;
import com.pulseconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Dev fallback: also allow login with Spring Boot's simple user if configured
    @Value("${spring.security.user.name:}")
    private String basicUserName;

    @Value("${spring.security.user.password:}")
    private String basicUserPassword;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegistrationDTO dto) {
        // Basic duplicate checks
        if (userRepository.existsByUsername(dto.getUsername())) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        // Create user
        User user = new User();
        user.setUsername(dto.getUsername() != null && !dto.getUsername().isBlank() ? dto.getUsername() : dto.getEmail());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFullName(dto.getFullName());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setBloodGroup(dto.getBloodGroup());
        user.setAddress(dto.getAddress());
        user.setCity(dto.getCity());
        user.setState(dto.getState() != null && !dto.getState().isBlank() ? dto.getState() : "Telangana");
        user.setPincode(dto.getPincode());
        user.setLatitude(dto.getLatitude());
        user.setLongitude(dto.getLongitude());
        user.setRole(dto.getRole() != null ? dto.getRole() : User.UserRole.DONOR);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setPreferredLanguage(dto.getPreferredLanguage() != null ? dto.getPreferredLanguage() : "en");

        // Persist user
        User saved = userRepository.save(user);

        // Create a donor profile by default for donors
        if (saved.getRole() == User.UserRole.DONOR) {
            Donor donor = new Donor();
            donor.setUser(saved);
            donor.setIsEligible(true);
            donor.setIsAvailable(true);
            donor.calculatePriorityScore();
            donorRepository.save(donor);
        }

        return ResponseEntity.ok(saved.getId());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body("Username/email and password are required");
        }

    // Allow login with username, email, or phone
    Optional<User> byUsername = userRepository.findByUsername(request.getUsername());
    Optional<User> byEmail = byUsername.isPresent() ? byUsername : userRepository.findByEmail(request.getUsername());
    Optional<User> byPhone = byEmail.isPresent() ? byEmail : userRepository.findByPhoneNumber(request.getUsername().replaceAll("[^0-9]", ""));
    Optional<User> candidate = byPhone.isPresent() ? byPhone : byEmail;

    if (candidate.isEmpty()) {
            // Fallback: permit the configured simple user (for demos/dev only)
            if (basicUserName != null && !basicUserName.isBlank()
                    && basicUserPassword != null && !basicUserPassword.isBlank()
                    && request.getUsername().equals(basicUserName)
                    && request.getPassword().equals(basicUserPassword)) {
                LoginResponseDTO response = new LoginResponseDTO(
                        "basic-auth-token",
                        0L,
                        basicUserName,
                        basicUserName,
                        "ADMIN"
                );
                response.setMessage("Login successful (admin)");
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(401).body("Invalid credentials");
        }

    User user = candidate.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // Same fallback for dev admin
            if (basicUserName != null && request.getUsername().equals(basicUserName)
                    && basicUserPassword != null && request.getPassword().equals(basicUserPassword)) {
                LoginResponseDTO response = new LoginResponseDTO(
                        "basic-auth-token",
                        0L,
                        basicUserName,
                        basicUserName,
                        "ADMIN"
                );
                response.setMessage("Login successful (admin)");
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        // Return a simple response (no real JWT for now)
        LoginResponseDTO response = new LoginResponseDTO(
                "demo-token",
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
        response.setMessage("Login successful");
        return ResponseEntity.ok(response);
    }
}
