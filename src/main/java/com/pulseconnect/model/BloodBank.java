package com.pulseconnect.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Blood Bank entity representing blood storage facilities
 */
@Entity
@Table(name = "blood_banks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BloodBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    private String pincode;

    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    private String phoneNumber;

    private String email;

    @Column(nullable = false)
    private Boolean isActive = true;

    // Current blood inventory
    private Integer aPositiveUnits = 0;
    private Integer aNegativeUnits = 0;
    private Integer bPositiveUnits = 0;
    private Integer bNegativeUnits = 0;
    private Integer abPositiveUnits = 0;
    private Integer abNegativeUnits = 0;
    private Integer oPositiveUnits = 0;
    private Integer oNegativeUnits = 0;

    private String operatingHours;

    @Column(columnDefinition = "TEXT")
    private String facilities;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastInventoryUpdate;

    /**
     * Get units available for a specific blood group
     */
    public Integer getUnitsForBloodGroup(User.BloodGroup bloodGroup) {
        return switch (bloodGroup) {
            case A_POSITIVE -> aPositiveUnits;
            case A_NEGATIVE -> aNegativeUnits;
            case B_POSITIVE -> bPositiveUnits;
            case B_NEGATIVE -> bNegativeUnits;
            case AB_POSITIVE -> abPositiveUnits;
            case AB_NEGATIVE -> abNegativeUnits;
            case O_POSITIVE -> oPositiveUnits;
            case O_NEGATIVE -> oNegativeUnits;
        };
    }

    /**
     * Update inventory for a specific blood group
     */
    public void updateInventory(User.BloodGroup bloodGroup, Integer units) {
        switch (bloodGroup) {
            case A_POSITIVE -> aPositiveUnits = units;
            case A_NEGATIVE -> aNegativeUnits = units;
            case B_POSITIVE -> bPositiveUnits = units;
            case B_NEGATIVE -> bNegativeUnits = units;
            case AB_POSITIVE -> abPositiveUnits = units;
            case AB_NEGATIVE -> abNegativeUnits = units;
            case O_POSITIVE -> oPositiveUnits = units;
            case O_NEGATIVE -> oNegativeUnits = units;
        }
        lastInventoryUpdate = LocalDateTime.now();
    }
}
