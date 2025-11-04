package com.pulseconnect.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Emergency Request entity for urgent blood requirements
 */
@Entity
@Table(name = "emergency_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String patientName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private User.BloodGroup requiredBloodGroup;

    @Column(nullable = false)
    private Integer unitsRequired;

    @Column(nullable = false)
    private String hospitalName;

    @Column(nullable = false)
    private String hospitalAddress;

    private String hospitalCity;
    private Double hospitalLatitude;
    private Double hospitalLongitude;

    @Column(nullable = false)
    private String contactNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UrgencyLevel urgencyLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime requiredBy;

    private Integer radiusKm = 10; // Search radius for donors

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "emergencyRequest", cascade = CascadeType.ALL)
    private Set<EmergencyAlert> alerts = new HashSet<>();

    public enum UrgencyLevel {
        CRITICAL, HIGH, MEDIUM, LOW
    }

    public enum RequestStatus {
        ACTIVE, FULFILLED, CANCELLED, EXPIRED
    }
}
