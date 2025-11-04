package com.pulseconnect.controller;

import com.pulseconnect.model.Donor;
import com.pulseconnect.repository.DonorRepository;
import com.pulseconnect.model.User;
import com.pulseconnect.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/donors")
public class DonorController {
    
    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private SmsService smsService;

    // Search donors from database
    @GetMapping("/search")
    public List<Map<String, Object>> searchDonors(@RequestParam(required = false) String bloodGroup,
                                    @RequestParam(required = false) String city,
                                    @RequestParam(required = false) String radius) {
        int radiusValue = 20;
        try {
            if (radius != null && !radius.isEmpty()) {
                radiusValue = Integer.parseInt(radius);
            }
        } catch (Exception e) {
            radiusValue = 20;
        }

        // Fetch all eligible donors from database
        List<Donor> allDonors = donorRepository.findAll();
        
        // Filter donors based on search criteria
        List<Map<String, Object>> filteredDonors = new ArrayList<>();
        Random rand = new Random();
        
        for (Donor donor : allDonors) {
            if (donor.getUser() == null) continue;
            
            int distance = rand.nextInt(30) + 1; // Simulate distance (1-30 km)
            
            String donorCity = donor.getUser().getCity();
        // Prepare blood group in both enum name and display formats to support either input
        String donorBloodGroupDisplay = donor.getUser().getBloodGroup() != null ?
            donor.getUser().getBloodGroup().getDisplayName() : "";
        String donorBloodGroupEnum = donor.getUser().getBloodGroup() != null ?
            donor.getUser().getBloodGroup().name() : "";
            
            boolean matchesCity = (city == null || city.isEmpty() || 
                                  (donorCity != null && donorCity.equalsIgnoreCase(city.trim())));
        boolean matchesBlood = (bloodGroup == null || bloodGroup.isEmpty() ||
            (donorBloodGroupDisplay != null && donorBloodGroupDisplay.equalsIgnoreCase(bloodGroup.trim())) ||
            (donorBloodGroupEnum != null && donorBloodGroupEnum.equalsIgnoreCase(bloodGroup.trim())));
            boolean withinRange = distance <= radiusValue;
            
            if (matchesCity && matchesBlood && withinRange && donor.getIsAvailable()) {
                Map<String, Object> donorInfo = new HashMap<>();
                donorInfo.put("id", donor.getId());
                donorInfo.put("name", donor.getUser().getFullName());
                donorInfo.put("bloodGroup", donorBloodGroupDisplay);
                donorInfo.put("city", donorCity);
                donorInfo.put("phone", donor.getUser().getPhoneNumber());
                donorInfo.put("distance", distance);
                donorInfo.put("totalDonations", donor.getTotalDonations());
                donorInfo.put("available", donor.getIsAvailable());
                
                filteredDonors.add(donorInfo);
            }
        }
        
        return filteredDonors;
    }

    // Submit emergency request
    @PostMapping("/emergency")
    public Map<String, Object> submitEmergency(@RequestBody Map<String, String> request) {
        String patient = request.getOrDefault("patientName", "Patient");
        String contact = request.getOrDefault("contactNumber", "");
        String bg = request.getOrDefault("requiredBloodGroup", "");
        String units = String.valueOf(request.getOrDefault("unitsRequired", "1"));
        String hospital = request.getOrDefault("hospitalLocation", "Hospital");
        String details = request.getOrDefault("additionalDetails", "");

        // Find eligible donors by blood group (limit to 25 to avoid SMS flood)
        List<Donor> donors = new ArrayList<>();
        try {
            if (bg != null && !bg.isBlank()) {
                User.BloodGroup group = User.BloodGroup.valueOf(bg.trim());
                donors = donorRepository.findEligibleDonorsByBloodGroup(group);
            }
        } catch (IllegalArgumentException ignored) { }

        List<String> phones = donors.stream()
                .filter(d -> d.getUser() != null && d.getUser().getPhoneNumber() != null)
                .limit(25)
                .map(d -> d.getUser().getPhoneNumber())
                .toList();

        String msg = String.format(
                "URGENT: %s needs %s unit(s) of %s at %s. Contact: %s. %s - PulseConnect",
                patient, units, bg.replace('_',' '), hospital, contact, (details == null ? "" : details));

        List<String> sent = smsService.sendBulk(phones, msg);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", sent.isEmpty() ? "Emergency request received (SMS disabled or not configured)" :
                ("Notified " + sent.size() + " donor(s) via SMS"));
        response.put("notified", sent.size());
        return response;
    }
}
