package com.pulseconnect.repository;

import com.pulseconnect.model.EmergencyAlert;
import com.pulseconnect.model.EmergencyRequest;
import com.pulseconnect.model.Donor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, Long> {
    
    List<EmergencyAlert> findByEmergencyRequest(EmergencyRequest emergencyRequest);
    
    List<EmergencyAlert> findByDonor(Donor donor);
    
    List<EmergencyAlert> findByDonorOrderBySentAtDesc(Donor donor);
    
    List<EmergencyAlert> findByStatus(EmergencyAlert.AlertStatus status);
    
    List<EmergencyAlert> findByResponse(EmergencyAlert.DonorResponse response);
    
    @Query("SELECT ea FROM EmergencyAlert ea WHERE ea.donor.id = :donorId " +
           "AND ea.status IN ('SENT', 'DELIVERED') ORDER BY ea.sentAt DESC")
    List<EmergencyAlert> findUnreadAlertsByDonorId(@Param("donorId") Long donorId);
    
    @Query("SELECT COUNT(ea) FROM EmergencyAlert ea WHERE ea.emergencyRequest = :request " +
           "AND ea.response = 'AVAILABLE'")
    Long countAvailableResponsesForRequest(@Param("request") EmergencyRequest request);
    
    @Query("SELECT COUNT(ea) FROM EmergencyAlert ea WHERE ea.donor = :donor " +
           "AND ea.status = 'SENT'")
    Long countPendingAlertsForDonor(@Param("donor") Donor donor);
}
