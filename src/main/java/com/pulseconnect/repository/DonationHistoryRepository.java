package com.pulseconnect.repository;

import com.pulseconnect.model.DonationHistory;
import com.pulseconnect.model.Donor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DonationHistoryRepository extends JpaRepository<DonationHistory, Long> {
    
    List<DonationHistory> findByDonor(Donor donor);
    
    List<DonationHistory> findByDonorOrderByDonationDateDesc(Donor donor);
    
    List<DonationHistory> findByStatus(DonationHistory.DonationStatus status);
    
    List<DonationHistory> findByDonationDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT dh FROM DonationHistory dh WHERE dh.donor.id = :donorId " +
           "ORDER BY dh.donationDate DESC")
    List<DonationHistory> findRecentDonationsByDonorId(@Param("donorId") Long donorId);
    
    @Query("SELECT COUNT(dh) FROM DonationHistory dh WHERE dh.donor = :donor " +
           "AND dh.status = 'COMPLETED'")
    Integer countCompletedDonationsByDonor(@Param("donor") Donor donor);
    
    @Query("SELECT dh FROM DonationHistory dh WHERE dh.donor.user.city = :city " +
           "AND dh.donationDate >= :fromDate")
    List<DonationHistory> findDonationsInCitySince(@Param("city") String city, 
                                                    @Param("fromDate") LocalDateTime fromDate);
    
    @Query("SELECT SUM(dh.unitsCollected) FROM DonationHistory dh " +
           "WHERE dh.status = 'COMPLETED' AND dh.donationDate >= :fromDate")
    Integer getTotalUnitsCollectedSince(@Param("fromDate") LocalDateTime fromDate);
    
    @Query("SELECT dh.donor.user.bloodGroup, COUNT(dh) FROM DonationHistory dh " +
           "WHERE dh.status = 'COMPLETED' GROUP BY dh.donor.user.bloodGroup")
    List<Object[]> getDonationCountByBloodGroup();
}
