package com.pulseconnect.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Emergency Alert entity tracking alerts sent to donors
 */
@Entity
@Table(name = "emergency_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "emergency_request_id", nullable = false)
    private EmergencyRequest emergencyRequest;

    @ManyToOne
    @JoinColumn(name = "donor_id", nullable = false)
    private Donor donor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status = AlertStatus.SENT;

    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime respondedAt;

    @Enumerated(EnumType.STRING)
    private DonorResponse response;

    @Column(columnDefinition = "TEXT")
    private String responseMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum AlertType {
        EMAIL, SMS, PUSH_NOTIFICATION, IN_APP
    }

    public enum AlertStatus {
        SENT, DELIVERED, READ, RESPONDED, FAILED
    }

    public enum DonorResponse {
        AVAILABLE, NOT_AVAILABLE, INTERESTED, DECLINED
    }
}
