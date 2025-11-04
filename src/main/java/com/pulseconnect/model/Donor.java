package com.pulseconnect.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Donor entity representing blood donors with health information
 */
@Entity
@Table(name = "donors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Donor {
    // Getters and setters for mock fields
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getPriorityScore() { return priorityScore; }
    public void setPriorityScore(int priorityScore) { this.priorityScore = priorityScore; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

        // Extra fields for mock data
        private String name;
        private String bloodGroup;
        private String city;
        private String phone;

        // Simple constructor for mock data (id, name, bloodGroup, city, phone)
        public Donor(Long id, String name, String bloodGroup, String city, String phone) {
            this.id = id;
            this.name = name;
            this.bloodGroup = bloodGroup;
            this.city = city;
            this.phone = phone;
            // Set other fields to default/null
            this.user = null;
            this.dateOfBirth = null;
            this.gender = null;
            this.weight = null;
            this.height = null;
            this.isEligible = true;
            this.lastDonationDate = null;
            this.nextEligibleDate = null;
            this.totalDonations = 0;
            this.impactScore = 0;
            this.medicalHistory = null;
            this.hasDiabetes = false;
            this.hasHypertension = false;
            this.hasHeartDisease = false;
            this.hasKidneyDisease = false;
            this.hasInfectiousDisease = false;
            this.isAvailable = true;
            this.willingToTravelFar = false;
            this.priorityScore = 0;
            this.donationHistory = new HashSet<>();
            this.emergencyAlerts = new HashSet<>();
        }

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private Double weight; // in kg
    private Double height; // in cm

    @Column(nullable = false)
    private Boolean isEligible = true;

    private LocalDate lastDonationDate;
    private LocalDate nextEligibleDate;

    private Integer totalDonations = 0;
    private Integer impactScore = 0; // Number of lives potentially saved

    @Column(columnDefinition = "TEXT")
    private String medicalHistory;

    private Boolean hasDiabetes = false;
    private Boolean hasHypertension = false;
    private Boolean hasHeartDisease = false;
    private Boolean hasKidneyDisease = false;
    private Boolean hasInfectiousDisease = false;

    private Boolean isAvailable = true;
    private Boolean willingToTravelFar = false;

    private Integer priorityScore = 0; // Calculated based on multiple factors

    @OneToMany(mappedBy = "donor", cascade = CascadeType.ALL)
    private Set<DonationHistory> donationHistory = new HashSet<>();

    @OneToMany(mappedBy = "donor")
    private Set<EmergencyAlert> emergencyAlerts = new HashSet<>();

    public enum Gender {
        MALE, FEMALE, OTHER
    }

    /**
     * Calculate if donor is eligible based on last donation date
     */
    public void calculateEligibility() {
        if (lastDonationDate == null) {
            isEligible = true;
            nextEligibleDate = LocalDate.now();
            return;
        }

        // Men can donate every 3 months, women every 4 months
        int monthsToWait = (gender == Gender.MALE) ? 3 : 4;
        nextEligibleDate = lastDonationDate.plusMonths(monthsToWait);
        isEligible = LocalDate.now().isAfter(nextEligibleDate) || LocalDate.now().isEqual(nextEligibleDate);
    }

    /**
     * Calculate priority score based on various factors
     */
    public void calculatePriorityScore() {
        int score = 100; // Base score

        // Health factors
        if (hasDiabetes) score -= 10;
        if (hasHypertension) score -= 10;
        if (hasHeartDisease) score -= 20;
        if (hasKidneyDisease) score -= 20;
        if (hasInfectiousDisease) score -= 50;

        // Donation history bonus
        score += Math.min(totalDonations * 5, 50);

        // Availability bonus
        if (isAvailable) score += 20;
        if (willingToTravelFar) score += 10;

        // Eligibility
        if (!isEligible) score -= 30;

        this.priorityScore = Math.max(0, Math.min(score, 100));
    }
}
