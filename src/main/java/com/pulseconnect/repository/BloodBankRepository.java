package com.pulseconnect.repository;

import com.pulseconnect.model.BloodBank;
import com.pulseconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BloodBankRepository extends JpaRepository<BloodBank, Long> {
    
    List<BloodBank> findByCity(String city);
    
    List<BloodBank> findByState(String state);
    
    List<BloodBank> findByIsActiveTrue();
    
    List<BloodBank> findByCityAndIsActiveTrue(String city);
    
    @Query("SELECT bb FROM BloodBank bb WHERE " +
           "(6371 * acos(cos(radians(:latitude)) * cos(radians(bb.latitude)) * " +
           "cos(radians(bb.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
           "sin(radians(bb.latitude)))) <= :radiusKm " +
           "AND bb.isActive = true " +
           "ORDER BY (6371 * acos(cos(radians(:latitude)) * cos(radians(bb.latitude)) * " +
           "cos(radians(bb.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
           "sin(radians(bb.latitude))))")
    List<BloodBank> findNearbyBloodBanks(@Param("latitude") Double latitude,
                                          @Param("longitude") Double longitude,
                                          @Param("radiusKm") Double radiusKm);
    
    @Query("SELECT bb FROM BloodBank bb WHERE bb.isActive = true AND " +
           "CASE :bloodGroup " +
           "WHEN 'A_POSITIVE' THEN bb.aPositiveUnits " +
           "WHEN 'A_NEGATIVE' THEN bb.aNegativeUnits " +
           "WHEN 'B_POSITIVE' THEN bb.bPositiveUnits " +
           "WHEN 'B_NEGATIVE' THEN bb.bNegativeUnits " +
           "WHEN 'AB_POSITIVE' THEN bb.abPositiveUnits " +
           "WHEN 'AB_NEGATIVE' THEN bb.abNegativeUnits " +
           "WHEN 'O_POSITIVE' THEN bb.oPositiveUnits " +
           "WHEN 'O_NEGATIVE' THEN bb.oNegativeUnits " +
           "END > 0")
    List<BloodBank> findBloodBanksWithAvailableBlood(@Param("bloodGroup") User.BloodGroup bloodGroup);
}
