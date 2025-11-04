package com.pulseconnect.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Blood Demand Prediction entity for AI-based forecasting
 */
@Entity
@Table(name = "blood_demand_predictions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BloodDemandPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private User.BloodGroup bloodGroup;

    @Column(nullable = false)
    private LocalDate predictionDate;

    @Column(nullable = false)
    private Integer predictedUnitsRequired;

    private Integer actualUnitsRequired;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private Double confidenceScore; // 0.0 to 1.0

    @Column(columnDefinition = "TEXT")
    private String factors; // JSON string of factors affecting prediction

    @Enumerated(EnumType.STRING)
    private SeasonalFactor seasonalFactor;

    private Boolean isHolidaySeason = false;
    private Boolean isAccidentPronePeriod = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum SeasonalFactor {
        HIGH_DEMAND, MEDIUM_DEMAND, LOW_DEMAND, NORMAL
    }
}
