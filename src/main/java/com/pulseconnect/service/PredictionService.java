package com.pulseconnect.service;

import com.pulseconnect.model.BloodDemandPrediction;
import com.pulseconnect.model.DonationHistory;
import com.pulseconnect.model.EmergencyRequest;
import com.pulseconnect.model.User;
import com.pulseconnect.repository.BloodDemandPredictionRepository;
import com.pulseconnect.repository.DonationHistoryRepository;
import com.pulseconnect.repository.EmergencyRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

/**
 * AI-based prediction service for blood demand forecasting
 */
@Service
@RequiredArgsConstructor
public class PredictionService {

    private final DonationHistoryRepository donationHistoryRepository;
    private final EmergencyRequestRepository emergencyRequestRepository;
    private final BloodDemandPredictionRepository predictionRepository;

    /**
     * Predict blood demand for the next 30 days
     */
    @Transactional
    public List<BloodDemandPrediction> predictBloodDemand(String region, User.BloodGroup bloodGroup) {
        List<BloodDemandPrediction> predictions = new ArrayList<>();

        // Get historical data
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        List<DonationHistory> historicalDonations = donationHistoryRepository
            .findDonationsInCitySince(region, threeMonthsAgo);
        
        List<EmergencyRequest> historicalRequests = emergencyRequestRepository
            .findByCreatedAtBetween(threeMonthsAgo, LocalDateTime.now());

        // Predict for next 30 days
        for (int i = 1; i <= 30; i++) {
            LocalDate predictionDate = LocalDate.now().plusDays(i);
            BloodDemandPrediction prediction = createPrediction(
                region, bloodGroup, predictionDate, historicalDonations, historicalRequests
            );
            predictions.add(prediction);
            predictionRepository.save(prediction);
        }

        return predictions;
    }

    /**
     * Create a prediction for a specific date
     */
    private BloodDemandPrediction createPrediction(String region,
                                                    User.BloodGroup bloodGroup,
                                                    LocalDate predictionDate,
                                                    List<DonationHistory> historicalDonations,
                                                    List<EmergencyRequest> historicalRequests) {
        BloodDemandPrediction prediction = new BloodDemandPrediction();
        prediction.setBloodGroup(bloodGroup);
        prediction.setPredictionDate(predictionDate);
        prediction.setRegion(region);

        // Base demand calculation
        int baseDemand = calculateBaseDemand(historicalRequests, bloodGroup);

        // Apply seasonal factors
        double seasonalMultiplier = getSeasonalMultiplier(predictionDate);
        prediction.setSeasonalFactor(getSeasonalFactor(predictionDate));

        // Check for holidays
        boolean isHoliday = isHolidayPeriod(predictionDate);
        prediction.setIsHolidaySeason(isHoliday);

        // Check for accident-prone periods (weekends, long weekends)
        boolean isAccidentProne = isAccidentPronePeriod(predictionDate);
        prediction.setIsAccidentPronePeriod(isAccidentProne);

        // Calculate predicted demand
        double holidayMultiplier = isHoliday ? 1.3 : 1.0;
        double accidentMultiplier = isAccidentProne ? 1.2 : 1.0;

        int predictedUnits = (int) Math.ceil(
            baseDemand * seasonalMultiplier * holidayMultiplier * accidentMultiplier
        );

        prediction.setPredictedUnitsRequired(predictedUnits);

        // Calculate confidence score
        double confidence = calculateConfidenceScore(historicalDonations.size(), historicalRequests.size());
        prediction.setConfidenceScore(confidence);

        // Store factors as JSON string
        Map<String, Object> factors = new HashMap<>();
        factors.put("baseDemand", baseDemand);
        factors.put("seasonalMultiplier", seasonalMultiplier);
        factors.put("holidayMultiplier", holidayMultiplier);
        factors.put("accidentMultiplier", accidentMultiplier);
        prediction.setFactors(factors.toString());

        return prediction;
    }

    /**
     * Calculate base demand from historical data
     */
    private int calculateBaseDemand(List<EmergencyRequest> historicalRequests, User.BloodGroup bloodGroup) {
        int totalUnits = historicalRequests.stream()
            .filter(req -> req.getRequiredBloodGroup() == bloodGroup)
            .mapToInt(EmergencyRequest::getUnitsRequired)
            .sum();

        int days = 90; // 3 months
        return Math.max(2, totalUnits / days); // At least 2 units per day
    }

    /**
     * Get seasonal multiplier based on month
     */
    private double getSeasonalMultiplier(LocalDate date) {
        Month month = date.getMonth();
        
        // Summer months (April-June) - higher demand due to heat-related issues
        if (month == Month.APRIL || month == Month.MAY || month == Month.JUNE) {
            return 1.25;
        }
        // Monsoon months (July-September) - higher accidents
        else if (month == Month.JULY || month == Month.AUGUST || month == Month.SEPTEMBER) {
            return 1.3;
        }
        // Winter months (December-February) - moderate demand
        else if (month == Month.DECEMBER || month == Month.JANUARY || month == Month.FEBRUARY) {
            return 1.1;
        }
        // Normal months
        return 1.0;
    }

    /**
     * Get seasonal factor enum
     */
    private BloodDemandPrediction.SeasonalFactor getSeasonalFactor(LocalDate date) {
        double multiplier = getSeasonalMultiplier(date);
        if (multiplier >= 1.25) return BloodDemandPrediction.SeasonalFactor.HIGH_DEMAND;
        if (multiplier >= 1.15) return BloodDemandPrediction.SeasonalFactor.MEDIUM_DEMAND;
        if (multiplier < 1.0) return BloodDemandPrediction.SeasonalFactor.LOW_DEMAND;
        return BloodDemandPrediction.SeasonalFactor.NORMAL;
    }

    /**
     * Check if date is in holiday period
     */
    private boolean isHolidayPeriod(LocalDate date) {
        // Major Indian holidays (simplified)
        List<LocalDate> holidays = Arrays.asList(
            LocalDate.of(date.getYear(), 1, 26),  // Republic Day
            LocalDate.of(date.getYear(), 8, 15),  // Independence Day
            LocalDate.of(date.getYear(), 10, 2),  // Gandhi Jayanti
            LocalDate.of(date.getYear(), 12, 25)  // Christmas
        );

        // Check if within 3 days of a holiday
        return holidays.stream().anyMatch(holiday -> 
            Math.abs(date.toEpochDay() - holiday.toEpochDay()) <= 3
        );
    }

    /**
     * Check if date is accident-prone period (weekends)
     */
    private boolean isAccidentPronePeriod(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * Calculate confidence score based on data availability
     */
    private double calculateConfidenceScore(int donationCount, int requestCount) {
        int totalDataPoints = donationCount + requestCount;
        
        if (totalDataPoints >= 100) return 0.95;
        if (totalDataPoints >= 50) return 0.85;
        if (totalDataPoints >= 25) return 0.75;
        if (totalDataPoints >= 10) return 0.65;
        return 0.5; // Minimum confidence
    }

    /**
     * Get high-demand predictions for alerts
     */
    @Transactional(readOnly = true)
    public List<BloodDemandPrediction> getHighDemandAlerts(String region) {
        LocalDate fromDate = LocalDate.now();
        return predictionRepository.findHighDemandPredictionsInRegion(region, fromDate)
            .stream()
            .filter(p -> p.getPredictedUnitsRequired() > 10)
            .limit(5)
            .toList();
    }
}
