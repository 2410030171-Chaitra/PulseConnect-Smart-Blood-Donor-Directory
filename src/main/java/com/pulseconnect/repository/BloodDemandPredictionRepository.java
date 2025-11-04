package com.pulseconnect.repository;

import com.pulseconnect.model.BloodDemandPrediction;
import com.pulseconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BloodDemandPredictionRepository extends JpaRepository<BloodDemandPrediction, Long> {
    
    List<BloodDemandPrediction> findByBloodGroup(User.BloodGroup bloodGroup);
    
    List<BloodDemandPrediction> findByRegion(String region);
    
    List<BloodDemandPrediction> findByPredictionDate(LocalDate predictionDate);
    
    @Query("SELECT bdp FROM BloodDemandPrediction bdp WHERE bdp.bloodGroup = :bloodGroup " +
           "AND bdp.region = :region AND bdp.predictionDate >= :fromDate " +
           "ORDER BY bdp.predictionDate ASC")
    List<BloodDemandPrediction> findPredictionsForBloodGroupInRegion(
            @Param("bloodGroup") User.BloodGroup bloodGroup,
            @Param("region") String region,
            @Param("fromDate") LocalDate fromDate);
    
    @Query("SELECT bdp FROM BloodDemandPrediction bdp WHERE bdp.predictionDate >= :startDate " +
           "AND bdp.predictionDate <= :endDate ORDER BY bdp.predictionDate ASC")
    List<BloodDemandPrediction> findPredictionsBetween(@Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT bdp FROM BloodDemandPrediction bdp WHERE bdp.region = :region " +
           "AND bdp.predictionDate >= :fromDate ORDER BY bdp.predictedUnitsRequired DESC")
    List<BloodDemandPrediction> findHighDemandPredictionsInRegion(@Param("region") String region,
                                                                   @Param("fromDate") LocalDate fromDate);
}
