package com.pulseconnect.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Donation History entity tracking all blood donations
 */
@Entity
@Table(name = "donation_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DonationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "donor_id", nullable = false)
    private Donor donor;

    @Column(nullable = false)
    private LocalDateTime donationDate;

    @Column(nullable = false)
    private String location;

    private String hospitalName;

    private Integer unitsCollected; // in ml

    @Enumerated(EnumType.STRING)
    private DonationType donationType;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    private DonationStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum DonationType {
        WHOLE_BLOOD, PLASMA, PLATELETS, POWER_RED
    }

    public enum DonationStatus {
        SCHEDULED, COMPLETED, CANCELLED, MISSED
    }
}
