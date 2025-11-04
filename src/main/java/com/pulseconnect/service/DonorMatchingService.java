package com.pulseconnect.service;

import com.pulseconnect.dto.DonorDTO;
import com.pulseconnect.model.Donor;
import com.pulseconnect.model.User;
import com.pulseconnect.repository.DonorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for smart donor matching based on multiple criteria
 */
@Service
@RequiredArgsConstructor
public class DonorMatchingService {

    private final DonorRepository donorRepository;

    /**
     * Find matching donors based on blood type, location, urgency, and priority score
     */
    @Transactional(readOnly = true)
    public List<DonorDTO> findMatchingDonors(User.BloodGroup bloodGroup, 
                                              Double latitude, 
                                              Double longitude,
                                              Double radiusKm,
                                              String urgencyLevel) {
        // Find eligible donors nearby
        List<Donor> donors = donorRepository.findEligibleDonorsNearby(
            bloodGroup, latitude, longitude, radiusKm
        );

        // Convert to DTOs and calculate distances
        List<DonorDTO> donorDTOs = donors.stream()
            .map(donor -> convertToDonorDTO(donor, latitude, longitude))
            .collect(Collectors.toList());

        // Sort by composite score (priority score + distance factor)
        donorDTOs.sort(Comparator.comparing(this::calculateMatchScore).reversed());

        return donorDTOs;
    }

    /**
     * Find compatible blood donors (considering universal donors)
     */
    @Transactional(readOnly = true)
    public List<DonorDTO> findCompatibleDonors(User.BloodGroup requiredBloodGroup,
                                                 Double latitude,
                                                 Double longitude,
                                                 Double radiusKm) {
        List<DonorDTO> compatibleDonors = new ArrayList<>();

        // Get compatible blood groups
        List<User.BloodGroup> compatibleGroups = getCompatibleBloodGroups(requiredBloodGroup);

        for (User.BloodGroup bloodGroup : compatibleGroups) {
            List<DonorDTO> donors = findMatchingDonors(bloodGroup, latitude, longitude, radiusKm, "HIGH");
            compatibleDonors.addAll(donors);
        }

        // Remove duplicates and sort by match score
        return compatibleDonors.stream()
            .distinct()
            .sorted(Comparator.comparing(this::calculateMatchScore).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Get compatible blood groups for transfusion
     */
    private List<User.BloodGroup> getCompatibleBloodGroups(User.BloodGroup requiredBloodGroup) {
        List<User.BloodGroup> compatible = new ArrayList<>();
        compatible.add(requiredBloodGroup); // Exact match first

        // Add compatible donors based on blood type compatibility
        switch (requiredBloodGroup) {
            case AB_POSITIVE:
                // Universal recipient - can receive from all
                compatible.add(User.BloodGroup.AB_NEGATIVE);
                compatible.add(User.BloodGroup.A_POSITIVE);
                compatible.add(User.BloodGroup.A_NEGATIVE);
                compatible.add(User.BloodGroup.B_POSITIVE);
                compatible.add(User.BloodGroup.B_NEGATIVE);
                compatible.add(User.BloodGroup.O_POSITIVE);
                compatible.add(User.BloodGroup.O_NEGATIVE);
                break;
            case AB_NEGATIVE:
                compatible.add(User.BloodGroup.A_NEGATIVE);
                compatible.add(User.BloodGroup.B_NEGATIVE);
                compatible.add(User.BloodGroup.O_NEGATIVE);
                break;
            case A_POSITIVE:
                compatible.add(User.BloodGroup.A_NEGATIVE);
                compatible.add(User.BloodGroup.O_POSITIVE);
                compatible.add(User.BloodGroup.O_NEGATIVE);
                break;
            case A_NEGATIVE:
                compatible.add(User.BloodGroup.O_NEGATIVE);
                break;
            case B_POSITIVE:
                compatible.add(User.BloodGroup.B_NEGATIVE);
                compatible.add(User.BloodGroup.O_POSITIVE);
                compatible.add(User.BloodGroup.O_NEGATIVE);
                break;
            case B_NEGATIVE:
                compatible.add(User.BloodGroup.O_NEGATIVE);
                break;
            case O_POSITIVE:
                compatible.add(User.BloodGroup.O_NEGATIVE);
                break;
            case O_NEGATIVE:
                // O- is universal donor but can only receive O-
                break;
        }

        return compatible;
    }

    /**
     * Calculate match score based on priority score and distance
     */
    private Double calculateMatchScore(DonorDTO donor) {
        double priorityWeight = 0.6;
        double distanceWeight = 0.4;

        double priorityScore = donor.getPriorityScore() != null ? donor.getPriorityScore() : 50.0;
        double distanceScore = calculateDistanceScore(donor.getDistanceKm());

        return (priorityScore * priorityWeight) + (distanceScore * distanceWeight);
    }

    /**
     * Calculate distance score (closer = higher score)
     */
    private Double calculateDistanceScore(Double distanceKm) {
        if (distanceKm == null) return 0.0;
        if (distanceKm <= 5) return 100.0;
        if (distanceKm <= 10) return 80.0;
        if (distanceKm <= 20) return 60.0;
        if (distanceKm <= 30) return 40.0;
        return 20.0;
    }

    /**
     * Calculate distance between two points using Haversine formula
     */
    private Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return null;
        }

        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Convert Donor entity to DTO with calculated distance
     */
    private DonorDTO convertToDonorDTO(Donor donor, Double targetLat, Double targetLon) {
        DonorDTO dto = new DonorDTO();
        dto.setId(donor.getId());
        dto.setUserId(donor.getUser().getId());
        dto.setFullName(donor.getUser().getFullName());
        dto.setBloodGroup(donor.getUser().getBloodGroup());
        dto.setDateOfBirth(donor.getDateOfBirth());
        dto.setGender(donor.getGender());
        dto.setWeight(donor.getWeight());
        dto.setHeight(donor.getHeight());
        dto.setIsEligible(donor.getIsEligible());
        dto.setLastDonationDate(donor.getLastDonationDate());
        dto.setNextEligibleDate(donor.getNextEligibleDate());
        dto.setTotalDonations(donor.getTotalDonations());
        dto.setImpactScore(donor.getImpactScore());
        dto.setIsAvailable(donor.getIsAvailable());
        dto.setPriorityScore(donor.getPriorityScore());
        dto.setCity(donor.getUser().getCity());
        dto.setPhoneNumber(donor.getUser().getPhoneNumber());
        dto.setLatitude(donor.getUser().getLatitude());
        dto.setLongitude(donor.getUser().getLongitude());
        
        // Calculate distance
        Double distance = calculateDistance(
            targetLat, targetLon, 
            donor.getUser().getLatitude(), donor.getUser().getLongitude()
        );
        dto.setDistanceKm(distance);
        
        return dto;
    }

    /**
     * Update donor priority score
     */
    @Transactional
    public void updateDonorPriorityScores() {
        List<Donor> donors = donorRepository.findAll();
        for (Donor donor : donors) {
            donor.calculateEligibility();
            donor.calculatePriorityScore();
            donorRepository.save(donor);
        }
    }
}
