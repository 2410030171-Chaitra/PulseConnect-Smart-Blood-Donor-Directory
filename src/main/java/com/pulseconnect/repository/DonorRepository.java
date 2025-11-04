package com.pulseconnect.repository;

import com.pulseconnect.model.Donor;
import com.pulseconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DonorRepository extends JpaRepository<Donor, Long> {
    
    Optional<Donor> findByUser(User user);
    
    Optional<Donor> findByUserId(Long userId);
    
    List<Donor> findByIsEligibleTrue();
    
    List<Donor> findByIsAvailableTrue();
    
    List<Donor> findByIsEligibleTrueAndIsAvailableTrue();
    
    @Query("SELECT d FROM Donor d WHERE d.user.bloodGroup = :bloodGroup " +
           "AND d.isEligible = true AND d.isAvailable = true")
    List<Donor> findEligibleDonorsByBloodGroup(@Param("bloodGroup") User.BloodGroup bloodGroup);
    
    @Query("SELECT d FROM Donor d WHERE d.user.bloodGroup = :bloodGroup " +
           "AND d.user.city = :city AND d.isEligible = true AND d.isAvailable = true")
    List<Donor> findEligibleDonorsByBloodGroupAndCity(@Param("bloodGroup") User.BloodGroup bloodGroup,
                                                        @Param("city") String city);
    
    @Query("SELECT d FROM Donor d WHERE d.user.bloodGroup = :bloodGroup " +
           "AND d.isEligible = true AND d.isAvailable = true " +
           "AND (6371 * acos(cos(radians(:latitude)) * cos(radians(d.user.latitude)) * " +
           "cos(radians(d.user.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
           "sin(radians(d.user.latitude)))) <= :radiusKm " +
           "ORDER BY d.priorityScore DESC")
    List<Donor> findEligibleDonorsNearby(@Param("bloodGroup") User.BloodGroup bloodGroup,
                                          @Param("latitude") Double latitude,
                                          @Param("longitude") Double longitude,
                                          @Param("radiusKm") Double radiusKm);
    
    @Query("SELECT d FROM Donor d WHERE d.nextEligibleDate <= :date")
    List<Donor> findDonorsEligibleBy(@Param("date") LocalDate date);
    
    List<Donor> findTop10ByOrderByTotalDonationsDesc();
    
    List<Donor> findTop10ByOrderByImpactScoreDesc();
    
    @Query("SELECT COUNT(d) FROM Donor d WHERE d.isEligible = true")
    Long countEligibleDonors();
    
    @Query("SELECT COUNT(d) FROM Donor d WHERE d.user.bloodGroup = :bloodGroup AND d.isEligible = true")
    Long countEligibleDonorsByBloodGroup(@Param("bloodGroup") User.BloodGroup bloodGroup);
}
