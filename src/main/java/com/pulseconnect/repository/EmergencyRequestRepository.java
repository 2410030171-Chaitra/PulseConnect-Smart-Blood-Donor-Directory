package com.pulseconnect.repository;

import com.pulseconnect.model.EmergencyRequest;
import com.pulseconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmergencyRequestRepository extends JpaRepository<EmergencyRequest, Long> {
    
    List<EmergencyRequest> findByUser(User user);
    
    List<EmergencyRequest> findByStatus(EmergencyRequest.RequestStatus status);
    
    List<EmergencyRequest> findByStatusOrderByCreatedAtDesc(EmergencyRequest.RequestStatus status);
    
    List<EmergencyRequest> findByUrgencyLevel(EmergencyRequest.UrgencyLevel urgencyLevel);
    
    @Query("SELECT er FROM EmergencyRequest er WHERE er.status = 'ACTIVE' " +
           "ORDER BY CASE er.urgencyLevel " +
           "WHEN 'CRITICAL' THEN 1 " +
           "WHEN 'HIGH' THEN 2 " +
           "WHEN 'MEDIUM' THEN 3 " +
           "WHEN 'LOW' THEN 4 END, er.createdAt ASC")
    List<EmergencyRequest> findActiveRequestsSortedByUrgency();
    
    @Query("SELECT er FROM EmergencyRequest er WHERE er.requiredBloodGroup = :bloodGroup " +
           "AND er.status = 'ACTIVE'")
    List<EmergencyRequest> findActiveRequestsByBloodGroup(@Param("bloodGroup") User.BloodGroup bloodGroup);
    
    @Query("SELECT er FROM EmergencyRequest er WHERE er.hospitalCity = :city " +
           "AND er.status = 'ACTIVE'")
    List<EmergencyRequest> findActiveRequestsByCity(@Param("city") String city);
    
    @Query("SELECT er FROM EmergencyRequest er WHERE " +
           "(6371 * acos(cos(radians(:latitude)) * cos(radians(er.hospitalLatitude)) * " +
           "cos(radians(er.hospitalLongitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
           "sin(radians(er.hospitalLatitude)))) <= :radiusKm " +
           "AND er.status = 'ACTIVE'")
    List<EmergencyRequest> findActiveRequestsNearby(@Param("latitude") Double latitude,
                                                     @Param("longitude") Double longitude,
                                                     @Param("radiusKm") Double radiusKm);
    
    List<EmergencyRequest> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT COUNT(er) FROM EmergencyRequest er WHERE er.status = 'ACTIVE'")
    Long countActiveRequests();
    
    @Query("SELECT er.requiredBloodGroup, COUNT(er) FROM EmergencyRequest er " +
           "WHERE er.status = 'ACTIVE' GROUP BY er.requiredBloodGroup")
    List<Object[]> getActiveRequestCountByBloodGroup();
}
