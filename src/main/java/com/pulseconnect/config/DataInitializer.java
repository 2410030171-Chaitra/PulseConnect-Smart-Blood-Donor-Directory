package com.pulseconnect.config;

import com.pulseconnect.model.Donor;
import com.pulseconnect.model.User;
import com.pulseconnect.repository.DonorRepository;
import com.pulseconnect.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, DonorRepository donorRepository) {
        return args -> {
            // Check if data already exists
            if (userRepository.count() > 0) {
                System.out.println("Database already initialized with " + userRepository.count() + " users.");
                return;
            }

            System.out.println("Initializing database with Telangana donor data...");

            // Create 5 donors for each blood group in each city/district
            String[] cities = {
                // Hyderabad Region
                "Hyderabad", "Secunderabad", "Kukatpally", "Madhapur", "Gachibowli", 
                "LB Nagar", "Shamshabad",
                // Major Districts
                "Warangal", "Hanamkonda", "Kazipet", "Nizamabad", "Armoor", "Karimnagar", 
                "Jagtial", "Khammam", "Kothagudem", "Nalgonda", "Miryalaguda",
                // Other Districts
                "Medak", "Sangareddy", "Mahbubnagar", "Gadwal", "Adilabad", "Mancherial",
                "Patancheru", "Zaheerabad", "Kamareddy", "Banswada", "Siddipet", 
                "Dubbak", "Vikarabad", "Tandur"
            };
            String[] bloodGroups = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
            
            String[][] firstNames = {
                {"Rajesh", "Priya", "Amit", "Sneha", "Vikram"},
                {"Suresh", "Kavita", "Ramesh", "Anjali", "Karan"},
                {"Srinivas", "Sowmya", "Krishna", "Madhavi", "Venkat"},
                {"Mahesh", "Rani", "Naresh", "Jyothi", "Ravi"},
                {"Harish", "Shalini", "Prakash", "Divya", "Santosh"}
            };
            
            String[][] lastNames = {
                {"Reddy", "Kumar", "Rao", "Naidu", "Goud"},
                {"Kumari", "Devi", "Latha", "Prasad", "Begum"},
                {"Singh", "Nair", "Varma", "Patel", "Sharma"},
                {"Babu", "Rani", "Teja", "Sekhar", "Murthy"},
                {"Naik", "Raju", "Chary", "Yadav", "Desai"}
            };
            
            int phoneCounter = 10000;
            int donorCount = 0;
            
            for (String city : cities) {
                for (int bgIdx = 0; bgIdx < bloodGroups.length; bgIdx++) {
                    String bloodGroup = bloodGroups[bgIdx];
                    // Create 5 donors for each blood group in each city
                    for (int i = 0; i < 5; i++) {
                        // Use blood group index and i to pick unique names for each group
                        String firstName = firstNames[(bgIdx + i) % firstNames.length][(bgIdx + i) % 5];
                        String lastName = lastNames[(bgIdx + i) % lastNames.length][(bgIdx * 2 + i) % 5];
                        String name = firstName + " " + lastName;
                        String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + bgIdx + i + phoneCounter + "@example.com";
                        String phone = "98765" + String.format("%05d", phoneCounter);

                        // Vary the physical attributes
                        Double weight = 55.0 + (Math.random() * 30) + bgIdx; // 55-85 kg, offset by blood group
                        Double height = 155.0 + (Math.random() * 30) + i; // 155-185 cm, offset by donor index
                        Integer totalDonations = 1 + (int)(Math.random() * 10) + bgIdx % 3; // 1-12 donations

                        createDonor(userRepository, donorRepository, name, email, phone,
                                  bloodGroup, city, "Telangana", weight, height, totalDonations);

                        phoneCounter++;
                        donorCount++;
                    }
                }
            }

            System.out.println("Database initialized with " + userRepository.count() + " Telangana donors across " + cities.length + " cities!");
            System.out.println("Each city has " + (bloodGroups.length * 5) + " donors (5 donors per blood group)");
        };
    }

    private void createDonor(UserRepository userRepository, DonorRepository donorRepository,
                           String name, String email, String phone, String bloodGroup,
                           String city, String state, Double weight, Double height, Integer totalDonations) {
        // Create User
        User user = new User();
        user.setUsername(email.split("@")[0]); // Use email prefix as username
        user.setFullName(name);
        user.setEmail(email);
        user.setPassword("$2a$10$dummypassword"); // Dummy password
        user.setPhoneNumber(phone);
        user.setBloodGroup(convertBloodGroup(bloodGroup));
        user.setAddress(city + ", " + state);
        user.setCity(city);
        user.setState(state);
        user.setPincode("000000");
        user.setRole(User.UserRole.DONOR);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setLatitude(getRandomLatitude());
        user.setLongitude(getRandomLongitude());
        
        user = userRepository.save(user);

        // Create Donor
        Donor donor = new Donor();
        donor.setUser(user);
        donor.setDateOfBirth(LocalDate.of(1990 + (int)(Math.random() * 10), (int)(Math.random() * 12) + 1, (int)(Math.random() * 28) + 1));
        donor.setGender(Math.random() > 0.5 ? Donor.Gender.MALE : Donor.Gender.FEMALE);
        donor.setWeight(weight);
        donor.setHeight(height);
        donor.setIsEligible(true);
        donor.setTotalDonations(totalDonations);
        donor.setIsAvailable(true);
        donor.setWillingToTravelFar(Math.random() > 0.5);
        donor.setPriorityScore(70 + (int)(Math.random() * 30)); // 70-100
        
        donorRepository.save(donor);
    }

    private Double getRandomLatitude() {
        return 15.8 + (Math.random() * 3.5); // Telangana latitude range: 15.8°N to 19.3°N
    }

    private Double getRandomLongitude() {
        return 77.2 + (Math.random() * 4.0); // Telangana longitude range: 77.2°E to 81.2°E
    }
    
    private User.BloodGroup convertBloodGroup(String bg) {
        switch (bg) {
            case "A+": return User.BloodGroup.A_POSITIVE;
            case "A-": return User.BloodGroup.A_NEGATIVE;
            case "B+": return User.BloodGroup.B_POSITIVE;
            case "B-": return User.BloodGroup.B_NEGATIVE;
            case "AB+": return User.BloodGroup.AB_POSITIVE;
            case "AB-": return User.BloodGroup.AB_NEGATIVE;
            case "O+": return User.BloodGroup.O_POSITIVE;
            case "O-": return User.BloodGroup.O_NEGATIVE;
            default: return User.BloodGroup.O_POSITIVE;
        }
    }
}
