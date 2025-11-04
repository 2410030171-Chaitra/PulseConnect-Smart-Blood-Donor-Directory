package com.pulseconnect.dto;

import com.pulseconnect.model.EmergencyRequest;
import com.pulseconnect.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyRequestDTO {
    
    private Long id;
    
    @NotBlank(message = "Patient name is required")
    private String patientName;
    
    @NotNull(message = "Blood group is required")
    private User.BloodGroup requiredBloodGroup;
    
    @NotNull(message = "Units required is required")
    @Min(value = 1, message = "At least 1 unit is required")
    private Integer unitsRequired;
    
    @NotBlank(message = "Hospital name is required")
    private String hospitalName;
    
    @NotBlank(message = "Hospital address is required")
    private String hospitalAddress;
    
    private String hospitalCity;
    private Double hospitalLatitude;
    private Double hospitalLongitude;
    
    @NotBlank(message = "Contact number is required")
    private String contactNumber;
    
    @NotNull(message = "Urgency level is required")
    private EmergencyRequest.UrgencyLevel urgencyLevel;
    
    private EmergencyRequest.RequestStatus status;
    
    private String description;
    
    private LocalDateTime requiredBy;
    
    private Integer radiusKm = 10;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
