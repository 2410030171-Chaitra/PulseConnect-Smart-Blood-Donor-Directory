package com.pulseconnect.mongo.doc;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "donors")
public class DonorDoc {
    @Id
    private String id; // Mongo _id

    private Long donorId; // relational donor id
    private Long userId;  // relational user id

    private String fullName;
    private String phoneNumber;
    private String bloodGroup; // e.g., A_POSITIVE
    private String city;
    private Boolean eligible;
    private Boolean available;
    private Integer priorityScore;
    private Double latitude;
    private Double longitude;

    public DonorDoc() {}

    public DonorDoc(Long donorId, Long userId, String fullName, String phoneNumber, String bloodGroup,
                    String city, Boolean eligible, Boolean available, Integer priorityScore,
                    Double latitude, Double longitude) {
        this.donorId = donorId;
        this.userId = userId;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.bloodGroup = bloodGroup;
        this.city = city;
        this.eligible = eligible;
        this.available = available;
        this.priorityScore = priorityScore;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getDonorId() { return donorId; }
    public void setDonorId(Long donorId) { this.donorId = donorId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public Boolean getEligible() { return eligible; }
    public void setEligible(Boolean eligible) { this.eligible = eligible; }
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
    public Integer getPriorityScore() { return priorityScore; }
    public void setPriorityScore(Integer priorityScore) { this.priorityScore = priorityScore; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
