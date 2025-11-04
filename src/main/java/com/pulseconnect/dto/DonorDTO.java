package com.pulseconnect.dto;

import com.pulseconnect.model.Donor;
import com.pulseconnect.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DonorDTO {
    private Long id;
    private Long userId;
    private String fullName;
    private User.BloodGroup bloodGroup;
    private LocalDate dateOfBirth;
    private Donor.Gender gender;
    private Double weight;
    private Double height;
    private Boolean isEligible;
    private LocalDate lastDonationDate;
    private LocalDate nextEligibleDate;
    private Integer totalDonations;
    private Integer impactScore;
    private Boolean isAvailable;
    private Integer priorityScore;
    private String city;
    private String phoneNumber;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
}
